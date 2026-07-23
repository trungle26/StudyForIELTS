import json
import logging

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from motor.motor_asyncio import (
    AsyncIOMotorCollection,
    AsyncIOMotorGridFSBucket,
)

from app.core.database import get_curated_videos, get_gridfs_bucket, get_writing_lessons
from app.core.security import require_admin_token
from app.models.admin import AddVideoRequest, AddVideoResponse, AdminVideo
from app.models.writing import (
    AdminLessonListResponse,
    AdminLessonUpsertResponse,
    LessonDifficulty,
    LessonStatus,
    TaskType,
    WritingLesson,
    WritingLessonResponse,
)
from app.services.admin_service import add_or_update_video
from app.services.writing_lesson_service import (
    create_lesson,
    delete_lesson,
    list_admin_lessons,
    update_lesson,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/admin", tags=["admin"])

# Cap uploads to ~8 MB so a single admin request can't exhaust memory. The
# chart images the spec asks for are far below this; raise in one place if
# higher-resolution source files are ever needed.
_MAX_IMAGE_BYTES = 8 * 1024 * 1024


@router.post(
    "/add-video",
    response_model=AddVideoResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin_token)],
)
async def add_video(
    request: AddVideoRequest,
    collection: AsyncIOMotorCollection = Depends(get_curated_videos),
) -> AddVideoResponse:
    video = await add_or_update_video(collection, request)
    return AddVideoResponse(video=AdminVideo(**video))


# --- Priority 3.2: Writing-lesson admin CRUD ---


@router.post(
    "/writing-lessons",
    response_model=AdminLessonUpsertResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin_token)],
)
async def create_writing_lesson(
    task_type: TaskType = Form(...),
    task_prompt: str = Form(..., min_length=1),
    sample_answer: str = Form(..., min_length=1),
    tips: str | None = Form(default=None),
    difficulty: LessonDifficulty | None = Form(default=None),
    status_value: LessonStatus = Form(default="draft", alias="status"),
    image: UploadFile | None = File(default=None),
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
    bucket: AsyncIOMotorGridFSBucket = Depends(get_gridfs_bucket),
) -> AdminLessonUpsertResponse:
    """Create a writing lesson. Optional image goes into GridFS (Task 1 only)."""
    parsed_tips = _parse_tips(tips)
    image_bytes, image_content_type = await _read_image(image)

    lesson = await create_lesson(
        collection,
        bucket,
        task_type=task_type,
        task_prompt=task_prompt,
        sample_answer=sample_answer,
        tips=parsed_tips,
        difficulty=difficulty,
        status=status_value,
        image_bytes=image_bytes,
        image_content_type=image_content_type,
    )
    return AdminLessonUpsertResponse(lesson=_to_response(lesson))


@router.put(
    "/writing-lessons/{lesson_id}",
    response_model=AdminLessonUpsertResponse,
    dependencies=[Depends(require_admin_token)],
)
async def update_writing_lesson(
    lesson_id: str,
    task_type: TaskType | None = Form(default=None),
    task_prompt: str | None = Form(default=None),
    sample_answer: str | None = Form(default=None),
    tips: str | None = Form(default=None),
    difficulty: LessonDifficulty | None = Form(default=None),
    status_value: LessonStatus | None = Form(default=None, alias="status"),
    image: UploadFile | None = File(default=None),
    clear_image: bool = Form(default=False),
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
    bucket: AsyncIOMotorGridFSBucket = Depends(get_gridfs_bucket),
) -> AdminLessonUpsertResponse:
    """Update a lesson. If ``image`` is provided it replaces the current one;
    if ``clear_image=true`` the image is removed. The two are mutually
    exclusive — sending both is rejected with 400."""
    if image is not None and clear_image:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Provide either `image` or `clear_image=true`, not both.",
        )
    if image is None and not clear_image:
        image_bytes, image_content_type = None, None
    else:
        image_bytes, image_content_type = await _read_image(image)

    fields: dict = {
        "task_type": task_type,
        "task_prompt": task_prompt,
        "sample_answer": sample_answer,
        "tips": _parse_tips(tips) if tips is not None else None,
        "difficulty": difficulty,
        "status": status_value,
    }
    # Drop keys the caller didn't send — Pydantic Form fields default to None
    # for missing values, which would otherwise overwrite stored data with null.
    fields = {k: v for k, v in fields.items() if v is not None}

    lesson = await update_lesson(
        collection,
        bucket,
        lesson_id,
        fields=fields,
        image_bytes=image_bytes,
        image_content_type=image_content_type,
        clear_image=clear_image and image is None,
    )
    if lesson is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="lesson_not_found")
    return AdminLessonUpsertResponse(lesson=_to_response(lesson))


@router.delete(
    "/writing-lessons/{lesson_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(require_admin_token)],
)
async def delete_writing_lesson(
    lesson_id: str,
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
    bucket: AsyncIOMotorGridFSBucket = Depends(get_gridfs_bucket),
) -> None:
    deleted = await delete_lesson(collection, bucket, lesson_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="lesson_not_found")


@router.get(
    "/writing-lessons",
    response_model=AdminLessonListResponse,
    dependencies=[Depends(require_admin_token)],
)
async def list_writing_lessons_admin(
    collection: AsyncIOMotorCollection = Depends(get_writing_lessons),
) -> AdminLessonListResponse:
    """Admin view: every lesson, including drafts, newest first."""
    lessons = await list_admin_lessons(collection)
    return AdminLessonListResponse(items=[_to_response(lesson) for lesson in lessons])


def _to_response(lesson: WritingLesson) -> WritingLessonResponse:
    return WritingLessonResponse.model_validate(lesson.model_dump())


def _parse_tips(raw: str | None) -> list[str] | None:
    """`tips` arrives as a JSON-encoded string. None => absent, "" => empty list."""
    if raw is None:
        return None
    if raw.strip() == "":
        return []
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"`tips` must be a JSON array of strings: {e}",
        ) from e
    if not isinstance(parsed, list) or not all(isinstance(item, str) for item in parsed):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="`tips` must be a JSON array of strings.",
        )
    return parsed


async def _read_image(image: UploadFile | None) -> tuple[bytes | None, str | None]:
    """Read the upload into memory; reject oversize files with 413."""
    if image is None:
        return None, None
    data = await image.read()
    if len(data) > _MAX_IMAGE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"image exceeds {_MAX_IMAGE_BYTES} bytes",
        )
    return data, image.content_type
