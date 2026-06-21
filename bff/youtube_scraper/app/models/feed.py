from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


CEFRLevel = Literal["A1", "A2", "B1", "B2", "C1", "C2"]


class FeedItem(BaseModel):
    id: str
    videoId: str
    title: str
    channelTitle: str = ""
    thumbnailUrl: str = ""
    durationSeconds: int | None = None
    publishDate: datetime | None = None
    level: CEFRLevel
    computedLevel: CEFRLevel | None = None
    confidence: float | None = None
    tags: list[str] = Field(default_factory=list)
    curatedAt: datetime | None = None


class FeedResponse(BaseModel):
    level: CEFRLevel
    page: int
    limit: int
    total: int
    totalPages: int
    items: list[FeedItem]


class TranscriptSegment(BaseModel):
    startTime: float
    endTime: float
    text: str


class FeedDetailItem(FeedItem):
    transcriptSegments: list[TranscriptSegment] = Field(default_factory=list)


class FeedDetailResponse(BaseModel):
    video: FeedDetailItem
