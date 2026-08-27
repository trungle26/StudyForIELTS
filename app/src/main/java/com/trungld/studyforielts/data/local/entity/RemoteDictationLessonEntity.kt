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
    /** Updated whenever the lesson is observed by the UI; powers LRU eviction for offline audio. */
    val lastAccessedAt: Long = cachedAt,
    /** Path to the downloaded audio file in app-private storage, or null if not cached. */
    val localAudioPath: String? = null,
    /** Size of the local audio file in bytes; 0 if not cached. */
    val localAudioBytes: Long = 0L,
    /** Epoch millis of the most recent completed download; 0 if never downloaded. */
    val audioDownloadedAt: Long = 0L,
)
