# StudyForIELTS

End-to-end IELTS prep app: Android (Jetpack Compose) client + Python (FastAPI + MongoDB) backend. Currently shipping the **Video Feed** and **Video Dictation** features. This README tracks the next phases: **Applied AI (LLM writing tutor)**, **Android UI for writing practice**, and **MLOps / cloud deployment**.

> The FastAPI backend has its own operational doc at [`bff/youtube_scraper/README.md`](bff/youtube_scraper/README.md). It covers Docker, env vars, endpoints, and troubleshooting. The root README you are reading is the project roadmap.

---

## Tech Stack

- **Client:** Kotlin · Jetpack Compose · Retrofit · Room
- **Backend:** Python 3.11 · FastAPI · Motor (async MongoDB) · Pydantic v2
- **Data:** MongoDB (local via Docker, Atlas for cloud)
- **ML/AI (next phase):** OpenAI or Google GenAI SDK
- **Infra (next phase):** Docker · Docker Compose · Render

---

## Current Status

- [x] Android client connected to FastAPI backend
- [x] Video Feed — paginated, CEFR-filtered, MongoDB-backed
- [x] Video Dictation — transcript-based typing practice
- [x] Local dev via `docker compose up` (backend + Mongo + Mongo Express)
- [x] Production Dockerfile (`python:3.11-slim`, non-root, no reload)
- [x] Initial Render deployment blueprint in `render.yaml`

---

## Phase 1 — Applied AI Backend: Simon's Band 9 Writing Tutor

**Why:** The backend already owns async I/O and MongoDB writes, so it's the natural home for an LLM round-trip. We add a strict structured-output contract so the Android client can render feedback predictably — no regex-parsing free text.

### LLM SDK setup

- [ ] **Decide provider**: OpenAI (`openai` SDK) vs Google GenAI (`google-generativeai`)
      *Why:* tradeoffs — OpenAI has the cleanest structured-outputs (JSON schema) API; GenAI is cheaper for long essays. Pick one and stay consistent.
- [ ] **Add SDK to `requirements.txt`** and rebuild the Docker image
      *Why:* pinning the version in the image keeps the dev/prod gap zero.
- [ ] **Add `LLM_API_KEY` (and optional `LLM_MODEL`, `LLM_PROVIDER`) to `.env.example`**
      *Why:* secrets stay outside the image; env-driven config keeps the same image deployable across environments.

### Data contracts

- [ ] **Create `app/models/writing.py`** with a Pydantic `WritingEvaluation` model:
      - `overall_band: float` (e.g. `7.5`)
      - `coherence_feedback: str`
      - `vocabulary_suggestions: list[str]`
      - `simon_style_rewrite: str`
- [ ] **Create `EssaySubmission` request model** (essay text + optional prompt/task type)
      *Why:* request/response models are the API contract for the Android client. Explicit types catch drift early.

### Endpoint

- [ ] **Create `app/routers/writing.py` with `POST /writing/evaluate`**
- [ ] **Wire it into `app/main.py`** under the FastAPI app
      *Why:* routers stay small; the service layer holds the LLM call so the endpoint is just orchestration + persistence.

### Prompt engineering (few-shot)

- [ ] **Author the system prompt** that:
      - States the tutor persona (Simon-style, Band 9 reference)
      - Defines the four output fields in plain language
      - Enforces the "linear, clear, cohesive" structure
- [ ] **Inject 2–3 few-shot examples** of (low-band essay → Band 9 rewrite + feedback)
      *Why:* few-shot is the cheapest, most reliable way to lock tone and output shape. In-context examples beat more instructions.
- [ ] **Enforce structured output** via the provider's JSON-schema mode (`response_format=json_schema` on OpenAI, or `generation_config.response_schema` on GenAI)
      *Why:* Pydantic parses the result into typed objects downstream — no fragile string scraping.

### Persistence

- [ ] **Save submission + evaluation to a `writing_evaluations` MongoDB collection** (one doc per attempt, include timestamp + band score)
      *Why:* unlocks future progress tracking (band-over-time graphs, per-user history) without a schema change.
- [ ] **Add index on `(userId, createdAt)`** for history queries
      *Why:* reads dominate once history exists; index at write time, not when it hurts.

### Smoke test

- [ ] **Hit `POST /writing/evaluate` with a sample essay via `curl`** and confirm the JSON shape matches `WritingEvaluation`
- [ ] **Verify a document landed in MongoDB** (Mongo Express or `mongosh`)

---

## Phase 2 — Android: Writing Practice Screen

**Why:** the LLM contract is only useful if the user can submit text and see structured feedback. The screen is also the home of the future "track my band over time" view.

### Networking

- [ ] **Add `WritingApi` Retrofit interface** with `submitEssay(essay: String): WritingEvaluationDto`
- [ ] **Add DTOs in `data/remote/model/`** mirroring the backend's `WritingEvaluation`
- [ ] **Register `WritingApi` in `NetworkModule` (Hilt)**
      *Why:* keep the dependency-injection graph consistent with the existing YouTube API wiring.

