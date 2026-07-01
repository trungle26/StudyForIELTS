import logging
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException, Request

from app.models.writing import EssaySubmission, WritingEvaluationDB
from app.services.llm_service import evaluate_essay_with_ai


router = APIRouter(prefix="/writing", tags=["writing"])
logger = logging.getLogger(__name__)

WRITING_COLLECTION_NAME = "writing_evaluations"


@router.post("/evaluate", response_model=WritingEvaluationDB)
async def evaluate_essay(submission: EssaySubmission, request: Request) -> WritingEvaluationDB:
    """Evaluate a user's essay with the LLM and persist the result.

    Workflow:
      1. Call the LLM service to get a strongly-typed `WritingEvaluation`.
      2. Decorate with a UUID and a UTC timestamp.
      3. Save the raw payload (as a plain dict) into the `writing_evaluations`
         MongoDB collection via `request.app.state.mongo_db` (asynchronously).
      4. Return the full `WritingEvaluationDB` to the caller.

    Errors:
      - LLM call failure (model not found, auth, network, bad JSON) -> 502
        with the underlying error message so the client can see what went wrong.
      - Database failure -> 500 with a generic message (details in server logs).
    """
    try:
        evaluation = await evaluate_essay_with_ai(
            task_prompt=submission.task_prompt,
            essay_text=submission.essay_text,
        )
    except RuntimeError as e:
        logger.exception("LLM evaluation failed")
        raise HTTPException(status_code=502, detail=f"LLM evaluation failed: {e}") from e
    except Exception as e:  # noqa: BLE001 - we want a clear message for any LLM-side error
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

    # `app.state.mongo_db` is set in `app.core.database.connect_mongo` during the
    # FastAPI lifespan startup. We write through the collection (async) so the
    # event loop is never blocked.
    try:
        collection = request.app.state.mongo_db[WRITING_COLLECTION_NAME]
        await collection.insert_one(record.model_dump(mode="json"))
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to save writing evaluation %s", record_id)
        raise HTTPException(status_code=500, detail=f"Database write failed: {e}") from e

    logger.info("Saved writing evaluation %s (band=%.1f)", record_id, record.overall_band)
    return record