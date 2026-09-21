# Web App Shell, Information Architecture, and Foundation Hub

## Status

Planned. Immediate priority to organize the web experience into a coherent learning system aligned with Ngoc Bach's pedagogy before implementing further skill slices.

## Goal

Restructure the web application from an unorganized single-page dump into an intentional, topic-driven learning system.

In Ngoc Bach's IELTS self-study methodology:
1. **Foundation English precedes exam-level IELTS drilling.** Beginners (CEFR A1/A2) must not be greeted with a confusing wall of 100+ intermediate listening passages.
2. **Learning is organized by Topic.** Each topic (starting with Daily Routines) forms a cohesive multi-skill cycle:
   ```text
   Topic Vocabulary -> Grammar in Context -> Transcript Dictation -> Reading Comprehension -> Review
   ```
3. **Skill libraries are practice resources, not the foundation path.** The 125-lesson dictation catalog is a valuable listening resource, but it belongs in its own dedicated section, distinct from the guided foundation track.

## Information Architecture (IA)

The web client must provide a clean app shell with two primary top-level areas:

```text
[ StudyForIELTS ] ── [ Foundation Track ] ── [ Listening & Dictation Library ] ── [ EN | VI ]
```

### 1. Foundation Track (`#/` or `#/foundation`) — Default View
The guided curriculum for foundation learners.
- **Header:**
  - Stage & Level: `Stage 1: Foundation · A1`
  - Topic: `Topic 1: Daily Routines (Thói quen hàng ngày)`
  - Objective: Master everyday routine vocabulary, basic present simple tense, listening transcription, and short reading comprehension.
  - Overall Topic Progress summary: e.g., `X of 4 activities completed` with accessible progress bar.
- **Sequential Activity Cards (The Learning Loop):**
  - **Step 1: Topic Vocabulary (Từ vựng chủ đề)**
    - Learn 8 daily-routines words in context with meaning, phonetic, and retrieval exercises.
    - Badges: `A1 · Vocabulary` | Status: `Not started` / `Completed`
    - Action: `Start vocabulary` / `Review vocabulary`
  - **Step 2: Present Simple Grammar (Ngữ pháp Hiện tại đơn)**
    - Affirmative sentence structures (`I/you/we/they + V`, `he/she + V-s`) and routine time expressions.
    - Badges: `A1 · Grammar` | Status: `Not started` / `Completed`
    - Action: `Practice grammar` / `Review grammar`
  - **Step 3: Daily Routines Dictation (Nghe chép chính tả)**
    - Listen and transcribe core routine sentences word-by-word with replay and instant check.
    - Badges: `A1 · Dictation` | Status: `Not started` / `Completed`
    - Action: `Start dictation` / `Practice again`
  - **Step 4: Reading Comprehension (Đọc hiểu chủ đề)**
    - Read a short original daily-routines passage and answer comprehension questions.
    - Badges: `A1 · Reading` | Status: `Coming soon` / `Start reading`

### 2. Listening & Dictation Library (`#/dictation`)
The full catalog of listening dictation lessons (currently 125 conversations):
- **Header:**
  - Title: `Listening & Dictation Library`
  - Description: Train ear-to-text accuracy and spelling across diverse conversations.
  - Level filter: `All`, `A1`, `B1`, etc.
- **Lesson Catalog Grid:**
  - Cards for individual audio lessons with level badge, title, duration, and open action.
- **Lesson Detail View (`#/dictation/lesson/:id`):**
  - Dedicated focused player view with audio playback, transcript sentences, vocabulary list, and dictation input.
  - Clear `← Back to Library` navigation.

### 3. Mutual View Exclusion (No Stacking/Dumping)
- When an activity (Vocabulary Review, Grammar Exercise, or Lesson Detail) is active, **only that activity renders in the main container**.
- The 125-card catalog or Foundation Hub cards **must never render underneath an active exercise**.
- Every activity screen must have a prominent, accessible `← Back to Foundation` or `← Back to Library` button returning to the parent hub.

## Local Progress Rollup

The Foundation Hub must aggregate progress from existing local storage keys:
- Vocabulary: `studyforielts.daily-routines.vocabulary` (`completed: boolean`)
- Grammar: `studyforielts.daily-routines.grammar` (`completed: boolean`)
- Dictation: `studyforielts.daily-routines.dictation` (or lesson completion)
- Reading: `studyforielts.daily-routines.reading` (reserved for reading slice)

If local storage is empty or fails, default gracefully to uncompleted without crashing.

## UI & Accessibility Requirements

