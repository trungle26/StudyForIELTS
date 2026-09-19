# Feature: Foundation Grammar — Present Simple

## Status

Complete as of 2026-09-18. Merged to `master` in commit `9595c14`.

## Goal

Teach A1 learners to form and recognize basic present-simple sentences using daily-routines vocabulary.

## Scope

- Seed a small daily-routines present-simple lesson.
- Show a rule summary with affirmative sentence patterns.
- Provide focused exercises for `I/you/we/they` and `he/she`.
- Include basic time expressions: `every day`, `in the morning`, `at night`.
- Persist completion and attempts locally using existing Room patterns where practical.
- Connect the grammar activity from the daily-routines path.

## Non-goals

- No generic grammar engine.
- No full tense system, exceptions, or advanced explanations.
- No server synchronization or authentication.
- No web implementation.
- No AI-generated exercises.
- No redesign of existing navigation.

## Content

Topic: `daily_routines`

Stage: `beginner`

CEFR: `A1`

Lesson examples should use existing daily-routines vocabulary and short, plain sentences such as waking up, eating breakfast, going to school, studying, and sleeping.

Exercise types for the first version:

1. Choose the correct verb form.
2. Choose the correct subject and verb pair.
3. Complete a short sentence with a time expression.

Normalize answer comparison for case and surrounding whitespace. Do not silently accept arbitrary grammatical alternatives unless explicitly included in the exercise answer set.

## Data model

Use the smallest model that fits existing Room conventions. Keep lesson content separate from learner progress.

```text
GrammarLesson
  id: String
  title: String
  topic: String
  cefrLevel: String
  explanation: String
  exercises: List<GrammarExercise>

GrammarExercise
  id: String
  prompt: String
  options: List<String>
  answer: String
  explanation: String
```

If Room cannot persist nested exercise content without unnecessary complexity, keep immutable seed content in Kotlin and persist only progress. Do not add a generic serialization framework for one lesson.

## User flow

```text
Daily routines path
  -> Present simple practice
  -> Read short explanation
  -> Answer exercises
  -> See immediate correctness feedback
  -> Complete lesson
  -> Return to path with completion state
```

Allow retry after an incorrect answer. Do not permanently block the learner.

## UI requirements

- One clear exercise at a time.
- Visible progress such as `2 of 5`.
- Immediate feedback that does not rely on color alone.
- Accessible option controls and semantics.
- Loading, empty, error, retry, and completed states where applicable.
- English and Vietnamese localization.
- Preserve existing daily-routines completion persistence and navigation.

## Acceptance criteria

- The daily-routines grammar activity opens a meaningful present-simple lesson.
- At least five focused A1 exercises are available.
- `I/you/we/they` and `he/she` forms are represented.
- Incorrect answers can be retried and provide a useful explanation.
- Completion survives app restart using the existing local persistence approach.
- The path marks grammar as completed without breaking the other four activities.
- CEFR, IELTS target band, learning stage, skill, and topic remain separate.
- English and Vietnamese resources compile.
- Focused tests cover answer normalization and exercise progression.
- Existing Android unit tests and build checks pass.

## Implementation boundary

Before editing, inspect the existing daily-routines activity screen/ViewModel, Room progress patterns, navigation, vocabulary models, and localization conventions. Prefer immutable seed data plus the smallest progress record if no reusable grammar storage exists.

Do not modify BFF endpoints, web code, authentication, offline downloads, or unrelated learning flows.

## Verification

- Run focused grammar tests.
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew :app:compileDebugKotlin`.
- Run `./gradlew assembleDebug`.
- Manually verify correct, incorrect, retry, completion, restart, accessibility, and Vietnamese flows.

## Follow-up

Implement the first beginner dictation content slice for daily routines, reusing the existing BFF dictation contract.

## Implementation handoff

- Branch: `feature/foundation-grammar-present-simple` merged to `master` and deleted.
- Commit: `9595c14` (`feat: add present simple foundation grammar`).
- Changed: `DailyRoutinesPresentSimple.kt`, `DailyRoutinesActivityScreen.kt`, English and Vietnamese strings, and `DailyRoutinesPresentSimpleTest.kt`.
- Verified: focused grammar tests, `testDebugUnitTest`, `:app:compileDebugKotlin`, `assembleDebug`, and `git diff --check` passed according to the implementation report.
- Manual limitation: device installation was blocked by device security policy; headless tests covered normalization, feedback, and completion progression.
- Known limitation: exercise seed content remains English-only; UI prompt and feedback wrappers are localized.
