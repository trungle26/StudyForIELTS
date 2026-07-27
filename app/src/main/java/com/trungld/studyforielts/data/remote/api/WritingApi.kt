package com.trungld.studyforielts.data.remote.api

import com.trungld.studyforielts.data.remote.model.EssaySubmissionDto
import com.trungld.studyforielts.data.remote.model.Task1EssaySubmissionDto
import com.trungld.studyforielts.data.remote.model.WritingEvaluationDto
import com.trungld.studyforielts.data.remote.model.WritingLessonDto
import com.trungld.studyforielts.data.remote.model.WritingLessonListDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit interface for the writing tutor backend.
 *
 * Endpoints:
 * - GET  /writing/lessons                   -> paginated published lessons
 * - GET  /writing/lessons/{id}              -> single lesson detail
 * - POST /writing/evaluate                  -> Task 2 blocking
 * - POST /writing/evaluate/stream           -> Task 2 SSE stream
 * - POST /writing/evaluate/task1/stream     -> Task 1 SSE stream
 *
 * The chart image (Task 1) is fetched by Coil directly from
 * `<baseUrl>/writing/lessons/{id}/image`; we don't route it through Retrofit
 * because Coil's `AsyncImage` model already handles a `URL`/`String` URL
 * source with caching and request headers built in.
 */
interface WritingApi {

    // --- Lessons (Phase 3.3) ---

    @GET("writing/lessons")
    suspend fun listLessons(
        @Query("task_type") taskType: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): WritingLessonListDto

    @GET("writing/lessons/{lessonId}")
    suspend fun getLesson(
        @Path("lessonId") lessonId: String,
    ): WritingLessonDto

    // --- Task 2 evaluation ---

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

    // --- Task 1 evaluation (Phase 3.6) ---

    /**
     * Streaming Task 1 evaluation. The lesson prompt and chart image come
     * from the server-side lesson document; the client only sends
     * `lesson_id` + `essay_text`. Same SSE protocol as Task 2.
     */
    @Streaming
    @POST("writing/evaluate/task1/stream")
    suspend fun evaluateTask1Stream(
        @Body body: Task1EssaySubmissionDto,
    ): ResponseBody
}