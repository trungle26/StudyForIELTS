from datetime import UTC, datetime
from typing import Any

from motor.motor_asyncio import AsyncIOMotorCollection
from pymongo import ReturnDocument

from app.models.admin import AddVideoRequest
from app.models.common import serialize_mongo_value
from app.services.cefr_classifier import classify_cefr, count_words
from app.services.youtube_service import fetch_transcript_and_metadata


async def add_or_update_video(collection: AsyncIOMotorCollection, request: AddVideoRequest) -> dict[str, Any]:
    youtube_data = await fetch_transcript_and_metadata(request.videoId, request.language)
    transcript_text = youtube_data["transcriptText"]
    classification = classify_cefr(transcript_text)
    computed_level = classification["level"]
    final_level = request.levelOverride or computed_level
    now = datetime.now(UTC)

    document = {
        **youtube_data,
        "wordCount": count_words(transcript_text),
        "level": final_level,
        "computedLevel": computed_level,
        "levelSource": "manual" if request.levelOverride else "computed",
        "classification": classification,
        "tags": normalize_tags(request.tags),
        "status": request.status,
        "curatedAt": now,
        "updatedAt": now,
    }

    video = await collection.find_one_and_update(
        {"videoId": request.videoId},
        {
            "$set": document,
            "$setOnInsert": {"createdAt": now},
        },
        upsert=True,
        return_document=ReturnDocument.AFTER,
    )

    return to_admin_video_response(video or {})


def normalize_tags(tags: list[str]) -> list[str]:
    normalized = []
    seen = set()
    for tag in tags:
        value = str(tag).strip().lower()
        if not value or value in seen:
            continue
        seen.add(value)
        normalized.append(value)
        if len(normalized) == 20:
            break
    return normalized


def to_admin_video_response(video: dict[str, Any]) -> dict[str, Any]:
    video = serialize_mongo_value(video)
    return {
        "id": video.get("_id", ""),
        "videoId": video.get("videoId", ""),
        "title": video.get("title", ""),
        "channelTitle": video.get("channelTitle", ""),
        "thumbnailUrl": video.get("thumbnailUrl", ""),
        "durationSeconds": video.get("durationSeconds"),
        "language": video.get("language"),
        "wordCount": video.get("wordCount", 0),
        "level": video.get("level"),
        "computedLevel": video.get("computedLevel"),
        "levelSource": video.get("levelSource", "computed"),
        "classification": video.get("classification"),
        "tags": video.get("tags") or [],
        "status": video.get("status", "published"),
        "transcriptSegmentCount": len(video.get("transcriptSegments") or []),
        "curatedAt": video.get("curatedAt"),
    }

