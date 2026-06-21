from pydantic import BaseModel, Field


class Thumbnail(BaseModel):
    url: str
    width: int | None = None
    height: int | None = None


class SearchResult(BaseModel):
    videoId: str
    title: str
    thumbnails: list[Thumbnail] = Field(default_factory=list)


class SearchResponse(BaseModel):
    query: str
    results: list[SearchResult]


class TranscriptSegment(BaseModel):
    startTime: float
    endTime: float
    text: str


class TranscriptResponse(BaseModel):
    videoId: str
    language: str | None = None
    languageCode: str | None = None
    isGenerated: bool | None = None
    segments: list[TranscriptSegment]

