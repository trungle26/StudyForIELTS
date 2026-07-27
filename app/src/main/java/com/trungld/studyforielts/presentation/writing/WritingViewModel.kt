package com.trungld.studyforielts.presentation.writing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.trungld.studyforielts.BuildConfig
import com.trungld.studyforielts.data.remote.api.WritingApi
import com.trungld.studyforielts.data.remote.model.EssaySubmissionDto
import com.trungld.studyforielts.data.remote.model.Task1EssaySubmissionDto
import com.trungld.studyforielts.data.remote.model.WritingEvaluationDto
import com.trungld.studyforielts.data.remote.model.WritingLessonDto
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
 * Supports two modes:
 * - **Free Task 2** (no `lessonId` arg): the existing flow — the user edits the
 *   prompt inline and submits to `/writing/evaluate/stream`.
 * - **Lesson-driven** (with `lessonId`): the lesson is fetched on init, the
 *   prompt is locked read-only, and submission goes to either
 *   `/writing/evaluate/task1/stream` (for Task 1 lessons with a chart image)
 *   or `/writing/evaluate/stream` (for Task 2 lessons). The Task 1 chart is
 *   loaded by Coil separately from
 *   `${BuildConfig.YOUTUBE_BFF_BASE_URL}writing/lessons/{id}/image`.
 *
 * Networking is intentionally direct (WritingApi -> ViewModel). The screen has
 * a single endpoint and a single error mapping; introducing a repository would
 * be YAGNI here.
 *
 * The submit path streams the LLM's response: it calls the appropriate
 * streaming endpoint, parses the Server-Sent-Events frames line-by-line, and
 * updates `Streaming` with the running text on every `data: <chunk>`. The
 * final `event: done` carries the validated `WritingEvaluation` JSON, which is
 * parsed and stored in `Success`.
 */
@HiltViewModel
class WritingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val writingApi: WritingApi,
) : ViewModel() {

    private val gson = Gson()

    /** Optional nav arg; when present we operate in lesson-driven mode. */
    private val lessonId: String? = savedStateHandle.get<String>(LESSON_ID_ARGUMENT)?.takeIf { it.isNotBlank() }

    private val _essayText = MutableStateFlow("")
    val essayText: StateFlow<String> = _essayText.asStateFlow()

    private val _uiState = MutableStateFlow<WritingUiState>(WritingUiState.Idle)
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    private val _lesson = MutableStateFlow<WritingLessonDto?>(null)
    val lesson: StateFlow<WritingLessonDto?> = _lesson.asStateFlow()

    private val _prompt = MutableStateFlow(DEFAULT_TASK_2_PROMPT)
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    /** True when the prompt card is read-only (lesson-driven mode). */
    val isPromptLocked: Boolean = lessonId != null

    /** Absolute URL of the lesson's chart image (Task 1 only); null otherwise. */
    val lessonImageUrl: String?
        get() = lesson.value?.let { l ->
            if (l.taskType == "task1") {
                BuildConfig.YOUTUBE_BFF_BASE_URL.trimEnd('/') +
                    "/writing/lessons/${l.id}/image"
            } else null
        }

    init {
        if (lessonId != null) loadLesson(lessonId)
    }

    private fun loadLesson(id: String) {
        viewModelScope.launch {
            try {
                val fetched = writingApi.getLesson(id)
                _lesson.value = fetched
                _prompt.value = fetched.taskPrompt
            } catch (e: Exception) {
                android.util.Log.e("WritingViewModel", "Failed to load lesson $id", e)
                _uiState.value = WritingUiState.Error(
                    "Failed to load lesson: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    fun onEssayChange(newText: String) {
        _essayText.value = newText
    }

    /** No-op when [isPromptLocked] is true; the prompt is read-only. */
    fun onPromptChange(newPrompt: String) {
        if (isPromptLocked) return
        _prompt.value = newPrompt
    }

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
        // If we have a lesson, prefer lesson-driven submission. Otherwise free Task 2.
        val activeLesson = _lesson.value
        if (activeLesson != null) {
            if (activeLesson.taskType == "task1") {
                submitTask1(lesson = activeLesson, essay = essay)
                return
            }
            // Task 2 lesson: submit using the lesson's prompt (which is already in _prompt).
            submitTask2(prompt = activeLesson.taskPrompt, essay = essay)
            return
        }
        // Free practice (no lesson).
        submitTask2(prompt = _prompt.value, essay = essay)
    }

    private fun submitTask1(lesson: WritingLessonDto, essay: String) {
        _uiState.value = WritingUiState.Submitting
        viewModelScope.launch {
            try {
                streamEvaluation {
                    writingApi.evaluateTask1Stream(
                        Task1EssaySubmissionDto(lessonId = lesson.id, essayText = essay)
                    )
                }
            } catch (e: HttpException) {
                android.util.Log.e("WritingViewModel", "HTTP error evaluating task1 essay", e)
                _uiState.value = WritingUiState.Error(
                    "Server error: ${e.code()} ${e.message()}".trim()
                )
            } catch (e: IOException) {
                android.util.Log.e("WritingViewModel", "Network error evaluating task1 essay", e)
                _uiState.value = WritingUiState.Error(
                    "Network error: please check your connection and try again."
                )
            } catch (e: Exception) {
                android.util.Log.e("WritingViewModel", "Unexpected error evaluating task1 essay", e)
                _uiState.value = WritingUiState.Error(
                    e.message ?: "Unexpected error."
                )
            }
        }
    }

    private fun submitTask2(prompt: String, essay: String) {
        _uiState.value = WritingUiState.Submitting
        viewModelScope.launch {
            try {
                streamEvaluation {
                    writingApi.evaluateEssayStream(
                        EssaySubmissionDto(taskPrompt = prompt, essayText = essay)
                    )
                }
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
     * caller (mapped in [submitTask1] / [submitTask2]); `event: error` frames
     * from the server are mapped to a user-friendly [WritingUiState.Error].
     */
    private suspend fun streamEvaluation(open: suspend () -> okhttp3.ResponseBody) = withContext(Dispatchers.IO) {
        val body = open()
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

        /** Optional `lessonId` nav argument; absent = free Task 2 practice. */
        const val LESSON_ID_ARGUMENT = "lessonId"

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