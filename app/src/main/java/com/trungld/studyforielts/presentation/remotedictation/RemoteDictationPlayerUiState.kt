package com.trungld.studyforielts.presentation.remotedictation

import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationProgressEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceProgressEntity
import com.trungld.studyforielts.data.cache.AudioDownloadState
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.presentation.dictation.DictationAudioUiState
import com.trungld.studyforielts.presentation.dictation.DictationStep

data class RemoteDictationPlayerUiState(
    val isLoading: Boolean = true,
    val lessonId: String? = null,
    val lesson: RemoteDictationLessonEntity? = null,
    val sentences: List<RemoteDictationSentenceEntity> = emptyList(),
    val sentenceProgresses: Map<Int, RemoteDictationSentenceProgressEntity> = emptyMap(),
    val progress: RemoteDictationProgressEntity? = null,
    val currentSentence: RemoteDictationSentenceEntity? = null,
    val currentDraft: String = "",
    val step: DictationStep = DictationStep.LOADING,
    val feedback: CheckResult? = null,
    val audioState: DictationAudioUiState = DictationAudioUiState(),
    val audioSource: AudioSource = AudioSource.REMOTE,
    val audioDownload: AudioDownloadState = AudioDownloadState.IDLE,
) {
    val progressPercentage: Float
        get() = progress?.progressPercentage ?: 0f
}

/** Where the player is currently reading audio from. */
enum class AudioSource { LOCAL, REMOTE }