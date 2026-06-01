from __future__ import annotations

import html
import os
import re
from typing import Any

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from youtube_transcript_api import YouTubeTranscriptApi
from youtube_transcript_api._errors import (
    NoTranscriptFound,
    TranscriptsDisabled,
    VideoUnavailable,
)
from yt_dlp import YoutubeDL


VIDEO_ID_PATTERN = re.compile(r"^[a-zA-Z0-9_-]{11}$")
DEFAULT_SEARCH_LIMIT = int(os.getenv("DEFAULT_SEARCH_LIMIT", "10"))
MAX_SEARCH_LIMIT = int(os.getenv("MAX_SEARCH_LIMIT", "25"))
SEARCH_SOCKET_TIMEOUT_SECONDS = int(os.getenv("SEARCH_SOCKET_TIMEOUT_SECONDS", "12"))
CORS_ALLOW_ORIGINS = [
    origin.strip()
    for origin in os.getenv("CORS_ALLOW_ORIGINS", "*").split(",")
    if origin.strip()
]

app = FastAPI(
    title="StudyForIELTS YouTube BFF",
    description="REST API for searching YouTube videos and fetching timestamped transcripts.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ALLOW_ORIGINS or ["*"],
    allow_credentials=False,
    allow_methods=["GET"],
    allow_headers=["*"],
)


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


class CaptionSegment(BaseModel):
    startTime: float
    endTime: float
    text: str


class TranscriptResponse(BaseModel):
    videoId: str
    language: str | None
    languageCode: str | None
    isGenerated: bool | None
    segments: list[CaptionSegment]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/search", response_model=SearchResponse)
def search(
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(DEFAULT_SEARCH_LIMIT, ge=1, le=MAX_SEARCH_LIMIT),
) -> SearchResponse:
    return _search_youtube(q=q, limit=limit)


@app.get("/transcript", response_model=TranscriptResponse)
def transcript(
    videoId: str = Query(..., min_length=11, max_length=11),
    language: str = Query("en", min_length=2, max_length=12),
) -> TranscriptResponse:
    return _get_transcript(video_id=videoId, language=language)


@app.get("/api/youtube/search", response_model=SearchResponse, include_in_schema=False)
def legacy_search_youtube(
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(DEFAULT_SEARCH_LIMIT, ge=1, le=MAX_SEARCH_LIMIT),
) -> SearchResponse:
    return _search_youtube(q=q, limit=limit)


@app.get("/api/youtube/captions/{video_id}", response_model=TranscriptResponse, include_in_schema=False)
def legacy_get_captions(
    video_id: str,
    language: str = Query("en", min_length=2, max_length=12),
) -> TranscriptResponse:
    return _get_transcript(video_id=video_id, language=language)


def _search_youtube(q: str, limit: int) -> SearchResponse:
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "extract_flat": True,
        "noplaylist": True,
        "socket_timeout": SEARCH_SOCKET_TIMEOUT_SECONDS,
    }

    try:
        with YoutubeDL(opts) as ydl:
            data = ydl.extract_info(f"ytsearch{limit}:{q}", download=False)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"YouTube search failed: {exc}") from exc

    entries = data.get("entries") or []
    results: list[SearchResult] = []
    for entry in entries:
        video_id = entry.get("id")
        title = entry.get("title")
        if not video_id or not title:
            continue

        thumbnails = _normalize_thumbnails(entry)
        results.append(
            SearchResult(
                videoId=video_id,
                title=html.unescape(title),
                thumbnails=thumbnails,
            )
        )

    return SearchResponse(query=q, results=results)


def _get_transcript(video_id: str, language: str) -> TranscriptResponse:
    if not VIDEO_ID_PATTERN.match(video_id):
        raise HTTPException(status_code=400, detail="video_id must be an 11-character YouTube video ID")

    languages = _language_priority(language)

    try:
        transcript = YouTubeTranscriptApi().fetch(video_id, languages=languages)
    except NoTranscriptFound as exc:
        raise HTTPException(status_code=404, detail=f"No transcript found for languages: {languages}") from exc
    except TranscriptsDisabled as exc:
        raise HTTPException(status_code=404, detail="Transcripts are disabled for this video") from exc
    except VideoUnavailable as exc:
        raise HTTPException(status_code=404, detail="Video is unavailable") from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Caption fetch failed: {exc}") from exc

    segments: list[CaptionSegment] = []
    for snippet in transcript:
        text = _clean_caption_text(snippet.text)
        if not text:
            continue
        segments.append(
            CaptionSegment(
                startTime=round(float(snippet.start), 3),
                endTime=round(float(snippet.start + snippet.duration), 3),
                text=text,
            )
        )

    return TranscriptResponse(
        videoId=video_id,
        language=getattr(transcript, "language", None),
        languageCode=getattr(transcript, "language_code", None),
        isGenerated=getattr(transcript, "is_generated", None),
        segments=segments,
    )


def _normalize_thumbnails(entry: dict[str, Any]) -> list[Thumbnail]:
    raw_thumbnails = entry.get("thumbnails") or []
    if not raw_thumbnails and entry.get("thumbnail"):
        raw_thumbnails = [{"url": entry["thumbnail"]}]

    thumbnails: list[Thumbnail] = []
    seen: set[str] = set()
    for thumbnail in raw_thumbnails:
        url = thumbnail.get("url")
        if not url or url in seen:
            continue
        seen.add(url)
        thumbnails.append(
            Thumbnail(
                url=url,
                width=thumbnail.get("width"),
                height=thumbnail.get("height"),
            )
        )

    return thumbnails


def _language_priority(language: str) -> list[str]:
    normalized = language.strip()
    if normalized.lower() == "en":
        return ["en", "en-US", "en-GB"]
    return [normalized, "en"]


def _clean_caption_text(text: str) -> str:
    cleaned = re.sub(r"<[^>]+>", "", html.unescape(text))
    cleaned = re.sub(r"\s+", " ", cleaned)
    return cleaned.strip()
