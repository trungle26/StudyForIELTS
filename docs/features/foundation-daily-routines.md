# Feature: Foundation Daily Routines Slice

## Status

Complete as of 2026-09-15. The daily-routines path, all five activity destinations, persistent completion via DataStore, path progress display, and final completion state are implemented and verified.

## Goal

Provide one small, usable A1 learning path around daily routines. It should give a beginner a clear next activity without requiring IELTS strategy knowledge.

## Scope

Build the smallest vertical slice for the existing Android app and BFF:

1. Daily-routines vocabulary.
2. Present-simple grammar practice.
3. Short transcript-supported listening dictation.
4. Basic sentence-writing exercise.
5. Vocabulary review using existing local progress patterns where practical.

The slice should be discoverable from the home recommendation based on the learner profile.

## Content requirements

Topic: `daily_routines`

Target stage: `beginner`

Target CEFR: `A1`

Suggested vocabulary themes:

- Wake up and go to bed.
- Eat breakfast and get dressed.
- Go to work or school.
- Start and finish activities.
- Have lunch and come home.
- Study, relax, and sleep.

Grammar focus:

- Present simple affirmative sentences.
- Basic subject-verb agreement with `I`, `you`, `we`, `they`, `he`, and `she`.
- Common time expressions such as `every day`, `in the morning`, and `at night`.

Avoid teaching exceptions, advanced tense contrasts, or a large content-management system in this feature.

## User flow

```text
Home recommendation
  -> Daily routines learning path
  -> Vocabulary
  -> Present simple practice
  -> Short dictation
  -> Sentence writing
  -> Vocabulary review
  -> Completion summary
```

A learner may open individual activities without being permanently blocked by an incomplete previous activity.

## Data and API boundary

- Reuse existing BFF dictation and published-content patterns where they fit.
- Keep content metadata separate from learner profile data.
- Use stable content IDs and `contentType`, `skill`, `cefrLevel`, and topic metadata.
- Store learner attempts and completion locally using existing Room/progress patterns where possible.
- Do not add authentication or server synchronization.
- Do not introduce a generic curriculum engine.
- Do not migrate all existing lesson types.

## UI requirements

- Explain the activity goal in plain English and existing Vietnamese localization patterns.
- Show the topic and beginner/A1 context without implying an IELTS band score.
- Provide clear empty, loading, error, retry, and completion states for network-backed content.
- Keep touch targets and semantics accessible.
- Preserve existing bottom navigation and dictation/writing flows.
- Reuse existing components and typography rather than redesigning the application shell.

## Acceptance criteria

- A learner with an A1 or unknown profile can reach the daily-routines path from Home.
- The path contains at least one vocabulary activity, one present-simple activity, one short dictation, one sentence-writing activity, and one review step, or clearly reports which content is unavailable.
- Existing remote dictation continues to work.
- Progress and completion state survive app restart where the activity uses local persistence.
- Unknown-level learners receive a safe beginner recommendation.
- CEFR level, IELTS target band, learning stage, skill, and topic remain separate values.
- English and Vietnamese resources compile for all new UI strings.
- Focused tests cover recommendation/path selection and any non-trivial answer or review rules.
- Existing Android unit tests and build checks remain green.

## Implementation boundary

Before editing, inspect current home recommendation code, navigation, local lesson/progress entities, remote dictation repository, writing practice entry points, and BFF published-content endpoints. Identify the smallest vertical slice.

Do not implement all future foundation curriculum, adaptive testing, authentication, web UI, offline audio downloads, or AWS storage in this feature.

Likely areas to inspect:

- `app/src/main/java/com/trungld/studyforielts/presentation/home/`
- `app/src/main/java/com/trungld/studyforielts/presentation/onboarding/`
- `app/src/main/java/com/trungld/studyforielts/navigation/`
- `app/src/main/java/com/trungld/studyforielts/data/local/`
- `app/src/main/java/com/trungld/studyforielts/data/repository/`
- `bff/youtube_scraper/app/routers/dictation.py`
- `bff/youtube_scraper/app/models/dictation.py`

## Verification

- Run focused tests for the new path and recommendation behavior.
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew :app:compileDebugKotlin`.
- Run `./gradlew assembleDebug`.
- Manually verify A1, unknown-level, empty-content, retry, completion, restart, and Vietnamese flows.

## Follow-up

Wire vocabulary and dictation activities to existing full lesson screens when the UX needs lesson-level behavior beyond the current focused daily-routines flow. Then expand to additional foundation topics and grammar.

## Implementation handoff

- Branch: current working branch.
- Phase 1 (2026-09-15): `DailyRoutinesPath` metadata, recommendation logic, `DailyRoutinesScreen`, five activity entries, English and Vietnamese strings, focused recommendation/path tests.
- Phase 2 (2026-09-15): `DailyRoutinesProgressPreferences.kt` (DataStore), `DailyRoutinesViewModel`, persistent completion for all five activities, path progress display, final completion state, completed-activity markers.
- Verified: `./gradlew testDebugUnitTest`, `./gradlew :app:compileDebugKotlin`, and `./gradlew assembleDebug` passed. No diagnostics in edited Kotlin files.
- Known limitations: vocabulary and dictation activities use focused local prompts rather than navigating to existing full lesson screens. No new BFF endpoint or remote content integration was added.
