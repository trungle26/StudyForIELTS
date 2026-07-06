# StudyForIELTS FastAPI BFF

FastAPI backend for the Android app's YouTube dictation experience and the AI
writing tutor. It searches YouTube, fetches transcripts, classifies transcript
difficulty into CEFR levels, stores curated videos in MongoDB, serves a paginated
feed to the Android app, and evaluates IELTS essays through an OpenAI-compatible
LLM.

> New to FastAPI? Read [`LEARN_FASTAPI.md`](./LEARN_FASTAPI.md) for a tutorial
> that walks through this codebase.

## Architecture

```text
bff/youtube_scraper/
  main.py                         # Uvicorn import target: main:app (re-exports app.main:app)
  populate_feed.py                # Bulk-seed the feed by hitting the running BFF
  app/main.py                     # FastAPI app wiring and lifespan
  app/core/config.py              # Environment-backed Settings dataclass
  app/core/database.py            # Motor client lifecycle + DI helpers
  app/core/security.py            # require_admin_token dependency
  app/models/                     # Pydantic request/response contracts
    common.py                     # serialize_mongo_value (ObjectId -> str)
    feed.py                       # FeedItem, FeedResponse, FeedDetailResponse, CEFRLevel
    youtube.py                    # SearchResponse, TranscriptResponse
    admin.py                      # AddVideoRequest/Response, AdminVideo
    writing.py                    # EssaySubmission, WritingEvaluation, WritingEvaluationDB
  app/routers/                    # Thin HTTP layer
    health.py                     # GET /, GET /health
    feed.py                       # GET /feed, GET /feed/{videoId}
    youtube.py                    # GET /search, GET /transcript (+ legacy /api/youtube/*)
    admin.py                      # POST /admin/add-video  (token-guarded)
    writing.py                    # POST /writing/evaluate
  app/services/                   # Real work lives here
    feed_service.py               # Mongo queries, projection, pagination
    youtube_service.py            # yt-dlp + youtube-transcript-api (runs in to_thread)
    admin_service.py              # Upsert curated videos + CEFR classification
    cefr_classifier.py            # Local readability-based CEFR estimator
    llm_service.py                # OpenAI-compatible essay evaluator (9router / OpenAI)
  Dockerfile                      # python:3.11-slim, non-root, uvicorn (no --reload)
  docker-compose.yml              # mongo + mongo-express + app, with --reload
  vercel.json                     # Optional Vercel function config
  requirements.txt                # Python dependencies
  .env.example                    # Copy to .env and fill in
  LEARN_FASTAPI.md                # FastAPI tutorial tied to this codebase
```

FastAPI owns the HTTP layer. Motor owns one async MongoDB client for the whole
application lifespan: connect on startup, reuse per request, close on shutdown.
Service modules contain the real work so routers stay small and easy to test.
Pydantic models keep the API contract explicit for your Android client.

## Endpoints

| Method | Path | Purpose | Notes |
| --- | --- | --- | --- |
| GET | `/` | Service banner with links to `/docs` and `/health` | |
| GET | `/health` | Liveness check | Returns `{"status":"ok"}` |
| GET | `/feed?level=B2&page=1&limit=20` | Paginated curated feed | `level` required (A1..C2); `limit` clamped to `MAX_FEED_PAGE_SIZE` |
| GET | `/feed/{videoId}` | One curated video with transcript segments | 404 if not in curated set |
| GET | `/search?q=...&limit=5` | YouTube search via `yt-dlp` | Auto-curates each result in a background task |
| GET | `/transcript?videoId=...&language=en` | Transcript segments for a video | Proxies through optional Webshare config |
| POST | `/admin/add-video` | Fetch + classify + upsert one curated video | Header `x-admin-token: $ADMIN_TOKEN` |
| POST | `/writing/evaluate` | LLM-band essay evaluation, persisted to Mongo | Body: `{task_prompt, essay_text}` |

Legacy routes kept for the earlier Android integration (hidden from `/docs`):

| Method | Path | Forwards to |
| --- | --- | --- |
| GET | `/api/youtube/search?q=...&limit=5` | `/search` |
| GET | `/api/youtube/captions/{videoId}?language=en` | `/transcript` |

