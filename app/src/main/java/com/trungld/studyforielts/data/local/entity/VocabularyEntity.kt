package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabularies",
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
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val lessonId: Long,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val exampleSentence: String,
)
