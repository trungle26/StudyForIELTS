# Learn FastAPI by reading this backend

This is a guided tour of the FastAPI patterns actually used in
`bff/youtube_scraper`. Each section pairs a FastAPI concept with the exact
file in this repo where you can see it. Read it top to bottom and you'll be
able to navigate and modify this backend on your own.

> Assumes you know Python and have used a web framework before. No prior
> FastAPI / async experience required.

## 0. What is FastAPI, in one paragraph

FastAPI is a Python web framework built on **Starlette** (HTTP/routing) and
**Pydantic** (data validation). You declare routes as Python functions with
type hints. FastAPI reads the hints and gives you, for free:

- request parsing and validation
- automatic JSON serialization
- a Swagger UI at `/docs`
- an OpenAPI schema at `/openapi.json`
- dependency injection
- an async event loop (built on `asyncio` and `uvicorn`)

The only "magic" is `Annotated` types and the `Depends()` system. Once those
click, the rest is just code.

## 1. The 10-second mental model of a request

```text
Client --HTTP--> Uvicorn (ASGI server) --calls--> FastAPI app
                                                     |
                                                     v
                                      lifespan startup ran earlier
                                                     |
                                                     v
                                        resolve Depends() tree
                                                     |
                                                     v
                                       parse path/query/header/body
                                                     |
                                                     v
                                        run your route function
                                                     |
                                                     v
                                  validate return value vs response_model
                                                     |
                                                     v
                                              send JSON to client
```

A few things to notice:

- The route function is plain Python. FastAPI is not a domain-specific
  language; it inspects your function signature and your return annotation.
- Validation and serialization happen at the edges. Inside the route, `level`
  is already a `Literal["A1", ...]`, not a raw string you still have to parse.

## 2. Run the app and look at it

```bash
cd bff/youtube_scraper
docker compose up --build
# API:   http://127.0.0.1:8001
# Docs:  http://127.0.0.1:8001/docs
# ReDoc: http://127.0.0.1:8001/redoc
# Mongo: http://127.0.0.1:8081
```

Two things to try first:

1. Open `/docs`. The whole API is clickable. Every model, every parameter, and
   every example is generated from the Python code. There is no separate
   schema file to maintain.
2. Open `/openapi.json`. That's the same data, in machine form. Useful when
   you want to generate an Android/Kotlin client.

## 3. The `app/` layout and what each file is for

```text
bff/youtube_scraper/
├── main.py                 # 1-line re-export: app.main:app  (uvicorn import target)
├── app/
│   ├── main.py             # The FastAPI() instance and lifespan
│   ├── core/
│   │   ├── config.py       # Settings dataclass reading env vars
│   │   ├── database.py     # Motor (async MongoDB) connect/close + DI helpers
│   │   └── security.py     # require_admin_token dependency
│   ├── models/             # Pydantic request/response contracts
│   │   ├── common.py
│   │   ├── feed.py
│   │   ├── youtube.py
│   │   ├── admin.py
│   │   └── writing.py
│   ├── routers/            # Thin HTTP layer
│   │   ├── health.py
│   │   ├── feed.py
│   │   ├── youtube.py
│   │   ├── admin.py
│   │   └── writing.py
│   └── services/           # The real work
│       ├── feed_service.py
│       ├── youtube_service.py
│       ├── admin_service.py
│       ├── cefr_classifier.py
│       └── llm_service.py
```

Rule of thumb (this is the whole architecture):

- **`models/`** = "what goes over the wire". Pydantic classes only.
- **`routers/`** = "what URL does what". Parse, call a service, return a model.
- **`services/`** = "what does the app actually do". Mongo, YouTube, LLM, etc.
- **`core/`** = "shared plumbing": settings, DB lifecycle, auth.

If you ever forget where to put new code: a new endpoint? **router**. A new
external system to talk to? **service**. A new field on the response?
**models**.

## 4. The `FastAPI()` instance and lifespan — `app/main.py`

