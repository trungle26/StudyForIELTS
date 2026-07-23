"""Priority 3.2 — admin CRUD for writing lessons.

Lessons live in the ``writing_lessons`` MongoDB collection; chart/graph
images for Task 1 lessons are stored in a GridFS bucket bound at app
startup. The service layer is the only place that talks to GridFS — the
router passes raw bytes in and gets back an image id string.

ponytail: image bytes are kept in memory and written to GridFS in a single
``upload_from_stream`` call. For the admin-curated, low-frequency workload
this is fine; if we ever accept user uploads larger than ~10 MB or in
volume, switch to streaming chunks via ``open_upload_stream``.
"""
import logging
from datetime import UTC, datetime
from typing import Any

from bson import ObjectId
from motor.motor_asyncio import (
    AsyncIOMotorCollection,
    AsyncIOMotorGridFSBucket,
)
from pymongo import ReturnDocument

from app.models.writing import (
    LessonDifficulty,
    LessonStatus,
    TaskType,
    WritingLesson,
)

logger = logging.getLogger(__name__)


# Fields an admin is allowed to update on an existing lesson. Everything else
# (id, created_at) is managed by this service.
_UPDATABLE_FIELDS = {
    "task_type",
    "task_prompt",
    "sample_answer",
    "tips",
    "difficulty",
    "status",
}


def _normalize_tips(raw: list[str] | None) -> list[str]:
    """Drop empty / dup tips, cap at 20 — keep ordering of the first appearance."""
    if not raw:
        return []
    seen: set[str] = set()
    out: list[str] = []
    for item in raw:
        if not isinstance(item, str):
            continue
        value = item.strip()
        if not value or value in seen:
            continue
        seen.add(value)
        out.append(value)
        if len(out) >= 20:
            break
    return out


async def _upload_image(
    bucket: AsyncIOMotorGridFSBucket,
    lesson_id: str,
    image_bytes: bytes,
    content_type: str | None,
) -> str:
    """Stream ``image_bytes`` into GridFS and return the new file id as str."""
    filename = f"{lesson_id}{_content_type_extension(content_type)}"
    upload = bucket.open_upload_stream(
        filename,
        metadata={
            "content_type": content_type or "application/octet-stream",
            "lesson_id": lesson_id,
        },
    )
    try:
        await upload.write(image_bytes)
    finally:
        await upload.close()
    return str(upload._id)


async def _delete_image(bucket: AsyncIOMotorGridFSBucket, image_id: str | None) -> None:
    """Best-effort GridFS delete; missing files are treated as success."""
    if not image_id:
        return
    try:
        await bucket.delete(ObjectId(image_id))
    except Exception as e:  # noqa: BLE001 — cleanup is best-effort
        logger.warning("GridFS delete failed for image_id=%s: %s", image_id, e)


def _content_type_extension(content_type: str | None) -> str:
    if not content_type:
        return ""
    mapping = {
        "image/png": ".png",
        "image/jpeg": ".jpg",
        "image/jpg": ".jpg",
        "image/webp": ".webp",
        "image/gif": ".gif",
    }
    return mapping.get(content_type.lower(), "")


async def create_lesson(
    collection: AsyncIOMotorCollection,
    bucket: AsyncIOMotorGridFSBucket,
    *,
    task_type: TaskType,
    task_prompt: str,
    sample_answer: str,
    tips: list[str] | None,
    difficulty: LessonDifficulty | None,
    status: LessonStatus,
    image_bytes: bytes | None,
    image_content_type: str | None,
) -> WritingLesson:
    """Insert a new lesson (and GridFS image if provided). Returns the saved doc."""
    now = datetime.now(UTC)
    lesson = WritingLesson(
        task_type=task_type,
        task_prompt=task_prompt.strip(),
        sample_answer=sample_answer.strip(),
        tips=_normalize_tips(tips),
        difficulty=difficulty,
        status=status,
        created_at=now,
        updated_at=now,
    )
    doc = lesson.model_dump(mode="json")
    # model_dump(mode='json') serializes datetimes as ISO strings; Mongo wants
    # native datetimes for the lifespan index to do its job.
    doc["created_at"] = now
    doc["updated_at"] = now

    if image_bytes:
        image_id = await _upload_image(bucket, lesson.id, image_bytes, image_content_type)
        doc["image_id"] = image_id
        lesson.image_id = image_id

    await collection.insert_one(doc)
    logger.info("Created writing lesson %s (task_type=%s, status=%s)", lesson.id, task_type, status)
    return lesson


async def update_lesson(
    collection: AsyncIOMotorCollection,
    bucket: AsyncIOMotorGridFSBucket,
    lesson_id: str,
    *,
    fields: dict[str, Any],
    image_bytes: bytes | None,
    image_content_type: str | None,
    clear_image: bool,
) -> WritingLesson | None:
    """Update an existing lesson; optionally replace or clear its image.

    Returns the updated lesson, or ``None`` if not found. ``clear_image`` and
    ``image_bytes`` are mutually exclusive — the router enforces this.
    """
    set_doc: dict[str, Any] = {}
    for key, value in fields.items():
        if key not in _UPDATABLE_FIELDS:
            continue
        if key == "tips":
            value = _normalize_tips(value)
        elif isinstance(value, str):
            value = value.strip()
        set_doc[key] = value

    existing = await collection.find_one({"id": lesson_id}, {"image_id": 1})
    if existing is None:
        return None
    old_image_id: str | None = existing.get("image_id")

    if image_bytes is not None:
        image_id = await _upload_image(bucket, lesson_id, image_bytes, image_content_type)
        set_doc["image_id"] = image_id
    elif clear_image:
        set_doc["image_id"] = None

    set_doc["updated_at"] = datetime.now(UTC)
    updated = await collection.find_one_and_update(
        {"id": lesson_id},
        {"$set": set_doc},
        return_document=ReturnDocument.AFTER,
    )
    if updated is None:
        return None

    # If we replaced or cleared the image, drop the old GridFS file. Done
    # after the doc write so a failed update never leaves the doc pointing
    # at a missing image.
    if (image_bytes is not None or clear_image) and old_image_id and old_image_id != set_doc.get("image_id"):
        await _delete_image(bucket, old_image_id)

    return _doc_to_lesson(updated)


async def delete_lesson(
    collection: AsyncIOMotorCollection,
    bucket: AsyncIOMotorGridFSBucket,
    lesson_id: str,
) -> bool:
    """Delete a lesson and its GridFS image. Returns False if not found."""
    existing = await collection.find_one({"id": lesson_id}, {"image_id": 1})
    if existing is None:
        return False
    await _delete_image(bucket, existing.get("image_id"))
    result = await collection.delete_one({"id": lesson_id})
    return result.deleted_count == 1


async def list_admin_lessons(collection: AsyncIOMotorCollection) -> list[WritingLesson]:
    """List every lesson (drafts included) for the admin view, newest first."""
    cursor = collection.find({}).sort([("created_at", -1)])
    return [_doc_to_lesson(doc) async for doc in cursor]


def _doc_to_lesson(doc: dict[str, Any]) -> WritingLesson:
    """Convert a Mongo document to a validated ``WritingLesson`` model."""
    return WritingLesson.model_validate({
        "id": doc["id"],
        "task_type": doc["task_type"],
        "task_prompt": doc["task_prompt"],
        "image_id": doc.get("image_id"),
        "sample_answer": doc["sample_answer"],
        "tips": doc.get("tips") or [],
        "difficulty": doc.get("difficulty"),
        "status": doc.get("status", "draft"),
        "created_at": doc["created_at"],
        "updated_at": doc["updated_at"],
    })
