import logging

from fastapi import APIRouter, BackgroundTasks, Query, Request

from app.core.config import settings
from app.models.admin import AddVideoRequest
from app.models.youtube import SearchResponse, TranscriptResponse
from app.services.admin_service import add_or_update_video
from app.services.youtube_service import fetch_transcript, search_youtube


router = APIRouter(tags=["youtube"])
logger = logging.getLogger(__name__)


async def _curate_search_result(app, video_id: str) -> None:
    try:
        collection = app.state.mongo_db[settings.mongodb_collection]
        await add_or_update_video(
            collection,
            AddVideoRequest(videoId=video_id, language="en", status="published"),
        )
    except Exception as exc:  # ponytail: keep search snappy, surface via logs only
        logger.warning("Auto-curate failed for %s: %s", video_id, exc)


@router.get("/search", response_model=SearchResponse)
async def search(
    request: Request,
    background_tasks: BackgroundTasks,
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(settings.default_search_limit, ge=1, le=settings.max_search_limit),
) -> SearchResponse:
    response = await search_youtube(q=q, limit=limit)
    for result in response.results:
        background_tasks.add_task(_curate_search_result, request.app, result.videoId)
    return response


@router.get("/transcript", response_model=TranscriptResponse)
async def transcript(
    videoId: str = Query(..., min_length=11, max_length=11),
    language: str = Query("en", min_length=2, max_length=12),
) -> TranscriptResponse:
    return await fetch_transcript(video_id=videoId, language=language)


@router.get("/api/youtube/search", response_model=SearchResponse, include_in_schema=False)
async def legacy_search(
    request: Request,
    background_tasks: BackgroundTasks,
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(settings.default_search_limit, ge=1, le=settings.max_search_limit),
) -> SearchResponse:
    response = await search_youtube(q=q, limit=limit)
    for result in response.results:
        background_tasks.add_task(_curate_search_result, request.app, result.videoId)
    return response


@router.get("/api/youtube/captions/{video_id}", response_model=TranscriptResponse, include_in_schema=False)
async def legacy_captions(
    video_id: str,
    language: str = Query("en", min_length=2, max_length=12),
) -> TranscriptResponse:
    return await fetch_transcript(video_id=video_id, language=language)
