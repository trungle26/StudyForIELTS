package com.trungld.studyforielts.data.remote.model

import com.google.gson.annotations.SerializedName

data class YoutubeSearchResponseDto(
    @SerializedName("query")
    val query: String,
    @SerializedName("results")
    val results: List<YoutubeSearchResultDto>,
)

data class YoutubeSearchResultDto(
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("thumbnails")
    val thumbnails: List<YoutubeThumbnailDto> = emptyList(),
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
)

data class YoutubeThumbnailDto(
    @SerializedName("url")
    val url: String,
    @SerializedName("width")
    val width: Int? = null,
    @SerializedName("height")
    val height: Int? = null,
)

data class YoutubeFeedResponseDto(
    @SerializedName("level")
    val level: String,
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("total")
    val total: Int,
    @SerializedName("totalPages")
    val totalPages: Int,
    @SerializedName("items")
    val items: List<YoutubeFeedItemDto>,
)

data class YoutubeFeedItemDto(
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("channelTitle")
    val channelTitle: String? = null,
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    @SerializedName("durationSeconds")
    val durationSeconds: Int? = null,
    @SerializedName("level")
    val level: String? = null,
    @SerializedName("computedLevel")
    val computedLevel: String? = null,
    @SerializedName("confidence")
    val confidence: Double? = null,
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
)

data class YoutubeTranscriptResponseDto(
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("language")
    val language: String? = null,
    @SerializedName("languageCode")
    val languageCode: String? = null,
    @SerializedName("isGenerated")
    val isGenerated: Boolean? = null,
    @SerializedName("segments")
    val segments: List<YoutubeTranscriptSegmentDto>,
)

data class YoutubeTranscriptSegmentDto(
    @SerializedName("startTime")
    val startTime: Double,
    @SerializedName("endTime")
    val endTime: Double,
    @SerializedName("text")
    val text: String,
)