OpenAPI / Swagger UI:

```text
http://127.0.0.1:8001/docs
http://127.0.0.1:8001/redoc
```

## Data population approach

For internal testing, `POST /admin/add-video` with an admin token is the
recommended curation workflow. The backend does the expensive and repeatable
work: fetching transcript data, fetching metadata, estimating CEFR level,
applying an optional manual level override, and upserting the final curated
document.

This is better than manually editing MongoDB documents because it keeps the
document shape consistent and lets you repeat the same workflow locally, on
Render, or from a future admin UI. Later, replace the single shared token with
proper admin accounts/roles if this becomes a public or multi-person production
tool.

### Bulk-seed helper

`populate_feed.py` hits the running BFF on 25 default IELTS-related queries,
relies on the search background task, and (optionally) explicitly POSTs
`/admin/add-video` for the first few results to guarantee published documents:

```bash
python populate_feed.py --base-url http://127.0.0.1:8001 --admin-token "$ADMIN_TOKEN"
# Or rely only on /search background tasks:
python populate_feed.py --only-search
```

## Environment

Create your local `.env` from the example:

```bash
cd bff/youtube_scraper
cp .env.example .env
```

Edit `.env` and set at least:

```text
MONGODB_URI=mongodb://studyforielts:studyforielts_local_password@localhost:27017/StudyForIELTS?authSource=admin
BFF_MONGODB_URI=mongodb://studyforielts:studyforielts_local_password@mongo:27017/StudyForIELTS?authSource=admin
MONGODB_DB_NAME=StudyForIELTS
MONGODB_COLLECTION=curatedvideos
ADMIN_TOKEN=replace-with-a-long-random-secret
BFF_HOST_PORT=8001
MONGO_HOST_PORT=27017
MONGO_ROOT_USERNAME=studyforielts
MONGO_ROOT_PASSWORD=studyforielts_local_password
MONGO_EXPRESS_HOST_PORT=8081
MONGO_EXPRESS_USERNAME=admin
MONGO_EXPRESS_PASSWORD=admin
CORS_ALLOW_ORIGINS=*
LLM_API_KEY=your-9router-or-openai-api-key
LLM_BASE_URL=https://api.9router.com/v1
LLM_MODEL=gpt-4o-mini
DEFAULT_SEARCH_LIMIT=10
MAX_SEARCH_LIMIT=25
FEED_PAGE_SIZE=20
MAX_FEED_PAGE_SIZE=50
YOUTUBE_REQUEST_TIMEOUT_SECONDS=15
YOUTUBE_PROXY_URL=
YOUTUBE_TRANSCRIPT_PROXY_PROVIDER=
YOUTUBE_TRANSCRIPT_PROXY_URL=
YOUTUBE_TRANSCRIPT_HTTP_PROXY_URL=
YOUTUBE_TRANSCRIPT_HTTPS_PROXY_URL=
WEBSHARE_PROXY_USERNAME=
WEBSHARE_PROXY_PASSWORD=
WEBSHARE_PROXY_LOCATIONS=us
WEBSHARE_RETRIES_WHEN_BLOCKED=10
```

Notes:

- `curatedvideos` matches the existing collection name used by the legacy
  Node BFF; change `MONGODB_COLLECTION` only if your MongoDB collection has a
  different name.
- For local Docker development, Compose starts MongoDB for you. The FastAPI
  container uses `BFF_MONGODB_URI` and connects to MongoDB by Docker service
  hostname `mongo`.
- If you run Python directly on your host instead of through Docker, use
  `MONGODB_URI` with `127.0.0.1:27017` or `localhost:27017` because then the
  app is outside the Docker network.
- For Atlas or Render, use your real Atlas URI in `MONGODB_URI` and Render
  environment variables. Do not use `mongo:27017` outside Docker Compose.
- `CORS_ALLOW_ORIGINS` is parsed as a comma-separated list. `*` allows the
  Android emulator and any browser tool. Tighten it for production.
- LLM settings are only required if you hit `/writing/evaluate`; missing
  `LLM_API_KEY` fails at request time with a clear error.

