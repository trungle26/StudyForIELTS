# Dictation Prepopulate — Implementation Plan

## Overview

Prepopulate offline dictation lessons from the BFF server. Content is managed
server-side (MongoDB); the Android app fetches, caches in Room, and plays back.
Audio is streamed from a remote URL. Content can be updated or removed on the
server without releasing a new APK.

## Architecture

```
Local audio folders (A1..C2)
  → Appwrite Storage (public bucket)
  → Whisper STT
  → POST /admin/dictation/vocabulary (BFF LLM)
  → POST /admin/dictation/import (BFF) → MongoDB (dictation_lessons, status=published)
  → GET /dictation/lessons (BFF, published only)
  → Android Retrofit → Room cache → UI
```

## Status

| # | Step | Status | Notes |
|---|------|--------|-------|
| 1 | BFF: Pydantic models (`app/models/dictation.py`) | ✅ Done | `DictationLesson`, `DictationSentence`, `DictationVocabulary`, list/detail responses |
| 2 | BFF: Service layer (`app/services/dictation_service.py`) | ✅ Done | `list_lessons`, `get_lesson`, `upsert_lesson` |
| 3 | BFF: Router (`app/routers/dictation.py`) | ✅ Done | `GET /dictation/lessons`, `GET /dictation/lessons/{id}`, `POST /admin/dictation/import` |
| 4 | BFF: Database helper (`app/core/database.py`) | ✅ Done | `get_dictation_lessons` → `dictation_lessons` collection |
| 5 | BFF: Router registered in `main.py` | ✅ Done | `app.include_router(dictation.router)` |
| 6 | BFF: MongoDB index for `(level, status, updatedAt)` | ✅ Done | Created in `connect_mongo()` in `database.py` |
| 7 | Android: Retrofit API (`DictationBffApi.kt`) | ✅ Done | `GET dictation/lessons`, `GET dictation/lessons/{id}` |
| 8 | Android: DTOs (`DictationDtos.kt`) | ✅ Done | Mirror BFF response shapes |
| 9 | Android: Room entities (`RemoteDictationLessonEntity`, `RemoteDictationSentenceEntity`) | ✅ Done | Cache tables with server `lessonId` as stable key |
| 10 | Android: Room DAO (`RemoteDictationDao.kt`) | ✅ Done | Upsert lessons+sentences, observe by level |
| 11 | Android: DB migration (v4 → v5) | ✅ Done | Added remote dictation cache entities, migration, DAO accessor, and Room wiring |
| 12 | Android: Domain models | ✅ Done | Added `RemoteDictationLesson` and `RemoteDictationSentence` |
| 13 | Android: Repository interface + impl | ✅ Done | Fetches BFF lessons, replaces/caches Room data, exposes Flow |
| 14 | Android: DI wiring (NetworkModule, DatabaseModule, RepositoryModule) | ✅ Done | Provided API, DAO, and repository bindings |
| 15 | Android: UI integration | ✅ Done | Remote list + player wired with audio playback; progress persistence deferred |
| 16 | Ingestion: Whisper transcript + Appwrite upload + BFF vocab + import | ✅ Done | `bff/youtube_scraper/generate_dictation_seed.py` (folder mode) |
| 17 | Ingestion: Import sample lessons into MongoDB | ⬜ Todo | Run script against a real BFF + Appwrite bucket |
| 18 | Validation: End-to-end test | ⬜ Todo | BFF → Android → playback with timestamp sync |

## File Map

### BFF (done)
- `bff/youtube_scraper/app/models/dictation.py`
- `bff/youtube_scraper/app/services/dictation_service.py`
- `bff/youtube_scraper/app/routers/dictation.py`
- `bff/youtube_scraper/app/core/database.py` (added `get_dictation_lessons`)
- `bff/youtube_scraper/app/main.py` (registered router)

