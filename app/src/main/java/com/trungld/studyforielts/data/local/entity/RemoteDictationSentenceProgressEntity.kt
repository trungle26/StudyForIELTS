package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "remote_dictation_sentence_progress",
    primaryKeys = ["lessonServerId", "orderIndex"],
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
data class RemoteDictationSentenceProgressEntity(
    val lessonServerId: String,
    val orderIndex: Int,
    val userAnswer: String,
    val isCorrect: Boolean,
    val attemptsCount: Int,
    val status: SentenceStatus,
    val lastCheckedAt: Long,
)