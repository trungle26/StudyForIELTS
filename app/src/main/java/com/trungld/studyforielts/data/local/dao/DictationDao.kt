package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.trungld.studyforielts.data.local.model.DictationLessonSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface DictationDao {

    @Transaction
    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun observeLessonSnapshot(lessonId: Long): Flow<DictationLessonSnapshot?>
}
