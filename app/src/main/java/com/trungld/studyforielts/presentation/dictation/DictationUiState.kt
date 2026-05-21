package com.trungld.studyforielts.presentation.dictation

import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.entity.SentenceProgressEntity
import com.trungld.studyforielts.domain.model.CheckResult

data class DictationUiState(
    val isLoading: Boolean = true,
    val lessonId: Long? = null,
    val lesson: LessonEntity? = null,
    val sentences: List<SentenceEntity> = emptyList(),
    val sentenceProgresses: Map<Long, SentenceProgressEntity> = emptyMap(),
    val progress: ProgressEntity? = null,
    val currentSentence: SentenceEntity? = null,
    val currentDraft: String = "",
    val step: DictationStep = DictationStep.LOADING,
    val feedback: CheckResult? = null,
) {
    val progressPercentage: Float
        get() = progress?.progressPercentage ?: 0f
}
