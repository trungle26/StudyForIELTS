#!/usr/bin/env python3
"""Generate a dictation lesson JSON file from an audio file using Whisper.

Usage:
    OPENAI_API_KEY=... python generate_dictation_seed.py \
        --config dictation_lessons.yaml --lesson-id dd-short-story-001 \
        --audio ./audio/short-story.mp3 --output ./dictation_lesson.json

The YAML file is intentionally JSON-compatible, so this utility has no extra
configuration-parser dependency. Install PyYAML only if you want ordinary YAML
syntax (the script will detect and use it when available).
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import Any

import httpx

from openai import OpenAI


def load_config(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8")
    try:
        value = json.loads(text)
    except json.JSONDecodeError:
        try:
            import yaml  # type: ignore[import-not-found]
        except ImportError as exc:
            raise SystemExit(
                f"{path} is not JSON-compatible YAML; install PyYAML or use JSON syntax: {exc}"
            ) from exc
        value = yaml.safe_load(text)
    if isinstance(value, dict):
        value = value.get("lessons")
    if not isinstance(value, list) or not all(isinstance(item, dict) for item in value):
        raise SystemExit("Config must contain a list of lessons or a {lessons: [...]} object.")
    return value


def make_lesson(metadata: dict[str, Any], lesson_id: str, transcription: Any) -> dict[str, Any]:
    segments = getattr(transcription, "segments", None) or []
    sentences = []
    for index, segment in enumerate(segments):
        text = str(getattr(segment, "text", "")).strip()
        start = int(round(float(getattr(segment, "start", 0)) * 1000))
        end = int(round(float(getattr(segment, "end", 0)) * 1000))
        if text and end > start:
            sentences.append({"orderIndex": index, "text": text, "startTimeMs": start, "endTimeMs": end})
    if not sentences:
        raise SystemExit("Whisper returned no usable timestamped segments.")
    lesson = {**metadata, "id": lesson_id, "sentences": sentences}
    duration = getattr(transcription, "duration", None)
    if duration is not None:
        lesson["durationSeconds"] = max(1, int(round(float(duration))))
    lesson.setdefault("vocabularies", [])
    return lesson


def transcribe_local(audio: Path, model_name: str, device: str, compute_type: str) -> Any:
    from faster_whisper import WhisperModel

    model = WhisperModel(model_name, device=device, compute_type=compute_type)
    segments, info = model.transcribe(str(audio), vad_filter=True)
    return type("Transcription", (), {"segments": list(segments), "duration": info.duration})()


def upload_lesson(lesson: dict[str, Any], base_url: str, token: str, publish: bool) -> None:
    headers = {"x-admin-token": token}
    with httpx.Client(timeout=60) as client:
        response = client.post(f"{base_url.rstrip('/')}/admin/dictation/import", json=lesson, headers=headers)
        response.raise_for_status()
        if publish:
            response = client.patch(
                f"{base_url.rstrip('/')}/admin/dictation/{lesson['id']}/status",
                params={"status": "published"}, headers=headers,
            )
            response.raise_for_status()


def appwrite_files(endpoint: str, project: str, bucket: str, api_key: str) -> list[dict[str, Any]]:
    response = httpx.get(
        f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files",
        params={"project": project, "limit": 100},
        headers={"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}, timeout=60,
    )
    response.raise_for_status()
    return response.json().get("files", [])


def download_appwrite_file(endpoint: str, project: str, bucket: str, file_id: str, output: Path, api_key: str) -> None:
    response = httpx.get(
        f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files/{file_id}/download",
        params={"project": project},
        headers={"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}, timeout=300,
    )
    response.raise_for_status()
    output.write_bytes(response.content)


def appwrite_audio_url(endpoint: str, project: str, bucket: str, file_id: str) -> str:
    return f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files/{file_id}/download?project={project}"


def sync_appwrite(args: argparse.Namespace) -> None:
    api_key = os.getenv("APPWRITE_API_KEY")
    if not api_key:
        raise SystemExit("Set APPWRITE_API_KEY in Colab Secrets first.")
    entries = {str(item.get("id")): item for item in load_config(args.config)}
    files = appwrite_files(args.appwrite_endpoint, args.appwrite_project, args.appwrite_bucket, api_key)
    audio_dir = args.audio_dir
    audio_dir.mkdir(parents=True, exist_ok=True)
    for file in files:
        if Path(file["name"]).suffix.lower() not in {".mp3", ".wav", ".m4a", ".ogg"}:
            continue
        lesson_id = Path(file["name"]).stem
        metadata = dict(entries.get(lesson_id, {
            "title": lesson_id.replace("-", " ").title(), "level": args.level,
            "source": "original", "sourceUrl": "", "licenseNote": "Use only licensed audio.",
        }))
        metadata.pop("id", None)
        metadata["audioUrl"] = appwrite_audio_url(args.appwrite_endpoint, args.appwrite_project, args.appwrite_bucket, file["$id"])
        audio = audio_dir / file["name"]
        download_appwrite_file(args.appwrite_endpoint, args.appwrite_project, args.appwrite_bucket, file["$id"], audio, api_key)
        transcription = transcribe_local(audio, args.model, "cuda" if args.device == "auto" else args.device, args.compute_type)
        lesson = make_lesson(metadata, lesson_id, transcription)
        entries[lesson_id] = lesson
        if args.bff_url:
            token = os.getenv("ADMIN_TOKEN")
            if not token:
                raise SystemExit("Set ADMIN_TOKEN before using --bff-url.")
            upload_lesson(lesson, args.bff_url, token, args.publish)
        print(f"Processed {file['name']}")
    args.config.write_text(json.dumps(list(entries.values()), indent=2) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=Path("dictation_lessons.yaml"))
    parser.add_argument("--lesson-id")
    parser.add_argument("--audio", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--model", default="small")
    parser.add_argument("--engine", choices=("openai", "local"), default="local")
    parser.add_argument("--device", choices=("auto", "cuda", "cpu"), default="auto")
    parser.add_argument("--compute-type", default="float16")
    parser.add_argument("--bff-url")
    parser.add_argument("--publish", action="store_true")
    parser.add_argument("--appwrite-endpoint")
    parser.add_argument("--appwrite-project")
    parser.add_argument("--appwrite-bucket")
    parser.add_argument("--audio-dir", type=Path, default=Path("audio"))
    parser.add_argument("--level", default="B1")
    args = parser.parse_known_args(argv)[0]
    if args.appwrite_endpoint:
        if not all((args.appwrite_project, args.appwrite_bucket)):
            raise SystemExit("--appwrite-project and --appwrite-bucket are required with --appwrite-endpoint.")
        sync_appwrite(args)
        return
    if not args.lesson_id or not args.audio or not args.output:
        raise SystemExit(
            "Provide --appwrite-endpoint for batch mode, or provide --lesson-id, --audio, and --output."
        )
    if not args.audio.is_file():
        raise SystemExit(f"Audio file not found: {args.audio}")
    entries = {str(item.get("id")): item for item in load_config(args.config)}
    if args.lesson_id not in entries:
        raise SystemExit(f"Lesson id not found in config: {args.lesson_id}")
    metadata = dict(entries[args.lesson_id])
    metadata.pop("id", None)
    if args.engine == "openai":
        if not os.getenv("OPENAI_API_KEY"):
            raise SystemExit("Set OPENAI_API_KEY first.")
        with args.audio.open("rb") as audio:
            transcription = OpenAI().audio.transcriptions.create(
                model=args.model, file=audio, response_format="verbose_json", timestamp_granularities=["segment"]
            )
    else:
        device = "cuda" if args.device == "auto" else args.device
        transcription = transcribe_local(args.audio, args.model, device, args.compute_type)
    lesson = make_lesson(metadata, args.lesson_id, transcription)
    args.output.write_text(json.dumps(lesson, indent=2) + "\n", encoding="utf-8")
    if args.bff_url:
        token = os.getenv("ADMIN_TOKEN")
        if not token:
            raise SystemExit("Set ADMIN_TOKEN before using --bff-url.")
        upload_lesson(lesson, args.bff_url, token, args.publish)
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
