package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.StudyActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyActivityDao {

    @Query("SELECT * FROM study_activity ORDER BY activityDate DESC")
    fun observeAll(): Flow<List<StudyActivityEntity>>

    /**
     * Insert-if-absent. Returns the new row id, or the existing row id if the
     * date is already recorded. Used to keep activity idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(activity: StudyActivityEntity): Long

    @Query("SELECT COUNT(*) FROM study_activity WHERE activityDate = :activityDate")
    suspend fun countForDate(activityDate: String): Int
}
