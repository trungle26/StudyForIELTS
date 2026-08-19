package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDictationSentenceProgressDao {

    @Query("SELECT * FROM remote_dictation_sentence_progress WHERE lessonServerId = :lessonServerId ORDER BY orderIndex ASC")
    fun observeSentenceProgressByLessonServerId(lessonServerId: String): Flow<List<RemoteDictationSentenceProgressEntity>>

    @Query(
        """
        SELECT * FROM remote_dictation_sentence_progress
        WHERE lessonServerId = :lessonServerId AND orderIndex = :orderIndex
        LIMIT 1
        """
    )
    suspend fun getSentenceProgressByOrderIndex(
        lessonServerId: String,
        orderIndex: Int,
    ): RemoteDictationSentenceProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSentenceProgress(progress: RemoteDictationSentenceProgressEntity)

    @Query("DELETE FROM remote_dictation_sentence_progress WHERE lessonServerId = :lessonServerId")
    suspend fun deleteSentenceProgressByLessonServerId(lessonServerId: String)
}