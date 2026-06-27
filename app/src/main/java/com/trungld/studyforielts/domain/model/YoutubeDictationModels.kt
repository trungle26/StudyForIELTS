package com.trungld.studyforielts.domain.model

data class YoutubeVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelTitle: String = "",
    val level: String? = null,
    val durationSeconds: Int? = null,
    val tags: List<String> = emptyList(),
    val isSaved: Boolean = false,
)

data class YoutubeSentence(
    val orderIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
)

data class YoutubeDictationLesson(
    val video: YoutubeVideo,
    val language: String?,
    val languageCode: String?,
    val isGenerated: Boolean?,
    val sentences: List<YoutubeSentence>,
)
