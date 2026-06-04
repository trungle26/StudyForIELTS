package com.trungld.studyforielts.presentation.youtube

import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.YoutubeDictationLesson
import com.trungld.studyforielts.domain.model.YoutubeSentence
import com.trungld.studyforielts.presentation.dictation.DictationStep

data class YoutubeDictationUiState(
    val videoId: String = "",
    val isLoading: Boolean = true,
    val lesson: YoutubeDictationLesson? = null,
    val currentSentenceIndex: Int = 0,
    val currentDraft: String = "",
    val step: DictationStep = DictationStep.LOADING,
    val feedback: CheckResult? = null,
    val currentSecond: Float = 0f,
    val errorMessage: String? = null,
) {
    val sentences: List<YoutubeSentence>
        get() = lesson?.sentences.orEmpty()

    val currentSentence: YoutubeSentence?
        get() = sentences.getOrNull(currentSentenceIndex)

    val completedCount: Int
        get() = when (step) {
            DictationStep.COMPLETED -> sentences.size
            else -> currentSentenceIndex.coerceAtMost(sentences.size)
        }

    val progressPercentage: Float
        get() = if (sentences.isEmpty()) {
            0f
        } else {
            (completedCount.toFloat() / sentences.size.toFloat()) * 100f
        }
}
