package com.trungld.studyforielts.presentation.youtube

import com.trungld.studyforielts.domain.model.YoutubeVideo

data class YoutubeBrowseUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isLoadingFeed: Boolean = false,
    val hasSearched: Boolean = false,
    val selectedLevel: String = "B1",
    val feedVideos: List<YoutubeVideo> = emptyList(),
    val searchResults: List<YoutubeVideo> = emptyList(),
    val savedVideos: List<YoutubeVideo> = emptyList(),
    val errorMessage: String? = null,
) {
    val isBusy: Boolean
        get() = isSearching || isLoadingFeed
}
