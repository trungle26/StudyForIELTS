%%writefile generate_dictation_seed.py
#!/usr/bin/env python3
"""Generate dictation lessons from a folder of audio files (Colab)."""
from __future__ import annotations

import argparse
import json
import logging
import os
import re
import sys
import unicodedata
import uuid
from pathlib import Path
from typing import Any, Iterable

import httpx

AUDIO_SUFFIXES = {".mp3", ".wav", ".m4a", ".ogg", ".flac", ".webm", ".mp4"}
MAX_TRANSCRIPT_CHARS = 12_000
DEFAULT_MIN_CONFIDENCE = 0.65

logger = logging.getLogger("generate_dictation_seed")

def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode("ascii")
    text = re.sub(r"[^A-Za-z0-9]+", "-", text).strip("-").lower()
    truncated_text = text[:33]
    while truncated_text.endswith('-'):
        truncated_text = truncated_text[:-1]
    return truncated_text or "lesson"

def discover_lessons(input_dir: Path, recursive: bool = True) -> list[dict[str, Any]]:
    if not input_dir.is_dir():
        raise SystemExit(f"Input directory not found: {input_dir}")
    globber = input_dir.rglob if recursive else input_dir.glob
    records = []
    seen = set()
    for audio in sorted(globber("*")):
        if audio.suffix.lower() not in AUDIO_SUFFIXES or not audio.is_file():
            continue
        lesson_id = f"dd-{slugify(audio.stem)}"
        if lesson_id in seen: continue
        seen.add(lesson_id)
        records.append({
            "lesson_id": lesson_id,
            "title": audio.stem.replace("-", " ").replace("_", " ").strip().title() or lesson_id,
            "audio": audio,
        })
    return records

