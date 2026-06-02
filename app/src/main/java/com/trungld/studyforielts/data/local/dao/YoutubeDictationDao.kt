package com.trungld.studyforielts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.trungld.studyforielts.data.local.entity.YoutubeSentenceEntity
import com.trungld.studyforielts.data.local.entity.YoutubeVideoEntity
import com.trungld.studyforielts.data.local.model.YoutubeVideoWithSentences
import kotlinx.coroutines.flow.Flow

@Dao
interface YoutubeDictationDao {

    @Query("SELECT * FROM youtube_videos WHERE isSaved = 1 ORDER BY cachedAt DESC")
    fun observeSavedVideos(): Flow<List<YoutubeVideoEntity>>

    @Transaction
    @Query("SELECT * FROM youtube_videos WHERE videoId = :videoId LIMIT 1")
    fun observeVideoWithSentences(videoId: String): Flow<YoutubeVideoWithSentences?>

    @Query("SELECT * FROM youtube_videos WHERE videoId = :videoId LIMIT 1")
    suspend fun getVideo(videoId: String): YoutubeVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<YoutubeVideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: YoutubeVideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<YoutubeSentenceEntity>)

    @Query("DELETE FROM youtube_sentences WHERE videoId = :videoId")
    suspend fun deleteSentencesByVideoId(videoId: String)
}
