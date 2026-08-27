# StudyForIELTS

End-to-end IELTS preparation app with an Android Jetpack Compose client and a Python FastAPI backend. The current product includes local dictation, BFF-backed remote dictation, YouTube listening practice, and AI-assisted IELTS writing practice for Task 1 and Task 2.

The next product focus is **cache-first study**: preserve progress locally, reduce repeated server requests, and make downloaded lessons and audio available offline where practical.

> The FastAPI backend has its own operational doc at [`bff/youtube_scraper/README.md`](bff/youtube_scraper/README.md). It covers Docker, environment variables, endpoints, and troubleshooting. This root README describes the product state and roadmap.

---

## Tech Stack

- **Client:** Kotlin · Jetpack Compose · Retrofit · Room
- **Backend:** Python 3.11 · FastAPI · Motor (async MongoDB) · Pydantic v2
- **Data:** MongoDB (local via Docker, Atlas for cloud)
- **ML/AI:** OpenAI-compatible LLM through the configured BFF provider; vision model support for Task 1 charts
- **Infra:** Docker · Docker Compose · Render blueprint

---

## Current Status

### Android client

- [x] Three-tab navigation: Home, Listening, and Writing.
- [x] Bundled local dictation flow with sentence practice, audio controls, vocabulary, and progress tracking.
- [x] Remote BFF dictation lesson list, player, vocabulary, and CEFR-based content.
- [x] Room cache for remote dictation lessons, sentences, vocabulary, drafts, playback position, sentence attempts, skipped state, and completion progress.
- [x] YouTube browse and transcript-based dictation flow.
- [x] Writing home, Task 1 and Task 2 lesson lists, practice screens, timers, word counts, chart images, streaming feedback, and retry states.
- [x] Local study activity and streak tracking.

### BFF backend

- [x] FastAPI services for dictation lessons, writing lessons, and writing evaluation.
- [x] MongoDB persistence, GridFS chart-image storage, admin lesson CRUD, and published-lesson endpoints.
- [x] Task 1 vision evaluation and Task 2 text evaluation with structured Pydantic output.
- [x] Validation retries, injection-defense delimiters, rate limiting, token/cost logging, and server-side response caching.
- [x] Docker Compose development stack and production Dockerfile.
- [x] Render deployment blueprint; production deployment and full external-service validation remain operational tasks.

### Important caching boundary

Remote dictation metadata and progress are cached in Android Room, but remote audio still streams from its Appwrite URL. Writing lessons, chart images, essay drafts, and evaluations are currently network-oriented and are not yet cached locally. The BFF response cache reduces repeated LLM work; it is separate from Android offline caching.

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
      *Note:* the screen is now available from the **Writing** bottom-nav tab. The `YoutubeBrowseScreen` card also switches to that tab.
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

## Implemented Writing and Dictation Services

The former Phase 3.5 work is complete for the currently implemented flows. See `Phase3.5_Hardening_and_Deployment.md` for the detailed hardening checklist.

Task 1 vision evaluation is end-to-end: chart → GridFS → vision LLM → persisted evaluation with `task_type="task1"`. The Android writing lesson screens and network layer are also implemented.

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
- [x] **3.6 Task 1 evaluate endpoints** — `POST /writing/evaluate/task1` (+ stream), `task_type` field on persisted doc
  - `app/models/writing.py` — new `Task1EssaySubmission(lesson_id, essay_text)`; `WritingEvaluationDB` gains `task_type: TaskType = "task2"` (default keeps old documents deserializable)
  - `app/services/llm_service.py` — extracted shared SSE stream/retry/usage code from `stream_essay_evaluation` into `_stream_evaluation_with_retry` so Task 1 and Task 2 share one event protocol; added `evaluate_task1_essay_with_ai_stream` (empty-image guard short-circuits with `event: error`, otherwise delegates with `SIMON_BAND9_TASK1_SYSTEM_PROMPT` + `settings.llm_vision_model`)
  - `app/routers/writing.py` — `_fingerprint` now namespaced by `task_type` so Task 1 / Task 2 with the same text never collide in the response cache; new `_load_lesson_and_image` helper (404 lesson_not_found / 400 lesson_has_no_image / 404 lesson_image_not_found)
  - `POST /writing/evaluate/task1` (rate-limited) — loads lesson + GridFS image, non-streaming call, persists with `task_type="task1"`, writes through to `response_cache`
  - `POST /writing/evaluate/task1/stream` (rate-limited) — same SSE format as Task 2 (`data: <delta>`, `event: usage`, `event: done`); cache hit emits a single `done` event; otherwise streams and persists out-of-band after the stream completes
  - Existing `/writing/evaluate` and `/writing/evaluate/stream` unchanged (backward compat)
  - Self-check: `python eval/check_task1_endpoints.py` (29 assertions; stubs OpenAI/pydantic/motor; covers model shape, fingerprint namespacing, vision-model routing, multimodal content, empty-image guard, usage event, and that the new endpoints are wired with rate-limit dependencies while Task 2 endpoints remain intact)
