#!/usr/bin/env python3
"""Generate dictation lessons from a folder of audio files (Colab).

Layout::

    input_dir/
        lesson-one.mp3
        batch-2/
            conversation.wav
        nested/folder/monologue.m4a

The script discovers audio files directly or recursively under arbitrary
subfolders. Folder names are NOT interpreted as CEFR levels. For every
audio file the script:

1. Uploads the file to Appwrite Storage (multipart; expects the bucket
   files to be publicly readable).
2. Transcribes it with faster-whisper (or the OpenAI API).
3. Asks the BFF endpoint ``/admin/dictation/classify`` to determine the
   CEFR level using transcript readability plus timestamp-aware speech
   speed. Speed is exposed separately as ``speedDifficulty`` and never
   promotes a lesson more than one CEFR band on its own.
4. Generates vocabulary through ``/admin/dictation/vocabulary`` using
   the computed level.
5. Imports the complete lesson via ``/admin/dictation/import`` and then
   patches the status to ``published``.

Required environment::

    APPWRITE_API_KEY       Appwrite project API key
    ADMIN_TOKEN            BFF admin token (matches BFF ``ADMIN_TOKEN``)

Required CLI args: ``--input-dir``, ``--appwrite-endpoint``,
``--appwrite-project``, ``--appwrite-bucket``, ``--bff-url``.
"""
from __future__ import annotations

import argparse
import json
import logging
import os
import re
import sys
import unicodedata
from pathlib import Path
from typing import Any, Iterable

import httpx

AUDIO_SUFFIXES = {".mp3", ".wav", ".m4a", ".ogg", ".flac", ".webm", ".mp4"}
MAX_TRANSCRIPT_CHARS = 12_000  # keep vocab prompt bounded; transcripts are usually short.
DEFAULT_MIN_CONFIDENCE = 0.65

logger = logging.getLogger("generate_dictation_seed")


# ---------------------------------------------------------------------------
# Filesystem discovery
# ---------------------------------------------------------------------------
def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode("ascii")
    text = re.sub(r"[^A-Za-z0-9]+", "-", text).strip("-").lower()
    return text or "lesson"


def discover_lessons(input_dir: Path, recursive: bool = True) -> list[dict[str, Any]]:
    """Walk ``input_dir`` and yield ``{lesson_id, title, audio}`` records.

    Folder names are NOT used as levels. Glob is recursive by default so
    callers can organize inputs however they like. Duplicate lesson IDs
    across the whole tree raise a clear error rather than overwriting.
    """
    if not input_dir.is_dir():
        raise SystemExit(f"Input directory not found: {input_dir}")
    globber = input_dir.rglob if recursive else input_dir.glob
    records: list[dict[str, Any]] = []
    seen: set[str] = set()
    for audio in sorted(globber("*")):
        if audio.suffix.lower() not in AUDIO_SUFFIXES or not audio.is_file():
            continue
        lesson_id = f"dd-{slugify(audio.stem)}"
        if lesson_id in seen:
            raise SystemExit(f"Duplicate lesson id {lesson_id}; rename one file.")
        seen.add(lesson_id)
        records.append({
            "lesson_id": lesson_id,
            "title": audio.stem.replace("-", " ").replace("_", " ").strip().title() or lesson_id,
            "audio": audio,
        })
    if not records:
        raise SystemExit(f"No audio files found under {input_dir}.")
    return records


