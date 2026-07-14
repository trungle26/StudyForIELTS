package com.trungld.studyforielts.presentation.writing

import com.trungld.studyforielts.data.remote.model.WritingEvaluationDto

/**
 * UI state for the Writing Practice screen.
 *
 * The screen has two independent state slots: the user-controlled essay text
 * (held in the ViewModel so it survives recomposition) and the LLM call status.
 * Call status is modelled as a sealed interface so the Compose layer can use
 * a simple `when` over the cases.
 *
 * During streaming we expose the running text + a `Streaming` state; once the
 * server emits `event: done` the state transitions to `Success`.
 */
sealed interface WritingUiState {
    /** Initial state — no submission yet. */
    data object Idle : WritingUiState

    /** Submission in flight; the UI shows a progress indicator. */
    data object Submitting : WritingUiState

    /**
     * Stream is open and we are receiving LLM deltas. [partialText] is the
     * running concatenation of all `data: <chunk>` events seen so far.
     */
    data class Streaming(val partialText: String) : WritingUiState

    /** LLM returned a structured evaluation. */
    data class Success(val evaluation: WritingEvaluationDto) : WritingUiState

    /** Network or parse failure. The message is user-safe to display. */
    data class Error(val message: String) : WritingUiState
}
