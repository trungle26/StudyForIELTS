package com.trungld.studyforielts.presentation.youtube

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.model.YoutubeSentence
import com.trungld.studyforielts.domain.repository.OnlineYoutubeDictationRepository
import com.trungld.studyforielts.domain.usecase.CheckAnswerUseCase
import com.trungld.studyforielts.presentation.dictation.DictationStep
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class YoutubeDictationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OnlineYoutubeDictationRepository,
    private val checkAnswerUseCase: CheckAnswerUseCase,
) : ViewModel() {

    private val videoId = checkNotNull(savedStateHandle.get<String>(VIDEO_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(YoutubeDictationUiState(videoId = videoId))
    val uiState: StateFlow<YoutubeDictationUiState> = _uiState.asStateFlow()

    private val _playerCommands = MutableSharedFlow<YoutubePlayerCommand>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val playerCommands: SharedFlow<YoutubePlayerCommand> = _playerCommands.asSharedFlow()

    private var lastLoopTriggerMs = 0L
    private var isPlayerReady = false

    init {
        viewModelScope.launch {
            repository.observeSavedLesson(videoId).collect { lesson ->
                _uiState.update { current ->
                    when {
                        lesson == null -> current.copy(
                            isLoading = false,
                            errorMessage = "This video is not cached. Go back and prepare it first.",
                        )

                        lesson.sentences.isEmpty() -> current.copy(
                            isLoading = false,
                            lesson = lesson,
                            errorMessage = "The cached transcript has no sentences.",
                        )

                        else -> current.copy(
                            isLoading = false,
                            lesson = lesson,
                            step = if (current.step == DictationStep.LOADING) {
                                DictationStep.INPUTTING
                            } else {
                                current.step
                            },
                            errorMessage = null,
                        )
                    }
                }
                if (lesson?.sentences?.isNotEmpty() == true && isPlayerReady) {
                    loadCurrentSentenceInPlayer()
                }
            }
        }
    }

    fun onPlayerReady() {
        isPlayerReady = true
        loadCurrentSentenceInPlayer()
    }

    private fun loadCurrentSentenceInPlayer() {
        val sentence = uiState.value.currentSentence ?: return
        emitPlayerCommand(
            YoutubePlayerCommand.LoadVideo(
                videoId = videoId,
                startSeconds = sentence.startSeconds,
            ),
        )
    }

    fun onCurrentSecond(second: Float) {
        _uiState.update { it.copy(currentSecond = second) }

        val state = uiState.value
        val sentence = state.currentSentence ?: return
        if (state.step == DictationStep.COMPLETED || state.step == DictationStep.LOADING) return
        if (second < sentence.startSeconds) return

        val reachedSegmentEnd = second >= sentence.endSeconds - LOOP_TOLERANCE_SECONDS
        val loopGuardElapsed = SystemClock.elapsedRealtime() - lastLoopTriggerMs > LOOP_GUARD_MS
        if (reachedSegmentEnd && loopGuardElapsed) {
            lastLoopTriggerMs = SystemClock.elapsedRealtime()
            emitPlayerCommand(YoutubePlayerCommand.SeekTo(sentence.startSeconds))
        }
    }

    fun onDraftChanged(draft: String) {
        _uiState.update {
            it.copy(
                currentDraft = draft,
                feedback = null,
                step = if (it.step == DictationStep.REVIEWING) DictationStep.INPUTTING else it.step,
            )
        }
    }

    fun onReplaySentence() {
        seekToCurrentSentence()
    }

    fun onPrimaryAction() {
        when (uiState.value.step) {
            DictationStep.INPUTTING -> submitCurrentAnswer()
            DictationStep.REVIEWING -> moveToNextSentence()
            else -> Unit
        }
    }

    fun submitCurrentAnswer() {
        val state = uiState.value
        val sentence = state.currentSentence ?: return
        val result = checkAnswerUseCase(
            expectedText = sentence.text,
            userText = state.currentDraft,
        )

        if (!result.isCorrect) {
            _uiState.update {
                it.copy(
                    feedback = result,
                    step = DictationStep.REVIEWING,
                )
            }
            return
        }

        if (state.currentSentenceIndex >= state.sentences.lastIndex) {
            _uiState.update {
                it.copy(
                    currentDraft = "",
                    feedback = null,
                    step = DictationStep.COMPLETED,
                )
            }
            emitPlayerCommand(YoutubePlayerCommand.Pause)
        } else {
            _uiState.update {
                it.copy(
                    currentSentenceIndex = it.currentSentenceIndex + 1,
                    currentDraft = "",
                    feedback = null,
                    step = DictationStep.INPUTTING,
                )
            }
            seekToCurrentSentence()
        }
    }

    fun skipCurrentSentence() {
        moveToNextSentence()
    }

    fun resetSession() {
        _uiState.update {
            it.copy(
                currentSentenceIndex = 0,
                currentDraft = "",
                feedback = null,
                step = if (it.sentences.isEmpty()) DictationStep.LOADING else DictationStep.INPUTTING,
            )
        }
        seekToCurrentSentence()
    }

    private fun moveToNextSentence() {
        val state = uiState.value
        if (state.currentSentence == null) return

        if (state.currentSentenceIndex >= state.sentences.lastIndex) {
            _uiState.update {
                it.copy(
                    currentDraft = "",
                    feedback = null,
                    step = DictationStep.COMPLETED,
                )
            }
            emitPlayerCommand(YoutubePlayerCommand.Pause)
        } else {
            _uiState.update {
                it.copy(
                    currentSentenceIndex = it.currentSentenceIndex + 1,
                    currentDraft = "",
                    feedback = null,
                    step = DictationStep.INPUTTING,
                )
            }
            seekToCurrentSentence()
        }
    }

    private fun seekToCurrentSentence() {
        lastLoopTriggerMs = 0L
        val sentence = uiState.value.currentSentence ?: return
        emitPlayerCommand(YoutubePlayerCommand.SeekTo(sentence.startSeconds))
    }

    private fun emitPlayerCommand(command: YoutubePlayerCommand) {
        viewModelScope.launch {
            _playerCommands.emit(command)
        }
    }

    private val YoutubeSentence.startSeconds: Float
        get() = startTimeMs / MILLIS_PER_SECOND

    private val YoutubeSentence.endSeconds: Float
        get() = endTimeMs / MILLIS_PER_SECOND

    companion object {
        const val VIDEO_ID_ARGUMENT = "videoId"
        private const val MILLIS_PER_SECOND = 1_000f
        private const val LOOP_TOLERANCE_SECONDS = 0.12f
        private const val LOOP_GUARD_MS = 700L
    }
}
