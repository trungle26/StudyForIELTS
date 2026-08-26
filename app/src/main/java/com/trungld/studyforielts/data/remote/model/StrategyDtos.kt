package com.trungld.studyforielts.data.remote.model

import com.google.gson.annotations.SerializedName
import com.trungld.studyforielts.domain.model.IeltsSkillType
import com.trungld.studyforielts.domain.model.StrategyGuide
import com.trungld.studyforielts.domain.model.StrategyStep

data class StrategyStepDto(
    @SerializedName("step_number") val stepNumber: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
) {
    fun toDomain(): StrategyStep = StrategyStep(
        stepNumber = stepNumber,
        title = title,
        description = description,
    )
}

data class StrategyGuideDto(
    @SerializedName("id") val id: String,
    @SerializedName("skill") val skill: String,
    @SerializedName("question_type") val questionType: String,
    @SerializedName("question_type_label") val questionTypeLabel: String,
    @SerializedName("title") val title: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("target_band") val targetBand: String? = null,
    @SerializedName("time_budget_min") val timeBudgetMin: Int? = null,
    @SerializedName("is_spotlight") val isSpotlight: Boolean? = null,
    @SerializedName("overview") val overview: String,
    @SerializedName("steps") val steps: List<StrategyStepDto>? = null,
    @SerializedName("dos") val dos: List<String>? = null,
    @SerializedName("donts") val donts: List<String>? = null,
    @SerializedName("common_traps") val commonTraps: List<String>? = null,
    @SerializedName("golden_rule") val goldenRule: String? = null,
) {
    fun toDomain(): StrategyGuide = StrategyGuide(
        id = id,
        skill = IeltsSkillType.fromKey(skill),
        questionType = questionType,
        questionTypeLabel = questionTypeLabel,
        title = title,
        summary = summary,
        targetBand = targetBand ?: "7.0+",
        timeBudgetMin = timeBudgetMin ?: 20,
        isSpotlight = isSpotlight ?: false,
        overview = overview,
        steps = steps?.map { it.toDomain() } ?: emptyList(),
        dos = dos ?: emptyList(),
        donts = donts ?: emptyList(),
        commonTraps = commonTraps ?: emptyList(),
        goldenRule = goldenRule,
    )
}

data class StrategyListResponseDto(
    @SerializedName("items") val items: List<StrategyGuideDto>,
    @SerializedName("total") val total: Int,
)
