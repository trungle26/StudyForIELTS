from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

from app.models.feed import CEFRLevel


VideoStatus = Literal["published", "draft"]


class AddVideoRequest(BaseModel):
    videoId: str = Field(..., min_length=11, max_length=11)
    language: str = Field(default="en", min_length=2, max_length=12)
    levelOverride: CEFRLevel | None = None
    tags: list[str] = Field(default_factory=list)
    status: VideoStatus = "published"


class Classification(BaseModel):
    level: CEFRLevel
    confidence: float
    metrics: dict
    explanation: str
    classifierVersion: str


class AdminVideo(BaseModel):
    id: str
    videoId: str
    title: str
    channelTitle: str = ""
    thumbnailUrl: str = ""
    durationSeconds: int | None = None
    language: str | None = None
    wordCount: int
    level: CEFRLevel
    computedLevel: CEFRLevel
    levelSource: Literal["computed", "manual"]
    classification: Classification
    tags: list[str] = Field(default_factory=list)
    status: VideoStatus
    transcriptSegmentCount: int
    curatedAt: datetime


class AddVideoResponse(BaseModel):
    video: AdminVideo

