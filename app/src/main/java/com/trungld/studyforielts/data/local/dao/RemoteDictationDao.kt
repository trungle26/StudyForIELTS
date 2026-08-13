package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDictationDao {

    @Query("SELECT * FROM remote_dictation_lessons ORDER BY title ASC")
    fun observeAllLessons(): Flow<List<RemoteDictationLessonEntity>>

    @Query("SELECT * FROM remote_dictation_lessons WHERE level = :level ORDER BY title ASC")
    fun observeLessonsByLevel(level: String): Flow<List<RemoteDictationLessonEntity>>

    @Query("SELECT * FROM remote_dictation_lessons WHERE serverId = :serverId LIMIT 1")
    fun observeLesson(serverId: String): Flow<RemoteDictationLessonEntity?>

    @Query("SELECT * FROM remote_dictation_sentences WHERE lessonServerId = :serverId ORDER BY orderIndex ASC")
    fun observeSentences(serverId: String): Flow<List<RemoteDictationSentenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessons(lessons: List<RemoteDictationLessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSentences(sentences: List<RemoteDictationSentenceEntity>)

    @Query("DELETE FROM remote_dictation_sentences WHERE lessonServerId = :serverId")
    suspend fun deleteSentences(serverId: String)

    @Query("DELETE FROM remote_dictation_lessons WHERE serverId NOT IN (:keepIds)")
    suspend fun deleteStaleLesson(keepIds: List<String>)

    @Transaction
    suspend fun replaceAllLessons(
        lessons: List<RemoteDictationLessonEntity>,
        sentences: List<RemoteDictationSentenceEntity>,
    ) {
        val keepIds = lessons.map { it.serverId }
        deleteStaleLesson(keepIds)
        upsertLessons(lessons)
        // CASCADE deletes orphan sentences; re-insert fresh ones.
        keepIds.forEach { deleteSentences(it) }
        upsertSentences(sentences)
    }
}
