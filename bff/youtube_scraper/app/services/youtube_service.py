from __future__ import annotations

import asyncio
import html
import re
from datetime import datetime
from typing import Any

from fastapi import HTTPException
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

from app.core.config import settings
from app.models.youtube import SearchResponse, SearchResult, Thumbnail, TranscriptResponse, TranscriptSegment


VIDEO_ID_PATTERN = re.compile(r"^[a-zA-Z0-9_-]{11}$")


async def search_youtube(q: str, limit: int) -> SearchResponse:
    return await asyncio.to_thread(_search_youtube_sync, q, limit)


async def fetch_transcript(video_id: str, language: str) -> TranscriptResponse:
    return await asyncio.to_thread(_fetch_transcript_sync, video_id, language)


async def fetch_transcript_and_metadata(video_id: str, language: str) -> dict[str, Any]:
    return await asyncio.to_thread(_fetch_transcript_and_metadata_sync, video_id, language)


def _search_youtube_sync(q: str, limit: int) -> SearchResponse:
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "extract_flat": True,
        "noplaylist": True,
        "socket_timeout": settings.youtube_request_timeout_seconds,
    }
    if settings.youtube_proxy_url:
        opts["proxy"] = settings.youtube_proxy_url

    try:
        with YoutubeDL(opts) as ydl:
            data = ydl.extract_info(f"ytsearch{limit}:{q}", download=False)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"YouTube search failed: {exc}") from exc

    results: list[SearchResult] = []
    for entry in data.get("entries") or []:
        video_id = entry.get("id")
        title = entry.get("title")
        if not video_id or not title:
            continue

        results.append(
            SearchResult(
                videoId=video_id,
                title=html.unescape(title),
                thumbnails=_normalize_thumbnails(entry),
            )
        )

    return SearchResponse(query=q, results=results)


def _fetch_transcript_sync(video_id: str, language: str) -> TranscriptResponse:
    transcript_data = _fetch_transcript_data(video_id, language)

    return TranscriptResponse(
        videoId=video_id,
        language=transcript_data["language"],
        languageCode=transcript_data["languageCode"],
        isGenerated=transcript_data["isGenerated"],
        segments=[TranscriptSegment(**segment) for segment in transcript_data["segments"]],
    )


def _fetch_transcript_and_metadata_sync(video_id: str, language: str) -> dict[str, Any]:
    transcript_data = _fetch_transcript_data(video_id, language)
    metadata = _fetch_video_metadata(video_id)
    segments = transcript_data["segments"]

    return {
        **metadata,
        "videoId": video_id,
        "language": transcript_data["languageCode"] or language,
        "transcriptSegments": segments,
        "transcriptText": " ".join(segment["text"] for segment in segments),
    }


def _fetch_transcript_data(video_id: str, language: str) -> dict[str, Any]:
    if not VIDEO_ID_PATTERN.match(video_id):
        raise HTTPException(status_code=400, detail="videoId must be an 11-character YouTube video ID.")

    languages = _language_priority(language)

    try:
        transcript = _build_transcript_api().fetch(video_id, languages=languages)
    except (RequestBlocked, IpBlocked) as exc:
        raise HTTPException(
            status_code=502,
            detail="YouTube blocked transcript access from this network. Configure a rotating proxy for production.",
        ) from exc
    except NoTranscriptFound as exc:
        raise HTTPException(status_code=404, detail=f"No transcript found for languages: {languages}.") from exc
    except TranscriptsDisabled as exc:
        raise HTTPException(status_code=404, detail="Transcripts are disabled for this video.") from exc
    except VideoUnavailable as exc:
        raise HTTPException(status_code=404, detail="Video is unavailable.") from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Transcript fetch failed: {exc}") from exc

    segments: list[dict[str, Any]] = []
    for snippet in transcript:
        text = _clean_caption_text(snippet.text)
        if not text:
            continue

        segments.append({
            "startTime": round(float(snippet.start), 3),
            "endTime": round(float(snippet.start + snippet.duration), 3),
            "text": text,
        })

    return {
        "language": getattr(transcript, "language", None),
        "languageCode": getattr(transcript, "language_code", None),
        "isGenerated": getattr(transcript, "is_generated", None),
        "segments": segments,
    }