```python
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.database import close_mongo, connect_mongo
from app.routers import admin, feed, health, writing, youtube
from app.routers.writing import WRITING_COLLECTION_NAME


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_mongo(app)
    writing_collection = app.state.mongo_db[WRITING_COLLECTION_NAME]
    await writing_collection.create_index([("created_at", -1)])
    try:
        yield
    finally:
        await close_mongo(app)


app = FastAPI(
    title="StudyForIELTS YouTube BFF",
    version="2.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allow_origins,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(feed.router)
app.include_router(youtube.router)
app.include_router(admin.router)
app.include_router(writing.router)
```

Concepts to internalize:

### `FastAPI(...)` — the application object

This is your app. You attach **routers**, **middleware**, **exception
handlers**, and a **lifespan** to it. That's it.

### `@asynccontextmanager` + `lifespan` — startup and shutdown hooks

Anything before `yield` runs **once** at startup. Anything after `yield` runs
**once** at shutdown. Use it for things that must live for the whole app:
database clients, ML models, thread pools, etc.

Here we:

- Open the MongoDB client and ping it (`connect_mongo`).
- Stash it on `app.state.mongo_db` so any request can grab it.
- Create the `writing_evaluations.created_at` index once.
- On shutdown, close the client.

In older FastAPI code you'll see `@app.on_event("startup")` and
`@app.on_event("shutdown")`. **Those are deprecated**; use `lifespan`.

### `app.state` — per-app storage

`app.state` is a plain attribute namespace that lives for the whole app. You
can attach anything to it during lifespan and read it later from a request
(`request.app.state.mongo_db`). This is how we pass the DB client to the rest
of the app without globals.

### `app.add_middleware(...)` — cross-cutting concerns

Middleware wraps every request. `CORSMiddleware` adds the
`Access-Control-Allow-*` headers browsers need. You could also add
`GZipMiddleware` for compression, or a custom one for logging / auth.

### `app.include_router(...)` — composition

Each module in `app/routers/` exposes an `APIRouter`. `include_router` mounts
it. You can give each router a `prefix` and a list of `tags` (used in `/docs`).

## 5. Your first route — `app/routers/health.py`

The smallest possible route, with everything spelled out:

```python
from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/")
async def root() -> dict[str, str]:
    return {
        "service": "studyforielts-youtube-bff",
        "docs": "/docs",
        "health": "/health",
    }
```

What to notice:

- `APIRouter` is a "mini FastAPI app". You register routes on it the same
  way, then `include_router` it from the main app. This is how big codebases
  stay readable.
- `tags=["health"]` groups routes in `/docs`.
- The return type (`dict[str, str]`) is used by FastAPI to build the response
  schema in `/docs`. It's not enforced as strictly as a Pydantic model, but it
  helps documentation.

## 6. Path params, query params, body, headers — `app/routers/feed.py`

```python
import math
from fastapi import APIRouter, Depends, HTTPException, Query
from motor.motor_asyncio import AsyncIOMotorCollection

from app.core.config import settings
from app.core.database import get_curated_videos
from app.models.feed import CEFRLevel, FeedDetailItem, FeedDetailResponse, FeedItem, FeedResponse
from app.services.feed_service import fetch_feed_page, fetch_feed_video

router = APIRouter(tags=["feed"])


@router.get("/feed", response_model=FeedResponse)
async def get_feed(
    level: CEFRLevel = Query(..., description="CEFR level: A1..C2."),
    page: int = Query(1, ge=1),
    limit: int = Query(settings.feed_page_size, ge=1, le=settings.max_feed_page_size),
    collection: AsyncIOMotorCollection = Depends(get_curated_videos),
) -> FeedResponse:
    total, items = await fetch_feed_page(collection, level=level, page=page, limit=limit)
    return FeedResponse(
        level=level,
        page=page,
        limit=limit,
        total=total,
        totalPages=math.ceil(total / limit) if total else 0,
        items=[FeedItem(**item) for item in items],
    )


@router.get("/feed/{video_id}", response_model=FeedDetailResponse)
async def get_feed_video(
    video_id: str,
    collection: AsyncIOMotorCollection = Depends(get_curated_videos),
) -> FeedDetailResponse:
    video = await fetch_feed_video(collection, video_id=video_id)
    if not video:
        raise HTTPException(status_code=404, detail="Video not found.")
    return FeedDetailResponse(video=FeedDetailItem(**video))
```

