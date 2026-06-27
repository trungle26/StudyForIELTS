package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.domain.model.YoutubeDictationLesson
import com.trungld.studyforielts.domain.model.YoutubeVideo
import kotlinx.coroutines.flow.Flow

interface OnlineYoutubeDictationRepository {

    fun observeSavedVideos(): Flow<List<YoutubeVideo>>

    fun observeSavedLesson(videoId: String): Flow<YoutubeDictationLesson?>

    suspend fun fetchFeed(
        level: String,
        page: Int = 1,
        limit: Int = 20,
    ): Result<List<YoutubeVideo>>

    suspend fun searchVideos(
        query: String,
        limit: Int = 10,
    ): Result<List<YoutubeVideo>>

    suspend fun cacheTranscript(
        video: YoutubeVideo,
        language: String = "en",
    ): Result<YoutubeDictationLesson>

    suspend fun saveForOffline(
        video: YoutubeVideo,
        language: String = "en",
    ): Result<YoutubeDictationLesson>
}
