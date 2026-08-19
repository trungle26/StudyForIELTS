package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "remote_dictation_progress",
    primaryKeys = ["lessonServerId"],
    foreignKeys = [
        ForeignKey(
            entity = RemoteDictationLessonEntity::class,
            parentColumns = ["serverId"],
            childColumns = ["lessonServerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["lessonServerId"])],
)
data class RemoteDictationProgressEntity(
    val lessonServerId: String,
    val currentSentenceIndex: Int,
    val progressPercentage: Float,
    val currentDraftText: String,
    val lastPlaybackPositionMs: Long,
    val isLessonCompleted: Boolean,
    val updatedAt: Long,
)