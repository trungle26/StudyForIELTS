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
