#!/usr/bin/env python3
"""Seed writing lessons from seed_writing_lessons.json into the BFF.

Usage:
    ADMIN_TOKEN=xxx python seed_writing_lessons.py
    ADMIN_TOKEN=xxx python seed_writing_lessons.py --base-url https://studyforielts-youtube-bff.onrender.com
"""
import asyncio
import json
import os
import sys
from pathlib import Path

import httpx

BASE_URL = os.getenv("BFF_BASE_URL", "http://127.0.0.1:8001")
TOKEN = os.getenv("ADMIN_TOKEN", "")
SEED_FILE = Path(__file__).with_name("seed_writing_lessons.json")


async def main() -> None:
    if "--base-url" in sys.argv:
        base = sys.argv[sys.argv.index("--base-url") + 1]
    else:
        base = BASE_URL

    if not TOKEN:
        sys.exit("Set ADMIN_TOKEN env var first.")

    raw_lessons = json.loads(SEED_FILE.read_text())
    lessons = []
    for lesson in raw_lessons:
        # Skip lessons that still have placeholder copy so a half-filled
        # seed file doesn't end up publishing stubs to the DB.
        if "PASTE THE" in str(lesson.get("task_prompt", "")):
            print(f"  [skip] placeholder task_prompt: {lesson.get('task_type')}")
            continue
        lessons.append(lesson)
    print(f"Loading {len(lessons)} lessons → {base}")

    async with httpx.AsyncClient(timeout=30) as client:
        for i, lesson in enumerate(lessons, 1):
            image_path = lesson.pop("image_path", None)
            resolved_image = None
            if image_path:
                p = Path(image_path)
                resolved_image = p if p.is_absolute() else (SEED_FILE.parent / p)

            # Read bytes up front so the file handle is closed before we
            # hand the multipart payload to httpx (httpx probes file length
            # lazily, after `with open(...)` would otherwise exit).
            image_bytes: bytes | None = None
            image_filename: str | None = None
            image_content_type: str | None = None
            if resolved_image is not None:
                if not resolved_image.exists():
                    print(f"  [!] image_path set but file missing: {resolved_image}")
                else:
                    image_bytes = resolved_image.read_bytes()
                    image_filename = resolved_image.name
                    if image_filename.lower().endswith(".png"):
                        image_content_type = "image/png"
                    elif image_filename.lower().endswith((".jpg", ".jpeg")):
                        image_content_type = "image/jpeg"

            data = {
                k: (json.dumps(v) if isinstance(v, list) else str(v))
                for k, v in lesson.items()
            }

            files: dict[str, tuple[str, bytes, str]] | None = None
            if image_bytes is not None and image_filename and image_content_type:
                files = {"image": (image_filename, image_bytes, image_content_type)}

            resp = await client.post(
                f"{base.rstrip('/')}/admin/writing-lessons",
                headers={"x-admin-token": TOKEN},
                data=data,
                files=files,
            )
            ok = resp.status_code in (200, 201)
            mark = "✓" if ok else "✗"
            print(
                f"  [{i}/{len(lessons)}] {mark} {resp.status_code} — "
                f"{lesson['task_type']}: {lesson['task_prompt'][:60]}…"
            )
            if not ok:
                print(f"    {resp.text[:200]}")

    print("Done.")


if __name__ == "__main__":
    asyncio.run(main())
