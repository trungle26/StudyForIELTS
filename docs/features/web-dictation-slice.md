# Feature: Web Dictation Vertical Slice

## Status

Complete as of 2026-09-19. The React/Vite web dictation slice was merged to `master` and is being validated through the `staging` deployment before production promotion.

## Goal

Provide a usable browser experience for published remote dictation lessons through the existing FastAPI BFF.

## Scope

- TypeScript React web client.
- Remote dictation lesson list.
- Lesson detail view.
- Browser audio playback using the existing media URL.
- Vocabulary display.
- Loading, empty, error, retry, and basic offline/network-unavailable states.
- Responsive layout for mobile and desktop browsers.

## Non-goals

- No authentication.
- No IndexedDB synchronization or service worker.
- No browser offline lesson downloads.
- No writing or evaluation UI.
- No AWS or media-storage migration.
- No Android changes.

## API boundary

- Call the FastAPI BFF only.
- Preserve existing dictation response fields and use additive compatibility mapping.
- Do not access MongoDB, GridFS, Appwrite, or the LLM provider directly.
- Configure CORS only for explicit development and production origins if required.

## Acceptance criteria

- A user can load published dictation lessons.
- A user can open one lesson and view its metadata, sentences, and vocabulary.
- A user can play available audio in the browser.
- Loading, empty, error, retry, and offline/network-unavailable states are visible and accessible.
- The layout works at mobile and desktop widths without horizontal overflow.
- API types are separate from UI components.
- Focused tests cover API mapping and lesson rendering.
- Existing BFF behavior and Android builds remain usable.

## Verification

- Run web lint, typecheck, and tests.
- Run relevant BFF tests.
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew :app:compileDebugKotlin`.
- Run `git diff --check`.
- Manually verify lesson list, detail, playback, retry, empty, offline, keyboard, and responsive states.

## Follow-up

Complete production polish and QA using `docs/features/web-dictation-polish-and-qa.md` before adding browser caching, authentication, or another major product capability.

## Implementation handoff

- Branch: `feature/web-dictation-slice` merged to `master`, pushed to `origin/master`, and deleted.
- Stack: React 18, TypeScript, Vite 5, Vitest, Testing Library.
- Added: lesson list/detail, audio playback, vocabulary display, loading/empty/error/offline states, responsive layout, API types, and focused tests.
- BFF changes: none; existing dictation endpoints and CORS configuration were used.
- Verified: web typecheck, production build, web tests, BFF self-checks, `./gradlew testDebugUnitTest`, `./gradlew :app:compileDebugKotlin`, and `git diff --check` passed.
- Known limitations: no authentication, IndexedDB, service worker, browser caching, E2E tests, writing support, or media migration. Manual production checks remain.
- Deployment: Render services should track `staging` for QA and `master` for production. Current staging URLs are web `https://studyforielts.onrender.com/` and BFF `https://studyforielts-youtube-bff.onrender.com/`; confirm them in Render before testing.
