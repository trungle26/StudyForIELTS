package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "progress",
    primaryKeys = ["lessonId"],
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["lessonId"]),
    ],
)
data class ProgressEntity(
    val lessonId: Long,
    val currentSentenceIndex: Int,
    val progressPercentage: Float,
    val currentDraftText: String,
    val lastPlaybackPositionMs: Long,
    val isLessonCompleted: Boolean,
    val updatedAt: Long,
)
