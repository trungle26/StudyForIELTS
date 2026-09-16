package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_progress",
    foreignKeys = [
        ForeignKey(
            entity = TopicVocabularyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vocabularyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VocabularyProgressEntity(
    @PrimaryKey val vocabularyId: String,
    val learned: Boolean = false,
    val attempts: Int = 0,
    val lastAttemptAt: Long = 0L,
)
