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
- [x] **Phase 1 complete** — `POST /writing/evaluate` live, returns strict JSON, persists to MongoDB

---

## Phase 1 — Applied AI Backend: Simon's Band 9 Writing Tutor ✅

**Why:** The backend already owns async I/O and MongoDB writes, so it's the natural home for an LLM round-trip. We add a strict structured-output contract so the Android client can render feedback predictably — no regex-parsing free text.

**Status:** All items complete. API tested end-to-end with a sample essay; JSON contract is enforced via Pydantic; submissions persist to `writing_evaluations` in MongoDB.

### LLM SDK setup

- [x] **Decide provider**: OpenAI (`openai` SDK) — via 9router proxy (`LLM_BASE_URL`)
- [x] **Add SDK to `requirements.txt`** and rebuild the Docker image
- [x] **Add `LLM_API_KEY` (and optional `LLM_MODEL`, `LLM_PROVIDER`) to `.env.example`**
      *Why:* secrets stay outside the image; env-driven config keeps the same image deployable across environments.

### Data contracts

- [x] **Create `app/models/writing.py`** with a Pydantic `WritingEvaluation` model:
      - `overall_band: float` (e.g. `7.5`)
      - `coherence_feedback: str`
      - `vocabulary_suggestions: list[str]`
      - `simon_style_rewrite: str`
- [x] **Create `EssaySubmission` request model** (essay text + optional prompt/task type)
      *Why:* request/response models are the API contract for the Android client. Explicit types catch drift early.

### Endpoint

- [x] **Create `app/routers/writing.py` with `POST /writing/evaluate`**
- [x] **Wire it into `app/main.py`** under the FastAPI app
      *Why:* routers stay small; the service layer holds the LLM call so the endpoint is just orchestration + persistence.

### Prompt engineering (few-shot)

- [x] **Author the system prompt** that:
      - States the tutor persona (Simon-style, Band 9 reference)
      - Defines the four output fields in plain language
      - Enforces the "linear, clear, cohesive" structure
- [x] **Inject one in-context paragraph example** (Simon-style Band 9 tone)
      *Why:* in-context example sets the voice; full few-shot pairs reserved for later when we have a labelled eval set.
- [x] **Enforce structured output** via `response_format={"type": "json_object"}` (basic JSON mode) + `model_validate` against the Pydantic schema
      *Why:* `beta.chat.completions.parse` (native OpenAI structured outputs) is **not** supported by 9router, so we use the simpler `json_object` mode and let Pydantic enforce the schema downstream.

### Persistence

- [x] **Save submission + evaluation to a `writing_evaluations` MongoDB collection** (one doc per attempt, include timestamp + band score)
      *Why:* unlocks future progress tracking (band-over-time graphs, per-user history) without a schema change.
- [x] **Add index on `created_at`** (descending) for history queries
      *Why:* reads dominate once history exists; index at write time, not when it hurts. *Note:* per-user index will be added when auth lands.

---

## Phase 2 — Android: Writing Practice Screen ✅

**Why:** the LLM contract is only useful if the user can submit text and see structured feedback. The screen is also the home of the future "track my band over time" view.

**Status:** All items complete. Screen lives at `presentation/writing/` and is reachable from the YouTube Browse screen via a "Writing Practice" card.

### Networking

- [x] **Add `WritingApi` Retrofit interface** with `evaluateEssay(body: EssaySubmissionDto): WritingEvaluationDto`
- [x] **Add DTOs in `data/remote/model/WritingDtos.kt`** mirroring the backend's `WritingEvaluation`
- [x] **Register `WritingApi` in `NetworkModule` (Hilt)**
      *Why:* keep the dependency-injection graph consistent with the existing YouTube API wiring.

### State layer

- [x] **Create `WritingViewModel`** exposing `uiState: StateFlow<WritingUiState>` with `Idle | Submitting | Success(eval) | Error`
      *Skipped a separate `WritingRepository`*: the screen has a single endpoint and a single error mapping; introducing a repo would be YAGNI. The ViewModel holds essay/prompt state and maps `HttpException` / `IOException` / generic exceptions to user-safe messages.

### UI

- [x] **Create `WritingPracticeScreen.kt`** with:
      - Multiline `OutlinedTextField` (essay input, 8-20 lines)
      - Submit button (disabled while empty / submitting / < 50 words)
      - `CircularProgressIndicator` + helper text during the LLM call
      - Editable prompt card (defaulted to a sample Task 2 question)
