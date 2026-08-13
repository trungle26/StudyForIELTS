package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_dictation_lessons")
data class RemoteDictationLessonEntity(
    @PrimaryKey val serverId: String,
    val title: String,
    val level: String,
    val source: String,
    val audioUrl: String,
    val durationSeconds: Int?,
    val updatedAt: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)
