import json
import logging
import uuid
from datetime import datetime, timezone
from typing import AsyncIterator

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse

from app.core.config import settings
from app.models.writing import EssaySubmission, WritingEvaluation, WritingEvaluationDB
from app.services.llm_service import evaluate_essay_with_ai, stream_essay_evaluation


router = APIRouter(prefix="/writing", tags=["writing"])
logger = logging.getLogger(__name__)

WRITING_COLLECTION_NAME = "writing_evaluations"


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


@router.post("/evaluate", response_model=WritingEvaluationDB)
async def evaluate_essay(submission: EssaySubmission, request: Request) -> WritingEvaluationDB:
    """Evaluate a user's essay with the LLM and persist the result (non-streaming).

    Kept for backward compatibility / debugging. The mobile app uses
    `POST /writing/evaluate/stream` to render feedback progressively.

    Workflow:
      1. Call the LLM service to get a strongly-typed `WritingEvaluation`.
      2. Decorate with a UUID and a UTC timestamp.
      3. Save the raw payload (as a plain dict) into the `writing_evaluations`
         MongoDB collection via `request.app.state.mongo_db` (asynchronously).
      4. Return the full `WritingEvaluationDB` to the caller.
    """
    try:
        evaluation = await evaluate_essay_with_ai(
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

    record = WritingEvaluationDB(
        id=record_id,
        task_prompt=submission.task_prompt,
        essay_text=submission.essay_text,
        created_at=created_at,
        **evaluation.model_dump(),
    )

    try:
        collection = request.app.state.mongo_db[WRITING_COLLECTION_NAME]
        await collection.insert_one(record.model_dump(mode="json"))
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to save writing evaluation %s", record_id)
        raise HTTPException(status_code=500, detail=f"Database write failed: {e}") from e

    logger.info("Saved writing evaluation %s (band=%.1f)", record_id, record.overall_band)
    return record


@router.post("/evaluate/stream")
async def evaluate_essay_stream(
    submission: EssaySubmission, request: Request
) -> StreamingResponse:
    """Stream the LLM evaluation as Server-Sent Events.

    Event format:
      - default `data: <chunk>` events contain the raw LLM delta text (the
        model's streaming JSON, not the validated one).
      - one `event: done` event with `data: <json>` carries the final
        `WritingEvaluation` (the same shape returned by `/evaluate`).
      - on error, one `event: error` event with `data: <message>` is emitted
        before the stream closes.

    Persistence happens in the background after the final `done` event so the
    user never waits on the Mongo write. If persistence fails we log it but
    still return the evaluation to the user.
    """
    record_id = str(uuid.uuid4())
    created_at = datetime.now(timezone.utc)
    # snapshot the values so the background task can't be mutated by a retried request
    snapshot_prompt = submission.task_prompt
    snapshot_essay = submission.essay_text

    async def event_source() -> AsyncIterator[str]:
        accumulated = bytearray()  # stream into a buffer; we can re-decode once done

        # Yield a "ping" so the client sees headers right away.
        yield _sse_format(None, "[connected]")

        final_evaluation: WritingEvaluation | None = None
        final_error: str | None = None

        try:
            async for raw_event in stream_essay_evaluation(
                task_prompt=snapshot_prompt, essay_text=snapshot_essay
            ):
                accumulated.extend(raw_event.encode("utf-8"))
                # Forward the LLM service's SSE-formatted lines straight through.
                yield raw_event

                # The LLM service emits a single `event: done` when finished.
                # We only forward the last `done` so we can re-parse the validated
                # JSON from the accumulated buffer and persist it.
                if raw_event.startswith("event: done"):
                    try:
                        body = accumulated.decode("utf-8", errors="replace")
                        # extract the last data line of the done event
                        last_data = ""
                        for line in body.splitlines()[::-1]:
                            if line.startswith("data: "):
                                last_data = line[len("data: "):]
                                break
                        if last_data:
                            final_evaluation = WritingEvaluation.model_validate_json(last_data)
                    except Exception as e:  # noqa: BLE001
                        final_error = f"Failed to parse final evaluation: {e}"
                elif raw_event.startswith("event: error"):
                    # The first line of the error event holds the message.
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
            record = WritingEvaluationDB(
                id=record_id,
                task_prompt=snapshot_prompt,
                essay_text=snapshot_essay,
                created_at=created_at,
                **final_evaluation.model_dump(),
            )
            collection = request.app.state.mongo_db[WRITING_COLLECTION_NAME]
            await collection.insert_one(record.model_dump(mode="json"))
            logger.info("Saved streamed writing evaluation %s (band=%.1f)", record_id, final_evaluation.overall_band)
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