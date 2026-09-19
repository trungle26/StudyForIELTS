# Web Topic Vocabulary Review

## Status

Planned. This is the next web-first foundation slice after web dictation QA.

## Goal

Give a beginner learner a short, topic-based vocabulary review experience that reuses the daily-routines vocabulary already available in the web dictation lesson.

The feature follows the repository learning method: vocabulary is learned in context, retrieved repeatedly, and reviewed after errors. It must use original or appropriately licensed content and must not reproduce copyrighted source material.

## Scope

- Web only; do not change Android in this feature.
- Topic: `daily routines`.
- Internal stage: `foundation`.
- CEFR level: `A1`.
- Skill: `vocabulary`.
- Content type: `vocabulary_review`.
- Use vocabulary returned by the existing BFF lesson/content contract where possible.
- Do not add authentication, server progress synchronization, or a new content service.

The learner should be able to:

1. Open vocabulary review from the web learning experience.
2. See a small set of daily-routines words with meaning and example context.
3. Mark an item as learned or review it again.
4. Complete a short retrieval review.
5. Receive immediate, understandable feedback.
6. Retry missed items.
7. Return later and retain local browser progress.

## Content experience

Show each item with:

- Word or phrase.
- Plain-English meaning.
- Existing translation when supplied by the content contract; otherwise use the existing localization pattern rather than inventing a backend field.
- Short original example sentence.
- Optional audio only when an existing safe audio URL is available.
- A clear review action.

Use a small deterministic set. Do not build a flashcard platform or spaced-repetition engine in this slice.

## Review exercise

Include at least one retrieval flow using existing vocabulary data, such as:

- Choose the meaning.
- Choose the word for a meaning.
- Complete a short sentence.

The exercise must:

- Normalize only harmless input differences where free text is used.
- Preserve the learner's answer.
- Show correct or incorrect status with text, not color alone.
- Explain the answer briefly.
- Offer retry for missed items.
- End with a clear completion state.

## Local progress

Use the smallest browser-native persistence approach that fits the existing web app, preferably `localStorage` for this initial slice.

Persist only the minimum required state:

- Learned item identifiers.
- Missed item identifiers or review-needed state.
- Completion for the daily-routines vocabulary review.

Handle unavailable or malformed local storage without crashing. Do not introduce IndexedDB, a service worker, accounts, or cloud synchronization.

## UI and accessibility

- Reuse the current web application's layout, API, loading, error, and offline patterns.
- Keep the experience responsive at narrow mobile widths and desktop widths.
- Do not create horizontal overflow.
- Use semantic headings and labels.
- Make every control keyboard reachable with visible focus.
- Use accessible names and state announcements for answer controls.
- Do not use color as the only correctness or progress signal.
- Keep all user-visible strings localized in English and Vietnamese using the current project approach.
- Respect reduced-motion preferences if motion is added.

## States

Implement and test:

- Loading.
- Empty vocabulary response.
- API error with retry.
- Offline/unavailable API state.
- Normal vocabulary list.
- Review in progress.
- Correct answer.
- Incorrect answer with retry.
- Completed review.
- Previously completed review restored from local storage.
- Local-storage read/write failure handled safely.

## Non-goals

- Android changes.
- New BFF endpoints unless the existing contract cannot support the feature.
- Authentication or server-side progress.
- IndexedDB or service workers.
- General-purpose spaced repetition.
- Audio generation or media migration.
- AI-generated vocabulary or feedback.
- Grammar, reading, writing, or IELTS question types in this feature.
- Broad visual redesign.

## Acceptance criteria

The feature is complete when:

- A learner can open daily-routines vocabulary review from the web app.
- Vocabulary items render from the existing content/API contract.
- Meaning, context, and available metadata are understandable.
- A deterministic retrieval exercise works.
- Correct and incorrect feedback is clear and accessible.
- Missed items can be retried.
- Completion is visible and persists locally after reload.
- Loading, empty, error, offline, and storage-failure states are usable.
- English and Vietnamese UI strings are present.
- Keyboard focus and narrow-screen layout work.
- Existing web dictation behavior remains unchanged.

## Verification

Run focused web checks first, then the full applicable checks:

- Web typecheck.
- Web tests, including local progress and review behavior.
- Web production build.
- `git diff --check`.
- Relevant BFF self-checks if API types or backend files change.

After merging to `staging`:

1. Wait for Render to redeploy the web and any changed BFF service.
2. Allow roughly 1–2 minutes while polling actual endpoints; do not rely on a fixed sleep alone.
3. Confirm the new staging commit is live before manual QA.
4. If deployment is still pending, report `Render deployment still pending; QA not completed` and stop.
5. Test the staging web URL and BFF URL documented in `docs/DEPLOYMENT_WORKFLOW.md`.
6. Record the exact deployed commit and QA result.

## Implementation boundary

Implement only this web vocabulary review slice. Inspect the existing web dictation app, API types, tests, styles, and deployment configuration first. Keep the diff minimal and additive. Reuse existing components and state patterns instead of introducing a UI framework or general abstraction.

The implementation chat must:

1. Pull the latest `staging`.
2. Create `feature/web-topic-vocabulary-review` from that updated branch.
3. Preserve unrelated local changes and never reset, stash, discard, or force-push them.
4. Implement and verify the feature.
5. Squash all feature work into one feature-only commit before merging to `staging`.
6. Merge that single commit into `staging` with fast-forward only and push it.
7. Wait for Render deployment readiness before manual QA.
8. Report the exact staging commit, deployed commit, checks, and limitations.
9. Do not promote to `master` automatically.

## Suggested handoff prompt

```text
Implement only `docs/features/web-topic-vocabulary-review.md`.

Read this document and `docs/IMPLEMENTATION_WORKFLOW.md` plus
`docs/DEPLOYMENT_WORKFLOW.md`. Pull the latest `staging`, inspect the
worktree, and stop if unrelated uncommitted work would be put at risk. Create
`feature/web-topic-vocabulary-review` from the latest staging commit.

Build the smallest complete web slice using the existing React, TypeScript,
Vite, API, layout, localization, loading/error, and testing patterns. Reuse
existing daily-routines vocabulary data. Use localStorage only for the small
local progress state. Handle storage failures safely. Do not add Android,
authentication, sync, IndexedDB, service workers, AI generation, or unrelated
redesigns.

Add focused tests for vocabulary mapping, review feedback, retry, persistence,
and failure states. Run web typecheck, tests, production build, and
git diff --check. Run relevant BFF checks only if backend/API files change.

Before merging, squash all feature work into one feature-only commit. Merge
that single commit into `staging` with fast-forward only and push `staging`.
Wait for Render to redeploy, poll the deployed endpoints until the new commit
is live, and perform manual staging QA only after deployment readiness is
confirmed. If deployment remains pending, report that QA was not completed.
Do not merge to `master`.

Report the exact staging and deployed commits, changed files, tests, builds,
manual checks, known limitations, and promotion recommendation.
```
