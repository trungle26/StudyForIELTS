package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "youtube_sentences",
    primaryKeys = ["videoId", "orderIndex"],
    foreignKeys = [
        ForeignKey(
            entity = YoutubeVideoEntity::class,
            parentColumns = ["videoId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["videoId"]),
    ],
)
data class YoutubeSentenceEntity(
    val videoId: String,
    val orderIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
)
