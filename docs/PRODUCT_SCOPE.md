# StudyForIELTS Product Scope

## Product direction

StudyForIELTS is a step-by-step English and IELTS learning system. It should support learners from basic English foundations through advanced IELTS preparation instead of presenting advanced IELTS practice as the default experience.

## Learning promise

The app helps a learner:

1. Understand their current English ability.
2. Build foundations in grammar, pronunciation, vocabulary, reading, and listening.
3. Progress into IELTS-specific listening, reading, speaking, and writing practice.
4. Practise in short, repeatable sessions.
5. Review mistakes and weak areas.
6. Track progress locally and continue studying with limited connectivity.

## Product boundaries

### Included

- Foundation English learning from approximately CEFR A1/A2 upward.
- IELTS preparation from beginner-compatible introduction through advanced practice.
- Local-first study progress.
- Curated content from the FastAPI backend and bundled seed content.
- Interactive practice, review, and progress feedback.
- Android offline support for previously cached or downloaded content where feasible.

### Not included yet

- A guaranteed IELTS score prediction.
- Fully automated speaking-band assessment.
- Unrestricted AI-generated curriculum without review.
- Copying or republishing copyrighted book content.
- Server synchronization of learner progress before authentication and a defined sync contract.

## Learner model

Keep these concepts separate:

- **CEFR level:** language difficulty, such as A1 through C2.
- **IELTS target band:** the learner's exam goal, such as 5.5 or 7.0.
- **Learning stage:** foundation English or IELTS preparation.
- **Skill:** grammar, pronunciation, vocabulary, listening, reading, speaking, or writing.
- **Study goal:** the learner's immediate priority and available time.

A learner can have CEFR A2 English and target IELTS 5.5. These values must not be treated as equivalent.

## Initial learner journey

```text
Onboarding
  -> Choose or estimate current level
  -> Choose target band and study goal
  -> Receive one short recommendation
  -> Complete a foundation lesson
  -> Review vocabulary and mistakes
  -> Continue from saved local progress
```

## Product principles

1. Assess before prescribing.
2. Build foundations before intensive exam strategy.
3. Teach vocabulary by topic, context, and repeated retrieval.
4. Use transcript-supported listening and dictation.
5. Start with short, achievable activities.
6. Give level-appropriate feedback instead of misleading band scores.
7. Make progress visible without requiring an account.
8. Prefer local-first state for study continuity.
9. Use AI for expensive, high-value feedback rather than every exercise.
10. Use original or properly licensed content and clearly separate inspiration from copied source material.

## Definition of a useful first release

The first expansion is successful when a learner below Band 6.5 can:

- Complete onboarding without already understanding IELTS terminology.
- Select or estimate a suitable level.
- See a clear next lesson.
- Finish a short vocabulary, grammar, or dictation activity.
- Review what they got wrong.
- Return later and continue from local progress.

## Non-goals for the first expansion

- Rebuild every top-level navigation tab.
- Add all four IELTS skills at once.
- Migrate the existing backend to AWS before the content model is stable.
- Introduce authentication only to store local preferences.
- Build a complex recommendation engine before basic progression works.