This file alone teaches you half of FastAPI. Let's go parameter by parameter.

### `response_model=FeedResponse`

Tells FastAPI:

- The response should be serialized as JSON.
- It must match `FeedResponse` (extra fields are dropped; missing fields cause
  a 500 if you typed it wrong).
- The OpenAPI schema is generated from `FeedResponse`.

`response_model` is the most important annotation on a route. If you don't
set it, FastAPI will serialize whatever you return, but the schema in `/docs`
will be loose.

### `Query(...)` — query string parameters

```python
level: CEFRLevel = Query(...)
page:  int       = Query(1, ge=1)
limit: int       = Query(settings.feed_page_size, ge=1, le=settings.max_feed_page_size)
```

- `Query(...)` (with the `...`) makes the parameter **required**. The three
  dots aren't a placeholder — they're `Ellipsis`, FastAPI's "you must send
  this" marker.
- `Query(1, ge=1)` defaults to `1` and must be `>= 1`. If a client sends
  `page=0`, FastAPI returns a 422 with a helpful error message before your
  function is called.
- `Query(default, ge=..., le=...)` adds both bounds in one go.

### Type-driven validation: `CEFRLevel`

In `app/models/feed.py`:

```python
from typing import Literal
CEFRLevel = Literal["A1", "A2", "B1", "B2", "C1", "C2"]
```

That's it. `Literal[...]` is a typing-language feature; FastAPI uses it to
restrict the allowed values. If a client sends `?level=Z9`, the request is
rejected with 422 before your code runs.

### `Depends(get_curated_videos)` — dependency injection

```python
collection: AsyncIOMotorCollection = Depends(get_curated_videos)
```

FastAPI sees this and:

1. Calls `get_curated_videos(...)`.
2. Whatever it returns is passed in as `collection`.
3. The dependency itself can `Depends(...)` on more dependencies.

Here's the dependency:

```python
# app/core/database.py
def get_curated_videos(request: Request) -> AsyncIOMotorCollection:
    return get_database(request)[settings.mongodb_collection]


def get_database(request: Request) -> AsyncIOMotorDatabase:
    return request.app.state.mongo_db
```

`request: Request` is a special type FastAPI injects automatically when it
appears in a `Depends`-resolved function. It gives you access to the current
HTTP request (headers, app state, etc.) without it being part of your route
signature.

### `raise HTTPException(status_code=404, detail="Video not found.")`

The standard way to bail out. FastAPI catches it, builds a JSON body of
`{"detail": "Video not found."}` with the right status code, and returns it
to the client. The OpenAPI schema also gets documented for it.

### Path parameters

```python
@router.get("/feed/{video_id}", ...)
async def get_feed_video(video_id: str, ...):
```

`{video_id}` in the path becomes a positional `video_id: str` in the
function. You can constrain it (`/feed/{video_id:str}` — less useful since
the type hint already does that) and add metadata with `Path(...)`.

## 7. Pydantic models — the contract

`app/models/feed.py`:

```python
from datetime import datetime
from typing import Literal
from pydantic import BaseModel, Field

CEFRLevel = Literal["A1", "A2", "B1", "B2", "C1", "C2"]


class FeedItem(BaseModel):
    id: str
    videoId: str
    title: str
    channelTitle: str = ""
    thumbnailUrl: str = ""
    durationSeconds: int | None = None
    publishDate: datetime | None = None
    level: CEFRLevel
    computedLevel: CEFRLevel | None = None
    confidence: float | None = None
    tags: list[str] = Field(default_factory=list)
    curatedAt: datetime | None = None


class FeedResponse(BaseModel):
    level: CEFRLevel
    page: int
    limit: int
    total: int
    totalPages: int
    items: list[FeedItem]
```

