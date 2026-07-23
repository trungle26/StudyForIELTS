"""Per-client-IP rate limiting backed by a MongoDB TTL collection.

Each call inserts a row with ``created_at = now``; a TTL index on that field
(Mongo-side) expires the row after 1 hour. The dependency counts the rows for
the calling IP within the window and rejects with HTTP 429 when the configured
per-hour limit is exceeded. No Redis, no background sweeper.

ponytail: a single document per request (not a counter) is fine for current
traffic; switch to a single counter doc + ``$inc`` once volume justifies it.
"""
import logging
from datetime import datetime, timezone

from fastapi import HTTPException, Request, status

from app.core.config import settings

logger = logging.getLogger(__name__)

RATE_LIMITS_COLLECTION = "rate_limits"


async def check_rate_limit(request: Request) -> None:
    """Reject the request if the client IP has exceeded ``RATE_LIMIT_PER_HOUR``."""
    client_ip = request.client.host if request.client else "unknown"
    db = request.app.state.mongo_db
    collection = db[RATE_LIMITS_COLLECTION]

    # Count + insert. We deliberately do this as two ops (not a transaction) —
    # a tiny race window where two parallel requests both pass the check is
    # acceptable; the user just gets one extra request.
    count = await collection.count_documents({"client_ip": client_ip})
    if count >= settings.rate_limit_per_hour:
        logger.info("Rate limit hit for %s (%d/%d)", client_ip, count, settings.rate_limit_per_hour)
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="rate_limit_exceeded",
        )

    await collection.insert_one(
        {"client_ip": client_ip, "created_at": datetime.now(timezone.utc)}
    )
