package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.VocabularyDao
import com.trungld.studyforielts.data.local.entity.VocabularyEntity
import com.trungld.studyforielts.domain.repository.VocabularyRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class VocabularyRepositoryImpl @Inject constructor(
    private val vocabularyDao: VocabularyDao,
) : VocabularyRepository {

    override fun observeVocabulariesByLessonId(lessonId: Long): Flow<List<VocabularyEntity>> {
        return vocabularyDao.observeVocabulariesByLessonId(lessonId)
    }

    override suspend fun updateVocabularyLearnedStatus(
        vocabId: Long,
        isLearned: Boolean,
    ) {
        vocabularyDao.updateVocabularyLearnedStatus(
            vocabId = vocabId,
            isLearned = isLearned,
        )
    }
}
