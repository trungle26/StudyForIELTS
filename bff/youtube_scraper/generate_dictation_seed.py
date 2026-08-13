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
import sys
from pathlib import Path
from typing import Any

from openai import OpenAI


def load_config(path: Path) -> list[dict[str, Any]]:
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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--lesson-id", required=True)
    parser.add_argument("--audio", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--model", default="whisper-1")
    args = parser.parse_args()
    if not os.getenv("OPENAI_API_KEY"):
        raise SystemExit("Set OPENAI_API_KEY first.")
    if not args.audio.is_file():
        raise SystemExit(f"Audio file not found: {args.audio}")

    entries = {str(item.get("id")): item for item in load_config(args.config)}
    if args.lesson_id not in entries:
        raise SystemExit(f"Lesson id not found in config: {args.lesson_id}")
    metadata = dict(entries[args.lesson_id])
    metadata.pop("id", None)
    client = OpenAI()
    with args.audio.open("rb") as audio:
        transcription = client.audio.transcriptions.create(
            model=args.model, file=audio, response_format="verbose_json", timestamp_granularities=["segment"]
        )
    args.output.write_text(json.dumps(make_lesson(metadata, args.lesson_id, transcription), indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
