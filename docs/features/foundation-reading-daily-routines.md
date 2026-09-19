# Foundation Reading: Daily Routines

## Status

Planned. This is the next foundation-first Android vertical slice.

## Goal

Give an A1 learner a short, supported reading lesson before introducing full IELTS Reading passages or question types.

The lesson follows the repository learning sequence:

```text
Preview vocabulary -> read for general meaning -> read for details -> answer -> review -> retry
```

The content and examples must be original or appropriately licensed. This feature applies the foundation-first and topic-based approach documented in `docs/LEARNING_METHOD.md`; it must not reproduce copyrighted source material.

## Learner and content scope

- Internal stage: `foundation`
- CEFR level: `A1`
- Topic: `daily routines`
- Skill: `reading`
- Content type: `reading_lesson`
- Target session length: 5–10 minutes
- No IELTS band score is shown.
- Do not treat the learner's IELTS target band as the lesson difficulty.

Create one original passage of approximately 80–120 words about daily routines. Reuse familiar A1 vocabulary from the existing daily-routines foundation content and mostly use the present simple. Keep the passage coherent, concrete, and suitable for a beginner learner.

The lesson should include:

- A title and short instruction.
- Five to eight target vocabulary items.
- The reading passage with comfortable line length and spacing.
- Four or more comprehension questions.
- Immediate feedback with a short explanation.
- Retry support for missed questions.
- A clear completion state.

## Exercise scope

Use a small deterministic question set containing these exercise purposes:

1. Main idea: identify what the passage is mainly about.
2. True, false, or not stated: find evidence in the passage.
3. Specific detail: answer who, when, where, or how often.
4. Vocabulary in context: infer the meaning of a target word from its sentence.

Each question must preserve the learner's answer and provide:

- Correct or incorrect status using text and not color alone.
- A concise explanation.
- The relevant passage evidence where practical.
- Retry behavior without losing the lesson state.

Do not add timed testing, IELTS question types, band scoring, AI-generated questions, or a large question bank.

## Progress and persistence

Reuse existing Android local-first patterns.

- Save lesson completion locally.
- Resume completion state after leaving and reopening the feature.
- Track question progress only if it fits the existing progress pattern without introducing a new backend contract.
- Do not add authentication, server synchronization, or a new database migration unless existing architecture makes it necessary.
- Make the final completion state available to the existing daily-routines path when practical.

## Navigation and UI

Reuse existing Compose navigation and activity-screen patterns rather than creating a new framework or abstraction.

The learner should be able to:

1. Open the daily-routines reading lesson from the foundation path.
2. Preview the target vocabulary.
3. Read the passage.
4. Answer the question set.
5. Review feedback and retry missed questions.
6. Complete the lesson and return to the path.
7. Reopen the lesson and see saved completion.

Use existing English/Vietnamese localization patterns. All user-visible strings must be localized.

## Accessibility

- Use semantic heading and reading order.
- Keep controls keyboard navigable where the platform supports a keyboard.
- Provide accessible names and state announcements for answer controls.
- Make focus visible.
- Do not communicate correctness through color alone.
- Keep text readable with adequate size, contrast, and spacing.
- Ensure long text does not create horizontal overflow.
- Respect reduced-motion settings if any transition is added.

## Non-goals

- Full IELTS Reading passages or exams.
- Matching headings, matching information, or other advanced IELTS question types.
- Timed reading tests.
- IELTS band prediction or scoring.
- AI-generated lesson content.
- Web implementation in this feature branch.
- New authentication or progress synchronization.
- Browser caching or media migration.
- Broad navigation redesign.

## Acceptance criteria

The feature is complete when an A1 learner can:

- Open the daily-routines reading lesson.
- Preview the target vocabulary.
- Read one original short passage.
- Answer at least four comprehension questions.
- Receive understandable feedback for incorrect answers.
- Retry missed questions.
- Complete the lesson and see local progress saved.
- Reopen the lesson and retain completion state.
- Use the feature in English and Vietnamese.
- Navigate the interactive controls accessibly.

## Verification

Run the smallest focused checks first, then the relevant project checks:

- Focused reading/domain tests.
- `./gradlew testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew assembleDebug`
- `git diff --check`

If the implementation changes shared BFF contracts or web behavior, also run the relevant BFF/web checks. Do not claim checks passed if an existing unrelated failure blocks a required command; record the exact failure and scope.

Manual checks should cover:

- Fresh lesson entry.
- Correct and incorrect answers.
- Retry behavior.
- Completion persistence after leaving and reopening.
- English and Vietnamese UI.
- Keyboard/focus and screen-reader semantics where available.
- Small-screen layout and long passage text.

## Implementation boundary

Implement only this Android foundation-reading slice. Inspect and reuse existing daily-routines vocabulary, grammar, dictation, progress, navigation, localization, and Compose patterns. Keep the diff minimal and do not refactor unrelated code.

The implementation chat must:

1. Pull the latest `staging`.
2. Create `feature/foundation-reading` from that updated branch.
3. Preserve unrelated local work and never reset, stash, discard, or force-push it.
4. Implement and verify this feature.
5. Squash the feature branch into one feature-only commit before merging to `staging`.
6. Merge that single commit into `staging` with fast-forward only and push it.
7. Report the exact staging commit and verification results.
8. Do not promote to `master` automatically.

## Suggested handoff prompt

```text
Implement only `docs/features/foundation-reading-daily-routines.md`.

Read the feature document and `docs/IMPLEMENTATION_WORKFLOW.md` first. Pull
latest `staging`, check the worktree, and stop if unrelated uncommitted work
would be put at risk. Create `feature/foundation-reading` from the latest
staging commit.

Build the smallest complete Android Compose vertical slice. Reuse existing
patterns for daily-routines navigation, vocabulary, local progress,
localization, and feedback. Keep CEFR, IELTS target band, skill, and learning
stage separate. Use original content. Add focused tests for non-trivial
reading and progression logic. Do not add web, auth, sync, AI generation, or
unrelated refactors.

Run the focused tests, `./gradlew testDebugUnitTest`,
`./gradlew :app:compileDebugKotlin`, `./gradlew assembleDebug`, and
`git diff --check`. Record any blocked command exactly.

Before merging, squash all feature work into one feature-only commit. Merge
that single commit into `staging` with fast-forward only and push `staging`.
Do not merge to `master`. Report the exact staging commit, changed files,
checks, manual checks, limitations, and promotion recommendation.
```
