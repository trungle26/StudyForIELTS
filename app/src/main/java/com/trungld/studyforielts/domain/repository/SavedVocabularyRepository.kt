package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.SavedVocabularyEntity
import kotlinx.coroutines.flow.Flow

interface SavedVocabularyRepository {
    fun observeSavedVocabularies(): Flow<List<SavedVocabularyEntity>>
    suspend fun saveVocabulary(
        word: String,
        phonetic: String,
        meaning: String,
        exampleSentence: String,
        sourceLessonId: String? = null,
    )
    suspend fun removeVocabulary(id: Long)
    suspend fun removeVocabularyByWord(word: String)
}
