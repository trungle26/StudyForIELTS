package com.trungld.studyforielts.presentation.remotedictation.vocabulary

import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity

data class RemoteVocabularyUiState(
    val lessonServerId: String = "",
    val isLoading: Boolean = true,
    val allVocabularies: List<RemoteVocabularyEntity> = emptyList(),
    val queueVocabularies: List<RemoteVocabularyEntity> = emptyList(),
    val errorMessage: String? = null,
) {
    val learnedCount: Int
        get() = allVocabularies.count { it.isLearned }

    val totalCount: Int
        get() = allVocabularies.size

    val remainingCount: Int
        get() = queueVocabularies.size

    val visibleStack: List<RemoteVocabularyEntity>
        get() = queueVocabularies.take(3)

    val isCompleted: Boolean
        get() = totalCount > 0 && queueVocabularies.isEmpty()

    val isEmpty: Boolean
        get() = !isLoading && totalCount == 0
}