## Run with Docker Compose

Build and start the local development stack:

```bash
docker compose up --build
```

The API runs at:

```text
http://127.0.0.1:8001
```

Mongo Express runs at:

```text
http://127.0.0.1:8081
```

Default Mongo Express login:

```text
username: admin
password: admin
```

The compose file mounts this folder into `/app` and starts Uvicorn with
`--reload`, so code changes restart the server automatically.

Docker maps a host port to a container port. In this project the container
still listens on `8000`, but your computer exposes it as `BFF_HOST_PORT`,
which defaults to `8001`. If port `8001` is also busy, change `BFF_HOST_PORT`
in `.env`, for example `BFF_HOST_PORT=8010`.

Docker containers also resolve each other by service name. That is why the
FastAPI container uses `mongo:27017`, while tools running directly on your
machine use `127.0.0.1:27017`.

Stop the server:

```bash
docker compose down
```

## Run without Docker (host machine)

The project targets Python 3.11 in the Dockerfile but `.python-version` says
3.12; both work because the only stdlib/typing features used are 3.10+.

```bash
cd bff/youtube_scraper
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # then edit, MONGODB_URI must use 127.0.0.1
uvicorn main:app --reload --port 8001
```

## Troubleshooting

- `http://127.0.0.1:8000` opens another app? That is expected when another
  local process owns host port `8000`. This container listens on `8000`
  inside Docker, but Docker publishes it to your machine on `BFF_HOST_PORT`,
  which defaults to `8001`. Use `http://127.0.0.1:8001`.
- Startup fails with `_mongodb._tcp.cluster.mongodb.net`? Your `.env` still
  contains the placeholder Atlas URI. Replace
  `mongodb+srv://USER:PASSWORD@CLUSTER.mongodb.net/?retryWrites=true&w=majority`
  with the real connection string from MongoDB Atlas.
- Mongo Express logs `mongo: Name does not resolve`? Recreate the Compose
  stack so it picks up the MongoDB health dependency:
  `docker compose down --remove-orphans && docker compose up --build`.
- `/writing/evaluate` returns `LLM_API_KEY is required`? Set
  `LLM_API_KEY`, `LLM_BASE_URL`, `LLM_MODEL` in `.env`.
- YouTube search/transcript returns `IP blocked`? Configure a rotating proxy
  via `WEBSHARE_PROXY_*` or `YOUTUBE_TRANSCRIPT_PROXY_*` env vars.

## Test the API

```bash
curl http://127.0.0.1:8001/
curl http://127.0.0.1:8001/health
curl "http://127.0.0.1:8001/feed?level=B2&page=1&limit=20"
curl "http://127.0.0.1:8001/feed/dQw4w9WgXcQ"
curl "http://127.0.0.1:8001/search?q=ielts%20listening%20practice&limit=5"
curl "http://127.0.0.1:8001/transcript?videoId=dQw4w9WgXcQ&language=en"
```

Add or refresh a curated video:

```bash
curl -X POST "http://127.0.0.1:8001/admin/add-video" \
  -H "x-admin-token: replace-with-a-long-random-secret" \
  -H "Content-Type: application/json" \
  -d '{"videoId":"dQw4w9WgXcQ","language":"en","tags":["listening"],"status":"published"}'
```

With a manual CEFR override:

```json
{
  "videoId": "dQw4w9WgXcQ",
  "language": "en",
  "levelOverride": "B2",
  "tags": ["ielts", "listening"],
  "status": "published"
}
```

For the Android emulator, use this base URL:

```text
http://10.0.2.2:8001
```

For a physical phone on the same Wi-Fi, use your computer's LAN IP:

```text
http://YOUR_PC_LAN_IP:8001
```

## Test the AI writing tutor

The LLM-backed `POST /writing/evaluate` endpoint can be tested without the
Android app. The Pydantic schemas act as the contract: the request body is an
`EssaySubmission` and the response is a `WritingEvaluationDB`.

### 1. Make sure your LLM keys are set

`bff/youtube_scraper/.env` must contain real values for:

```text
LLM_API_KEY=your-9router-or-openai-api-key
LLM_BASE_URL=https://api.9router.com/v1
LLM_MODEL=gpt-4o-mini
```

