import hashlib
import json
import logging
import uuid
from datetime import datetime, timezone
from typing import Any, AsyncIterator

from fastapi import APIRouter, Depends, HTTPException, Query, Request
from fastapi.responses import Response, StreamingResponse
from motor.motor_asyncio import (
    AsyncIOMotorCollection,
    AsyncIOMotorGridFSBucket,
)

from app.core.config import settings
from app.core.database import get_gridfs_bucket, get_writing_lessons
from app.core.rate_limit import check_rate_limit
from app.models.writing import (
    EssaySubmission,
    Task1EssaySubmission,
    TaskType,
    WritingEvaluation,
    WritingEvaluationDB,
    WritingLessonListResponse,
    WritingLessonResponse,
)
from app.services.llm_service import (
    ACTIVE_PROMPT_VERSION,
    ACTIVE_TASK1_PROMPT_VERSION,
    evaluate_essay_with_ai,
    evaluate_task1_essay_with_ai,
    evaluate_task1_essay_with_ai_stream,
    stream_essay_evaluation,
)
from app.services.writing_lesson_service import (
    LessonImageNotFound,
    get_published_lesson,
    list_published_lessons,
    open_lesson_image,
)

router = APIRouter(prefix="/writing", tags=["writing"])
logger = logging.getLogger(__name__)

WRITING_COLLECTION_NAME = "writing_evaluations"
CACHE_COLLECTION_NAME = "response_cache"

def _sse_format(event: str | None, data: str) -> str:
    """Format a Server-Sent Events message.

    A single event spans multiple lines in `data` — each new line is prefixed
    with `data: ` by the SSE spec.
    """
    lines = data.split("\n")
    body = "\n".join(f"data: {line}" for line in lines)
    if event:
        return f"event: {event}\n{body}\n\n"
    return f"{body}\n\n"

def _fingerprint(
    prompt_version: str, task_prompt: str, essay_text: str, *, task_type: str = "task2"
) -> str:
    """Stable SHA-256 of the inputs that determine an evaluation result.

    Including the prompt version means upgrading the system prompt
    (e.g. v2 -> v3) automatically invalidates the cache, no manual flush.
    Including the task type means a Task 1 essay and a Task 2 essay with
    identical text never collide in the cache.
    """
    h = hashlib.sha256()
    h.update(task_type.encode("utf-8"))
    h.update(b"\x00")
    h.update(prompt_version.encode("utf-8"))
    h.update(b"\x00")
    h.update(task_prompt.strip().encode("utf-8"))
    h.update(b"\x00")
    h.update(essay_text.strip().encode("utf-8"))
    return h.hexdigest()

async def _load_lesson_and_image(
    lessons: AsyncIOMotorCollection,
    bucket: AsyncIOMotorGridFSBucket,
    lesson_id: str,
) -> tuple[Any, bytes]:
    """Fetch a published lesson and read its GridFS image fully into memory.

    Returns ``(lesson, image_bytes)``; raises ``HTTPException(404)`` if the
    lesson is missing, a draft, or has no image attached. We read the whole
    image into memory because the upstream LLM call needs a bytes blob for
    the data-URI payload; admin-curated chart images are well under the
    8 MB upload cap.
    """
    lesson = await get_published_lesson(lessons, lesson_id)
    if lesson is None:
        raise HTTPException(status_code=404, detail="lesson_not_found")
    if not lesson.image_id:
        raise HTTPException(status_code=400, detail="lesson_has_no_image")
    try:
        chunks, _content_type = await open_lesson_image(bucket, lesson.image_id)
    except LessonImageNotFound:
        raise HTTPException(status_code=404, detail="lesson_image_not_found")
    image_bytes = b"".join([chunk async for chunk in chunks])
    if not image_bytes:
        # GridFS returned no bytes — treat as missing rather than passing
        # empty bytes downstream (the service layer rejects those anyway).
        raise HTTPException(status_code=404, detail="lesson_image_not_found")
    return lesson, image_bytes

