from typing import Any

from motor.motor_asyncio import AsyncIOMotorCollection

from app.models.common import serialize_mongo_value


FEED_PROJECTION = {
    "transcriptText": 0,
    "transcriptSegments": 0,
    "description": 0,
}


async def fetch_feed_page(
    collection: AsyncIOMotorCollection,
    *,
    level: str,
    page: int,
    limit: int,
) -> tuple[int, list[dict[str, Any]]]:
    query = {"level": level, "status": "published"}
    skip = (page - 1) * limit

    total = await collection.count_documents(query)
    cursor = (
        collection.find(query, FEED_PROJECTION)
        .sort([("curatedAt", -1), ("createdAt", -1)])
        .skip(skip)
        .limit(limit)
    )

    items: list[dict[str, Any]] = []
    async for video in cursor:
        items.append(_to_feed_item(video))

    return total, items


async def fetch_feed_video(collection: AsyncIOMotorCollection, *, video_id: str) -> dict[str, Any] | None:
    video = await collection.find_one({"videoId": video_id, "status": "published"})
    if not video:
        return None

    item = _to_feed_item(video)
    item["transcriptSegments"] = serialize_mongo_value(video.get("transcriptSegments") or [])
    return item


def _to_feed_item(video: dict[str, Any]) -> dict[str, Any]:
    video = serialize_mongo_value(video)
    classification = video.get("classification") or {}

    return {
        "id": video.get("_id", ""),
        "videoId": video.get("videoId", ""),
        "title": video.get("title", ""),
        "channelTitle": video.get("channelTitle", ""),
        "thumbnailUrl": video.get("thumbnailUrl", ""),
        "durationSeconds": video.get("durationSeconds"),
        "publishDate": video.get("publishDate"),
        "level": video.get("level"),
        "computedLevel": video.get("computedLevel"),
        "confidence": classification.get("confidence"),
        "tags": video.get("tags") or [],
        "curatedAt": video.get("curatedAt"),
    }