Missing or placeholder values will fail at request time with a clear
`LLM_API_KEY is required` error.

### 2. Start the stack

```bash
cd bff/youtube_scraper
docker compose up --build
```

Wait for the FastAPI container to show "Application startup complete."

### 3. Option A — Interactive Swagger UI (easiest)

FastAPI auto-generates an interactive testing UI directly from the Pydantic
models.

```text
http://127.0.0.1:8001/docs
```

1. Open the URL in your browser.
2. Expand the **writing** section and click `POST /writing/evaluate`.
3. Click **Try it out**.
4. Replace the example body with your own essay:

```json
{
  "task_prompt": "Some people believe that unpaid community service should be a compulsory part of high school programmes. To what extent do you agree or disagree?",
  "essay_text": "I think community service is good. It helps students learn about real life. But maybe not compulsory because students are busy with study. They have a lot of homework."
}
```

5. Click **Execute**. The response body matches `WritingEvaluationDB` exactly,
   and the same record is written to MongoDB.

### 4. Option B — curl from the terminal

```bash
curl -X POST "http://127.0.0.1:8001/writing/evaluate" \
  -H "Content-Type: application/json" \
  -d '{
    "task_prompt": "Do you agree or disagree with the following statement? Modern technology has made life more complicated.",
    "essay_text": "Modern technology is good. It helps us in daily life. People use phones and computers. But sometimes it is too much for old people. They feel stressed and confused."
  }'
```

A successful response looks like:

```json
{
  "id": "5e9b1f5c-2c8b-4f5e-9c0e-9a1d4f8a3b2e",
  "task_prompt": "Do you agree or disagree with the following statement? Modern technology has made life more complicated.",
  "essay_text": "Modern technology is good. ...",
  "overall_band": 5.5,
  "coherence_feedback": "...",
  "vocabulary_suggestions": ["..."],
  "simon_style_rewrite": "...",
  "created_at": "2026-01-07T09:18:42.512000+00:00"
}
```

### 5. Verify it landed in MongoDB

Open Mongo Express:

```text
http://127.0.0.1:8081
```

Login with `admin` / `admin` (defaults), then navigate to:

```text
StudyForIELTS  ->  writing_evaluations
```

You should see a new document for each call. The collection has a descending
index on `created_at` (created in the FastAPI lifespan startup), so list/history
queries stay fast.

### 6. Reading the response

Every field in the response is generated by the LLM in a strict JSON schema and
is therefore always present:

- `overall_band` — estimated IELTS band, clamped to `0.0`–`9.0`.
- `coherence_feedback` — short paragraph on structure, paragraphing, and linking.
- `vocabulary_suggestions` — array of specific replacement words or phrases.
- `simon_style_rewrite` — a Band 9 style rewrite in the linear/clear/cohesive/simple voice.

The Android client will eventually render these fields directly. For now, this
endpoint proves the prompt engineering, LLM integration, and database
persistence end-to-end.

## Build the production image

```bash
docker build -t studyforielts-youtube-bff .
docker run --env-file .env -p 8000:8000 studyforielts-youtube-bff
```

The Dockerfile uses `python:3.11-slim`, installs only `requirements.txt`, runs
as a non-root user, and starts Uvicorn without reload.

## Notes for learning

- `/feed` is async end to end: FastAPI awaits Motor queries without blocking
  the event loop.
- `/admin/add-video` stores the same MongoDB document shape as the Node BFF,
  so existing curated data remains compatible.
- `/search` and `/transcript` call third-party libraries that are synchronous,
  so the service runs them in a worker thread with `asyncio.to_thread`.
- `/search` auto-curates results in a `BackgroundTasks` task so the response
  is not blocked on Mongo writes.
- YouTube search and transcripts rely on unofficial web behavior. For hosted
  environments, transcript requests may need a rotating residential proxy.
- Secrets stay out of Docker images. `.env` is loaded by Compose at runtime
  and should not be committed.
- For a guided tour of the FastAPI patterns used in this codebase, read
  [`LEARN_FASTAPI.md`](./LEARN_FASTAPI.md).