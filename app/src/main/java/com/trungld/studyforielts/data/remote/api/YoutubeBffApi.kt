package com.trungld.studyforielts.data.remote.api

import com.trungld.studyforielts.data.remote.model.YoutubeSearchResponseDto
import com.trungld.studyforielts.data.remote.model.YoutubeFeedResponseDto
import com.trungld.studyforielts.data.remote.model.YoutubeTranscriptResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeBffApi {

    @GET("feed")
    suspend fun getFeed(
        @Query("level") level: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): YoutubeFeedResponseDto

    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
    ): YoutubeSearchResponseDto

    @GET("transcript")
    suspend fun getTranscript(
        @Query("videoId") videoId: String,
        @Query("language") language: String = "en",
    ): YoutubeTranscriptResponseDto
}
