"""Bulk-populate the curatedvideos feed by hitting the running BFF.

Usage:
    python populate_feed.py --base-url http://127.0.0.1:8001 --admin-token "$ADMIN_TOKEN"

For each query below, hits GET /search (which auto-curates results in the
background), then also explicitly POSTs /admin/add-video for the first few
results to guarantee a published document even if the background task is slow.
"""

from __future__ import annotations

import argparse
import asyncio
import os
import sys
from typing import Any

import httpx


DEFAULT_QUERIES: list[str] = [
    "ielts listening practice test",
    "ielts speaking band 9 sample",
    "ielts writing task 2 tips",
    "ielts reading practice",
    "ielts vocabulary advanced",
    "british council ielts preparation",
    "ielts listening tips for band 9",
    "ielts speaking cue card",
    "ielts academic writing task 1",
    "ielts general training reading",
    "english listening comprehension intermediate",
    "english podcast for ielts learners",
    "ted talk english learners",
    "bbc learning english",
    "daily english conversation practice",
    "english pronunciation practice",
    "english grammar for ielts",
    "academic english vocabulary",
    "english idioms and phrases",
    "english collocations for ielts",
    "ielts mock test full",
    "ielts listening section 1 2 3 4",
    "ielts reading true false not given",
    "ielts writing task 2 essay structure",
    "ielts speaking part 1 questions",
]


async def _search(client: httpx.AsyncClient, base_url: str, query: str, limit: int) -> list[dict[str, Any]]:
    response = await client.get(f"{base_url}/search", params={"q": query, "limit": limit})
    response.raise_for_status()
    payload = response.json()
    return payload.get("results") or []


async def _curate(client: httpx.AsyncClient, base_url: str, token: str, video_id: str, tags: list[str]) -> bool:
    response = await client.post(
        f"{base_url}/admin/add-video",
        headers={"x-admin-token": token},
        json={"videoId": video_id, "language": "en", "tags": tags, "status": "published"},
    )
    if response.status_code in (200, 201):
        return True
    print(f"  ! {video_id} -> {response.status_code} {response.text[:120]}")
    return False


async def populate(base_url: str, token: str, queries: list[str], per_query: int, only_search: bool) -> None:
    total_curated = 0
    seen: set[str] = set()
    async with httpx.AsyncClient(timeout=120.0) as client:
        for query in queries:
            print(f">> {query}")
            try:
                results = await _search(client, base_url, query, per_query)
            except Exception as exc:
                print(f"  search failed: {exc}")
                continue

            for result in results:
                video_id = result.get("videoId")
                if not video_id or video_id in seen:
                    continue
                seen.add(video_id)

                if only_search:
                    print(f"  + {video_id} (via search background task)")
                    total_curated += 1
                    continue

                ok = await _curate(client, base_url, token, video_id, tags=[query.split()[0].lower()])
                if ok:
                    print(f"  + {video_id}")
                    total_curated += 1

            await asyncio.sleep(0.5)  # ponytail: be polite to YouTube + your DB

    print(f"\nDone. Processed {total_curated} unique videos across {len(queries)} queries.")


def main() -> int:
    parser = argparse.ArgumentParser(description="Populate the curatedvideos feed via the BFF.")
    parser.add_argument("--base-url", default=os.environ.get("BFF_BASE_URL", "http://127.0.0.1:8001"))
    parser.add_argument("--admin-token", default=os.environ.get("ADMIN_TOKEN", ""))
    parser.add_argument("--per-query", type=int, default=5, help="Results per search query (max 25).")
    parser.add_argument("--only-search", action="store_true", help="Skip explicit /admin/add-video calls; rely on background tasks only.")
    args = parser.parse_args()

    if not args.only_search and not args.admin_token:
        print("ERROR: pass --admin-token or set ADMIN_TOKEN (or use --only-search).", file=sys.stderr)
        return 2

    asyncio.run(populate(args.base_url, args.admin_token, DEFAULT_QUERIES, args.per_query, args.only_search))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())