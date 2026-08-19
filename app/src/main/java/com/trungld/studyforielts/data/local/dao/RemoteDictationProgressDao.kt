package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.RemoteDictationProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDictationProgressDao {

    @Query("SELECT * FROM remote_dictation_progress WHERE lessonServerId = :lessonServerId LIMIT 1")
    fun observeProgressByLessonServerId(lessonServerId: String): Flow<RemoteDictationProgressEntity?>

    @Query("SELECT * FROM remote_dictation_progress WHERE lessonServerId = :lessonServerId LIMIT 1")
    suspend fun getProgressByLessonServerId(lessonServerId: String): RemoteDictationProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: RemoteDictationProgressEntity)

    @Query("DELETE FROM remote_dictation_progress WHERE lessonServerId = :lessonServerId")
    suspend fun deleteProgressByLessonServerId(lessonServerId: String)
}