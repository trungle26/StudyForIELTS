# Feature: Foundation Dictation — Daily Routines

## Status

done. This is the next narrow foundation feature after present simple grammar.

## Goal

Provide A1 beginner learners with their first short dictation practice using daily-routines vocabulary and sentences.

## Scope

- Seed a beginner A1 daily-routines dictation lesson (e.g. 3–5 short sentences).
- Connect the dictation activity in the daily-routines path to this lesson.
- Support sentence playback/audio controls or text-to-speech fallback.
- Immediate correctness check per sentence with case- and punctuation-insensitive comparison.
- Sentence-by-sentence progression with retry on mistake.
- Persist completion locally via existing daily-routines preferences/Room patterns.
- English and Vietnamese localization.

## Non-goals

- No new BFF endpoint or server synchronization.
- No complex audio editing or custom waveforms.
- No IELTS band scoring algorithm.
- No AI transcript generation.

## Content

- Topic: `daily_routines`
- CEFR: `A1`
- Target vocabulary: `wake up`, `eat breakfast`, `go to school`, `study`, `sleep`

Sentences:
1. "I wake up early."
2. "She eats breakfast."
3. "We go to school."
4. "He studies every day."
5. "They sleep at night."

## Acceptance criteria

- Opening dictation from the daily-routines path launches the daily-routines dictation lesson.
- Sentences can be listened to and typed.
- Case, surrounding whitespace, and ending punctuation differences are normalized.
- Incorrect attempts can be retried with feedback.
- Completing all sentences marks the dictation activity complete.
- All unit tests and build checks pass.

## Verification

- `./gradlew testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew assembleDebug`
