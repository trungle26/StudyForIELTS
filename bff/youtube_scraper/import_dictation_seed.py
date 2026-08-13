#!/usr/bin/env python3
"""Import one generated dictation JSON document into the BFF."""
import argparse
import asyncio
import json
import os
from pathlib import Path

import httpx


async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("seed", type=Path)
    parser.add_argument("--base-url", default=os.getenv("BFF_BASE_URL", "http://127.0.0.1:8001"))
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    token = os.getenv("ADMIN_TOKEN", "")
    if not token:
        raise SystemExit("Set ADMIN_TOKEN first.")
    lesson = json.loads(args.seed.read_text(encoding="utf-8"))
    headers = {"x-admin-token": token}
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post(f"{args.base_url.rstrip('/')}/admin/dictation/import", json=lesson, headers=headers)
        response.raise_for_status()
        if args.publish:
            lesson_id = lesson["id"]
            response = await client.patch(
                f"{args.base_url.rstrip('/')}/admin/dictation/{lesson_id}/status",
                params={"status": "published"},
                headers=headers,
            )
            response.raise_for_status()
    print(f"Imported {lesson['id']} ({'published' if args.publish else 'draft'}).")


if __name__ == "__main__":
    asyncio.run(main())
