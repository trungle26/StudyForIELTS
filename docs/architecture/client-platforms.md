# Client Platform Architecture

## Decision

StudyForIELTS will support both Android and web clients through the existing FastAPI BFF.

- Android remains native Kotlin and Jetpack Compose.
- Web will be a separate TypeScript client.
- The BFF is the shared API and business boundary.
- The Android client will not be rewritten in Flutter or React Native.
- Kotlin Multiplatform is deferred until duplicated domain logic justifies it.

## System shape

```text
Android app ------------------\
                               -> FastAPI BFF -> MongoDB / GridFS or S3 / LLM provider
Web app ----------------------/
```

Clients must never connect directly to MongoDB, GridFS, S3 private objects, or the LLM provider.

## Responsibilities

### Android

- Jetpack Compose UI.
- Room for lesson, vocabulary, progress, and offline content.
- Retrofit for BFF communication.
- Hilt dependency injection.
- WorkManager for future retryable synchronization.
- Native audio playback, downloads, TTS, and notifications.
- Android-specific connectivity and storage controls.

### Web

- TypeScript UI, initially React with Next.js or Vite.
- Responsive layouts for desktop and mobile browsers.
- TanStack Query or an equivalent server-state layer.
- IndexedDB for selected offline data.
- Browser audio APIs for playback and downloads.
- Service worker only after online flows are stable.
- Browser-specific authentication, CORS, and storage handling.

### BFF

- Authentication and authorization.
- Stable public API contracts.
- Content filtering and pagination.
- Lesson and media access.
- LLM evaluation and streaming events.
- Rate limiting, validation, and response caching.
- MongoDB persistence.
- Progress synchronization after an authenticated contract exists.

## Shared API contract

The BFF should expose platform-neutral JSON contracts for:

- Learner profile.
- Published lessons.
- Dictation sentences and vocabulary.
- Writing lessons and chart images.
- Evaluation requests and responses.
- Progress events or snapshots.
- Pagination.
- Structured errors.

Use OpenAPI as the source for endpoint documentation. Generate or validate client models where practical, but do not force both clients to share implementation code.

## Required API rules

- Use stable identifiers for all server content.
- Return consistent error shapes with a machine-readable `code` and user-safe `message`.
- Keep pagination parameters and response metadata consistent.
- Version breaking API changes rather than silently changing fields.
- Never accept a client-supplied user ID as proof of identity.
- Derive user identity from a validated authentication token when accounts exist.
- Keep stream event names and payload formats documented.

## Writing streaming contract

The Android and web clients should consume the same event protocol:

```text
data: <partial text>
event: usage
event: done
event: error
```

The exact payload schema must be documented before adding the web client. A cache hit should remain valid for both clients without requiring an LLM request.

## Authentication direction

Authentication is deferred until local-first learner profiles and progress are useful without an account.

When added:

```text
Client -> authentication provider -> access token -> BFF validation -> user-scoped data
```

The BFF owns authorization decisions. The clients only manage sign-in state and attach tokens.

## Caching boundaries

### Android

- Room stores structured lessons, vocabulary, drafts, and progress.
- App-private storage stores downloaded audio when implemented.
- Cached data remains usable offline where content is complete.

### Web

- HTTP caching handles immutable media such as lesson images and audio.
- TanStack Query handles server-state freshness during a session.
- IndexedDB stores explicitly supported offline lesson data and progress.
- Service workers may cache the application shell later.

### BFF

- Response cache reduces repeated LLM work and server cost.
- It is not a replacement for client offline storage.
- Cache keys must include task type, prompt version, and all evaluation inputs.

## Media

Prefer stable media URLs or API endpoints that work for both clients. Keep private storage behind the BFF or signed URLs.

Planned direction:

1. Keep current Appwrite/GridFS behavior while contracts stabilize.
2. Define a media abstraction in the BFF.
3. Move suitable audio and image assets to S3 when AWS learning and deployment begin.
4. Preserve client behavior by changing storage behind the BFF.

## Rollout order

1. Document and stabilize existing BFF contracts.
2. Add consistent structured errors and CORS configuration.
3. Build a small web vertical slice using existing dictation endpoints.
4. Add web writing lesson and streaming evaluation flows.
5. Add browser caching after online behavior is reliable.
6. Add authentication and progress synchronization.
7. Evaluate S3 media storage and ECS/Fargate deployment.

## Explicit non-goals

- Do not rewrite Android for cross-platform UI.
- Do not duplicate LLM, MongoDB, or authorization logic in clients.
- Do not introduce Kotlin Multiplatform only for DTO sharing.
- Do not build browser offline synchronization before defining server conflict rules.
- Do not migrate storage providers solely to use an AWS service before the API abstraction exists.

## Definition of done for the first web slice

- Web can list published remote dictation lessons.
- Web can open a lesson and play its audio.
- Web can view lesson vocabulary.
- Web can show loading, empty, error, and offline states.
- BFF CORS allows only configured development and production origins.
- Existing Android tests and build remain green.
