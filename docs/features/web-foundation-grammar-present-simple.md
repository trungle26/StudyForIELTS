# Web Foundation Grammar: Present Simple

## Status

Planned. This is the next web-first foundation slice after web topic vocabulary review.

## Goal

Teach A1 learners to form and recognize basic present-simple affirmative sentences using daily-routines vocabulary on the web application.

The feature applies the repository learning sequence:
```text
Read rule summary -> answer focused exercises -> get immediate feedback with explanation -> retry missed items -> save completion locally
```

The content and examples must be original or appropriately licensed. This feature applies the foundation-first and topic-based approach documented in `docs/LEARNING_METHOD.md`; it must not reproduce copyrighted source material.

## Scope

- Web only; do not modify Android or backend in this feature.
- Topic: `daily routines`.
- Internal stage: `foundation`.
- CEFR level: `A1`.
- Skill: `grammar`.
- Content type: `grammar_exercise`.
- Seed a deterministic set of 5 A1 present-simple exercises aligned with the existing Android grammar content:
  1. `I ___ up every day.` (`wake` / `wakes`)
  2. `She ___ breakfast in the morning.` (`eat` / `eats`)
  3. `We ___ to school every day.` (`go` / `goes`)
  4. `He ___ at night.` (`sleep` / `sleeps`)
  5. `They study ___.` (`at night` / `every day`)
- Show a concise rule summary: base verb after `I/you/we/they`, verb-`s` after `he/she`, plus common routine time expressions.
- Support option selection with immediate feedback and explanation.
- Allow retry on incorrect answers without resetting previously mastered state.
- Save lesson progress and completion in browser `localStorage`.
- Support English and Vietnamese UI text.

## Exercise experience

Show each exercise with:
- Clear sentence prompt with blank (`___`).
- Accessible option choices (e.g. buttons/radio).
- Immediate feedback showing whether the answer is correct or incorrect using text and not color alone.
- Short explanation reinforcing the rule.
- "Continue" button to move to the next item when correct.
- "Try again" action when incorrect.
- Progress indicator (e.g. `2 of 5`).
- Completed review screen with options to reset or revisit.

## Local progress

Use `localStorage` with a scoped key, e.g. `studyforielts.daily-routines.grammar`.

Store:
- Completed status (`completed: boolean`).
- Current exercise index or answered items.
- Missed attempts for review.

Handle storage unavailability, quotas, or corrupted JSON gracefully without crashing the application.

## UI and accessibility

- Consistent with existing web application typography and colors.
- Responsive layout across narrow mobile viewports (down to 280px/320px) and desktop.
- Zero horizontal overflow.
- Accessible focus rings on all interactive controls (`:focus-visible`).
- Accessible labels, semantic headings, and ARIA live status announcements (`role="status"` or `role="alert"`).
- Color contrast compliant with WCAG 2.2 AA (minimum 4.5:1 for normal text).
- No emojis anywhere in code, UI, or commit messages.

## Non-goals

- Android changes.
- Backend API changes.
- Full tense system or irregular verbs outside daily routines.
- Authentication or server-side progress syncing.
- Complex grammar engines or AI-generated prompts.
- Web redesign outside the activity container.

## Acceptance criteria

1. Learner can open the Present Simple grammar activity from the web interface.
2. Rule summary is clear, concise, and visible before or alongside practice.
3. All 5 daily-routines exercises are playable in sequence.
4. Correct answers trigger confirmation, explanation, and progress advance.
5. Incorrect answers offer immediate retry with instructional feedback.
6. Completion state is recorded in `localStorage` and persists across page reloads.
7. Reset / review again allows repeating the exercise set.
8. Storage exceptions are handled safely without breaking the UI.
9. All interactive elements are accessible via keyboard with visible focus.
10. UI strings are localized in English and Vietnamese.
11. Existing web dictation and vocabulary review flows remain functional.

## Verification

Run focused checks:
- Web typecheck: `npm run typecheck`
- Web unit tests: `npm test` (verify exercise logic, answer checking, retry, persistence, and edge cases)
- Web production build: `npm run build`
- `git diff --check`

After merging to `staging`:
1. Wait for Render deployment to complete (approx. 1-2 minutes).
2. Poll health and web endpoints until the new commit is live.
3. Manually verify staging deployment at `https://studyforielts.onrender.com/`.

## Suggested handoff prompt

```text
Implement only `docs/features/web-foundation-grammar-present-simple.md`.

Read this document and `docs/IMPLEMENTATION_WORKFLOW.md` plus
`docs/DEPLOYMENT_WORKFLOW.md`. Pull the latest `staging`, inspect the
worktree, and stop if unrelated uncommitted work would be put at risk. Create
`feature/web-foundation-grammar-present-simple` from the latest staging commit.

Build the smallest complete web slice using the existing React, TypeScript,
Vite, layout, localization, loading/error, and testing patterns. Reuse
the daily-routines present-simple rules and exercises from the Android domain
model. Use localStorage only for local progress state. Handle storage failures safely.
Do not add Android, backend, authentication, sync, IndexedDB, or unrelated redesigns.

Add focused tests for grammar exercise evaluation, feedback, retry, persistence,
and failure states. Run web typecheck, tests, production build, and
git diff --check.

Before merging, squash all feature work into one feature-only commit. Merge
that single commit into `staging` with fast-forward only and push `staging`.
Wait for Render to redeploy, poll the deployed endpoints until the new commit
is live, and perform manual staging QA only after deployment readiness is
confirmed. If deployment remains pending, report that QA was not completed.
Do not merge to `master`.

Report the exact staging and deployed commits, changed files, tests, builds,
manual checks, known limitations, and promotion recommendation.
```
