package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "remote_vocabularies",
    primaryKeys = ["lessonServerId", "word"],
    foreignKeys = [
        ForeignKey(
            entity = RemoteDictationLessonEntity::class,
            parentColumns = ["serverId"],
            childColumns = ["lessonServerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["lessonServerId"]),
    ],
)
data class RemoteVocabularyEntity(
    val lessonServerId: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val exampleSentence: String,
    val isLearned: Boolean = false,
)