def _compute_cost_usd(input_tokens: int | None, output_tokens: int | None) -> float | None:
    """Convert token counts to estimated USD using configured per-million pricing.

    Returns None if either token count is missing — cost without both inputs
    is meaningless, and we don't want to write 0.0 cost for a provider that
    didn't report usage.
    """
    if input_tokens is None or output_tokens is None:
        return None
    in_cost = input_tokens * settings.input_token_cost_per_million / 1_000_000
    out_cost = output_tokens * settings.output_token_cost_per_million / 1_000_000
    return round(in_cost + out_cost, 8)

async def _cache_get(db: Any, fp: str) -> dict | None:
    """Return the cached evaluation dict (without the persisted token fields) or None."""
    return await db[CACHE_COLLECTION_NAME].find_one({"fingerprint": fp})

async def _cache_put(db: Any, fp: str, evaluation: WritingEvaluation) -> None:
    """Persist a fresh evaluation to the response cache. Best-effort."""
    try:
        await db[CACHE_COLLECTION_NAME].insert_one(
            {
                "fingerprint": fp,
                "prompt_version": ACTIVE_PROMPT_VERSION,
                "evaluation": evaluation.model_dump(),
                "created_at": datetime.now(timezone.utc),
            }
        )
    except Exception as e:  # noqa: BLE001 — caching is best-effort
        logger.warning("Failed to write to response cache: %s", e)

@router.post("/evaluate", response_model=WritingEvaluationDB, dependencies=[Depends(check_rate_limit)])
async def evaluate_essay(submission: EssaySubmission, request: Request) -> WritingEvaluationDB:
    """Evaluate a user's essay with the LLM and persist the result (non-streaming).

    Kept for backward compatibility / debugging. The mobile app uses
    `POST /writing/evaluate/stream` to render feedback progressively.

    Workflow:
      1. SHA-256 fingerprint the request and check the response cache. If
         a non-expired entry exists, return a re-decorated ``WritingEvaluationDB``
         immediately (a fresh id/timestamp; no LLM call, no cost).
      2. Call the LLM service to get an ``EvaluationResult`` (evaluation +
         token counts).
      3. Decorate with a UUID, UTC timestamp, and computed cost.
      4. Save to ``writing_evaluations`` and write to ``response_cache``.
    """
    db = request.app.state.mongo_db
    fp = _fingerprint(ACTIVE_PROMPT_VERSION, submission.task_prompt, submission.essay_text)
    cached = await _cache_get(db, fp)
    if cached is not None:
        logger.info("Cache hit for writing evaluation fingerprint=%s", fp[:12])
        return WritingEvaluationDB(
            id=str(uuid.uuid4()),
            task_prompt=submission.task_prompt,
            essay_text=submission.essay_text,
            created_at=datetime.now(timezone.utc),
            **cached["evaluation"],
        )

    try:
        result = await evaluate_essay_with_ai(
            task_prompt=submission.task_prompt,
            essay_text=submission.essay_text,
        )
    except RuntimeError as e:
        logger.exception("LLM evaluation failed")
        raise HTTPException(status_code=502, detail=f"LLM evaluation failed: {e}") from e
    except Exception as e:  # noqa: BLE001
        logger.exception("Unexpected error while calling the LLM")
        raise HTTPException(status_code=502, detail=f"LLM evaluation failed: {e}") from e

    record_id = str(uuid.uuid4())
    created_at = datetime.now(timezone.utc)
    cost = _compute_cost_usd(result.input_tokens, result.output_tokens)

    record = WritingEvaluationDB(
        id=record_id,
        task_prompt=submission.task_prompt,
        essay_text=submission.essay_text,
        created_at=created_at,
        input_tokens=result.input_tokens,
        output_tokens=result.output_tokens,
        estimated_cost_usd=cost,
        **result.evaluation.model_dump(),
    )

    try:
        collection = db[WRITING_COLLECTION_NAME]
        await collection.insert_one(record.model_dump(mode="json"))
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to save writing evaluation %s", record_id)
        raise HTTPException(status_code=500, detail=f"Database write failed: {e}") from e

    await _cache_put(db, fp, result.evaluation)
    logger.info(
        "Saved writing evaluation %s (band=%.1f, tokens=%s+%s, cost=$%s)",
        record_id, record.overall_band, result.input_tokens, result.output_tokens, cost,
    )
    return record

