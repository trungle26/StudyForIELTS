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
    IpBlocked,
    NoTranscriptFound,
    RequestBlocked,
    TranscriptsDisabled,
    VideoUnavailable,
)
from youtube_transcript_api.proxies import GenericProxyConfig, WebshareProxyConfig
from yt_dlp import YoutubeDL


VIDEO_ID_PATTERN = re.compile(r"^[a-zA-Z0-9_-]{11}$")
DEFAULT_SEARCH_LIMIT = int(os.getenv("DEFAULT_SEARCH_LIMIT", "10"))
MAX_SEARCH_LIMIT = int(os.getenv("MAX_SEARCH_LIMIT", "25"))
SEARCH_SOCKET_TIMEOUT_SECONDS = int(os.getenv("SEARCH_SOCKET_TIMEOUT_SECONDS", "12"))
YOUTUBE_PROXY_URL = os.getenv("YOUTUBE_PROXY_URL", "").strip()
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
    if YOUTUBE_PROXY_URL:
        opts["proxy"] = YOUTUBE_PROXY_URL

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
        transcript = _build_transcript_api().fetch(video_id, languages=languages)
    except (RequestBlocked, IpBlocked) as exc:
        proxy_hint = (
            "The configured proxy was blocked by YouTube; rotate or replace the proxy pool."
            if _is_transcript_proxy_configured()
            else "YouTube blocked this server IP; configure a rotating residential proxy."
        )
        raise HTTPException(status_code=502, detail=proxy_hint) from exc
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


def _build_transcript_api() -> YouTubeTranscriptApi:
    proxy_provider = os.getenv("YOUTUBE_TRANSCRIPT_PROXY_PROVIDER", "").strip().lower()
    webshare_username = os.getenv("WEBSHARE_PROXY_USERNAME", "").strip()
    webshare_password = os.getenv("WEBSHARE_PROXY_PASSWORD", "").strip()
    generic_proxy_url = os.getenv("YOUTUBE_TRANSCRIPT_PROXY_URL", "").strip() or YOUTUBE_PROXY_URL
    generic_http_url = os.getenv("YOUTUBE_TRANSCRIPT_HTTP_PROXY_URL", "").strip() or generic_proxy_url
    generic_https_url = os.getenv("YOUTUBE_TRANSCRIPT_HTTPS_PROXY_URL", "").strip() or generic_proxy_url

    if proxy_provider and proxy_provider not in {"webshare", "generic", "none"}:
        raise HTTPException(
            status_code=500,
            detail="YOUTUBE_TRANSCRIPT_PROXY_PROVIDER must be one of: webshare, generic, none.",
        )

    if proxy_provider == "webshare" or (not proxy_provider and webshare_username and webshare_password):
        if not webshare_username or not webshare_password:
            raise HTTPException(
                status_code=500,
                detail="Webshare proxy is enabled but WEBSHARE_PROXY_USERNAME or WEBSHARE_PROXY_PASSWORD is missing.",
            )

        return YouTubeTranscriptApi(
            proxy_config=WebshareProxyConfig(
                proxy_username=webshare_username,
                proxy_password=webshare_password,
                filter_ip_locations=_csv_env("WEBSHARE_PROXY_LOCATIONS"),
                retries_when_blocked=_int_env("WEBSHARE_RETRIES_WHEN_BLOCKED", 10),
            )
        )

    if proxy_provider == "generic" or (not proxy_provider and (generic_http_url or generic_https_url)):
        if not generic_http_url and not generic_https_url:
            raise HTTPException(
                status_code=500,
                detail="Generic transcript proxy is enabled but no proxy URL is configured.",
            )

        return YouTubeTranscriptApi(
            proxy_config=GenericProxyConfig(
                http_url=generic_http_url or None,
                https_url=generic_https_url or generic_http_url or None,
            )
        )

    return YouTubeTranscriptApi()


def _is_transcript_proxy_configured() -> bool:
    return any(
        os.getenv(name, "").strip()
        for name in (
            "WEBSHARE_PROXY_USERNAME",
            "WEBSHARE_PROXY_PASSWORD",
            "YOUTUBE_PROXY_URL",
            "YOUTUBE_TRANSCRIPT_PROXY_URL",
            "YOUTUBE_TRANSCRIPT_HTTP_PROXY_URL",
            "YOUTUBE_TRANSCRIPT_HTTPS_PROXY_URL",
        )
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


def _csv_env(name: str) -> list[str] | None:
    values = [value.strip().lower() for value in os.getenv(name, "").split(",") if value.strip()]
    return values or None


def _int_env(name: str, default: int) -> int:
    raw_value = os.getenv(name, "").strip()
    if not raw_value:
        return default

    try:
        return int(raw_value)
    except ValueError as exc:
        raise HTTPException(status_code=500, detail=f"{name} must be an integer.") from exc


def _clean_caption_text(text: str) -> str:
    cleaned = re.sub(r"<[^>]+>", "", html.unescape(text))
    cleaned = re.sub(r"\s+", " ", cleaned)
    return cleaned.strip()
