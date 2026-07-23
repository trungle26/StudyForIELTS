from functools import lru_cache
from os import getenv

from pydantic import Field
from pydantic.dataclasses import dataclass


def _csv_env(name: str, default: str = "") -> list[str]:
    return [item.strip() for item in getenv(name, default).split(",") if item.strip()]


@dataclass(frozen=True)
class Settings:
    mongodb_uri: str = Field(default_factory=lambda: getenv("MONGODB_URI", "").strip())
    mongodb_db_name: str = Field(default_factory=lambda: getenv("MONGODB_DB_NAME", "StudyForIELTS").strip())
    mongodb_collection: str = Field(default_factory=lambda: getenv("MONGODB_COLLECTION", "curatedvideos").strip())
    admin_token: str = Field(default_factory=lambda: getenv("ADMIN_TOKEN", "").strip())
    cors_allow_origins: list[str] = Field(default_factory=lambda: _csv_env("CORS_ALLOW_ORIGINS", "*") or ["*"])
    llm_api_key: str = Field(default_factory=lambda: getenv("LLM_API_KEY", "").strip())
    llm_base_url: str = Field(default_factory=lambda: getenv("LLM_BASE_URL", "").strip())
    llm_model: str = Field(default_factory=lambda: getenv("LLM_MODEL", "gpt-4o-mini").strip())
    llm_request_timeout_seconds: float = Field(
        default_factory=lambda: float(getenv("LLM_REQUEST_TIMEOUT_SECONDS", "180"))
    )
    llm_stream: bool = Field(
        default_factory=lambda: getenv("LLM_STREAM", "true").strip().lower() in {"1", "true", "yes", "on"}
    )
    # Cost per 1M tokens, in USD. Defaults are gpt-4o-mini list price; override
    # via env when routing through 9router to a different provider.
    input_token_cost_per_million: float = Field(
        default_factory=lambda: float(getenv("INPUT_TOKEN_COST_PER_M", "0.15"))
    )
    output_token_cost_per_million: float = Field(
        default_factory=lambda: float(getenv("OUTPUT_TOKEN_COST_PER_M", "0.60"))
    )
    # Rate limit (per client IP) on /writing/evaluate. Window is 1h via MongoDB TTL index.
    rate_limit_per_hour: int = Field(
        default_factory=lambda: int(getenv("RATE_LIMIT_PER_HOUR", "10"))
    )
    # Response cache TTL (seconds). 86400 = 24h.
    cache_ttl_seconds: int = Field(
        default_factory=lambda: int(getenv("CACHE_TTL_SECONDS", "86400"))
    )
    default_search_limit: int = Field(default_factory=lambda: int(getenv("DEFAULT_SEARCH_LIMIT", "10")))
    max_search_limit: int = Field(default_factory=lambda: int(getenv("MAX_SEARCH_LIMIT", "25")))
    feed_page_size: int = Field(default_factory=lambda: int(getenv("FEED_PAGE_SIZE", "20")))
    max_feed_page_size: int = Field(default_factory=lambda: int(getenv("MAX_FEED_PAGE_SIZE", "50")))
    youtube_request_timeout_seconds: int = Field(
        default_factory=lambda: int(getenv("YOUTUBE_REQUEST_TIMEOUT_SECONDS", "15"))
    )
    youtube_proxy_url: str = Field(default_factory=lambda: getenv("YOUTUBE_PROXY_URL", "").strip())
    youtube_transcript_proxy_provider: str = Field(
        default_factory=lambda: getenv("YOUTUBE_TRANSCRIPT_PROXY_PROVIDER", "").strip().lower()
    )
    youtube_transcript_proxy_url: str = Field(default_factory=lambda: getenv("YOUTUBE_TRANSCRIPT_PROXY_URL", "").strip())
    youtube_transcript_http_proxy_url: str = Field(
        default_factory=lambda: getenv("YOUTUBE_TRANSCRIPT_HTTP_PROXY_URL", "").strip()
    )
    youtube_transcript_https_proxy_url: str = Field(
        default_factory=lambda: getenv("YOUTUBE_TRANSCRIPT_HTTPS_PROXY_URL", "").strip()
    )
    webshare_proxy_username: str = Field(default_factory=lambda: getenv("WEBSHARE_PROXY_USERNAME", "").strip())
    webshare_proxy_password: str = Field(default_factory=lambda: getenv("WEBSHARE_PROXY_PASSWORD", "").strip())
    webshare_proxy_locations: list[str] = Field(default_factory=lambda: _csv_env("WEBSHARE_PROXY_LOCATIONS", "us"))
    webshare_retries_when_blocked: int = Field(default_factory=lambda: int(getenv("WEBSHARE_RETRIES_WHEN_BLOCKED", "10")))


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