@router.post("/evaluate/stream", dependencies=[Depends(check_rate_limit)])
async def evaluate_essay_stream(
    submission: EssaySubmission, request: Request
) -> StreamingResponse:
    """Stream the LLM evaluation as Server-Sent Events.

    Event format:
      - default `data: <chunk>` events contain the raw LLM delta text (the
        model's streaming JSON, not the validated one).
      - one ``event: usage`` event with ``data: {"input_tokens", "output_tokens"}``
        is emitted right before ``event: done`` so the router can persist cost.
        The Android client can ignore unknown event types.
      - one `event: done` event with `data: <json>` carries the final
        `WritingEvaluation` (the same shape returned by `/evaluate`).
      - on error, one `event: error` event with `data: <message>` is emitted
        before the stream closes.

    If the request matches a cached fingerprint, the cached evaluation is
    returned as a single ``done`` event with zero streamed chunks — the
    client still sees a valid response.

    Persistence happens in the background after the final `done` event so the
    user never waits on the Mongo write. If persistence fails we log it but
    still return the evaluation to the user.
    """
    db = request.app.state.mongo_db
    fp = _fingerprint(ACTIVE_PROMPT_VERSION, submission.task_prompt, submission.essay_text)
    cached = await _cache_get(db, fp)

    record_id = str(uuid.uuid4())
    created_at = datetime.now(timezone.utc)
    # snapshot the values so the background task can't be mutated by a retried request
    snapshot_prompt = submission.task_prompt
    snapshot_essay = submission.essay_text

    async def event_source() -> AsyncIterator[str]:
        yield _sse_format(None, "[connected]")

        if cached is not None:
            logger.info("Cache hit (stream) for fingerprint=%s", fp[:12])
            # Re-decorate with a fresh id/timestamp; emit as a single done event.
            cached_eval = WritingEvaluation.model_validate(cached["evaluation"])
            yield f"event: usage\ndata: {json.dumps({'input_tokens': None, 'output_tokens': None})}\n\n"
            yield f"event: done\ndata: {cached_eval.model_dump_json()}\n\n"
            return

        final_evaluation: WritingEvaluation | None = None
        final_input_tokens: int | None = None
        final_output_tokens: int | None = None
        final_error: str | None = None

        try:
            async for raw_event in stream_essay_evaluation(
                task_prompt=snapshot_prompt, essay_text=snapshot_essay
            ):
                # Forward the LLM service's SSE-formatted lines straight through.
                yield raw_event

                if raw_event.startswith("event: done"):
                    # The validated JSON is on the data: line of this event.
                    data_line = next(
                        (ln for ln in raw_event.splitlines() if ln.startswith("data: ")),
                        None,
                    )
                    if data_line:
                        try:
                            final_evaluation = WritingEvaluation.model_validate_json(
                                data_line[len("data: "):]
                            )
                        except Exception as e:  # noqa: BLE001
                            final_error = f"Failed to parse final evaluation: {e}"
                elif raw_event.startswith("event: usage"):
                    data_line = next(
                        (ln for ln in raw_event.splitlines() if ln.startswith("data: ")),
                        None,
                    )
                    if data_line:
                        try:
                            usage = json.loads(data_line[len("data: "):])
                            final_input_tokens = usage.get("input_tokens")
                            final_output_tokens = usage.get("output_tokens")
                        except Exception:  # noqa: BLE001 — usage is best-effort
                            pass
                elif raw_event.startswith("event: error"):
                    first_data_line = next(
                        (ln for ln in raw_event.splitlines() if ln.startswith("data: ")),
                        None,
                    )
                    if first_data_line:
                        final_error = first_data_line[len("data: "):]
        except Exception as e:  # noqa: BLE001 - last-ditch safety net
            logger.exception("Streaming evaluation failed")
            final_error = str(e)
            yield _sse_format("error", str(e))

        if final_evaluation is None:
            return

        # Persist out-of-band; don't make the user wait on Mongo.
        try:
            cost = _compute_cost_usd(final_input_tokens, final_output_tokens)
            record = WritingEvaluationDB(
                id=record_id,
                task_prompt=snapshot_prompt,
                essay_text=snapshot_essay,
                created_at=created_at,
                input_tokens=final_input_tokens,
                output_tokens=final_output_tokens,
                estimated_cost_usd=cost,
                **final_evaluation.model_dump(),
            )
            await db[WRITING_COLLECTION_NAME].insert_one(record.model_dump(mode="json"))
            await _cache_put(db, fp, final_evaluation)
            logger.info(
                "Saved streamed writing evaluation %s (band=%.1f, tokens=%s+%s, cost=$%s)",
                record_id, final_evaluation.overall_band, final_input_tokens, final_output_tokens, cost,
            )
        except Exception as e:  # noqa: BLE001
            logger.exception("Failed to save streamed writing evaluation %s", record_id)
            # Inform the client but don't fail the request.
            yield _sse_format("warn", f"persistence_failed: {e}")

    # `text/event-stream` is the canonical SSE content type; X-Accel-Buffering=no
    # disables proxy buffering so chunks reach the client as they arrive.
    headers = {"X-Accel-Buffering": "no", "Cache-Control": "no-cache"}
    return StreamingResponse(
        event_source(),
        media_type="text/event-stream",
        headers=headers,
    )


