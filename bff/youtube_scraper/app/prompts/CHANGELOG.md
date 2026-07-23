# Writing System Prompt — Changelog

## Task 2 (Essay)

- **v2** — Added essay delimiter instruction (`<<<ESSAY_START>>>` / `<<<ESSAY_END>>>`) for prompt injection defense.
- **v1** — Initial Simon-style Band 9 system prompt (extracted from inline `llm_service.py`).

## Task 1 (Academic)

- **v1** — Initial Task 1 system prompt: Task Achievement focus (overview placement, key feature selection, data specificity, no opinion), same `WritingEvaluation` JSON schema as Task 2, image-aware injection defense.
