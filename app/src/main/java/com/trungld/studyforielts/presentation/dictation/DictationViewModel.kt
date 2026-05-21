package com.trungld.studyforielts.presentation.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.model.DictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.repository.DictationRepository
import com.trungld.studyforielts.domain.usecase.CheckAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DictationViewModel @Inject constructor(
    private val dictationRepository: DictationRepository,
    private val checkAnswerUseCase: CheckAnswerUseCase,
) : ViewModel() {

    private val activeLessonId = MutableStateFlow<Long?>(null)
    private val sessionStep = MutableStateFlow(DictationStep.LOADING)
    private val currentFeedback = MutableStateFlow<CheckResult?>(null)
    private val draftOverride = MutableStateFlow<String?>(null)
    private var persistDraftJob: Job? = null

    val uiState: StateFlow<DictationUiState> = combine(
        activeLessonId,
        activeLessonId.flatMapLatest { lessonId ->
            if (lessonId == null) {
                flowOf(null)
            } else {
                dictationRepository.observeLessonSnapshot(lessonId)
            }
        },
        sessionStep,
        currentFeedback,
        draftOverride,
    ) { lessonId, snapshot, step, feedback, draft ->
        buildUiState(
            lessonId = lessonId,
            snapshot = snapshot,
            step = step,
            feedback = feedback,
            draftOverride = draft,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DictationUiState(),
    )

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

    fun onPlaybackPositionChanged(positionMs: Long) {
        val lessonId = activeLessonId.value ?: return
        viewModelScope.launch {
            dictationRepository.updatePlaybackPosition(
                lessonId = lessonId,
                playbackPositionMs = positionMs,
            )
        }
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

    private fun buildUiState(
        lessonId: Long?,
        snapshot: DictationLessonSnapshot?,
        step: DictationStep,
        feedback: CheckResult?,
        draftOverride: String?,
    ): DictationUiState {
        if (lessonId == null || snapshot == null) {
            return DictationUiState(
                isLoading = true,
                lessonId = lessonId,
                step = DictationStep.LOADING,
            )
        }

        val progress = snapshot.progress
        val currentSentence = snapshot.sentences
            .sortedBy(SentenceEntity::orderIndex)
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
            sentences = snapshot.sentences.sortedBy(SentenceEntity::orderIndex),
            sentenceProgresses = snapshot.sentenceProgressEntries.associateBy { it.sentenceId },
            progress = progress,
            currentSentence = currentSentence,
            currentDraft = draftOverride ?: progress?.currentDraftText.orEmpty(),
            step = resolvedStep,
            feedback = feedback,
        )
    }

    private fun isLastSentence(
        state: DictationUiState,
        sentence: SentenceEntity,
    ): Boolean {
        val totalSentences = state.sentences.size
        return totalSentences == 0 || sentence.orderIndex >= totalSentences - 1
    }
}
