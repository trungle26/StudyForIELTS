package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.SavedVocabularyDao
import com.trungld.studyforielts.data.local.entity.SavedVocabularyEntity
import com.trungld.studyforielts.domain.repository.SavedVocabularyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedVocabularyRepositoryImpl @Inject constructor(
    private val savedVocabularyDao: SavedVocabularyDao,
) : SavedVocabularyRepository {

    override fun observeSavedVocabularies(): Flow<List<SavedVocabularyEntity>> =
        savedVocabularyDao.observeAll()

    override suspend fun saveVocabulary(
        word: String,
        phonetic: String,
        meaning: String,
        exampleSentence: String,
        sourceLessonId: String?,
    ) {
        savedVocabularyDao.insertOrReplace(
            SavedVocabularyEntity(
                word = word.trim(),
                phonetic = phonetic.trim(),
                meaning = meaning.trim(),
                exampleSentence = exampleSentence.trim(),
                sourceLessonId = sourceLessonId,
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun removeVocabulary(id: Long) {
        savedVocabularyDao.deleteById(id)
    }

    override suspend fun removeVocabularyByWord(word: String) {
        savedVocabularyDao.deleteByWord(word.trim())
    }
}
