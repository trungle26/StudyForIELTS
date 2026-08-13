package com.trungld.studyforielts.presentation.remotedictation

import com.trungld.studyforielts.domain.model.RemoteDictationSentence
import com.trungld.studyforielts.presentation.dictation.DictationAudioUiState
import com.trungld.studyforielts.presentation.dictation.DictationStep

data class RemoteDictationPlayerUiState(
    val isLoading: Boolean = true,
    val lessonId: String? = null,
    val title: String = "",
    val level: String = "",
    val sentences: List<RemoteDictationSentence> = emptyList(),
    val currentIndex: Int = 0,
    val currentSentence: RemoteDictationSentence? = null,
    val step: DictationStep = DictationStep.LOADING,
    val audioState: DictationAudioUiState = DictationAudioUiState(),
)