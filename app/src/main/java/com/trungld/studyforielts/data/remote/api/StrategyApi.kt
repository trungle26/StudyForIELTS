package com.trungld.studyforielts.data.remote.api

import com.trungld.studyforielts.data.remote.model.StrategyGuideDto
import com.trungld.studyforielts.data.remote.model.StrategyListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StrategyApi {

    @GET("strategies")
    suspend fun listStrategies(
        @Query("skill") skill: String? = null,
        @Query("question_type") questionType: String? = null,
    ): StrategyListResponseDto

    @GET("strategies/spotlight")
    suspend fun getSpotlightStrategy(): StrategyGuideDto

    @GET("strategies/{id}")
    suspend fun getStrategyDetail(
        @Path("id") id: String,
    ): StrategyGuideDto
}