# --- Priority 3.6: Task 1 (Academic) evaluation endpoints ---
#
# Task 1 grades an essay against a chart image stored alongside the lesson.
# The client only sends ``lesson_id`` + ``essay_text``; the task prompt and
# the chart come from the server-side lesson document, so a user can't
# submit a different chart than what the lesson is meant to test against.

@router.post(
    "/evaluate/task1",
    response_model=WritingEvaluationDB,
    dependencies=[Depends(check_rate_limit)],
)
async def evaluate_task1_essay(
    submission: Task1EssaySubmission,
    request: Request,
    lessons: AsyncIOMotorCollection = Depends(get_writing_lessons),
    bucket: AsyncIOMotorGridFSBucket = Depends(get_gridfs_bucket),
) -> WritingEvaluationDB:
    """Grade a Task 1 essay against a published lesson's chart image.

    Workflow mirrors ``evaluate_essay`` (Task 2):
      1. Look up the lesson + load its GridFS chart image. 404 if the lesson
         is missing, a draft, or has no image.
      2. SHA-256 fingerprint and check the response cache (Task 1 namespace).
         Cache hit returns a re-decorated ``WritingEvaluationDB`` immediately.
      3. Call the vision LLM service; persist to ``writing_evaluations``
         with ``task_type="task1"``; write to ``response_cache``.
    """
    db = request.app.state.mongo_db
    lesson, image_bytes = await _load_lesson_and_image(lessons, bucket, submission.lesson_id)
    task_prompt = lesson.task_prompt

    fp = _fingerprint(
        ACTIVE_TASK1_PROMPT_VERSION, task_prompt, submission.essay_text, task_type="task1"
    )
    cached = await _cache_get(db, fp)
    if cached is not None:
        logger.info("Cache hit for task1 evaluation fingerprint=%s", fp[:12])
        return WritingEvaluationDB(
            id=str(uuid.uuid4()),
            task_prompt=task_prompt,
            essay_text=submission.essay_text,
            created_at=datetime.now(timezone.utc),
            task_type="task1",
            **cached["evaluation"],
        )

    try:
        result = await evaluate_task1_essay_with_ai(
            task_prompt=task_prompt,
            essay_text=submission.essay_text,
            image_bytes=image_bytes,
        )
    except ValueError as e:
        # Router-side guard: empty image bytes. The service layer also rejects
        # these, but raising here gives the client a clean 400 instead of 502.
        raise HTTPException(status_code=400, detail=str(e)) from e
    except RuntimeError as e:
        logger.exception("LLM task1 evaluation failed")
        raise HTTPException(status_code=502, detail=f"LLM evaluation failed: {e}") from e
    except Exception as e:  # noqa: BLE001
        logger.exception("Unexpected error while calling the LLM (task1)")
        raise HTTPException(status_code=502, detail=f"LLM evaluation failed: {e}") from e

    record_id = str(uuid.uuid4())
    created_at = datetime.now(timezone.utc)
    cost = _compute_cost_usd(result.input_tokens, result.output_tokens)

    record = WritingEvaluationDB(
        id=record_id,
        task_prompt=task_prompt,
        essay_text=submission.essay_text,
        created_at=created_at,
        task_type="task1",
        input_tokens=result.input_tokens,
        output_tokens=result.output_tokens,
        estimated_cost_usd=cost,
        **result.evaluation.model_dump(),
    )

    try:
        await db[WRITING_COLLECTION_NAME].insert_one(record.model_dump(mode="json"))
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to save task1 writing evaluation %s", record_id)
        raise HTTPException(status_code=500, detail=f"Database write failed: {e}") from e

    await _cache_put(db, fp, result.evaluation)
    logger.info(
        "Saved task1 writing evaluation %s (band=%.1f, tokens=%s+%s, cost=$%s)",
        record_id, record.overall_band, result.input_tokens, result.output_tokens, cost,
    )
    return record

