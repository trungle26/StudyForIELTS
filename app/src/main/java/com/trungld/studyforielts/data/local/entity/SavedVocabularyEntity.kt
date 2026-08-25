package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_vocabularies",
    indices = [
        Index(value = ["word"], unique = true),
    ],
)
data class SavedVocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val exampleSentence: String,
    val sourceLessonId: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
)