def appwrite_upload(endpoint: str, project: str, bucket: str, api_key: str, audio: Path, file_id: str) -> dict[str, Any]:
    url = f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files"
    data = {"fileId": file_id}
    headers = {"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}
    with audio.open("rb") as handle:
        files = {"file": (audio.name, handle, "application/octet-stream")}
        response = httpx.post(url, data=data, headers=headers, files=files, timeout=600)

    if response.status_code == 409:
        logger.info(f"  File {file_id} already exists in storage. Skipping upload.")
        return {"$id": file_id}

    if response.is_error:
        raise RuntimeError(f"Appwrite upload failed ({response.status_code}): {response.text[:500]}")
    return response.json()

def appwrite_audio_url(endpoint: str, project: str, bucket: str, file_id: str) -> str:
    return f"{endpoint.rstrip('/')}/storage/buckets/{bucket}/files/{file_id}/download?project={project}"

def transcribe_local(audio: Path, model_name: str, device: str, compute_type: str) -> Any:
    from faster_whisper import WhisperModel
    model = WhisperModel(model_name, device=device, compute_type=compute_type)
    segments, info = model.transcribe(str(audio), vad_filter=True)
    return type("Transcription", (), {"segments": list(segments), "duration": info.duration})()

def transcription_to_sentences(transcription: Any) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    segments = getattr(transcription, "segments", [])
    sentences, raw_segments = [], []
    for index, segment in enumerate(segments):
        text = str(getattr(segment, "text", "")).strip()
        start = int(round(float(getattr(segment, "start", 0)) * 1000))
        end = int(round(float(getattr(segment, "end", 0)) * 1000))
        if text and end > start:
            sentences.append({"orderIndex": index, "text": text, "startTimeMs": start, "endTimeMs": end})
            raw_segments.append({"start": float(getattr(segment, "start", 0)), "end": float(getattr(segment, "end", 0)), "text": text})
    return sentences, raw_segments

def classify_lesson(bff_url: str, token: str, title: str, transcript: str, segments: list[dict[str, Any]], duration: float | None) -> dict[str, Any]:
    headers = {"x-admin-token": token}
    payload = {"title": title, "transcript": transcript, "segments": segments, "durationSeconds": duration}
    res = httpx.post(f"{bff_url.rstrip('/')}/admin/dictation/classify", json=payload, headers=headers, timeout=60)
    res.raise_for_status()
    return res.json()["classification"]

def generate_vocabulary(bff_url: str, token: str, level: str, title: str, transcript: str) -> list[dict[str, Any]]:
    headers = {"x-admin-token": token}
    res = httpx.post(f"{bff_url.rstrip('/')}/admin/dictation/vocabulary", json={"level": level, "title": title, "transcript": transcript}, headers=headers, timeout=120)
    res.raise_for_status()
    return res.json().get("vocabularies", [])

def import_lesson(bff_url: str, token: str, lesson: dict[str, Any]) -> dict[str, Any]:
    headers = {"x-admin-token": token}
    res = httpx.post(f"{bff_url.rstrip('/')}/admin/dictation/import", json=lesson, headers=headers, timeout=60)
    res.raise_for_status()
    return res.json()

def publish_lesson(bff_url: str, token: str, lesson_id: str) -> None:
    headers = {"x-admin-token": token}
    httpx.patch(f"{bff_url.rstrip('/')}/admin/dictation/{lesson_id}/status", params={"status": "published"}, headers=headers, timeout=30).raise_for_status()

def process_record(record, **kwargs):
    try:
        uploaded = appwrite_upload(kwargs['appwrite_endpoint'], kwargs['appwrite_project'], kwargs['appwrite_bucket'], kwargs['appwrite_key'], record['audio'], record['lesson_id'])
        audio_url = appwrite_audio_url(kwargs['appwrite_endpoint'], kwargs['appwrite_project'], kwargs['appwrite_bucket'], uploaded['$id'])
        transcription = transcribe_local(record['audio'], kwargs['whisper_model'], kwargs['whisper_device'], kwargs['whisper_compute'])
        sentences, raw_segments = transcription_to_sentences(transcription)
        transcript_text = " ".join(s['text'] for s in sentences)[:MAX_TRANSCRIPT_CHARS]
        classif = classify_lesson(kwargs['bff_url'], kwargs['bff_token'], record['title'], transcript_text, raw_segments, transcription.duration)
        vocab = generate_vocabulary(kwargs['bff_url'], kwargs['bff_token'], classif['level'], record['title'], transcript_text)
        lesson = {"id": record['lesson_id'], "title": record['title'], "level": classif['level'], "sentences": sentences, "vocabularies": vocab, "audioUrl": audio_url, "classification": classif}
        import_lesson(kwargs['bff_url'], kwargs['bff_token'], lesson)
        publish_lesson(kwargs['bff_url'], kwargs['bff_token'], record['lesson_id'])
        kwargs['out_dir'].mkdir(parents=True, exist_ok=True)
        (kwargs['out_dir'] / f"{record['lesson_id']}.json").write_text(json.dumps(lesson, indent=2))
        logger.info(f"OK {record['lesson_id']}")
        return lesson
    except Exception as e:
        logger.error(f"FAIL {record['lesson_id']}: {e}")
        if not kwargs['continue_on_error']: raise
        return None

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, default=Path("dictation_seeds"))
    parser.add_argument("--appwrite-endpoint", required=True)
    parser.add_argument("--appwrite-project", required=True)
    parser.add_argument("--appwrite-bucket", required=True)
    parser.add_argument("--bff-url", required=True)
    parser.add_argument("--model", default="small")
    parser.add_argument("--device", default="auto")
    parser.add_argument("--compute-type", default="float16")
    parser.add_argument("--continue-on-error", action="store_true")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO)
    appwrite_key = os.getenv("APPWRITE_API_KEY", "")
    bff_token = os.getenv("ADMIN_TOKEN", "")
    records = discover_lessons(args.input_dir)
    for record in records:
        process_record(record, appwrite_endpoint=args.appwrite_endpoint, appwrite_project=args.appwrite_project, appwrite_bucket=args.appwrite_bucket, appwrite_key=appwrite_key, bff_url=args.bff_url, bff_token=bff_token, whisper_model=args.model, whisper_device="cuda" if args.device=="auto" else args.device, whisper_compute=args.compute_type, out_dir=args.output_dir, continue_on_error=args.continue_on_error)

if __name__ == "__main__":
    main()