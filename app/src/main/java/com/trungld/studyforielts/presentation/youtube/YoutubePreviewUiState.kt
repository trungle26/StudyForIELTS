package com.trungld.studyforielts.presentation.youtube

import com.trungld.studyforielts.domain.model.YoutubeDictationLesson
import com.trungld.studyforielts.domain.model.YoutubeVideo

data class YoutubePreviewUiState(
    val videoId: String = "",
    val video: YoutubeVideo? = null,
    val lesson: YoutubeDictationLesson? = null,
    val isLoadingVideo: Boolean = true,
    val isLoadingTranscript: Boolean = false,
    val errorMessage: String? = null,
) {
    val canStartDictation: Boolean
        get() = lesson?.sentences?.isNotEmpty() == true && !isLoadingTranscript
}