### Android (todo)
- `app/src/main/java/.../data/remote/api/DictationBffApi.kt` — new
- `app/src/main/java/.../data/remote/model/DictationDtos.kt` — new
- `app/src/main/java/.../data/local/entity/RemoteDictationLessonEntity.kt` — new
- `app/src/main/java/.../data/local/entity/RemoteDictationSentenceEntity.kt` — new
- `app/src/main/java/.../data/local/dao/RemoteDictationDao.kt` — new
- `app/src/main/java/.../data/local/database/AppDatabase.kt` — add entities + migration
- `app/src/main/java/.../domain/model/RemoteDictationModels.kt` — new (or reuse)
- `app/src/main/java/.../domain/repository/RemoteDictationRepository.kt` — new
- `app/src/main/java/.../data/repository/RemoteDictationRepositoryImpl.kt` — new
- `app/src/main/java/.../di/NetworkModule.kt` — add `DictationBffApi` provider
- `app/src/main/java/.../di/DatabaseModule.kt` — add DAO provider
- `app/src/main/java/.../di/RepositoryModule.kt` — bind repository

### Ingestion (folder-driven; done)
- `bff/youtube_scraper/generate_dictation_seed.py` — scans `input_dir/{A1..C2}/audio.ext`,
  uploads each file to Appwrite Storage, transcribes with faster-whisper (or
  OpenAI Whisper), calls `POST /admin/dictation/vocabulary` on the BFF for
  vocabulary, then `POST /admin/dictation/import` and patches status to
  `published`.
- `bff/youtube_scraper/app/prompts/dictation_vocab_v1.txt` — system prompt
  for vocabulary generation (constrained JSON, 5-10 entries, transcript-only
  words).
- `bff/youtube_scraper/app/routers/dictation.py` — adds
  `POST /admin/dictation/vocabulary` (admin-protected, returns 502 on LLM
  failure).

#### Colab usage

```bash
export APPWRITE_API_KEY=...
export ADMIN_TOKEN=...               # matches BFF ADMIN_TOKEN

python generate_dictation_seed.py \
    --input-dir ./input \
    --output-dir ./dictation_seeds \
    --appwrite-endpoint https://cloud.appwrite.io/v1 \
    --appwrite-project YOUR_PROJECT_ID \
    --appwrite-bucket YOUR_BUCKET_ID \
    --bff-url https://your-bff.example.com \
    --model small                    # or gpt-4o-transcribe with --engine openai
```

Input layout::

```
input/
    A1/story-1.mp3
    A1/another.wav
    B1/conversation.m4a
```

The bucket must be set to public-read on Appwrite so the Android app can
stream `audioUrl`.

## BFF API Contract

### `GET /dictation/lessons?level=B1&page=1&limit=20`
```json
{
  "level": "B1",
  "page": 1,
  "limit": 20,
  "total": 12,
  "totalPages": 1,
  "items": [
    {
      "id": "dd-short-story-001",
      "title": "The Lost Key",
      "level": "B1",
      "source": "dailydictation",
      "sourceUrl": "",
      "licenseNote": "",
      "audioUrl": "https://storage.example/audio/dd-short-story-001.mp3",
      "durationSeconds": 184,
      "sentences": [
        { "orderIndex": 0, "text": "She walked slowly.", "startTimeMs": 0, "endTimeMs": 3200 }
      ],
      "vocabularies": [],
      "updatedAt": "2026-08-04T12:00:00Z"
    }
  ]
}
```

### `GET /dictation/lessons/{lesson_id}`
```json
{
  "lesson": { /* same shape as items[] above */ }
}
```

## Design Decisions

1. **Separate Room tables** — not reusing `youtube_videos`/`youtube_sentences` because lifecycle, fields, and progress tracking differ.
2. **Server ID as primary key** — `RemoteDictationLessonEntity.serverId` (String) is the PK, matching BFF `id`.
3. **Sentences embedded in list response** — avoids a second network call per lesson. If payload gets too large, move sentences to detail-only later.
4. **Audio streaming** — no download/cache in v1. ExoPlayer/Media3 handles remote URLs natively.
5. **Progress tracking** — reuse existing `progress`/`sentence_progress` tables by mapping server lesson ID to a synthetic Room lesson ID, OR add new progress tables. Decision deferred to step 15.
6. **DailyDictation licensing** — content stored server-side with `source`/`licenseNote` fields. Can be unpublished/deleted without APK update.
