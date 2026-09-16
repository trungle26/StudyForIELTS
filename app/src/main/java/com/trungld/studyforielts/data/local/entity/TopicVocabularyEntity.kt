package com.trungld.studyforielts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topic_vocabularies")
data class TopicVocabularyEntity(
    @PrimaryKey val id: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val exampleSentence: String,
    val topic: String,
    val cefrLevel: String,
    val skill: String,
)
