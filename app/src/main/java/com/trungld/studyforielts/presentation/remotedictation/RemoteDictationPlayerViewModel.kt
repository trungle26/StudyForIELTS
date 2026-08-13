package com.trungld.studyforielts.presentation.remotedictation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.presentation.dictation.AudioPlayerManager
import com.trungld.studyforielts.presentation.dictation.DictationAudioUiState
import com.trungld.studyforielts.presentation.dictation.DictationStep
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemoteDictationPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RemoteDictationRepository,
    private val audioPlayerManager: AudioPlayerManager,
) : ViewModel() {

    private val lessonId: String = checkNotNull(savedStateHandle[LESSON_ID_ARGUMENT])
    private val indexFlow = MutableStateFlow(0)
    private val audioUrlFlow = MutableStateFlow("")

    private val lessonFlow = kotlinx.coroutines.flow.flow {
        val result = repository.refreshLesson(lessonId)
        result.getOrNull()?.let {
            audioUrlFlow.value = it.audioUrl
            emit(it)
        }
        repository.observeLesson(lessonId).collect { cached ->
            cached?.let {
                audioUrlFlow.value = it.audioUrl
                emit(it)
            }
        }
    }

    val uiState: StateFlow<RemoteDictationPlayerUiState> = combine(
        lessonFlow,
        indexFlow,
        audioPlayerManager.audioState,
    ) { lesson, index, audioState ->
        val sentence = lesson.sentences.getOrNull(index)
        val step = if (lesson.sentences.isEmpty()) DictationStep.LOADING else DictationStep.INPUTTING
        RemoteDictationPlayerUiState(
            isLoading = false,
            lessonId = lesson.id,
            title = lesson.title,
            level = lesson.level,
            sentences = lesson.sentences,
            currentIndex = index,
            currentSentence = sentence,
            step = step,
            audioState = audioState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        RemoteDictationPlayerUiState(lessonId = lessonId),
    )

    init { configureAudio() }

    fun onTogglePlayback() = audioPlayerManager.togglePlayback()
    fun onReplay() = audioPlayerManager.replaySegment()
    fun onNextSentence() {
        val current = uiState.value
        if (current.currentIndex < current.sentences.lastIndex) {
            indexFlow.value = current.currentIndex + 1
        }
    }

    private fun configureAudio() {
        viewModelScope.launch {
            uiState.collect { state ->
                val sentence = state.currentSentence ?: run {
                    audioPlayerManager.clearSegment(); return@collect
                }
                audioPlayerManager.configureSegment(
                    audioUrl = audioUrlFlow.value,
                    startMs = sentence.startTimeMs.toLong(),
                    endMs = sentence.endTimeMs.toLong(),
                    resumePositionMs = sentence.startTimeMs.toLong(),
                    shouldAutoPlay = state.step == DictationStep.INPUTTING,
                )
            }
        }
    }

    override fun onCleared() {
        audioPlayerManager.clearSegment()
        super.onCleared()
    }

    companion object { const val LESSON_ID_ARGUMENT = "remoteLessonId" }
}