- [x] **Inline results section** (in same file) showing:
      - `overall_band` as a large `displayMedium` headline
      - `coherence_feedback` paragraph
      - `vocabulary_suggestions` as a `FlowRow` of `AssistChip`s
      - `simon_style_rewrite` in a distinct "suggested rewrite" `Surface`
- [x] **Add the screen to `StudyForIeltsNavGraph.kt`** as `WritingPractice("writing/practice")` and a clickable launcher card in `YoutubeBrowseScreen`
- [x] **Handle error state** (network failure, malformed response) with a dedicated error card + Retry button

### Polish

- [x] **Local word counter** ("N / 50+ words") in the input section, primary-coloured once the threshold is met
- [x] **Disable submit when essay is below 50 words** to avoid noisy LLM calls
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

## Phase 3.5 — Hardening, Deployment & Writing Lessons (in progress)

**Why:** A deployed Task 2 tutor with retry-on-validation, injection defense, token/cost logging, IP rate limiting, and a response cache is already shipped (see `Phase3.5_Hardening_and_Deployment.md` for the full checklist). This phase extends the writing feature to Task 1 (chart images → vision LLM) and gives the Android app a curated lesson system instead of a free-form text box.

**Status:** Priorities 0, 1, and 2 from the addendum are complete on the Task 2 path. 3.1 is complete. 3.2 (admin CRUD for lessons) is in progress. 3.3–3.6 unblock the Task 1 vision flow; 3.7–3.9 add the Android bottom nav and writing screens.

- [x] **3.1 Writing Lessons — MongoDB collection + GridFS image storage**
  - `WritingLesson` / `WritingLessonResponse` / `WritingLessonListResponse` Pydantic models in `app/models/writing.py`
  - `AsyncIOMotorGridFSBucket` bound in `connect_mongo` (`writing_lesson_images` bucket)
  - `get_writing_lessons` + `get_gridfs_bucket` FastAPI dependencies
  - Compound index `(task_type, status, created_at: -1)` created in lifespan
- [x] **3.2 Admin CRUD for writing lessons**
  - `app/services/writing_lesson_service.py` — create / update (with image replace) / delete (cascades to GridFS) / list (drafts included)
  - `POST /admin/writing-lessons` (multipart, `tips` as JSON-encoded field, optional `image`)
  - `PUT /admin/writing-lessons/{id}` (partial update, `image` replaces, `clear_image=true` removes; mutually exclusive)
  - `DELETE /admin/writing-lessons/{id}` (204 on success, drops the GridFS file)
  - `GET /admin/writing-lessons` (admin view, drafts visible, newest first)
  - 8 MB upload cap with HTTP 413 on oversize; all endpoints gated by `require_admin_token`
  - Self-check: `python eval/check_admin_lessons.py` (14 assertions; stubs motor/fastapi/pydantic so it runs without installed deps)
