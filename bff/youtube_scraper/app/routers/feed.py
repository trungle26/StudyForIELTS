import math

from fastapi import APIRouter, Depends, HTTPException, Query
from motor.motor_asyncio import AsyncIOMotorCollection

from app.core.config import settings
from app.core.database import get_curated_videos
from app.models.feed import CEFRLevel, FeedDetailItem, FeedDetailResponse, FeedItem, FeedResponse
from app.services.feed_service import fetch_feed_page, fetch_feed_video


router = APIRouter(tags=["feed"])


@router.get("/feed", response_model=FeedResponse)
async def get_feed(
    level: CEFRLevel = Query(..., description="CEFR level: A1, A2, B1, B2, C1, or C2."),
    page: int = Query(1, ge=1),
    limit: int = Query(settings.feed_page_size, ge=1, le=settings.max_feed_page_size),
    collection: AsyncIOMotorCollection = Depends(get_curated_videos),
) -> FeedResponse:
    total, items = await fetch_feed_page(collection, level=level, page=page, limit=limit)

    return FeedResponse(
        level=level,
        page=page,
        limit=limit,
        total=total,
        totalPages=math.ceil(total / limit) if total else 0,
        items=[FeedItem(**item) for item in items],
    )


@router.get("/feed/{video_id}", response_model=FeedDetailResponse)
async def get_feed_video(
    video_id: str,
    collection: AsyncIOMotorCollection = Depends(get_curated_videos),
) -> FeedDetailResponse:
    video = await fetch_feed_video(collection, video_id=video_id)
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")

    return FeedDetailResponse(video=FeedDetailItem(**video))
