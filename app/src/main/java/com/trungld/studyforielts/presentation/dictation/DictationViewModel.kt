package com.trungld.studyforielts.presentation.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.model.DictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.repository.DictationRepository
import com.trungld.studyforielts.domain.usecase.CheckAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DictationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dictationRepository: DictationRepository,
    private val checkAnswerUseCase: CheckAnswerUseCase,
    private val audioPlayerManager: AudioPlayerManager,
) : ViewModel() {

    private val activeLessonId = MutableStateFlow<Long?>(null)
    private val sessionStep = MutableStateFlow(DictationStep.LOADING)
    private val currentFeedback = MutableStateFlow<CheckResult?>(null)
    private val draftOverride = MutableStateFlow<String?>(null)
    private var persistDraftJob: Job? = null

    private val lessonSnapshotFlow = activeLessonId.flatMapLatest { lessonId ->
        if (lessonId == null) {
            flowOf(null)
        } else {
            dictationRepository.observeLessonSnapshot(lessonId)
        }
    }

    private val sessionState = combine(
        activeLessonId,
        lessonSnapshotFlow,
        sessionStep,
        currentFeedback,
        draftOverride,
    ) { lessonId, snapshot, step, feedback, draft ->
        SessionState(
            lessonId = lessonId,
            snapshot = snapshot,
            step = step,
            feedback = feedback,
            draftOverride = draft,
        )
    }

    val uiState: StateFlow<DictationUiState> = combine(
        sessionState,
        audioPlayerManager.audioState,
    ) { session, audioState ->
        buildUiState(
            lessonId = session.lessonId,
            snapshot = session.snapshot,
            step = session.step,
            feedback = session.feedback,
            draftOverride = session.draftOverride,
            audioState = audioState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DictationUiState(),
    )

    init {
        val lessonId = checkNotNull(savedStateHandle.get<Long>(LESSON_ID_ARGUMENT))
        loadLesson(lessonId)
        observeAudioConfiguration()
        persistPlaybackProgress()
    }

    fun loadLesson(lessonId: Long) {
        activeLessonId.value = lessonId
        draftOverride.value = null
        currentFeedback.value = null
        sessionStep.value = DictationStep.LOADING

        viewModelScope.launch {
            dictationRepository.ensureLessonProgress(lessonId)
            sessionStep.value = DictationStep.INPUTTING
        }
    }

    fun onDraftChanged(newDraft: String) {
        val lessonId = activeLessonId.value ?: return
        draftOverride.value = newDraft

        persistDraftJob?.cancel()
        persistDraftJob = viewModelScope.launch {
            delay(250)
            dictationRepository.saveDraft(
                lessonId = lessonId,
                draft = newDraft,
            )
        }
    }

    fun onTogglePlayback() {
        audioPlayerManager.togglePlayback()
    }

    fun onReplaySegment() {
        audioPlayerManager.replaySegment()
    }

    fun onPrimaryAction() {
        when (uiState.value.step) {
            DictationStep.INPUTTING -> submitCurrentAnswer()
            DictationStep.REVIEWING -> continueToNextSentence()
            else -> Unit
        }
    }

    fun submitCurrentAnswer() {
        val lessonId = activeLessonId.value ?: return
        val state = uiState.value
        val currentSentence = state.currentSentence ?: return
        val currentDraft = state.currentDraft
        val result = checkAnswerUseCase(
            expectedText = currentSentence.correctText,
            userText = currentDraft,
        )

        viewModelScope.launch {
            dictationRepository.submitSentenceAnswer(
                lessonId = lessonId,
                sentence = currentSentence,
                userAnswer = currentDraft,
                result = result,
            )

            draftOverride.value = if (result.isCorrect) "" else currentDraft
            currentFeedback.value = if (result.isCorrect) null else result
            sessionStep.value = when {
                result.isCorrect && isLastSentence(state, currentSentence) -> DictationStep.COMPLETED
                result.isCorrect -> DictationStep.INPUTTING
                else -> DictationStep.REVIEWING
            }
        }
    }

    fun continueToNextSentence() {
        val lessonId = activeLessonId.value ?: return
        val state = uiState.value
        val currentSentence = state.currentSentence ?: return

        viewModelScope.launch {
            dictationRepository.continueAfterReview(
                lessonId = lessonId,
                sentence = currentSentence,
            )

            draftOverride.value = ""
            currentFeedback.value = null
            sessionStep.value = if (isLastSentence(state, currentSentence)) {
                DictationStep.COMPLETED
            } else {
                DictationStep.INPUTTING
            }
        }
    }

    fun skipCurrentSentence() {
        val lessonId = activeLessonId.value ?: return
        val state = uiState.value
        val currentSentence = state.currentSentence ?: return

        viewModelScope.launch {
            dictationRepository.skipSentence(
                lessonId = lessonId,
                sentence = currentSentence,
                draft = state.currentDraft,
            )

            draftOverride.value = ""
            currentFeedback.value = null
            sessionStep.value = if (isLastSentence(state, currentSentence)) {
                DictationStep.COMPLETED
            } else {
                DictationStep.INPUTTING
            }
        }
    }

    fun resetLessonProgress() {
        val lessonId = activeLessonId.value ?: return
        viewModelScope.launch {
            dictationRepository.resetLessonProgress(lessonId)
            draftOverride.value = ""
            currentFeedback.value = null
            sessionStep.value = DictationStep.INPUTTING
        }
    }

    override fun onCleared() {
        audioPlayerManager.release()
        super.onCleared()
    }

    private fun observeAudioConfiguration() {
        viewModelScope.launch {
            uiState.map { state ->
                val sentence = state.currentSentence
                val lesson = state.lesson
                if (
                    state.isLoading ||
                    sentence == null ||
                    lesson == null ||
                    state.step == DictationStep.COMPLETED
                ) {
                    null
                } else {
                    AudioConfig(
                        audioUrl = lesson.audioUrl,
                        startMs = sentence.startTime,
                        endMs = sentence.endTime,
                        resumePositionMs = state.progress?.lastPlaybackPositionMs ?: sentence.startTime,
                        shouldAutoPlay = state.step == DictationStep.INPUTTING,
                    )
                }
            }
                .distinctUntilChanged()
                .collect { config ->
                    if (config == null) {
                        audioPlayerManager.clearSegment()
                    } else {
                        audioPlayerManager.configureSegment(
                            audioUrl = config.audioUrl,
                            startMs = config.startMs,
                            endMs = config.endMs,
                            resumePositionMs = config.resumePositionMs,
                            shouldAutoPlay = config.shouldAutoPlay,
                        )
                    }
                }
        }
    }

    private fun persistPlaybackProgress() {
        viewModelScope.launch {
            activeLessonId
                .filterNotNull()
                .collectLatest { lessonId ->
                    var lastPersistedSecond = -1L
                    audioPlayerManager.audioState.collectLatest { audioState ->
                        val secondBucket = audioState.currentPositionMs / 1_000L
                        if (
                            audioState.isAvailable &&
                            audioState.currentPositionMs > 0L &&
                            secondBucket != lastPersistedSecond
                        ) {
                            lastPersistedSecond = secondBucket
                            dictationRepository.updatePlaybackPosition(
                                lessonId = lessonId,
                                playbackPositionMs = audioState.currentPositionMs,
                            )
                        }
                    }
                }
        }
    }

    private fun buildUiState(
        lessonId: Long?,
        snapshot: DictationLessonSnapshot?,
        step: DictationStep,
        feedback: CheckResult?,
        draftOverride: String?,
        audioState: DictationAudioUiState,
    ): DictationUiState {
        if (lessonId == null || snapshot == null) {
            return DictationUiState(
                isLoading = true,
                lessonId = lessonId,
                step = DictationStep.LOADING,
                audioState = audioState,
            )
        }

        val progress = snapshot.progress
        val orderedSentences = snapshot.sentences.sortedBy(SentenceEntity::orderIndex)
        val currentSentence = orderedSentences
            .firstOrNull { it.orderIndex == (progress?.currentSentenceIndex ?: 0) }

        val resolvedStep = when {
            progress?.isLessonCompleted == true -> DictationStep.COMPLETED
            step == DictationStep.LOADING -> DictationStep.INPUTTING
            else -> step
        }

        return DictationUiState(
            isLoading = false,
            lessonId = lessonId,
            lesson = snapshot.lesson,
            sentences = orderedSentences,
            sentenceProgresses = snapshot.sentenceProgressEntries.associateBy { it.sentenceId },
            progress = progress,
            currentSentence = currentSentence,
            currentDraft = draftOverride ?: progress?.currentDraftText.orEmpty(),
            step = resolvedStep,
            feedback = feedback,
            audioState = audioState,
        )
    }

    private fun isLastSentence(
        state: DictationUiState,
        sentence: SentenceEntity,
    ): Boolean {
        val totalSentences = state.sentences.size
        return totalSentences == 0 || sentence.orderIndex >= totalSentences - 1
    }

    private data class AudioConfig(
        val audioUrl: String,
        val startMs: Long,
        val endMs: Long,
        val resumePositionMs: Long,
        val shouldAutoPlay: Boolean,
    )

    private data class SessionState(
        val lessonId: Long?,
        val snapshot: DictationLessonSnapshot?,
        val step: DictationStep,
        val feedback: CheckResult?,
        val draftOverride: String?,
    )

    companion object {
        const val LESSON_ID_ARGUMENT = "lessonId"
    }
}
