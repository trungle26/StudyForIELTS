# Feature: Learner Profile and Onboarding

## Status

Implementation complete on 2026-09-14. Automated verification passed; manual acceptance checks remain to be confirmed.

## Goal

Capture enough learner context to recommend an appropriate first activity without requiring authentication or pretending to provide a precise IELTS score assessment.

## Non-goals

- No account or server synchronization.
- No official IELTS placement score.
- No complex adaptive testing engine.
- No AI-generated curriculum.
- No redesign of all top-level navigation.

## User flow

```text
First launch
  -> Welcome
  -> Choose level, estimate level, or explore
  -> Select current CEFR level if known
  -> Select target IELTS band, optional
  -> Select daily study time
  -> Select one or more focus skills
  -> Save profile locally
  -> Show recommended first activity
```

Users must be able to skip unknown fields and change the profile later.

## Profile fields

Minimum first version:

```text
onboardingCompleted: Boolean
currentCefrLevel: A1 | A2 | B1 | B2 | C1 | C2 | UNKNOWN
targetBand: 4.5 | 5.0 | 5.5 | 6.0 | 6.5 | 7.0 | 7.5 | 8.0 | 8.5 | 9.0 | NONE
dailyGoalMinutes: 10 | 20 | 30 | 45 | 60
focusSkills: set of LISTENING, READING, WRITING, SPEAKING, GRAMMAR, VOCABULARY, PRONUNCIATION
updatedAt: timestamp
```

The exact Kotlin representation should follow existing project conventions. Store this state locally with DataStore, not Room, because it is small preference-like state rather than lesson data.

## Placement option

The first version may use a short self-selection flow:

- I am a beginner.
- I can handle basic everyday English.
- I can understand general English but need IELTS preparation.
- I already know my CEFR level.
- I am not sure.

If the user is unsure, use `UNKNOWN` and recommend a low-risk diagnostic activity. Do not call the result a certified level.

A formal placement test is a separate feature.

## Recommendation rules

Initial deterministic rules are sufficient:

- `A1` or `A2`: foundation vocabulary, grammar, pronunciation, or beginner dictation.
- `B1`: vocabulary, grammar review, general listening, or short writing.
- `B2` and above with a target band: IELTS skill content related to the focus skill.
- `UNKNOWN`: a short mixed diagnostic or beginner-friendly activity.
- No target band: foundation/general-English recommendations remain valid.

Prefer one clear recommended action over a large list.

## UI requirements

- Explain CEFR and IELTS band in plain language.
- Do not imply that a target band is the learner's current score.
- Keep each screen focused on one decision.
- Provide Back, Skip, and Edit Profile actions.
- Support Vietnamese strings alongside English strings where localization already exists.
- Ensure all choices are keyboard, screen-reader, and touch accessible.
- Show a progress indicator for the onboarding steps.

## Files likely affected

Inspect before editing:

- `app/src/main/java/com/trungld/studyforielts/navigation/StudyForIeltsNavGraph.kt`
- `app/src/main/java/com/trungld/studyforielts/presentation/level/LevelListScreen.kt`
- `app/src/main/java/com/trungld/studyforielts/presentation/level/LevelListViewModel.kt`
- `app/src/main/java/com/trungld/studyforielts/di/`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-vi/strings.xml`

Likely new files:

- `app/src/main/java/com/trungld/studyforielts/data/preferences/LearnerProfilePreferences.kt`
- `app/src/main/java/com/trungld/studyforielts/domain/model/LearnerProfile.kt`
- `app/src/main/java/com/trungld/studyforielts/presentation/onboarding/OnboardingScreen.kt`
- `app/src/main/java/com/trungld/studyforielts/presentation/onboarding/OnboardingViewModel.kt`

Do not create these files automatically if an existing profile/preferences pattern already exists.

## Acceptance criteria

- A first-time user is routed to onboarding or can explicitly skip it.
- A returning user is not shown onboarding repeatedly.
- The selected profile survives app restart.
- The profile can be edited from an accessible location.
- A recommendation is shown after onboarding.
- `UNKNOWN` level and no target band are valid states.
- Existing local dictation, listening, writing, and navigation flows remain usable.
- English and Vietnamese resources compile.
- Unit tests cover serialization/defaults and recommendation rules.

## Verification

```text
./gradlew testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
```

Also manually verify:

- Fresh install flow.
- Skip flow.
- App restart.
- Edit profile.
- Unknown-level recommendation.
- Back navigation and bottom navigation state.

## Risks

- Mixing CEFR and IELTS band into one level field.
- Blocking existing users with mandatory onboarding.
- Adding a new navigation controller unnecessarily.
- Storing profile data in Room without a real content relationship.
- Making recommendations too complex before content metadata exists.