- [x] **3.7 Android — Bottom navigation** (3 tabs: Home | Listening | Writing) with nested nav graphs
  - `navigation/BottomNavItem.kt` (new) — `enum class BottomNavItem { Home, Listening, Writing }` with `route` / `labelRes` / `icon`
  - `navigation/StudyForIeltsNavGraph.kt` — replaced the single flat `NavHost` with a top-level `Scaffold` + `NavigationBar` (`StudyBottomBar`) and three per-tab `NavHost`s, each with its own `rememberNavController()` and per-tab `sealed class …Destination` so route paths don't collide
  - Each tab keeps its own back stack (inactive tabs restore from saveable state on return)
  - **Tab allocation**: Home = `LevelListScreen` + local level-based flow (LevelList → LessonList → Vocabulary → Dictation); Listening = YouTube browse + preview + dictation; Writing = free-form Task 2 and lesson-driven Task 1/Task 2 practice. The "Online YouTube Dictation" card on LevelList and the "Writing Practice" card on YouTubeBrowse switch tabs via the `onTabSelected` callback rather than cross-controller navigation
  - `app/src/main/res/values/strings.xml` — `bottom_nav_home` / `bottom_nav_listening` / `bottom_nav_writing`
- [x] **3.8 Android — Writing section screens** (Home → lesson list → practice with optional `lessonId`)
- [x] **3.9 Android — Network layer additions** (lesson DTOs, Task 1 submit, Coil for chart images)

---

## Next Plan — Cache-First and Offline Study

**Goal:** Make the local Room database the fast, reliable study surface while keeping the BFF authoritative for published content and AI evaluation. Offline mode should support studying previously downloaded content, not pretend that new LLM evaluations can run without a network connection.

### Phase A — Cache policy and progress reliability

- [ ] Define cache ownership for each content type: local dictation, remote dictation, YouTube metadata, writing lessons, images, and audio.
- [ ] Add freshness and invalidation rules for remote lessons instead of refetching unchanged content.
- [ ] Make progress writes local-first and resilient across process death and network changes.
- [ ] Add clear cached, stale, loading, empty, and offline UI states.
- [ ] Add tests for cache reads, replacement, stale-content handling, and progress restoration.

### Phase B — Offline dictation

- [ ] Download remote lesson audio into app-private storage with a resumable or restart-safe flow.
- [ ] Store the local audio path and download status alongside the cached lesson.
- [ ] Play local audio when available, then fall back to the remote URL when online.
- [ ] Add storage management: download, remove, retry, and eventually automatic eviction.
- [ ] Validate offline playback, answer checking, vocabulary review, and progress updates.

### Phase C — Cached writing lessons

- [ ] Cache published writing lesson metadata in Room.
- [ ] Cache Task 1 chart images using an app-managed image cache and define eviction behavior.
- [ ] Preserve essay drafts and the latest received evaluation locally.
- [ ] Allow users to review cached lessons and previous feedback offline.
- [ ] Keep submitting new evaluations as an explicit online action with a useful offline message.

### Phase D — Optional synchronization

- [ ] Decide whether progress should sync to the server after authentication is available.
- [ ] Add a small sync queue only when a server-side progress contract exists.
- [ ] Use WorkManager for retryable background synchronization, not for local-only progress.
- [ ] Resolve conflicts using a documented last-write or event-based policy.

### Cache design decisions to make before implementation

- [ ] Cache TTL and refresh behavior.
- [ ] Maximum audio/image storage and eviction policy.
- [ ] Whether downloads require Wi-Fi or charging.
- [ ] Whether YouTube content can be persisted beyond metadata and local transcript data.
- [ ] Privacy behavior for locally stored essays and downloaded media.

### Existing backlog: Task prompt bank in MongoDB

**Why:** A curated prompt bank makes the practice loop self-contained and enables progress tracking by prompt.

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
│   ├── app/routers/                  # HTTP endpoints, including writing.py
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