What Pydantic gives you here:

- **Validation** — incoming JSON must match. If `level` is `"Z9"`, the
  request fails with 422.
- **Defaults** — `channelTitle: str = ""` and `Field(default_factory=list)`
  handle missing fields. `default_factory=list` is used (not `= []`) because
  mutable defaults are shared across instances in Python.
- **Serialization** — `datetime` becomes an ISO string automatically. `ObjectId`
  doesn't — that's why `app/models/common.py::serialize_mongo_value` exists
  to recursively convert `ObjectId` → `str` before Pydantic sees the data.
- **Composition** — `FeedResponse` embeds `list[FeedItem]`. Pydantic builds
  the full schema recursively.
- **Inheritance** — `class FeedDetailItem(FeedItem):` adds
  `transcriptSegments` on top, reusing all the validation.

Request models are the same shape but typically with `Field(...)` for
required fields and constraint metadata (see `EssaySubmission` in
`app/models/writing.py`).

## 8. Settings, the easy way — `app/core/config.py`

```python
from functools import lru_cache
from os import getenv
from pydantic import Field
from pydantic.dataclasses import dataclass


def _csv_env(name: str, default: str = "") -> list[str]:
    return [item.strip() for item in getenv(name, default).split(",") if item.strip()]


@dataclass(frozen=True)
class Settings:
    mongodb_uri: str = Field(default_factory=lambda: getenv("MONGODB_URI", "").strip())
    llm_api_key: str = Field(default_factory=lambda: getenv("LLM_API_KEY", "").strip())
    # ... many more


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
```

Take-away patterns:

- **`pydantic.dataclasses.dataclass`** gives you a dataclass with Pydantic
  validation, so `int(getenv(...))` failures become clear errors.
- **`default_factory=lambda: getenv(...)`** reads the env var **at import
  time** (because the dataclass is built when the module is imported). That's
  fine for server processes; it's not fine for tests that mutate env vars.
- **`@lru_cache`** on the factory means `settings` is built once. Subsequent
  imports get the same instance.
- **`frozen=True`** prevents accidental mutation across the app.

For tests or reloads, you'd switch to reading env vars at call time, or use
`pydantic-settings`. For this app, the simple version is enough.

## 9. Database: Motor + `app.state` — `app/core/database.py`

```python
from fastapi import FastAPI, Request
from motor.motor_asyncio import AsyncIOMotorClient

from app.core.config import settings


async def connect_mongo(app: FastAPI) -> None:
    if not settings.mongodb_uri:
        raise RuntimeError("MONGODB_URI is required.")

    client = AsyncIOMotorClient(
        settings.mongodb_uri,
        appname="studyforielts-youtube-bff",
        serverSelectionTimeoutMS=8000,
    )
    await client.admin.command("ping")  # fail fast if creds are wrong

    app.state.mongo_client = client
    app.state.mongo_db = client[settings.mongodb_db_name]
    await app.state.mongo_db[settings.mongodb_collection].create_index("videoId", unique=True)
    await app.state.mongo_db[settings.mongodb_collection].create_index(
        [("level", 1), ("status", 1), ("curatedAt", -1)]
    )
```

What to learn here:

- **Motor is the async PyMongo.** Same query API, but every call returns an
  awaitable. That's why your route can do `await collection.find(...)` and
  not block the event loop.
- **One client, one DB handle, one collection handle.** Created in `lifespan`,
  stored on `app.state`, reused per request. Creating a new client per
  request would be a performance bug.
- **Indexes created at startup.** `create_index` is idempotent and cheap if
  the index already exists. Doing it here means the first real request
  doesn't pay the build cost.
- **`await client.admin.command("ping")` is a "fail fast" check.** If the
  URI is wrong, you want the app to crash on boot, not on the first request.

