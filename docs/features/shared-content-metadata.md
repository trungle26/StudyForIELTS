# Feature: Shared Content Metadata

## Status

Planned. This feature defines the smallest platform-neutral metadata contract needed by Android, web, and the FastAPI BFF.

## Goal

Make published learning content describe its level, type, provenance, and update state consistently without mixing content metadata with learner profile data or lesson-specific payloads.

## Why now

The BFF already exposes richer dictation metadata than Android currently stores. A stable vocabulary should be documented before adding a web client or expanding recommendations.

## Non-goals

- No Room migration in this feature.
- No new shared Kotlin/Python abstraction.
- No endpoint redesign or breaking response change.
- No learner profile changes.
- No CEFR classification algorithm changes.
- No sentence timing, transcript, vocabulary, cache, or download metadata changes.
- No writing-specific prompt, image, sample-answer, or tips fields in the shared envelope.

## Contract

The following fields form the first shared metadata vocabulary. Existing endpoint-specific response objects may contain additional fields.

| Field | Type | Required | Meaning |
|---|---|---:|---|
| `id` | string | yes | Stable server-owned content identifier. |
| `title` | string | yes | User-facing display title. |
| `contentType` | enum | yes for new shared responses | `dictation`, `listening`, `reading`, `writing`, `speaking`, `grammar`, `vocabulary`, or `pronunciation`. |
| `skill` | enum | optional | Primary learning skill: `listening`, `reading`, `writing`, `speaking`, `grammar`, `vocabulary`, or `pronunciation`. |
| `cefrLevel` | enum | optional | `A1`, `A2`, `B1`, `B2`, `C1`, or `C2`. Use `null` when not classified. |
| `durationSeconds` | integer | optional | Estimated or media duration. Must be zero or greater when present. |
| `source` | string | optional | Human-readable origin, such as `youtube` or `curated`. |
| `sourceUrl` | string | optional | Original source URL. |
| `licenseNote` | string | optional | Provenance or usage note safe to display to clients. |
| `tags` | array of strings | optional | Topic or filtering labels. |
| `updatedAt` | ISO-8601 timestamp | optional | Last server-side content update. |

`level` may remain as a backwards-compatible alias in existing dictation endpoints during migration. New shared contracts should prefer `cefrLevel` to avoid ambiguity with IELTS target bands and internal stages.

## Ownership rules

- The BFF owns stable identifiers, publication status, classification values, provenance, and timestamps.
- Clients render and filter the contract but do not infer or overwrite server metadata.
- Learner profile values remain separate: current CEFR, target IELTS band, daily goal, and focus skills are not content fields.
- An IELTS target band is never represented as a content CEFR level.
- Classification confidence and classifier version remain endpoint-specific until a real UI need exists.

## Compatibility strategy

1. Do not remove existing `level` fields from dictation responses yet.
2. Add or document `contentType`, `skill`, and `cefrLevel` through additive changes.
3. Map existing Android `level` to `cefrLevel` at the DTO boundary.
4. Preserve unknown or missing metadata as nullable/default values rather than rejecting otherwise valid lessons.
5. Version the API before making a breaking rename or changing identifier semantics.

## Acceptance criteria

- The contract distinguishes CEFR content difficulty from IELTS learner target band.
- Dictation, listening, and writing content can be identified without relying on title or URL parsing.
- Existing BFF responses remain compatible with Android.
- `sourceUrl`, `licenseNote`, tags, duration, and update timestamp have defined ownership and nullability.
- Clients do not need to access MongoDB, GridFS, or media providers directly.
- The contract is sufficient for the first web dictation slice to display level, duration, provenance, and topic labels.
- No database migration is required merely to document this contract.

## Implementation boundary

The implementation chat should inspect current BFF models, routers, Android DTOs, and mapping code before editing. Prefer the smallest additive changes. Do not create a generic content framework or migrate all lesson types in one change.

Likely files to inspect:

- `bff/youtube_scraper/app/models/dictation.py`
- `bff/youtube_scraper/app/routers/dictation.py`
- `app/src/main/java/com/trungld/studyforielts/data/remote/model/DictationDtos.kt`
- `app/src/main/java/com/trungld/studyforielts/domain/model/RemoteDictationModels.kt`
- `app/src/main/java/com/trungld/studyforielts/data/repository/RemoteDictationRepositoryImpl.kt`

## Verification

- Run the existing BFF tests or focused model/endpoint tests.
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew :app:compileDebugKotlin`.
- Run `./gradlew assembleDebug`.
- Confirm existing Android dictation behavior remains usable.

## Follow-up

After this contract is implemented and verified, specify the first beginner-friendly content slice, beginning with daily-routines vocabulary and dictation.
