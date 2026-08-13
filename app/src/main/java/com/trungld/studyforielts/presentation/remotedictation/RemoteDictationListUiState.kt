package com.trungld.studyforielts.presentation.remotedictation

import com.trungld.studyforielts.domain.model.RemoteDictationLesson

data class RemoteDictationListUiState(
    val level: String = "",
    val lessons: List<RemoteDictationLesson> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