The DI helpers at the bottom of the file are how routes get the collection:

```python
def get_database(request: Request) -> AsyncIOMotorDatabase:
    return request.app.state.mongo_db


def get_curated_videos(request: Request) -> AsyncIOMotorCollection:
    return get_database(request)[settings.mongodb_collection]
```

You could skip `get_curated_videos` and use `get_database` directly, but
having one dependency per collection makes the routes short and explicit.

## 10. Auth, the simple version — `app/core/security.py`

```python
from fastapi import Header, HTTPException, status

from app.core.config import settings


async def require_admin_token(x_admin_token: str | None = Header(default=None)) -> None:
    if not settings.admin_token:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="ADMIN_TOKEN is not configured.",
        )
    if x_admin_token != settings.admin_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid admin token.",
        )
```

Used like this in `app/routers/admin.py`:

```python
@router.post(
    "/add-video",
    response_model=AddVideoResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin_token)],
)
async def add_video(request: AddVideoRequest, collection: ... = Depends(get_curated_videos)):
    ...
```

Three things to internalize:

- **`x_admin_token: str | None = Header(default=None)`** — FastAPI injects
  the request header named `x-admin-token` (Python variable name → header
  name: snake_case → kebab-case, and the `x_` prefix is fine in Python but
  the wire name is `X-Admin-Token`).
- **`dependencies=[Depends(require_admin_token)]`** at the route level runs
  the guard before the route body. The route body never sees the token; it
  just runs only if auth passed.
- **Returning `None`** from a dependency means "I'm a guard, I have nothing
  to inject."

For real production, replace this with OAuth2 / JWT / API keys. The
dependency pattern is the same — only the implementation changes.

## 11. Async vs sync: why `asyncio.to_thread` is everywhere

`yt-dlp` and `youtube-transcript-api` are **synchronous** Python libraries
that hit the network and can take several seconds. If you call them directly
from an `async def` route, **you block the event loop** and every other
request stalls.

`app/services/youtube_service.py` shows the standard fix:

```python
import asyncio
from youtube_transcript_api import YouTubeTranscriptApi
from yt_dlp import YoutubeDL

async def search_youtube(q: str, limit: int) -> SearchResponse:
    return await asyncio.to_thread(_search_youtube_sync, q, limit)


def _search_youtube_sync(q: str, limit: int) -> SearchResponse:
    # blocking calls here run on a worker thread, not the event loop
    with YoutubeDL({...}) as ydl:
        data = ydl.extract_info(f"ytsearch{limit}:{q}", download=False)
    ...
```

`asyncio.to_thread(fn, *args)` runs `fn` on a thread-pool worker and awaits
its return. The event loop keeps spinning, so other requests keep flowing.

Rule of thumb for async in FastAPI:

- If a library has an `async` API or returns awaitables, use it directly.
- If it's sync and I/O-bound, wrap with `asyncio.to_thread`.
- If it's CPU-bound, use `asyncio.to_thread` or a `ProcessPoolExecutor` so
  you don't block the loop even for non-I/O work.

## 12. Background work — `BackgroundTasks` in `app/routers/youtube.py`

```python
@router.get("/search", response_model=SearchResponse)
async def search(
    request: Request,
    background_tasks: BackgroundTasks,
    q: str = Query(..., min_length=1, max_length=120),
    limit: int = Query(settings.default_search_limit, ge=1, le=settings.max_search_limit),
) -> SearchResponse:
    response = await search_youtube(q=q, limit=limit)
    for result in response.results:
        background_tasks.add_task(_curate_search_result, request.app, result.videoId)
    return response
```

`BackgroundTasks` is a built-in dependency. Whatever you `add_task(...)` runs
**after** the response is sent, on the same event loop. It's perfect for
"do this eventually but don't make the user wait" — like fire-and-forget
curation in the search endpoint.

It is **not** a queue. It is **not** durable. If the process dies between
sending the response and running the task, the task is lost. For durable
work, use a real task queue (Celery, RQ, Arq). For "best-effort follow-ups",
`BackgroundTasks` is exactly right.