@router.post(
    "/evaluate/task1/stream",
    dependencies=[Depends(check_rate_limit)],
)
async def evaluate_task1_essay_stream(
    submission: Task1EssaySubmission,
    request: Request,
    lessons: AsyncIOMotorCollection = Depends(get_writing_lessons),
    bucket: AsyncIOMotorGridFSBucket = Depends(get_gridfs_bucket),
) -> StreamingResponse:
    """Stream a Task 1 evaluation as Server-Sent Events.

    Same event protocol as the Task 2 streaming endpoint: raw ``data:`` deltas,
    one ``event: usage`` line, and one ``event: done`` carrying the final
    ``WritingEvaluation``. The chart image is loaded once before the stream
    starts; if the lesson is missing, a draft, or has no image, the endpoint
    returns HTTP 404 before any SSE bytes are written.
    """
    db = request.app.state.mongo_db
    lesson, image_bytes = await _load_lesson_and_image(lessons, bucket, submission.lesson_id)
    task_prompt = lesson.task_prompt

    fp = _fingerprint(
        ACTIVE_TASK1_PROMPT_VERSION, task_prompt, submission.essay_text, task_type="task1"
    )
    cached = await _cache_get(db, fp)

    record_id = str(uuid.uuid4())
    created_at = datetime.now(timezone.utc)
    snapshot_prompt = task_prompt
    snapshot_essay = submission.essay_text

    async def event_source() -> AsyncIterator[str]:
        yield _sse_format(None, "[connected]")

        if cached is not None:
            logger.info("Cache hit (task1 stream) for fingerprint=%s", fp[:12])
            cached_eval = WritingEvaluation.model_validate(cached["evaluation"])
            yield f"event: usage\ndata: {json.dumps({'input_tokens': None, 'output_tokens': None})}\n\n"
            yield f"event: done\ndata: {cached_eval.model_dump_json()}\n\n"
            return

        final_evaluation: WritingEvaluation | None = None
        final_input_tokens: int | None = None
        final_output_tokens: int | None = None

        try:
            async for raw_event in evaluate_task1_essay_with_ai_stream(
                task_prompt=snapshot_prompt,
                essay_text=snapshot_essay,
                image_bytes=image_bytes,
            ):
                yield raw_event

                if raw_event.startswith("event: done"):
                    data_line = next(
                        (ln for ln in raw_event.splitlines() if ln.startswith("data: ")),
                        None,
                    )
                    if data_line:
                        try:
                            final_evaluation = WritingEvaluation.model_validate_json(
                                data_line[len("data: "):]
                            )
                        except Exception as e:  # noqa: BLE001
                            logger.warning("Failed to parse final task1 evaluation: %s", e)
                elif raw_event.startswith("event: usage"):
                    data_line = next(
                        (ln for ln in raw_event.splitlines() if ln.startswith("data: ")),
                        None,
                    )
                    if data_line:
                        try:
                            usage = json.loads(data_line[len("data: "):])
                            final_input_tokens = usage.get("input_tokens")
                            final_output_tokens = usage.get("output_tokens")
                        except Exception:  # noqa: BLE001 — usage is best-effort
                            pass
        except Exception as e:  # noqa: BLE001 — last-ditch safety net
            logger.exception("Streaming task1 evaluation failed")
            yield _sse_format("error", str(e))

        if final_evaluation is None:
            return

        try:
            cost = _compute_cost_usd(final_input_tokens, final_output_tokens)
            record = WritingEvaluationDB(
                id=record_id,
                task_prompt=snapshot_prompt,
                essay_text=snapshot_essay,
                created_at=created_at,
                task_type="task1",
                input_tokens=final_input_tokens,
                output_tokens=final_output_tokens,
                estimated_cost_usd=cost,
                **final_evaluation.model_dump(),
            )
            await db[WRITING_COLLECTION_NAME].insert_one(record.model_dump(mode="json"))
            await _cache_put(db, fp, final_evaluation)
            logger.info(
                "Saved streamed task1 writing evaluation %s (band=%.1f, tokens=%s+%s, cost=$%s)",
                record_id, final_evaluation.overall_band, final_input_tokens, final_output_tokens, cost,
            )
        except Exception as e:  # noqa: BLE001
            logger.exception("Failed to save streamed task1 writing evaluation %s", record_id)
            yield _sse_format("warn", f"persistence_failed: {e}")

    headers = {"X-Accel-Buffering": "no", "Cache-Control": "no-cache"}
    return StreamingResponse(
        event_source(),
        media_type="text/event-stream",
        headers=headers,
    )


