# Feature: Web Dictation Polish and QA

## Status

Planned. The online web dictation slice is deployed and needs staging verification before adding new product capability.

## Goal

Verify and harden the staging web dictation experience at `https://studyforielts.onrender.com/` against the staging BFF at `https://studyforielts-youtube-bff.onrender.com/`. Promote to `master` only after the staging release gate passes.

See `docs/DEPLOYMENT_WORKFLOW.md` for the branch and promotion process.

## Scope

- Staging smoke checks for lesson list, lesson detail, audio, vocabulary, retry, empty, and offline states.
- Fix only confirmed deployment, CORS, routing, accessibility, or responsive defects.
- Add lightweight browser E2E coverage if the current web setup supports it without a large dependency expansion.
- Document the required Render web and BFF environment variables without secrets.
- Confirm direct refresh of the deployed web route works.
- Record the exact staging commit tested before promotion.

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

After staging QA passes, promote the tested staging commit to `master`. Then add the next product capability: web writing lesson browsing and practice, or browser caching if offline access is the higher priority.

## Cross-computer implementation prompt

Copy this prompt into the implementation chat on any computer:

```text
Implement only the feature described in
`docs/features/web-dictation-polish-and-qa.md`.

Read `docs/DEPLOYMENT_WORKFLOW.md` from the repository before editing.
Do not rely on another chat's context or files created outside Git.

Git workflow:
1. Run `git status --short --untracked-files=all`.
2. Run `git fetch origin`.
3. Switch to staging and update it:
   `git checkout staging && git pull --ff-only origin staging`
4. Confirm the working tree is clean. If unrelated changes exist, do not
   delete, reset, stash, or overwrite them; report them and stop.
5. Create the branch:
   `git checkout -b feature/web-dictation-polish-qa`

Before editing:
1. Read this feature document completely.
2. Inspect the current web files and Render/deployment configuration.
3. Confirm the deployed services are tracking `staging`.
4. Confirm the staging web URL and BFF URL in Render.
5. Identify the smallest required change.

Staging URLs to verify, after confirming they are current in Render:
- Web: https://studyforielts.onrender.com/
- BFF: https://studyforielts-youtube-bff.onrender.com/

Verify:
- Lesson list loads.
- A lesson opens.
- Metadata, sentences, and vocabulary appear.
- Audio playback works when `audioUrl` exists.
- Browser console and network requests show no CORS errors.
- Retry and network-unavailable states work.
- Direct route refresh works.
- Keyboard focus and primary controls work.
- Mobile and desktop layouts have no horizontal overflow.

Fix only confirmed defects. Do not add authentication, IndexedDB,
service workers, writing support, media migration, a new framework, or
Android changes.

Run:
- Web typecheck
- Web production build
- Web tests
- Relevant BFF self-checks directly
- `./gradlew testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `git diff --check`

After all scoped checks pass:
1. Commit only feature-related files:
   `fix: harden deployed web dictation experience`
2. Merge into staging with fast-forward only:
   `git checkout staging && git merge --ff-only feature/web-dictation-polish-qa`
3. Push staging:
   `git push origin staging`
4. Record the exact staging commit and manual QA results.
5. Delete the feature branch:
   `git branch -d feature/web-dictation-polish-qa`

Do not merge to `master` automatically. Promote only after staging acceptance:
`git checkout master && git pull --ff-only origin master`
`git merge --ff-only staging`
`git push origin master`

Never force-push or discard unrelated work.

Report:
Feature:
Branch:
Staging commit tested:
Changed files:
Production/staging checks:
Tests:
Builds:
Manual checks:
Git operations:
Known limitations:
Promotion recommendation:
```
