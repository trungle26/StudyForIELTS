# StudyForIELTS FastAPI BFF

FastAPI backend for the Android app's YouTube dictation experience. It searches
YouTube, fetches transcripts, classifies transcript difficulty into CEFR levels,
stores curated videos in MongoDB, and serves a paginated feed to the Android app.

## Architecture

```text
bff/youtube_scraper/
  main.py                         # Uvicorn import target: main:app
  app/main.py                     # FastAPI app wiring and lifespan
  app/core/config.py              # Environment-backed settings
  app/core/database.py            # Motor client lifecycle
  app/core/security.py            # Admin token dependency
  app/models/                     # Pydantic response contracts
  app/routers/                    # HTTP endpoints
  app/services/                   # MongoDB and YouTube business logic
  Dockerfile                      # Production image
  docker-compose.yml              # Local dev container with reload
  requirements.txt                # Python dependencies
```

FastAPI owns the HTTP layer. Motor owns one async MongoDB client for the whole
application lifespan: connect on startup, reuse per request, close on shutdown.
Service modules contain the real work so routers stay small and easy to test.
Pydantic models keep the API contract explicit for your Android client.

## Data Population Approach

For internal testing, `POST /admin/add-video` with an admin token is a good
first curation workflow. The backend does the expensive and repeatable work:
fetching transcript data, fetching metadata, estimating CEFR level, applying an
optional manual level override, and upserting the final curated document.

This is better than manually editing MongoDB documents because it keeps document
shape consistent and lets you repeat the same workflow locally, on Render, or
from a future admin UI. Later, replace the single shared token with proper admin
accounts/roles if this becomes a public or multi-person production tool.

## Endpoints

```text
GET /health
GET /feed?level=B2&page=1&limit=20
GET /feed/dQw4w9WgXcQ
GET /search?q=ielts%20listening%20practice&limit=5
GET /transcript?videoId=dQw4w9WgXcQ&language=en
POST /admin/add-video
```

Compatibility routes from the earlier Android integration are still available:

```text
GET /api/youtube/search?q=ielts%20listening%20practice&limit=5
GET /api/youtube/captions/{videoId}?language=en
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
```

`curatedvideos` matches the existing Mongoose collection generated from the
Node `CuratedVideo` model. Change it only if your Atlas collection uses a
different name.

For local Docker development, Compose starts MongoDB for you. The FastAPI
container uses `BFF_MONGODB_URI` and connects to MongoDB by Docker service
hostname:

```text
mongodb://studyforielts:studyforielts_local_password@mongo:27017/StudyForIELTS?authSource=admin
```

If you run Python directly on your host machine instead of through Docker, use
`MONGODB_URI` with `127.0.0.1:27017` or `localhost:27017` because then the app
is outside the Docker network.

For Atlas or Render, use your real Atlas URI in `MONGODB_URI` and Render
environment variables. Do not use `mongo:27017` outside Docker Compose.

## Run With Docker Compose

Build and start the local development container:

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

Docker maps a host port to a container port. In this project the container still
listens on `8000`, but your computer exposes it as `BFF_HOST_PORT`, which
defaults to `8001`. If port `8001` is also busy, change `BFF_HOST_PORT` in
`.env`, for example `BFF_HOST_PORT=8010`.

Docker containers also resolve each other by service name. That is why the
FastAPI container uses `mongo:27017`, while tools running directly on your
machine use `127.0.0.1:27017`.

Stop the server:

```bash
docker compose down
```

## Troubleshooting

If `http://127.0.0.1:8000` opens another app, that is expected when another
local process owns host port `8000`. This container listens on port `8000`
inside Docker, but Docker publishes it to your machine on `BFF_HOST_PORT`, which
defaults to `8001`. Use:

```text
http://127.0.0.1:8001
```

If startup fails with a message about `_mongodb._tcp.cluster.mongodb.net`, your
`.env` still contains the placeholder Atlas URI. Replace this:

```text
MONGODB_URI=mongodb+srv://USER:PASSWORD@CLUSTER.mongodb.net/?retryWrites=true&w=majority
```

with the real connection string from MongoDB Atlas. It should contain your real
cluster host, for example `cluster0.xxxxx.mongodb.net`, not `CLUSTER.mongodb.net`
or `cluster.mongodb.net`.

If Mongo Express logs `mongo: Name does not resolve`, recreate the Compose
stack so it picks up the MongoDB health dependency:

```bash
docker compose down --remove-orphans
docker compose up --build
```

## Test The API

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

## Build The Production Image

```bash
docker build -t studyforielts-youtube-bff .
docker run --env-file .env -p 8000:8000 studyforielts-youtube-bff
```

The Dockerfile uses `python:3.11-slim`, installs only `requirements.txt`, runs as
a non-root user, and starts Uvicorn without reload.

## Notes For Learning

- `/feed` is async end to end: FastAPI awaits Motor queries without blocking the
  event loop.
- `/admin/add-video` stores the same MongoDB document shape as the Node BFF, so
  existing curated data remains compatible.
- `/search` and `/transcript` call third-party libraries that are synchronous, so
  the service runs them in a worker thread with `asyncio.to_thread`.
- YouTube search and transcripts rely on unofficial web behavior. For hosted
  environments, transcript requests may need a rotating residential proxy.
- Secrets stay out of Docker images. `.env` is loaded by Compose at runtime and
  should not be committed.
