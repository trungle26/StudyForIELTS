package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteVocabularyDao {

    @Query(
        "SELECT * FROM remote_vocabularies WHERE lessonServerId = :lessonServerId " +
            "ORDER BY isLearned ASC, word ASC"
    )
    fun observeVocabulariesByLessonServerId(lessonServerId: String): Flow<List<RemoteVocabularyEntity>>

    @Query("SELECT * FROM remote_vocabularies")
    fun observeAllVocabularies(): Flow<List<RemoteVocabularyEntity>>

    @Query(
        "SELECT * FROM remote_vocabularies WHERE lessonServerId = :lessonServerId " +
            "AND word = :word LIMIT 1"
    )
    suspend fun getVocabulary(lessonServerId: String, word: String): RemoteVocabularyEntity?

    @Query("SELECT COUNT(*) FROM remote_vocabularies WHERE lessonServerId = :lessonServerId")
    suspend fun getVocabularyCountByLessonServerId(lessonServerId: String): Int

    @Query(
        "UPDATE remote_vocabularies SET isLearned = :isLearned " +
            "WHERE lessonServerId = :lessonServerId AND word = :word"
    )
    suspend fun updateVocabularyLearnedStatus(
        lessonServerId: String,
        word: String,
        isLearned: Boolean,
    )

    @MapInfo(keyColumn = "word", valueColumn = "isLearned")
    @Query(
        "SELECT word, isLearned FROM remote_vocabularies WHERE lessonServerId = :lessonServerId"
    )
    suspend fun getLearnedSnapshot(lessonServerId: String): Map<String, Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVocabularies(vocabularies: List<RemoteVocabularyEntity>)

    @Query("DELETE FROM remote_vocabularies WHERE lessonServerId = :lessonServerId")
    suspend fun deleteVocabulariesByLessonServerId(lessonServerId: String)

    @Transaction
    suspend fun replaceLessonVocabularies(
        lessonServerId: String,
        vocabularies: List<RemoteVocabularyEntity>,
    ) {
        val learnedByWord = getLearnedSnapshot(lessonServerId)
        deleteVocabulariesByLessonServerId(lessonServerId)
        val merged = vocabularies.map { vocab ->
            if (learnedByWord[vocab.word] == true) vocab.copy(isLearned = true) else vocab
        }
        upsertVocabularies(merged)
    }
}