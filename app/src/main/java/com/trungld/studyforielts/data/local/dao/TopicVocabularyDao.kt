package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungld.studyforielts.data.local.entity.TopicVocabularyEntity
import com.trungld.studyforielts.data.local.entity.VocabularyProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicVocabularyDao {

    @Query("SELECT * FROM topic_vocabularies WHERE topic = :topic AND cefrLevel = :cefrLevel ORDER BY word ASC")
    fun observeByTopicAndLevel(topic: String, cefrLevel: String): Flow<List<TopicVocabularyEntity>>

    @Query("SELECT * FROM topic_vocabularies WHERE topic = :topic AND cefrLevel = :cefrLevel ORDER BY word ASC")
    suspend fun getByTopicAndLevel(topic: String, cefrLevel: String): List<TopicVocabularyEntity>

    @Query("SELECT COUNT(*) FROM topic_vocabularies WHERE topic = :topic AND cefrLevel = :cefrLevel")
    suspend fun countByTopicAndLevel(topic: String, cefrLevel: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vocabularies: List<TopicVocabularyEntity>)

    // --- Progress ---

    @Query("SELECT * FROM vocabulary_progress WHERE vocabularyId IN (SELECT id FROM topic_vocabularies WHERE topic = :topic AND cefrLevel = :cefrLevel)")
    fun observeProgressByTopicAndLevel(topic: String, cefrLevel: String): Flow<List<VocabularyProgressEntity>>

    @Query("SELECT * FROM vocabulary_progress WHERE vocabularyId = :vocabularyId")
    suspend fun getProgress(vocabularyId: String): VocabularyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: VocabularyProgressEntity)

    @Query("SELECT COUNT(*) FROM vocabulary_progress WHERE learned = 1 AND vocabularyId IN (SELECT id FROM topic_vocabularies WHERE topic = :topic AND cefrLevel = :cefrLevel)")
    suspend fun countLearnedByTopicAndLevel(topic: String, cefrLevel: String): Int
}