## 13. Hiding routes from `/docs` — legacy compatibility

```python
@router.get("/api/youtube/search", response_model=SearchResponse, include_in_schema=False)
async def legacy_search(...): ...
```

`include_in_schema=False` keeps the route callable (your old Android client
can still hit it) but removes it from `/docs` and `/openapi.json`. Useful
during a migration window.

## 14. Errors and OpenAPI documentation

```python
raise HTTPException(
    status_code=502,
    detail="YouTube blocked transcript access from this network. Configure a rotating proxy for production.",
)
```

`HTTPException` is the only error you need. The body is
`{"detail": "<message>"}` by default; for structured errors, pass
`detail={"error": "...", "code": "..."}` and it'll be JSON-encoded.

In `app/routers/writing.py`, errors are translated carefully:

```python
try:
    evaluation = await evaluate_essay_with_ai(...)
except RuntimeError as e:
    raise HTTPException(status_code=502, detail=f"LLM evaluation failed: {e}") from e
```

- LLM failure → 502 (bad gateway; upstream is the LLM provider).
- DB failure → 500 (our problem).
- Validation failure → 422 (FastAPI does this for you before your code runs).

The `from e` preserves the original exception's traceback for server logs.

## 15. Where to go from here

Suggested exercises, in order of value:

1. **Add a new endpoint** that returns the 10 most recently curated videos
   across all levels, ignoring the level filter. Hint: new route in
   `app/routers/feed.py`, new service function in `app/services/feed_service.py`,
   new Pydantic model or reuse `FeedResponse`. Mirror the existing pattern.
2. **Tighten CORS.** Change `CORS_ALLOW_ORIGINS=*` in `.env` to your
   frontend's real origin and observe `/docs` start sending CORS headers.
3. **Swap the admin token for OAuth2.** Keep `require_admin_token` as the
   dependency name; replace its body. The route doesn't change.
4. **Add a real task queue** for curation. Replace
   `background_tasks.add_task(_curate_search_result, ...)` with an Arq /
   RQ enqueue. The route still doesn't change.
5. **Write one test** with FastAPI's `TestClient`. Hint:
   `from fastapi.testclient import TestClient`; the test does not need a
   real Mongo if you override the `get_curated_videos` dependency with
   `app.dependency_overrides[...]`.

## 16. Cheat sheet

| Concept | Where you see it in this repo |
| --- | --- |
| `FastAPI()` app | `app/main.py` |
| `lifespan` startup/shutdown | `app/main.py::lifespan` |
| `app.state.*` | `app/core/database.py` (set) + `get_database` (read) |
| `APIRouter` + `include_router` | every file in `app/routers/`, called from `app/main.py` |
| `response_model=` | every `@router.<verb>(...)` in routers |
| `Query(...)`, `Header(default=...)`, `Path(...)` | `app/routers/feed.py`, `app/routers/youtube.py`, `app/core/security.py` |
| `Depends(...)` (DI) | `app/routers/feed.py`, `app/routers/admin.py`, `app/routers/writing.py` |
| `dependencies=[Depends(...)]` (route guard) | `app/routers/admin.py` |
| `HTTPException(...)` | `app/routers/feed.py`, `app/routers/youtube.py`, `app/routers/writing.py`, `app/services/youtube_service.py` |
| `BackgroundTasks` | `app/routers/youtube.py::search` and `legacy_search` |
| `asyncio.to_thread(...)` | `app/services/youtube_service.py` |
| Pydantic `BaseModel`, `Field`, `Literal[...]` | `app/models/*.py` |
| `include_in_schema=False` | `app/routers/youtube.py` (legacy routes) |
| `CORSMiddleware` | `app/main.py` |
| `lru_cache` on settings factory | `app/core/config.py` |

When in doubt, follow the path: `models/` defines the shape, `routers/`
defines the URL, `services/` does the work, `core/` wires the rest.