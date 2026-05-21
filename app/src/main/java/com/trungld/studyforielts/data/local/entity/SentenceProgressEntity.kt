package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "sentence_progress",
    primaryKeys = ["lessonId", "sentenceId"],
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SentenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["lessonId"]),
        Index(value = ["sentenceId"]),
    ],
)
data class SentenceProgressEntity(
    val lessonId: Long,
    val sentenceId: Long,
    val userAnswer: String,
    val isCorrect: Boolean,
    val attemptsCount: Int,
    val status: SentenceStatus,
    val lastCheckedAt: Long,
)
