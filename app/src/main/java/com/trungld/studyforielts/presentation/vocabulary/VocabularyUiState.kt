package com.trungld.studyforielts.presentation.vocabulary

import com.trungld.studyforielts.data.local.entity.VocabularyEntity

data class VocabularyUiState(
    val lessonId: Long = 0L,
    val allVocabularies: List<VocabularyEntity> = emptyList(),
    val queueVocabularies: List<VocabularyEntity> = emptyList(),
) {
    val learnedCount: Int
        get() = allVocabularies.count { it.isLearned }

    val totalCount: Int
        get() = allVocabularies.size

    val remainingCount: Int
        get() = queueVocabularies.size

    val currentVocabulary: VocabularyEntity?
        get() = queueVocabularies.firstOrNull()

    val visibleStack: List<VocabularyEntity>
        get() = queueVocabularies.take(3)

    val isCompleted: Boolean
        get() = totalCount > 0 && queueVocabularies.isEmpty()
}
