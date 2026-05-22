package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    @Query("SELECT * FROM vocabularies WHERE lessonId = :lessonId ORDER BY isLearned ASC, id ASC")
    fun observeVocabulariesByLessonId(lessonId: Long): Flow<List<VocabularyEntity>>

    @Query("SELECT COUNT(*) FROM vocabularies WHERE lessonId = :lessonId")
    suspend fun getVocabularyCountByLessonId(lessonId: Long): Int

    @Query("UPDATE vocabularies SET isLearned = :isLearned WHERE id = :vocabId")
    suspend fun updateVocabularyLearnedStatus(
        vocabId: Long,
        isLearned: Boolean,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabularies(vocabularies: List<VocabularyEntity>)
}