- [x] **3.3 Public lesson endpoints for Android**
  - `app/services/writing_lesson_service.py` — `list_published_lessons` (filter by `task_type`, paginated, drafts never exposed), `get_published_lesson` (drafts return None so the router's 404 covers both missing and unpublished), `open_lesson_image` (64 KiB chunks out of GridFS, restores `Content-Type` from upload metadata), `LessonImageNotFound` for the missing-file case
  - `GET /writing/lessons?task_type=task1&page=1&limit=20` (rate-limited; `limit` clamped to 50; same compound index from 3.1)
  - `GET /writing/lessons/{lesson_id}` (404 if missing or draft)
  - `GET /writing/lessons/{lesson_id}/image` (binary stream, 1-day `Cache-Control: public, max-age=86400`; image ids are immutable so caching is safe)
  - Self-check: `python eval/check_public_lessons.py` (22 assertions, exercises list/filter/pagination + draft hiding + model round-trip with a fake motor cursor)
- [x] **3.4 Task 1 system prompt (vision)** — `app/prompts/writing_task1_v1.txt` + changelog
  - Task Achievement rubric (overview placement, key feature selection, data specificity, no opinion); same `WritingEvaluation` JSON schema as Task 2
  - `ACTIVE_TASK1_PROMPT_VERSION = "v1"` constant next to Task 2's `ACTIVE_PROMPT_VERSION` in `app/services/llm_service.py`; loaded from disk via the same pattern
  - Image-aware injection defense: `<<<ESSAY_START>>>` / `<<<ESSAY_END>>>` for essay + `<<<IMAGE_START>>>` / `<<<IMAGE_END>>>` for chart
  - `app/prompts/CHANGELOG.md` updated with a Task 1 section
- [x] **3.5 Vision LLM call for Task 1 evaluation** — `LLM_VISION_MODEL` config, `evaluate_task1_essay_with_ai`
  - `app/core/config.py` — new `llm_vision_model` setting; `LLM_VISION_MODEL` env var, falls back to `LLM_MODEL` so a single model can serve both
  - `app/services/llm_service.py` — extracted shared retry/usage loop into `_run_with_validation_retries`; `evaluate_essay_with_ai` refactored to use it
  - `_build_task1_user_message` + `_sniff_image_media_type` build an OpenAI-compatible multimodal user message (text + `image_url` data URI), with `<<<IMAGE_START>>>` / `<<<IMAGE_END>>>` and `<<<ESSAY_START>>>` / `<<<ESSAY_END>>>` delimiters for injection defense
  - `evaluate_task1_essay_with_ai(task_prompt, essay_text, image_bytes) -> EvaluationResult` — same `WritingEvaluation` schema, same retry + suspicious-score check; service layer is pure (no DB), image bytes come from the router/GridFS
  - Streaming variant (`evaluate_task1_essay_with_ai_stream`) lands in 3.6
  - Self-check: `python eval/check_task1_llm_service.py` — stubs OpenAI/pydantic, asserts both functions + shared helper are wired correctly, retry/usage accumulation behavior is preserved, and Task 1 sends a multimodal message with a base64 image part
- [ ] **3.6 Task 1 evaluate endpoints** — `POST /writing/evaluate/task1` (+ stream), `task_type` field on persisted doc
- [ ] **3.7 Android — Bottom navigation** (3 tabs: Home | Listening | Writing) with nested nav graphs
- [ ] **3.8 Android — Writing section screens** (Home → lesson list → practice with optional `lessonId`)
- [ ] **3.9 Android — Network layer additions** (lesson DTOs, Task 1 submit, Coil for chart images)

---

## Phase 4 — Future Enhancements (Backlog)

**Why:** Capture the natural next steps that aren't in the current scope but are already on the roadmap. These don't block Phase 2/3; revisit when the writing feature has real users.

### Writing Task 1 support

- [ ] **Extend `EssaySubmission` with a `task_type: Literal[1, 2]` field** in `app/models/writing.py`
      *Why:* Task 1 has different criteria (Task Achievement: describing trends, comparisons, data selection) than Task 2 (Argument + Coherence). One system prompt can't do both well.
- [ ] **Add a `task1_chart_image` (base64 or multipart upload) field** to the request, plus a vision-capable model in the SDK
      *Why:* Task 1 requires reading a chart/graph/diagram; text-only prompts lose the whole point of the task.
- [ ] **Branch the system prompt by `task_type`**: separate `SIMON_TASK1_SYSTEM_PROMPT` and `SIMON_TASK2_SYSTEM_PROMPT`
      *Why:* keep tone/persona consistent (Simon) while changing the evaluation rubric to match the task.
- [ ] **Update the Android Writing Practice screen** with a Task 1 / Task 2 toggle; Task 1 also lets the user pick an image from the gallery

### Task prompt bank in MongoDB

- [ ] **Add a `writing_prompts` MongoDB collection** with schema `{ id, task_type, prompt, source, created_at }`
- [ ] **Seed the collection** with a small set of official IELTS Task 1 + Task 2 prompts
- [ ] **Add `GET /writing/prompts?task_type=2` endpoint** that returns a random prompt (or a paginated list)
- [ ] **Add a corresponding `WritingPromptsApi` + DTOs** in the Android client
- [ ] **Update `WritingPracticeScreen`** to fetch a random prompt on launch and display it as a card above the text field; user can "shuffle" for a new prompt
      *Why:* a curated prompt bank makes the practice loop self-contained (no copy-pasting from the internet) and lets us track per-prompt progress.

### Auth + per-user history (stretch)

- [ ] **Add Firebase Auth (or similar)** to the Android client; pass a `userId` on every `/writing/evaluate` call
- [ ] **Add a `userId` field to the `writing_evaluations` schema** and `(userId, createdAt)` compound index
- [ ] **Add `GET /writing/history?userId=...` endpoint** with pagination
- [ ] **Add a "My Band History" screen** on Android — line chart of band score over time

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