package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.SentenceProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceProgressDao {

    @Query("SELECT * FROM sentence_progress WHERE lessonId = :lessonId ORDER BY sentenceId ASC")
    fun observeSentenceProgressByLessonId(lessonId: Long): Flow<List<SentenceProgressEntity>>

    @Query(
        """
        SELECT * FROM sentence_progress
        WHERE lessonId = :lessonId AND sentenceId = :sentenceId
        LIMIT 1
        """
    )
    suspend fun getSentenceProgressByIds(
        lessonId: Long,
        sentenceId: Long,
    ): SentenceProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSentenceProgress(progress: SentenceProgressEntity)

    @Query("DELETE FROM sentence_progress WHERE lessonId = :lessonId")
    suspend fun deleteSentenceProgressByLessonId(lessonId: Long)
}