# --- Priority 3.3: public lesson endpoints for the Android client ---

# Hard caps so a misbehaving client can't ask for 10k lessons. Mirrors the
# feed endpoint's MAX_FEED_PAGE_SIZE pattern.
_LESSON_PAGE_LIMIT = 50


@router.get(
    "/lessons",
    response_model=WritingLessonListResponse,
)
async def list_lessons(
    task_type: TaskType | None = Query(default=None, description="Filter by task1 or task2."),
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=20, ge=1, le=_LESSON_PAGE_LIMIT),
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
) -> WritingLessonListResponse:
    """Paginated list of *published* lessons for the Android client.

    Drafts are never exposed. ``task_type`` is optional; omit it to get both.
    Backed by the compound ``(task_type, status, created_at desc)`` index
    created in the FastAPI lifespan.
    """
    items, total = await list_published_lessons(
        collection, task_type=task_type, page=page, limit=limit
    )
    total_pages = (total + limit - 1) // limit if total else 0
    return WritingLessonListResponse(
        page=page,
        limit=limit,
        total=total,
        total_pages=total_pages,
        items=[WritingLessonResponse.model_validate(item.model_dump()) for item in items],
    )


@router.get(
    "/lessons/{lesson_id}",
    response_model=WritingLessonResponse,
    dependencies=[Depends(check_rate_limit)],
)
async def get_lesson(
    lesson_id: str,
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
) -> WritingLessonResponse:
    """Single lesson detail. 404 if the lesson is missing or still a draft."""
    lesson = await get_published_lesson(collection, lesson_id)
    if lesson is None:
        raise HTTPException(status_code=404, detail="lesson_not_found")
    return WritingLessonResponse.model_validate(lesson.model_dump())


@router.get(
    "/lessons/{lesson_id}/image",
    dependencies=[Depends(check_rate_limit)],
)
async def get_lesson_image(
    lesson_id: str,
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
    bucket: AsyncIOMotorGridFSBucket = Depends(get_gridfs_bucket),
) -> Response:
    """Stream a Task 1 lesson's chart image out of GridFS as binary.

    404 if the lesson doesn't exist, is a draft, or has no image attached.
    Content-Type comes from the GridFS metadata recorded at upload time so
    Coil (and browsers) render PNGs/JPEGs correctly without sniffing.
    """
    lesson = await get_published_lesson(collection, lesson_id)
    if lesson is None or not lesson.image_id:
        raise HTTPException(status_code=404, detail="lesson_image_not_found")
    try:
        chunks, content_type = await open_lesson_image(bucket, lesson.image_id)
    except LessonImageNotFound:
        raise HTTPException(status_code=404, detail="lesson_image_not_found")
    # Long max-age is safe: lesson image ids are immutable; replacing a lesson's
    # image uploads a new id and updates the lesson doc atomically.
    headers = {"Cache-Control": "public, max-age=86400"}
    return StreamingResponse(chunks, media_type=content_type, headers=headers)
