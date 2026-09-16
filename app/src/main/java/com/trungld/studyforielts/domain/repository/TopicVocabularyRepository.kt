package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.TopicVocabularyEntity
import com.trungld.studyforielts.data.local.entity.VocabularyProgressEntity
import kotlinx.coroutines.flow.Flow

interface TopicVocabularyRepository {
    fun observeVocabulary(topic: String, cefrLevel: String): Flow<List<TopicVocabularyEntity>>
    fun observeProgress(topic: String, cefrLevel: String): Flow<List<VocabularyProgressEntity>>
    suspend fun seedIfEmpty(topic: String, cefrLevel: String, items: List<TopicVocabularyEntity>)
    suspend fun markLearned(vocabularyId: String, learned: Boolean)
    suspend fun recordAttempt(vocabularyId: String, learned: Boolean)
}
