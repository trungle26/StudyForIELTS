package com.trungld.studyforielts.data.remote.api

import com.trungld.studyforielts.data.remote.model.EssaySubmissionDto
import com.trungld.studyforielts.data.remote.model.WritingEvaluationDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Retrofit interface for the writing tutor backend.
 *
 * Endpoints:
 * - POST /writing/evaluate       -> blocking, returns the full JSON
 * - POST /writing/evaluate/stream -> Server-Sent Events; the raw response body
 *                                    is consumed by the ViewModel which parses
 *                                    SSE frames as they arrive.
 */
interface WritingApi {

    @POST("writing/evaluate")
    suspend fun evaluateEssay(
        @Body body: EssaySubmissionDto,
    ): WritingEvaluationDto

    /**
     * Streaming variant. The response body is returned as-is so the caller can
     * `source().use { ... }` and parse SSE frames line-by-line.
     *
     * `@Streaming` tells Retrofit to read the body incrementally instead of
     * buffering the entire response (which would defeat the purpose of SSE).
     */
    @Streaming
    @POST("writing/evaluate/stream")
    suspend fun evaluateEssayStream(
        @Body body: EssaySubmissionDto,
    ): ResponseBody
}