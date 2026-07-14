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