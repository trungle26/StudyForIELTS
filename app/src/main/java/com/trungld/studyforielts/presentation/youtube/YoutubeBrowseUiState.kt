package com.trungld.studyforielts.presentation.youtube

import com.trungld.studyforielts.domain.model.YoutubeVideo

data class YoutubeBrowseUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchResults: List<YoutubeVideo> = emptyList(),
    val savedVideos: List<YoutubeVideo> = emptyList(),
    val errorMessage: String? = null,
)
