# Feature: Topic Vocabulary Metadata and Review

## Status

Complete as of 2026-09-17. Merged to `master` in commit `fae417b`.

## Goal

Define a narrow vocabulary data model and review flow that works for any topic, starting with daily routines. The vocabulary activity in the daily-routines path should use this model instead of inline content.

## Scope

1. A local vocabulary data model with word, meaning, phonetic, example, topic, and CEFR level.
2. A vocabulary review activity that shows learned words, tracks mastery, and supports retry.
3. Seed data for the daily-routines topic.
4. Integration with the existing daily-routines path vocabulary and review activities.

## Non-goals

- No BFF vocabulary endpoint in this feature.
- No spaced-repetition algorithm.
- No audio pronunciation playback.
- No vocabulary import or export.
- No cross-topic vocabulary merge or deduplication.

## Data model

```text
TopicVocabulary
  id: String (stable, e.g. "daily_routines_wake_up")
  word: String
  phonetic: String (optional)
  meaning: String
  exampleSentence: String
  topic: String (e.g. "daily_routines")
  cefrLevel: String (e.g. "A1")
  skill: String (e.g. "vocabulary")
```

Store in Room. Keep the schema additive — do not break existing tables.

## Review model

```text
VocabularyProgress
  vocabularyId: String
  learned: Boolean
  attempts: Int
  lastAttemptAt: Long (timestamp)
```

Store in Room alongside vocabulary data.

## User flow

```text
Daily routines path -> Vocabulary activity
  -> Show word, meaning, phonetic, example
  -> Mark as learned or retry
  -> Track progress locally

Daily routines path -> Review activity
  -> Show learned words
  -> Quiz or flashcard review
  -> Update mastery
```

## UI requirements

- Show word, meaning, and example clearly.
- Provide a simple learned/not-learned toggle or quiz interaction.
- Show review progress (e.g. 4/10 learned).
- Support English and Vietnamese strings.
- Accessible touch targets and screen-reader labels.
- Reuse existing typography and component patterns.

## Acceptance criteria

- Daily-routines vocabulary activity uses the Room-backed model instead of inline content.
- Daily-routines review activity queries learned vocabulary and provides a review interaction.
- Vocabulary progress survives app restart.
- Adding a new topic requires only inserting seed data, not changing the review UI.
- Existing daily-routines completion persistence continues to work.
- Existing remote dictation and writing flows remain usable.
- English and Vietnamese resources compile.
- Focused tests cover vocabulary data operations and review logic.
- All Android build checks pass.

## Implementation boundary

Inspect before editing:

- `app/src/main/java/com/trungld/studyforielts/presentation/dailyroutines/` or equivalent path screen.
- `app/src/main/java/com/trungld/studyforielts/data/local/database/AppDatabase.kt`
- `app/src/main/java/com/trungld/studyforielts/data/local/entity/`
- `app/src/main/java/com/trungld/studyforielts/data/local/dao/`
- Existing vocabulary patterns in remote dictation.

Do not build a generic curriculum engine, spaced-repetition system, BFF vocabulary API, or cross-topic merge.

## Verification

- Run focused vocabulary and review tests.
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew :app:compileDebugKotlin`.
- Run `./gradlew assembleDebug`.
- Manually verify vocabulary display, learned toggle, review quiz, restart persistence, and Vietnamese strings.

## Follow-up

After this feature is stable, expand to foundation grammar lessons using a similar narrow data model.

## Implementation handoff

- Branch: `feature/topic-vocabulary-review` merged to `master`.
- Commit: `fae417b`.
- 17 changed files including Room v10→11 migration, TopicVocabulary and VocabularyProgress entities, DAO, repository, ViewModel, activity screen, 10 daily-routines A1 seed words, 10 EN + 10 VI strings, 8 focused tests, and Room schema export.
- Verified: `testDebugUnitTest`, `compileDebugKotlin`, and `assembleDebug` all passed.
- Known limitations: seed data covers daily-routines A1 only; no instrumented Room tests (pure-function tests per project convention).
