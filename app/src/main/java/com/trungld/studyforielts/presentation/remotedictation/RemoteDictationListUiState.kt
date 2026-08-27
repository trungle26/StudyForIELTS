package com.trungld.studyforielts.presentation.remotedictation

import com.trungld.studyforielts.domain.model.CachedRemoteDictationLesson
import com.trungld.studyforielts.domain.model.RemoteDictationLesson

data class RemoteDictationListUiState(
    val level: String = "",
    val lessons: List<CachedRemoteDictationLesson> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
) {
    /** True when the cache is empty AND we have nothing to show (vs. just a refresh failure). */
    val isEmpty: Boolean get() = !isLoading && lessons.isEmpty() && errorMessage == null
}