def _fetch_video_metadata(video_id: str) -> dict[str, Any]:
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        "socket_timeout": settings.youtube_request_timeout_seconds,
    }
    if settings.youtube_proxy_url:
        opts["proxy"] = settings.youtube_proxy_url

    try:
        with YoutubeDL(opts) as ydl:
            info = ydl.extract_info(f"https://www.youtube.com/watch?v={video_id}", download=False)
    except Exception:
        return _fallback_metadata(video_id)

    thumbnails = info.get("thumbnails") or []
    thumbnail_url = ""
    if thumbnails:
        thumbnail_url = thumbnails[-1].get("url") or ""

    return {
        "title": html.unescape(info.get("title") or f"YouTube video {video_id}"),
        "description": info.get("description") or "",
        "channelId": info.get("channel_id") or "",
        "channelTitle": info.get("channel") or info.get("uploader") or "",
        "publishDate": _parse_publish_date(info.get("upload_date")),
        "durationSeconds": int(info["duration"]) if info.get("duration") is not None else None,
        "thumbnailUrl": thumbnail_url or f"https://i.ytimg.com/vi/{video_id}/hqdefault.jpg",
        "embedUrl": f"https://www.youtube.com/embed/{video_id}",
    }


def _fallback_metadata(video_id: str) -> dict[str, Any]:
    return {
        "title": f"YouTube video {video_id}",
        "description": "",
        "channelId": "",
        "channelTitle": "",
        "publishDate": None,
        "durationSeconds": None,
        "thumbnailUrl": f"https://i.ytimg.com/vi/{video_id}/hqdefault.jpg",
        "embedUrl": f"https://www.youtube.com/embed/{video_id}",
    }


def _parse_publish_date(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.strptime(value, "%Y%m%d")
    except ValueError:
        return None


def _language_priority(language: str) -> list[str]:
    normalized = language.strip().lower()
    if normalized == "en":
        return ["en", "en-US", "en-GB"]
    return [normalized, "en"]


def _build_transcript_api() -> YouTubeTranscriptApi:
    provider = settings.youtube_transcript_proxy_provider
    if provider and provider not in {"webshare", "generic", "none"}:
        raise HTTPException(
            status_code=500,
            detail="YOUTUBE_TRANSCRIPT_PROXY_PROVIDER must be one of: webshare, generic, none.",
        )

    if provider == "webshare" or (
        not provider and settings.webshare_proxy_username and settings.webshare_proxy_password
    ):
        if not settings.webshare_proxy_username or not settings.webshare_proxy_password:
            raise HTTPException(
                status_code=500,
                detail="Webshare proxy is enabled but username or password is missing.",
            )

        return YouTubeTranscriptApi(
            proxy_config=WebshareProxyConfig(
                proxy_username=settings.webshare_proxy_username,
                proxy_password=settings.webshare_proxy_password,
                filter_ip_locations=settings.webshare_proxy_locations or None,
                retries_when_blocked=settings.webshare_retries_when_blocked,
            )
        )

    generic_proxy_url = settings.youtube_transcript_proxy_url or settings.youtube_proxy_url
    http_url = settings.youtube_transcript_http_proxy_url or generic_proxy_url
    https_url = settings.youtube_transcript_https_proxy_url or generic_proxy_url

    if provider == "generic" or (not provider and (http_url or https_url)):
        return YouTubeTranscriptApi(
            proxy_config=GenericProxyConfig(
                http_url=http_url or None,
                https_url=https_url or http_url or None,
            )
        )

    return YouTubeTranscriptApi()


def _normalize_thumbnails(entry: dict) -> list[Thumbnail]:
    thumbnails = entry.get("thumbnails") or []
    normalized: list[Thumbnail] = []

    for thumbnail in thumbnails:
        url = thumbnail.get("url")
        if not url:
            continue
        normalized.append(
            Thumbnail(
                url=url,
                width=thumbnail.get("width"),
                height=thumbnail.get("height"),
            )
        )

    if not normalized and entry.get("id"):
        normalized.append(Thumbnail(url=f"https://i.ytimg.com/vi/{entry['id']}/hqdefault.jpg"))

    return normalized


def _clean_caption_text(text: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"\[[^\]]*]", "", html.unescape(str(text)))).strip()
