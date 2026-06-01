package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "youtube_videos",
    indices = [
        Index(value = ["isSaved"]),
    ],
)
data class YoutubeVideoEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val transcriptLanguage: String?,
    val transcriptLanguageCode: String?,
    val isTranscriptGenerated: Boolean?,
    val isSaved: Boolean,
    val cachedAt: Long,
)
