package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.SavedVocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedVocabularyDao {
    @Query("SELECT * FROM saved_vocabularies ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedVocabularyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(vocabulary: SavedVocabularyEntity)

    @Query("DELETE FROM saved_vocabularies WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("DELETE FROM saved_vocabularies WHERE id = :id")
    suspend fun deleteById(id: Long)
}