# ---------------------------------------------------------------------------
# Appwrite upload
# ---------------------------------------------------------------------------
def appwrite_upload(
    endpoint: str,
    project: str,
    bucket: str,
    api_key: str,
    audio: Path,
    file_id: str,
) -> dict[str, Any]:
    """Upload ``audio`` to Appwrite Storage using multipart form data."""
    url = f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files"
    # ponytail: custom file_id keeps reruns idempotent; Appwrite requires ID to
    # match ^[a-zA-Z0-9][a-zA-Z0-9._-]{0,35}$, so we sanitize aggressively.
    safe_id = re.sub(r"[^A-Za-z0-9._-]", "-", file_id)[:36]
    params = {"project": project, "fileId": safe_id}
    headers = {"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}
    with audio.open("rb") as handle:
        files = {"file": (audio.name, handle, "application/octet-stream")}
        response = httpx.post(url, params=params, headers=headers, files=files, timeout=600)
    response.raise_for_status()
    return response.json()


def appwrite_audio_url(endpoint: str, project: str, bucket: str, file_id: str) -> str:
    return f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files/{file_id}/download?project={project}"


# ---------------------------------------------------------------------------
# Transcription
# ---------------------------------------------------------------------------
def transcribe_local(audio: Path, model_name: str, device: str, compute_type: str) -> Any:
    from faster_whisper import WhisperModel

    model = WhisperModel(model_name, device=device, compute_type=compute_type)
    segments, info = model.transcribe(str(audio), vad_filter=True)
    return type("Transcription", (), {"segments": list(segments), "duration": info.duration})()


def transcribe_openai(audio: Path, model_name: str) -> Any:
    from openai import OpenAI

    with audio.open("rb") as handle:
        result = OpenAI().audio.transcriptions.create(
            model=model_name, file=handle, response_format="verbose_json", timestamp_granularities=["segment"]
        )
    return result


def transcription_to_sentences(transcription: Any) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Return ``(sentences, raw_segments_seconds)``.

    Sentences follow the BFF contract. Raw segments carry ``start``/``end``
    in seconds and the segment-level text for the classifier request.
    """
    segments = getattr(transcription, "segments", None) or []
    sentences: list[dict[str, Any]] = []
    raw_segments: list[dict[str, Any]] = []
    for index, segment in enumerate(segments):
        text = str(getattr(segment, "text", "")).strip()
        start = int(round(float(getattr(segment, "start", 0)) * 1000))
        end = int(round(float(getattr(segment, "end", 0)) * 1000))
        if text and end > start:
            sentences.append({"orderIndex": index, "text": text, "startTimeMs": start, "endTimeMs": end})
            raw_segments.append({
                "start": float(getattr(segment, "start", 0)),
                "end": float(getattr(segment, "end", 0)),
                "text": text,
            })
    if not sentences:
        raise SystemExit("Whisper returned no usable timestamped segments.")
    return sentences, raw_segments


def make_lesson(
    metadata: dict[str, Any],
    lesson_id: str,
    transcription: Any,
    classification: dict[str, Any],
) -> dict[str, Any]:
    sentences, _raw = transcription_to_sentences(transcription)
    lesson = {**metadata, "id": lesson_id, "sentences": sentences}
    duration = getattr(transcription, "duration", None)
    if duration is not None:
        lesson["durationSeconds"] = max(1, int(round(float(duration))))
    lesson["classification"] = classification
    # Trust the BFF classifier over caller-provided metadata; folder names
    # are no longer a source of truth.
    lesson["level"] = classification["level"]
    return lesson


# ---------------------------------------------------------------------------
# BFF client
# ---------------------------------------------------------------------------
def join_transcript(sentences: Iterable[dict[str, Any]]) -> str:
    return " ".join(sentence["text"] for sentence in sentences)[:MAX_TRANSCRIPT_CHARS]


def classify_lesson(
    bff_url: str,
    token: str,
    title: str,
    transcript: str,
    segments: list[dict[str, Any]],
    duration_seconds: float | None,
) -> dict[str, Any]:
    headers = {"x-admin-token": token}
    payload: dict[str, Any] = {
        "title": title,
        "transcript": transcript,
        "segments": [{"start": s["start"], "end": s["end"], "text": s["text"]} for s in segments],
    }
    if duration_seconds is not None:
        payload["durationSeconds"] = duration_seconds
    response = httpx.post(
        f"{bff_url.rstrip('/')}/admin/dictation/classify",
        json=payload,
        headers=headers,
        timeout=60,
    )
    response.raise_for_status()
    return response.json()["classification"]


def generate_vocabulary(bff_url: str, token: str, level: str, title: str, transcript: str) -> list[dict[str, Any]]:
    headers = {"x-admin-token": token}
    response = httpx.post(
        f"{bff_url.rstrip('/')}/admin/dictation/vocabulary",
        json={"level": level, "title": title, "transcript": transcript},
        headers=headers,
        timeout=120,
    )
    response.raise_for_status()
    return response.json().get("vocabularies", [])


def import_lesson(bff_url: str, token: str, lesson: dict[str, Any]) -> dict[str, Any]:
    headers = {"x-admin-token": token}
    response = httpx.post(
        f"{bff_url.rstrip('/')}/admin/dictation/import",
        json=lesson,
        headers=headers,
        timeout=60,
    )
    response.raise_for_status()
    return response.json()


def publish_lesson(bff_url: str, token: str, lesson_id: str) -> None:
    headers = {"x-admin-token": token}
    response = httpx.patch(
        f"{bff_url.rstrip('/')}/admin/dictation/{lesson_id}/status",
        params={"status": "published"},
        headers=headers,
        timeout=30,
    )
    response.raise_for_status()


# ---------------------------------------------------------------------------
# Orchestration
# ---------------------------------------------------------------------------
def require_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise SystemExit(f"Environment variable {name} is required.")
    return value


def process_record(
    record: dict[str, Any],
    *,
    appwrite_endpoint: str,
    appwrite_project: str,
    appwrite_bucket: str,
    appwrite_key: str,
    bff_url: str,
    bff_token: str,
    whisper_model: str,
    whisper_device: str,
    whisper_compute: str,
    engine: str,
    out_dir: Path,
    skip_vocab: bool,
    require_confidence: bool,
    min_confidence: float,
    continue_on_error: bool,
) -> dict[str, Any] | None:
    audio: Path = record["audio"]
    lesson_id: str = record["lesson_id"]
    title: str = record["title"]
    logger.info("Processing %s [%s]", lesson_id, audio.name)
    try:
        file_id = f"dictation-{lesson_id}"
        uploaded = appwrite_upload(appwrite_endpoint, appwrite_project, appwrite_bucket, appwrite_key, audio, file_id)
        actual_id = uploaded.get("$id", file_id)
        audio_url = appwrite_audio_url(appwrite_endpoint, appwrite_project, appwrite_bucket, actual_id)

        if engine == "openai":
            transcription = transcribe_openai(audio, whisper_model)
        else:
            transcription = transcribe_local(audio, whisper_model, whisper_device, whisper_compute)

        sentences, raw_segments = transcription_to_sentences(transcription)
        transcript_text = join_transcript(sentences)
        media_duration = getattr(transcription, "duration", None)

        classification = classify_lesson(
            bff_url, bff_token, title, transcript_text, raw_segments, media_duration
        )
        level = classification["level"]
        logger.info(
            "  level=%s confidence=%.2f speed=%s review=%s",
            level,
            classification["confidence"],
            classification["speedDifficulty"],
            classification["reviewRecommended"],
        )
        if require_confidence and classification["confidence"] < min_confidence:
            raise SystemExit(
                f"Confidence {classification['confidence']:.2f} below threshold "
                f"{min_confidence:.2f}; review required."
            )
        elif classification["reviewRecommended"]:
            logger.warning(
                "  review recommended (confidence=%.2f speed=%s)",
                classification["confidence"],
                classification["speedDifficulty"],
            )

        metadata = {
            "title": title,
            "level": level,
            "source": "original",
            "sourceUrl": "",
            "licenseNote": "Use only audio you own or are licensed to distribute.",
            "audioUrl": audio_url,
        }
        lesson = make_lesson(metadata, lesson_id, transcription, classification)

        if skip_vocab:
            lesson["vocabularies"] = []
        else:
            lesson["vocabularies"] = generate_vocabulary(bff_url, bff_token, level, title, transcript_text)

        import_lesson(bff_url, bff_token, lesson)
        publish_lesson(bff_url, bff_token, lesson_id)

        out_dir.mkdir(parents=True, exist_ok=True)
        (out_dir / f"{lesson_id}.json").write_text(json.dumps(lesson, indent=2) + "\n", encoding="utf-8")
        logger.info("OK  %s -> %s", lesson_id, audio_url)
        return lesson
    except Exception as exc:  # noqa: BLE001 - want to surface the first failure
        logger.error("FAIL %s: %s", lesson_id, exc)
        if not continue_on_error:
            raise
        return None


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--input-dir", type=Path, required=True, help="Folder containing audio files (recursively).")
    parser.add_argument("--no-recursive", action="store_true", help="Disable recursive discovery (only --input-dir's immediate children).")
    parser.add_argument("--output-dir", type=Path, default=Path("dictation_seeds"))
    parser.add_argument("--appwrite-endpoint", required=True)
    parser.add_argument("--appwrite-project", required=True)
    parser.add_argument("--appwrite-bucket", required=True)
    parser.add_argument("--bff-url", required=True)
    parser.add_argument("--model", default="small", help="Whisper model name (small/medium/large-v3) or gpt-4o-transcribe for OpenAI.")
    parser.add_argument("--engine", choices=("local", "openai"), default="local")
    parser.add_argument("--device", choices=("auto", "cuda", "cpu"), default="auto")
    parser.add_argument("--compute-type", default="float16")
    parser.add_argument("--skip-vocab", action="store_true", help="Skip vocab generation (import with empty vocabularies).")
    parser.add_argument("--require-confidence", action="store_true", help="Stop before import when classifier confidence is below --min-confidence.")
    parser.add_argument("--min-confidence", type=float, default=DEFAULT_MIN_CONFIDENCE)
    parser.add_argument("--continue-on-error", action="store_true", help="Keep going after a per-lesson failure.")
    parser.add_argument("--verbose", "-v", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO, format="%(levelname)s %(message)s")

    appwrite_key = require_env("APPWRITE_API_KEY")
    bff_token = require_env("ADMIN_TOKEN")

    records = discover_lessons(args.input_dir, recursive=not args.no_recursive)
    logger.info("Discovered %d lesson(s) under %s", len(records), args.input_dir)

    successes: list[str] = []
    failures: list[tuple[str, str]] = []
    for record in records:
        lesson = process_record(
            record,
            appwrite_endpoint=args.appwrite_endpoint,
            appwrite_project=args.appwrite_project,
            appwrite_bucket=args.appwrite_bucket,
            appwrite_key=appwrite_key,
            bff_url=args.bff_url,
            bff_token=bff_token,
            whisper_model=args.model,
            whisper_device="cuda" if args.device == "auto" else args.device,
            whisper_compute=args.compute_type,
            engine=args.engine,
            out_dir=args.output_dir,
            skip_vocab=args.skip_vocab,
            require_confidence=args.require_confidence,
            min_confidence=args.min_confidence,
            continue_on_error=args.continue_on_error,
        )
        if lesson:
            successes.append(lesson["id"])
        else:
            failures.append((record["lesson_id"], "see logs"))

    logger.info("Done. %d succeeded, %d failed.", len(successes), len(failures))
    for lesson_id, reason in failures:
        logger.error("FAILED %s: %s", lesson_id, reason)
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())