- **Zero Emojis:** Per project design policy (`CLAUDE.md`), never emit emojis in UI, code, or labels. Use plain words or accessible SVGs.
- **Navigation Shell:**
  - Top nav bar with persistent brand title, tab switcher (`Foundation` vs `Dictation Library`), and language switcher (`EN` / `VI`).
  - Active tab indicated visually with border/background and semantically via `aria-current="page"`.
- **Keyboard & Focus:**
  - All interactive controls have `:focus-visible` styling.
  - Semantic landmark regions (`<header>`, `<nav>`, `<main>`).
- **Responsive Layout:**
  - Seamless layout from 320px mobile up to 1200px desktop.
  - No horizontal scrolling.
  - Mobile view collapses nav tabs into clean touch targets.
- **Localization:**
  - All shell copy, tab names, activity descriptions, and status badges localized in English and Vietnamese.

## Non-goals

- Backend API changes (use existing `/dictation/lessons` endpoints).
- Android changes.
- Authentication or remote database syncing.
- Complex state management libraries (use clean React hooks and URL hash routing).
- Flashy animations or extraneous UI libraries.

## Acceptance Criteria

1. Navigating to the root URL loads the **Foundation Track: Daily Routines Hub** by default.
2. Foundation Hub displays the sequential learning steps: Vocabulary → Grammar → Dictation → Reading.
3. Each activity card displays clear metadata, skill badge, and local progress status (`Completed` or `Not started`).
4. Clicking an activity opens ONLY that activity view, with a clear Back link to return to the Foundation Hub.
5. Switching to the **Listening & Dictation Library** tab displays the catalog of conversation lessons.
6. Opening a lesson from the library displays only the lesson detail view with a Back link to the library.
7. Language switcher toggles UI between English and Vietnamese across the shell and hub.
8. Direct URL hashes (`#/`, `#/foundation`, `#/foundation/vocabulary`, `#/foundation/grammar`, `#/dictation`, `#/dictation/lesson/:id`) link directly to the correct views and survive browser refresh.
9. Layout is fully responsive down to 320px with zero horizontal scroll and passes all automated tests.
10. Existing functionality of vocabulary review, grammar exercises, and dictation playback remains completely preserved and verified.

## Verification

Run focused checks:
- Web typecheck: `npm run typecheck`
- Web tests: `npm test` (test tab switching, hash routing, activity view isolation, progress badges, language toggle)
- Web production build: `npm run build`
- `git diff --check`

After merging to `staging`:
1. Wait for Render deployment to complete (1-2 minutes).
2. Verify live deployment at `https://studyforielts.onrender.com/`.
3. Confirm the clean Foundation Hub renders first, navigation tabs work, and active activities render without background clutter.

## Suggested Handoff Prompt

```text
Implement only `docs/features/web-app-shell-and-navigation.md`.

Read this document, `docs/IMPLEMENTATION_WORKFLOW.md`, `docs/DEPLOYMENT_WORKFLOW.md`,
and `CLAUDE.md`. Pull the latest `staging`, verify clean worktree, and create
`feature/web-app-shell-and-navigation` from the latest staging commit.

Refactor the web application shell to establish proper Information Architecture:
1. Two primary tabs in the top navigation: "Foundation Track" (default) and "Dictation Library".
2. Foundation Hub for "Daily Routines" showing sequential steps:
   Step 1: Vocabulary -> Step 2: Grammar -> Step 3: Dictation -> Step 4: Reading (Coming soon).
3. Ensure mutual view exclusivity: when an activity or lesson detail is open, do not render
   the catalog or hub underneath it. Provide clear Back navigation.
4. Support URL hash routing: `#/foundation`, `#/foundation/vocabulary`, `#/foundation/grammar`,
   `#/dictation`, `#/dictation/lesson/:id`.
5. Support English and Vietnamese language switching across all shell and hub elements.
6. Strictly adhere to the zero-emoji rule (use text or SVGs).
7. Preserve all existing functionality of vocabulary review, grammar, and lesson audio/sentences.

Add focused tests in `App.test.tsx` for view switching, hash routing, progress display,
and language toggle. Run `npm run typecheck`, `npm test`, `npm run build`, and `git diff --check`.

Before merging, squash all feature work into one feature-only commit. Merge
that single commit into `staging` with fast-forward only and push `staging`.
Wait for Render to redeploy, confirm deployment readiness, and perform manual QA.
Do not merge to `master`.

Report the exact staging commit, changed files, test output, manual checks,
known limitations, and promotion recommendation.
```
