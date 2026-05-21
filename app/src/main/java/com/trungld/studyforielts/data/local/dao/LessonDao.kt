package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.data.local.model.LessonOverviewLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons ORDER BY id ASC")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE level = :level ORDER BY id ASC")
    fun observeLessonsByLevel(level: String): Flow<List<LessonEntity>>

    @Query(
        """
        SELECT
            lessons.id AS lessonId,
            lessons.title AS title,
            lessons.level AS level,
            COALESCE(progress.progressPercentage, 0) AS progressPercentage
        FROM lessons
        LEFT JOIN progress ON progress.lessonId = lessons.id
        WHERE lessons.level = :level
        ORDER BY lessons.id ASC
        """
    )
    fun observeLessonOverviewsByLevel(level: String): Flow<List<LessonOverviewLocal>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun observeLessonById(lessonId: Long): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: Long): LessonEntity?

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun getLessonCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}
