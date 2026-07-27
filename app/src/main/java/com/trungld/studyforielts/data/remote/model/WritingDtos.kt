package com.trungld.studyforielts.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for POST /writing/evaluate.
 * Field names mirror the backend Pydantic `EssaySubmission` model.
 */
data class EssaySubmissionDto(
    @SerializedName("task_prompt")
    val taskPrompt: String,
    @SerializedName("essay_text")
    val essayText: String,
)

/**
 * Request body for POST /writing/evaluate/task1[/stream].
 * Mirrors the backend Pydantic `Task1EssaySubmission` model. The task prompt
 * and chart image come from the server-side lesson document, so the client
 * only needs to send the lesson id and the essay text.
 */
data class Task1EssaySubmissionDto(
    @SerializedName("lesson_id")
    val lessonId: String,
    @SerializedName("essay_text")
    val essayText: String,
)

/**
 * Response body from POST /writing/evaluate.
 * Mirrors the backend Pydantic `WritingEvaluation` model (the four core fields).
 * MongoDB-only fields (id, created_at, etc.) are intentionally omitted — we
 * don't currently need them on the client.
 */
data class WritingEvaluationDto(
    @SerializedName("overall_band")
    val overallBand: Double,
    @SerializedName("coherence_feedback")
    val coherenceFeedback: String,
    @SerializedName("vocabulary_suggestions")
    val vocabularySuggestions: List<String>,
    @SerializedName("simon_style_rewrite")
    val simonStyleRewrite: String,
)

// --- Writing lessons (Phase 3.3 + 3.9) ---

/**
 * Single writing lesson returned by GET /writing/lessons/{id} and as an item
 * in the paginated list. Field names mirror the backend Pydantic
 * `WritingLessonResponse` model; nullables are used where the backend allows
 * absence (e.g. `imageId` is null for Task 2 lessons, `difficulty` is optional).
 */
data class WritingLessonDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("task_type")
    val taskType: String,
    @SerializedName("task_prompt")
    val taskPrompt: String,
    @SerializedName("image_id")
    val imageId: String? = null,
    @SerializedName("sample_answer")
    val sampleAnswer: String,
    @SerializedName("tips")
    val tips: List<String> = emptyList(),
    @SerializedName("difficulty")
    val difficulty: String? = null,
    @SerializedName("status")
    val status: String = "published",
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
)

/**
 * Paginated list response from GET /writing/lessons.
 * Mirrors the backend `WritingLessonListResponse` model.
 */
data class WritingLessonListDto(
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("total")
    val total: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("items")
    val items: List<WritingLessonDto>,
)