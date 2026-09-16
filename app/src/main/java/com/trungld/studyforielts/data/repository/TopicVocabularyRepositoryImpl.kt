package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.TopicVocabularyDao
import com.trungld.studyforielts.data.local.entity.TopicVocabularyEntity
import com.trungld.studyforielts.data.local.entity.VocabularyProgressEntity
import com.trungld.studyforielts.domain.repository.TopicVocabularyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicVocabularyRepositoryImpl @Inject constructor(
    private val dao: TopicVocabularyDao,
) : TopicVocabularyRepository {

    override fun observeVocabulary(topic: String, cefrLevel: String): Flow<List<TopicVocabularyEntity>> =
        dao.observeByTopicAndLevel(topic, cefrLevel)

    override fun observeProgress(topic: String, cefrLevel: String): Flow<List<VocabularyProgressEntity>> =
        dao.observeProgressByTopicAndLevel(topic, cefrLevel)

    override suspend fun seedIfEmpty(topic: String, cefrLevel: String, items: List<TopicVocabularyEntity>) {
        if (dao.countByTopicAndLevel(topic, cefrLevel) == 0) {
            dao.insertAll(items)
        }
    }

    override suspend fun markLearned(vocabularyId: String, learned: Boolean) {
        val existing = dao.getProgress(vocabularyId)
        dao.upsertProgress(
            (existing ?: VocabularyProgressEntity(vocabularyId = vocabularyId)).copy(
                learned = learned,
                lastAttemptAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun recordAttempt(vocabularyId: String, learned: Boolean) {
        val existing = dao.getProgress(vocabularyId)
        val attempts = (existing?.attempts ?: 0) + 1
        dao.upsertProgress(
            VocabularyProgressEntity(
                vocabularyId = vocabularyId,
                learned = learned,
                attempts = attempts,
                lastAttemptAt = System.currentTimeMillis(),
            ),
        )
    }
}
