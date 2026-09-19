# Feature: Web Dictation Polish and QA

## Status

Planned. The online web dictation slice is deployed and needs production verification before adding new product capability.

## Goal

Verify and harden the staging web dictation experience at `https://studyforielts.onrender.com/` against the staging BFF at `https://studyforielts-youtube-bff.onrender.com/`. Promote to `master` only after the staging release gate passes.

See `docs/DEPLOYMENT_WORKFLOW.md` for the branch and promotion process.

## Scope

- Production smoke checks for lesson list, lesson detail, audio, vocabulary, retry, empty, and offline states.
- Fix only confirmed deployment, CORS, routing, accessibility, or responsive defects.
- Add lightweight browser E2E coverage if the current web setup supports it without a large dependency expansion.
- Document the required Render web and BFF environment variables.
- Confirm direct refresh of the deployed web route works.

## Non-goals

- No authentication.
- No IndexedDB or service worker.
- No writing lesson support.
- No media-storage migration.
- No broad redesign or generic design system.
- No Android changes unless a shared BFF contract regression is proven.

## Acceptance criteria

- Production lesson list loads without CORS or network errors.
- A lesson opens and displays metadata, sentences, and vocabulary.
- Audio playback works when the lesson has an available `audioUrl`.
- Retry and network-unavailable states are understandable and accessible.
- Refreshing the deployed site does not produce a routing failure.
- Mobile and desktop layouts have no horizontal overflow.
- Keyboard navigation and visible focus work for primary controls.
- Any required Render configuration is documented without secrets.
- Existing Android tests and BFF self-checks remain green.

## Verification

- Confirm Render services are tracking the `staging` branch.
- Confirm the web build points to the staging BFF URL.
- Test both deployed staging URLs manually.
- Inspect browser console and network requests for CORS failures.
- Run web typecheck, production build, and tests.
- Run relevant BFF self-checks directly.
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew :app:compileDebugKotlin`.
- Run `git diff --check`.
- Record the exact staging commit tested before promotion.

## Implementation boundary

Inspect the current web files and deployment configuration before editing. Keep fixes minimal and additive. Do not add a new framework or dependency unless the existing setup cannot support the required check.

## Follow-up

After production QA passes, add the next product capability: web writing lesson browsing and practice, or browser caching if offline access is the higher priority.
