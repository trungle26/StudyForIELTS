package com.trungld.studyforielts.presentation.writing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.trungld.studyforielts.data.remote.api.WritingApi
import com.trungld.studyforielts.data.remote.model.EssaySubmissionDto
import com.trungld.studyforielts.data.remote.model.WritingEvaluationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * ViewModel for the Writing Practice screen.
 *
 * Holds the in-progress essay text so it survives configuration changes, and
 * exposes a [WritingUiState] flow that the Compose layer renders.
 *
 * Networking is intentionally direct (WritingApi -> ViewModel). The screen has
 * a single endpoint and a single error mapping; introducing a repository would
 * be YAGNI here.
 *
 * The submit path streams the LLM's response: it calls `/writing/evaluate/stream`,
 * parses the Server-Sent-Events frames line-by-line, and updates `Streaming`
 * with the running text on every `data: <chunk>`. The final `event: done` carries
 * the validated `WritingEvaluation` JSON, which is parsed and stored in `Success`.
 */
@HiltViewModel
class WritingViewModel @Inject constructor(
    private val writingApi: WritingApi,
) : ViewModel() {

    private val gson = Gson()

    private val _essayText = MutableStateFlow("")
    val essayText: StateFlow<String> = _essayText.asStateFlow()

    private val _uiState = MutableStateFlow<WritingUiState>(WritingUiState.Idle)
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    fun onEssayChange(newText: String) {
        _essayText.value = newText
    }

    /** Allow the prompt to be edited inline (e.g. user pastes their own task). */
    fun onPromptChange(newPrompt: String) {
        _prompt.value = newPrompt
    }

    private val _prompt = MutableStateFlow(DEFAULT_TASK_2_PROMPT)
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    /**
     * Submit the current essay to the LLM tutor.
     * No-op if a request is already in flight or the essay is too short.
     */
    fun submit() {
        val essay = _essayText.value.trim()
        if (essay.isEmpty()) return
        val current = _uiState.value
        if (current is WritingUiState.Submitting || current is WritingUiState.Streaming) return
        if (essay.wordCount() < MIN_ESSAY_WORDS) {
            _uiState.value = WritingUiState.Error(
                "Please write at least $MIN_ESSAY_WORDS words so the tutor can give meaningful feedback."
            )
            return
        }

        _uiState.value = WritingUiState.Submitting
        viewModelScope.launch {
            try {
                streamEvaluation(essay)
            } catch (e: HttpException) {
                android.util.Log.e("WritingViewModel", "HTTP error evaluating essay", e)
                _uiState.value = WritingUiState.Error(
                    "Server error: ${e.code()} ${e.message()}".trim()
                )
            } catch (e: IOException) {
                android.util.Log.e("WritingViewModel", "Network error evaluating essay", e)
                _uiState.value = WritingUiState.Error(
                    "Network error: please check your connection and try again."
                )
            } catch (e: Exception) {
                android.util.Log.e("WritingViewModel", "Unexpected error evaluating essay", e)
                _uiState.value = WritingUiState.Error(
                    e.message ?: "Unexpected error."
                )
            }
        }
    }

    /**
     * Open the streaming endpoint, parse SSE frames, and update the UI state
     * as data arrives. Errors raised by the network call propagate to the
     * caller (mapped in [submit]); `event: error` frames from the server are
     * mapped to a user-friendly [WritingUiState.Error].
     */
    private suspend fun streamEvaluation(essay: String) = withContext(Dispatchers.IO) {
        val body = writingApi.evaluateEssayStream(
            EssaySubmissionDto(
                taskPrompt = _prompt.value,
                essayText = essay,
            )
        )
        body.source().use { source ->
            val buffer = StringBuilder()
            val accumulated = StringBuilder()
            // An SSE frame is one or more lines ending in a blank line (\n\n).
            // We read line-by-line and split on blank lines ourselves.
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isEmpty()) {
                    // End of frame: parse it and update UI.
                    if (buffer.isNotEmpty()) {
                        processSseFrame(buffer.toString(), accumulated)
                        buffer.clear()
                    }
                    continue
                }
                buffer.append(line).append('\n')
            }
            // Flush any trailing frame that didn't end with a blank line.
            if (buffer.isNotEmpty()) {
                processSseFrame(buffer.toString(), accumulated)
            }
        }
    }

    /**
     * Handle one SSE frame: emit `data:` chunks into [accumulated] (driving the
     * streaming UI) and short-circuit on `event: done` or `event: error`.
     */
    private fun processSseFrame(frame: String, accumulated: StringBuilder) {
        var eventName: String? = null
        val dataLines = mutableListOf<String>()
        for (line in frame.split('\n')) {
            when {
                line.startsWith("event: ") -> eventName = line.substring("event: ".length).trim()
                line.startsWith("data: ") -> dataLines.add(line.substring("data: ".length))
                line.startsWith("data:") -> dataLines.add(line.substring("data:".length).trimStart())
            }
        }
        val data = dataLines.joinToString("\n")
        when (eventName) {
            "done" -> {
                try {
                    val evaluation: WritingEvaluationDto = gson.fromJson(data, WritingEvaluationDto::class.java)
                    _uiState.value = WritingUiState.Success(evaluation)
                } catch (e: Exception) {
                    android.util.Log.e("WritingViewModel", "Failed to parse final evaluation JSON", e)
                    _uiState.value = WritingUiState.Error("Failed to parse evaluation results.")
                }
            }
            "error" -> {
                _uiState.value = WritingUiState.Error(data.ifBlank { "Server reported an error." })
            }
            "warn" -> {
                // Non-fatal (e.g. persistence failed). Log and continue.
                android.util.Log.w("WritingViewModel", "Server warn: $data")
            }
            else -> {
                if (data.isNotEmpty() && data != "[connected]") {
                    accumulated.append(data)
                    _uiState.value = WritingUiState.Streaming(accumulated.toString())
                }
            }
        }
    }

    /** Reset to Idle so the user can edit and submit again. */
    fun reset() {
        _uiState.update { WritingUiState.Idle }
    }

    companion object {
        /** Minimum essay length to avoid noisy / low-signal LLM calls. */
        const val MIN_ESSAY_WORDS = 50

        /**
         * Hardcoded sample IELTS Writing Task 2 prompt.
         * Replaced by a fetched prompt from the backend in Phase 4.
         */
        const val DEFAULT_TASK_2_PROMPT =
            "Some people believe that the best way to deal with traffic congestion is to " +
                "build more roads, while others think that improving public transport is a " +
                "better solution. Discuss both views and give your own opinion."
    }
}

/** Whitespace-delimited word count. */
private fun String.wordCount(): Int =
    trim().split(Regex("\\s+")).count { it.isNotBlank() }