package com.trungld.studyforielts.presentation.remotedictation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.cache.AudioCacheManager
import com.trungld.studyforielts.data.cache.AudioDownloadState
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.model.RemoteDictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.domain.usecase.CheckAnswerUseCase
import com.trungld.studyforielts.presentation.dictation.AudioPlayerManager
import com.trungld.studyforielts.presentation.dictation.DictationAudioUiState
import com.trungld.studyforielts.presentation.dictation.DictationStep
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
class RemoteDictationPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RemoteDictationRepository,
    private val checkAnswerUseCase: CheckAnswerUseCase,
    private val audioPlayerManager: AudioPlayerManager,
    private val audioCacheManager: AudioCacheManager,
) : ViewModel() {

    private val activeLessonId = MutableStateFlow<String?>(null)
    private val sessionStep = MutableStateFlow(DictationStep.LOADING)
    private val currentFeedback = MutableStateFlow<CheckResult?>(null)
    private val draftOverride = MutableStateFlow<String?>(null)
    private var persistDraftJob: Job? = null

    private val snapshotFlow = activeLessonId.flatMapLatest { lessonId ->
        if (lessonId == null) flowOf(null)
        else repository.observeLessonSnapshot(lessonId)
    }

    private val sessionState = combine(
        activeLessonId,
        snapshotFlow,
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

    val uiState: StateFlow<RemoteDictationPlayerUiState> = combine(
        sessionState,
        audioPlayerManager.audioState,
        audioCacheManager.state,
    ) { session, audioState, downloadMap ->
        val lessonId = session.lessonId
        val downloadState = lessonId?.let { downloadMap[it] } ?: AudioDownloadState.IDLE
        buildUiState(
            lessonId = lessonId,
            snapshot = session.snapshot,
            step = session.step,
            feedback = session.feedback,
            draftOverride = session.draftOverride,
            audioState = audioState,
            downloadState = downloadState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemoteDictationPlayerUiState(),
    )

    init {
        val lessonId = checkNotNull(savedStateHandle.get<String>(LESSON_ID_ARGUMENT))
        loadLesson(lessonId)
        observeAudioConfiguration()
        persistPlaybackProgress()
    }

    fun loadLesson(lessonId: String) {
        activeLessonId.value = lessonId
        draftOverride.value = null
        currentFeedback.value = null
        sessionStep.value = DictationStep.LOADING

        viewModelScope.launch {
            repository.refreshLesson(lessonId)
            repository.ensureLessonProgress(lessonId)
            // LRU touch for offline audio eviction. No-op if not yet downloaded.
            repository.touchLesson(lessonId)
            sessionStep.value = DictationStep.INPUTTING
        }
    }

    fun onDraftChanged(newDraft: String) {
        val lessonId = activeLessonId.value ?: return
        draftOverride.value = newDraft

        persistDraftJob?.cancel()
        persistDraftJob = viewModelScope.launch {
            delay(250)
            repository.saveDraft(lessonId = lessonId, draft = newDraft)
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
            expectedText = currentSentence.text,
            userText = currentDraft,
        )

        viewModelScope.launch {
            repository.submitSentenceAnswer(
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
            repository.continueAfterReview(lessonId = lessonId, sentence = currentSentence)

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
            repository.skipSentence(
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
            repository.resetLessonProgress(lessonId)
            draftOverride.value = ""
            currentFeedback.value = null
            sessionStep.value = DictationStep.INPUTTING
        }
    }

    fun removeDownloadedAudio() {
        val lessonId = activeLessonId.value ?: return
        viewModelScope.launch { audioCacheManager.remove(lessonId) }
    }

    override fun onCleared() {
        audioPlayerManager.release()
        super.onCleared()
    }

    private fun observeAudioConfiguration() {
        viewModelScope.launch {
            uiState
                .map { state ->
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
                        // Prefer the local file when available, otherwise stream the remote URL.
                        // Auto-download kicks off on first play if the lesson has a remote URL.
                        val localFile = audioCacheManager.localFile(lesson.serverId)
                        val effectiveUrl = when {
                            localFile != null -> "file://${localFile.absolutePath}"
                            else -> lesson.audioUrl
                        }
                        if (localFile == null && lesson.audioUrl.isNotBlank()) {
                            audioCacheManager.ensureLocalAudio(lesson.serverId, lesson.audioUrl)
                        }
                        AudioConfig(
                            audioUrl = effectiveUrl,
                            startMs = sentence.startTimeMs.toLong(),
                            endMs = sentence.endTimeMs.toLong(),
                            resumePositionMs = state.progress?.lastPlaybackPositionMs
                                ?: sentence.startTimeMs.toLong(),
                            shouldAutoPlay = state.step == DictationStep.INPUTTING,
                        )
                    }
                }
                .distinctUntilChanged { old, new ->
                    old?.audioUrl == new?.audioUrl &&
                        old?.startMs == new?.startMs &&
                        old?.endMs == new?.endMs &&
                        old?.shouldAutoPlay == new?.shouldAutoPlay
                }
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
                            repository.updatePlaybackPosition(
                                lessonId = lessonId,
                                playbackPositionMs = audioState.currentPositionMs,
                            )
                        }
                    }
                }
        }
    }

    private fun buildUiState(
        lessonId: String?,
        snapshot: RemoteDictationLessonSnapshot?,
        step: DictationStep,
        feedback: CheckResult?,
        draftOverride: String?,
        audioState: DictationAudioUiState,
        downloadState: AudioDownloadState,
    ): RemoteDictationPlayerUiState {
        if (lessonId == null || snapshot == null) {
            return RemoteDictationPlayerUiState(
                isLoading = true,
                lessonId = lessonId,
                step = DictationStep.LOADING,
                audioState = audioState,
                audioDownload = downloadState,
            )
        }

        val progress = snapshot.progress
        val orderedSentences = snapshot.sentences.sortedBy(RemoteDictationSentenceEntity::orderIndex)
        val currentSentence = orderedSentences
            .firstOrNull { it.orderIndex == (progress?.currentSentenceIndex ?: 0) }

        val resolvedStep = when {
            progress?.isLessonCompleted == true -> DictationStep.COMPLETED
            step == DictationStep.LOADING -> DictationStep.INPUTTING
            else -> step
        }

        val hasLocal = !snapshot.lesson.localAudioPath.isNullOrBlank()

        return RemoteDictationPlayerUiState(
            isLoading = false,
            lessonId = lessonId,
            lesson = snapshot.lesson,
            sentences = orderedSentences,
            sentenceProgresses = snapshot.sentenceProgressEntries.associateBy { it.orderIndex },
            progress = progress,
            currentSentence = currentSentence,
            currentDraft = draftOverride ?: progress?.currentDraftText.orEmpty(),
            step = resolvedStep,
            feedback = feedback,
            audioState = audioState,
            audioSource = if (hasLocal) AudioSource.LOCAL else AudioSource.REMOTE,
            audioDownload = downloadState,
        )
    }

    private fun isLastSentence(
        state: RemoteDictationPlayerUiState,
        sentence: RemoteDictationSentenceEntity,
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
        val lessonId: String?,
        val snapshot: RemoteDictationLessonSnapshot?,
        val step: DictationStep,
        val feedback: CheckResult?,
        val draftOverride: String?,
    )

    companion object {
        const val LESSON_ID_ARGUMENT = "remoteLessonId"
    }
}