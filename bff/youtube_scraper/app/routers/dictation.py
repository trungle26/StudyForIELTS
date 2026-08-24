import math
import logging

from fastapi import APIRouter, Depends, HTTPException, Query, status
from motor.motor_asyncio import AsyncIOMotorCollection

from app.core.database import get_dictation_lessons
from app.core.security import require_admin_token
from app.models.dictation import (
    DictationClassification,
    DictationClassifyRequest,
    DictationClassifyResponse,
    DictationLesson,
    DictationLessonListResponse,
    DictationLessonResponse,
    DictationVocabRequest,
    DictationVocabResponse,
)
from app.services.cefr_classifier import classify_dictation_cefr
from app.services.dictation_service import get_lesson, list_lessons, upsert_lesson
from app.services.llm_service import generate_dictation_vocabulary

logger = logging.getLogger(__name__)

router = APIRouter(tags=["dictation"])


@router.patch(
    "/admin/dictation/{lesson_id}/status",
    response_model=DictationLessonResponse,
    dependencies=[Depends(require_admin_token)],
)
async def update_dictation_status(
    lesson_id: str,
    lesson_status: str = Query(alias="status", pattern="^(draft|published|removed)$"),
    collection: AsyncIOMotorCollection = Depends(get_dictation_lessons),
) -> DictationLessonResponse:
    document = await collection.find_one({"id": lesson_id}, {"_id": 0})
    if document is None:
        raise HTTPException(status_code=404, detail="Dictation lesson not found.")
    document["status"] = lesson_status
    stored = await upsert_lesson(collection, document)
    return DictationLessonResponse(lesson=DictationLesson(**stored))


@router.get("/dictation/lessons", response_model=DictationLessonListResponse)
async def list_dictation_lessons(
    level: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=20, ge=1, le=100),
    collection: AsyncIOMotorCollection = Depends(get_dictation_lessons),
) -> DictationLessonListResponse:
    total, items = await list_lessons(collection, level, page, limit)
    return DictationLessonListResponse(
        level=level,
        page=page,
        limit=limit,
        total=total,
        totalPages=math.ceil(total / limit) if total else 0,
        items=[DictationLesson(**item) for item in items],
    )


@router.get("/dictation/lessons/{lesson_id}", response_model=DictationLessonResponse)
async def get_dictation_lesson(
    lesson_id: str,
    collection: AsyncIOMotorCollection = Depends(get_dictation_lessons),
) -> DictationLessonResponse:
    lesson = await get_lesson(collection, lesson_id)
    if lesson is None:
        raise HTTPException(status_code=404, detail="Dictation lesson not found.")
    return DictationLessonResponse(lesson=DictationLesson(**lesson))


@router.post(
    "/admin/dictation/import",
    response_model=DictationLessonResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin_token)],
)
async def import_dictation_lesson(
    lesson: DictationLesson,
    collection: AsyncIOMotorCollection = Depends(get_dictation_lessons),
) -> DictationLessonResponse:
    if not lesson.sentences:
        raise HTTPException(status_code=400, detail="A dictation lesson needs at least one sentence.")
    if any(sentence.endTimeMs <= sentence.startTimeMs for sentence in lesson.sentences):
        raise HTTPException(status_code=400, detail="Sentence end times must be after start times.")

    document = lesson.model_dump()
    document["status"] = "draft"
    stored = await upsert_lesson(collection, document)
    return DictationLessonResponse(lesson=DictationLesson(**stored))


@router.post(
    "/admin/dictation/vocabulary",
    response_model=DictationVocabResponse,
    dependencies=[Depends(require_admin_token)],
)
async def generate_dictation_vocabulary_endpoint(payload: DictationVocabRequest) -> DictationVocabResponse:
    try:
        items = await generate_dictation_vocabulary(payload.level, payload.title, payload.transcript)
    except RuntimeError as e:
        logger.exception("Dictation vocabulary generation failed")
        raise HTTPException(status_code=502, detail=f"Vocab generation failed: {e}") from e
    return DictationVocabResponse(vocabularies=items)


@router.post(
    "/admin/dictation/classify",
    response_model=DictationClassifyResponse,
    dependencies=[Depends(require_admin_token)],
)
async def classify_dictation_endpoint(payload: DictationClassifyRequest) -> DictationClassifyResponse:
    segments = [segment.model_dump() for segment in payload.segments]
    raw = classify_dictation_cefr(
        payload.transcript,
        segments=segments,
        duration_seconds=payload.durationSeconds,
    )
    return DictationClassifyResponse(classification=DictationClassification(**raw))
