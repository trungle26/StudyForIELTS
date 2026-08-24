package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.RemoteVocabularyDao
import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import com.trungld.studyforielts.domain.repository.RemoteVocabularyRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RemoteVocabularyRepositoryImpl @Inject constructor(
    private val dao: RemoteVocabularyDao,
) : RemoteVocabularyRepository {

    override fun observeVocabulariesByLessonServerId(
        lessonServerId: String,
    ): Flow<List<RemoteVocabularyEntity>> = dao.observeVocabulariesByLessonServerId(lessonServerId)

    override suspend fun updateVocabularyLearnedStatus(
        lessonServerId: String,
        word: String,
        isLearned: Boolean,
    ) {
        dao.updateVocabularyLearnedStatus(lessonServerId, word, isLearned)
    }
}