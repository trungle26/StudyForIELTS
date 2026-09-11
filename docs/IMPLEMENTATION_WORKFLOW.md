# Multi-Chat Implementation Workflow

Use the planning chat for product decisions and one implementation chat per vertical feature.

## Planning chat responsibilities

This chat should maintain:

- `docs/PRODUCT_SCOPE.md`
- `docs/LEARNING_METHOD.md`
- `docs/CURRICULUM_MAP.md`
- Feature specifications under `docs/features/`
- Architecture and product decisions
- Feature ordering and acceptance criteria

It should not implement several unrelated features in one change.

## Implementation chat responsibilities

Each implementation chat should:

1. Read the assigned feature document.
2. Inspect the current code and existing conventions.
3. Confirm the implementation boundary before editing.
4. Make the smallest working change.
5. Add focused tests or a runnable self-check for non-trivial logic.
6. Run the specified verification commands.
7. Report changed files, test results, and remaining limitations.

## Recommended Git workflow

Create one branch per feature:

```text
feature/learner-profile
feature/shared-content-metadata
feature/foundation-dictation
feature/topic-vocabulary
feature/grammar-lessons
feature/foundation-reading
feature/foundation-writing
feature/ielts-listening
feature/speaking-recording
feature/offline-audio
```

Keep commits small and reversible. Do not combine AWS deployment with curriculum features.

## Prompt template for a new implementation chat

Copy this prompt and replace the feature name:

```text
Implement only the feature described in
`docs/features/<feature-name>.md`.

Before editing:
1. Read the feature document completely.
2. Inspect the existing related files and project conventions.
3. Identify the smallest set of files required.
4. Do not redesign unrelated code or add future abstractions.

Implementation requirements:
- Preserve existing behavior.
- Follow current Kotlin, Compose, Room, Retrofit, and Hilt patterns.
- Keep CEFR level, IELTS target band, skill, and learning stage separate.
- Include accessibility and localization where UI changes are made.
- Add focused tests for non-trivial logic.

Verification:
- Run the commands listed in the feature document.
- If a command fails, diagnose and fix only issues within this feature scope.
- Report changed files, verification output, and known limitations.

Do not update the roadmap status unless the acceptance criteria are actually verified.
```

## Review chat prompt

After implementation, use a separate chat with:

```text
Review the implementation of `<feature-name>` against
`docs/features/<feature-name>.md`.

Check:
- Scope creep
- Existing behavior regressions
- Data model separation
- Offline and error states
- Accessibility
- Localization
- Tests and build verification
- Security and privacy implications

Do not edit code initially. Return prioritized findings with file paths and line references.
```

Only create a fix chat after the review identifies a concrete issue.

## Recommended order

1. Learner profile and onboarding.
2. Shared content metadata and level vocabulary.
3. Beginner-friendly dictation content.
4. Topic vocabulary metadata and review.
5. Foundation grammar lessons.
6. Foundation reading.
7. Foundation writing.
8. IELTS listening question types.
9. Speaking recording and playback.
10. Personalized home recommendations.
11. Offline audio downloads.
12. Writing lesson and chart-image cache.
13. Progress synchronization.
14. S3 media storage.
15. ECS/Fargate deployment.

## Handoff format

At the end of every implementation chat, record:

```text
Feature:
Branch:
Status:
Changed files:
Tests run:
Builds run:
Known limitations:
Follow-up feature:
```

The repository documents and Git history are the shared memory between chats. Do not depend on a previous chat's context being available.
