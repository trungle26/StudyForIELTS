package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {

    @Query("SELECT * FROM sentences WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    fun observeSentencesByLessonId(lessonId: Long): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences WHERE id = :sentenceId LIMIT 1")
    fun observeSentenceById(sentenceId: Long): Flow<SentenceEntity?>

    @Query("SELECT * FROM sentences WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    suspend fun getSentencesByLessonId(lessonId: Long): List<SentenceEntity>

    @Query("SELECT * FROM sentences WHERE lessonId = :lessonId AND orderIndex = :orderIndex LIMIT 1")
    suspend fun getSentenceByLessonIdAndOrderIndex(
        lessonId: Long,
        orderIndex: Int,
    ): SentenceEntity?

    @Query("SELECT COUNT(*) FROM sentences WHERE lessonId = :lessonId")
    suspend fun getSentenceCountByLessonId(lessonId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(sentence: SentenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<SentenceEntity>)

    @Update
    suspend fun updateSentence(sentence: SentenceEntity)

    @Delete
    suspend fun deleteSentence(sentence: SentenceEntity)

    @Query("DELETE FROM sentences WHERE lessonId = :lessonId")
    suspend fun deleteSentencesByLessonId(lessonId: Long)
}
