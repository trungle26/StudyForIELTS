package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId LIMIT 1")
    fun observeProgressByLessonId(lessonId: Long): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getProgressByLessonId(lessonId: Long): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE lessonId = :lessonId")
    suspend fun deleteProgressByLessonId(lessonId: Long)
}
