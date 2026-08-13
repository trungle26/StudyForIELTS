from datetime import datetime, timezone
from typing import Any

from bson import ObjectId
from motor.motor_asyncio import AsyncIOMotorCollection


def _public_lesson(document: dict[str, Any]) -> dict[str, Any]:
    lesson = {key: value for key, value in document.items() if not key.startswith("_")}
    if isinstance(lesson.get("_id"), ObjectId):
        lesson.pop("_id", None)
    return lesson


async def list_lessons(
    collection: AsyncIOMotorCollection,
    level: str | None,
    page: int,
    limit: int,
) -> tuple[int, list[dict[str, Any]]]:
    query: dict[str, Any] = {"status": "published"}
    if level:
        query["level"] = level

    total = await collection.count_documents(query)
    cursor = collection.find(query, {"_id": 0}).sort("updatedAt", -1).skip((page - 1) * limit).limit(limit)
    return total, [_public_lesson(document) async for document in cursor]


async def get_lesson(
    collection: AsyncIOMotorCollection,
    lesson_id: str,
) -> dict[str, Any] | None:
    document = await collection.find_one({"id": lesson_id, "status": "published"}, {"_id": 0})
    return _public_lesson(document) if document else None


async def upsert_lesson(
    collection: AsyncIOMotorCollection,
    lesson: dict[str, Any],
) -> dict[str, Any]:
    lesson = dict(lesson)
    lesson["updatedAt"] = datetime.now(timezone.utc).isoformat()
    await collection.replace_one({"id": lesson["id"]}, lesson, upsert=True)
    return lesson