### State layer

- [ ] **Create `WritingViewModel`** exposing `uiState: StateFlow<WritingUiState>` with `Idle | Submitting | Success(eval) | Error`
- [ ] **Create `WritingRepository`** in domain + data layers
      *Why:* ViewModels stay free of Retrofit types; repository owns the network call and error mapping.

### UI

- [ ] **Create `WritingPracticeScreen.kt`** with:
      - Multiline `TextField` (essay input)
      - Submit button (disabled while empty or `Submitting`)
      - Loading indicator during the LLM call
- [ ] **Create `WritingResultsCard.kt`** showing:
      - `overall_band` as a large score chip
      - `coherence_feedback` paragraph
      - `vocabulary_suggestions` as a chip group
      - `simon_style_rewrite` in a distinct "suggested rewrite" panel
      *Why:* cards are easier to test and reuse than one monolithic screen.
- [ ] **Add the screen to `StudyForIeltsNavGraph.kt`** and a launcher entry point
- [ ] **Handle error state** (network failure, malformed response) with a retry action

### Polish

- [ ] **Local word/char counter** in the input field
- [ ] **Disable submit when essay is below a min length** (e.g. 50 words) to avoid noisy LLM calls
      *Why:* short essays produce low-signal feedback and burn API quota.

---

## Phase 3 — MLOps, Containerization & Deployment

**Why:** the writing tutor is the first feature that costs real money per request. Containerization gives us a reproducible image; Render gives us a public URL for the Android client to talk to.

### Container hygiene (mostly done — verify)

- [x] **`python:3.11-slim` base image** in `bff/youtube_scraper/Dockerfile`
- [x] **Non-root user** (`app`) inside the container
- [x] **`--no-cache-dir` pip install** for a smaller layer
- [x] **Uvicorn entrypoint without `--reload`** in the production image
- [ ] **Add `.dockerignore` guard** for `.env`, `__pycache__`, `.git`, `.pytest_cache` (verify exists at `bff/youtube_scraper/.dockerignore`)
      *Why:* secrets and venvs in the image are the #1 deployment-time incident.

### Local Compose

- [x] **`docker-compose.yml`** wires FastAPI + Mongo + Mongo Express
- [x] **Health check on Mongo** so FastAPI waits for it
- [x] **Volume mount for hot reload** in dev
- [ ] **Confirm `LLM_API_KEY` flows into the container** via `.env` (no hard-coded secrets)

### Cloud deployment (Render)

- [ ] **Push image to Render** as a Web Service from the existing `render.yaml` (or rebuild from the Dockerfile)
- [ ] **Set env vars in Render dashboard**:
      - `MONGODB_URI` (Atlas SRV string)
      - `LLM_API_KEY`
      - `LLM_PROVIDER` / `LLM_MODEL`
      - `ADMIN_TOKEN`
- [ ] **Whitelist Render's outbound IP in MongoDB Atlas** (or use a private network)
      *Why:* Atlas rejects connections from unknown IPs by default; this is the most common "first deploy" failure.
- [ ] **Point Android client base URL at the Render host** in `NetworkModule`
      *Why:* emulator uses `10.0.2.2`; physical devices and the deployed app need the real URL.
- [ ] **Verify `/health` returns 200** on the deployed URL before wiring the client

### Observability (stretch)

- [ ] **Log LLM call latency + token usage** to stdout (Render captures it) — *skip until traffic justifies it*
      *ponytail: ceiling = free-tier logs. Upgrade path: Ship to a log drain or OpenTelemetry.*

---

## Repo Layout

```
StudyForIELTS/
├── app/                              # Android client (Kotlin, Jetpack Compose)
│   └── src/main/java/com/trungld/studyforielts/
│       ├── data/remote/              # Retrofit interfaces + DTOs
│       ├── presentation/             # Compose screens + ViewModels
│       └── navigation/               # Nav graph
├── bff/youtube_scraper/              # FastAPI backend
│   ├── app/routers/                  # HTTP endpoints (writing.py will land here)
│   ├── app/models/                   # Pydantic contracts
│   ├── app/services/                 # LLM call + Mongo logic
│   ├── Dockerfile                    # Production image
│   ├── docker-compose.yml            # Local dev stack
│   ├── requirements.txt
│   └── README.md                     # Backend ops doc (env, Docker, endpoints)
├── render.yaml                       # Render deployment blueprint
└── README.md                         # ← you are here
```

---

## Reference

- Backend operations: [`bff/youtube_scraper/README.md`](bff/youtube_scraper/README.md)
- Render blueprint: [`render.yaml`](render.yaml)
- AI roadmaps tracked:
  - LLM Integration (SDK + structured outputs)
  - Prompt Engineering (few-shot, JSON-schema enforcement)
  - MLOps (Docker, Compose, cloud deploy)