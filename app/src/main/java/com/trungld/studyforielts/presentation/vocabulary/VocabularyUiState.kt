package com.trungld.studyforielts.presentation.vocabulary

import com.trungld.studyforielts.data.local.entity.VocabularyEntity

data class VocabularyUiState(
    val lessonId: Long = 0L,
    val vocabularies: List<VocabularyEntity> = emptyList(),
) {
    val totalCount: Int
        get() = vocabularies.size
}
