from fastapi import APIRouter, Query

from app.core.config import settings
from app.models.youtube import SearchResponse, TranscriptResponse
from app.services.youtube_service import fetch_transcript, search_youtube


router = APIRouter(tags=["youtube"])


@router.get("/search", response_model=SearchResponse)
async def search(
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(settings.default_search_limit, ge=1, le=settings.max_search_limit),
) -> SearchResponse:
    return await search_youtube(q=q, limit=limit)


@router.get("/transcript", response_model=TranscriptResponse)
async def transcript(
    videoId: str = Query(..., min_length=11, max_length=11),
    language: str = Query("en", min_length=2, max_length=12),
) -> TranscriptResponse:
    return await fetch_transcript(video_id=videoId, language=language)


@router.get("/api/youtube/search", response_model=SearchResponse, include_in_schema=False)
async def legacy_search(
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(settings.default_search_limit, ge=1, le=settings.max_search_limit),
) -> SearchResponse:
    return await search_youtube(q=q, limit=limit)


@router.get("/api/youtube/captions/{video_id}", response_model=TranscriptResponse, include_in_schema=False)
async def legacy_captions(
    video_id: str,
    language: str = Query("en", min_length=2, max_length=12),
) -> TranscriptResponse:
    return await fetch_transcript(video_id=video_id, language=language)
