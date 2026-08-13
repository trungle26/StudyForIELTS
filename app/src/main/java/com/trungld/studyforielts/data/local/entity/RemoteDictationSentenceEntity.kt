package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "remote_dictation_sentences",
    primaryKeys = ["lessonServerId", "orderIndex"],
    foreignKeys = [
        ForeignKey(
            entity = RemoteDictationLessonEntity::class,
            parentColumns = ["serverId"],
            childColumns = ["lessonServerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lessonServerId")],
)
data class RemoteDictationSentenceEntity(
    val lessonServerId: String,
    val orderIndex: Int,
    val text: String,
    val startTimeMs: Int,
    val endTimeMs: Int,
)
