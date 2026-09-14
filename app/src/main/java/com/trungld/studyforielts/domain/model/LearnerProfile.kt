package com.trungld.studyforielts.domain.model

import kotlinx.coroutines.flow.Flow

enum class CefrLevel { A1, A2, B1, B2, C1, C2, UNKNOWN }
enum class TargetBand(val value: String) { B4_5("4.5"), B5_0("5.0"), B5_5("5.5"), B6_0("6.0"), B6_5("6.5"), B7_0("7.0"), B7_5("7.5"), B8_0("8.0"), B8_5("8.5"), B9_0("9.0"), NONE("none") }
enum class FocusSkill { LISTENING, READING, WRITING, SPEAKING, GRAMMAR, VOCABULARY, PRONUNCIATION }

data class LearnerProfile(
    val onboardingCompleted: Boolean = false,
    val currentCefrLevel: CefrLevel = CefrLevel.UNKNOWN,
    val targetBand: TargetBand = TargetBand.NONE,
    val dailyGoalMinutes: Int = 10,
    val focusSkills: Set<FocusSkill> = emptySet(),
    val updatedAt: Long = 0L,
)

data class Recommendation(val title: RecommendationTitle, val skill: FocusSkill?)
enum class RecommendationTitle { FOUNDATION, GENERAL, IELTS_SKILL, DIAGNOSTIC }

fun recommend(profile: LearnerProfile): Recommendation = when {
    profile.currentCefrLevel == CefrLevel.UNKNOWN -> Recommendation(RecommendationTitle.DIAGNOSTIC, profile.focusSkills.firstOrNull())
    profile.currentCefrLevel in setOf(CefrLevel.A1, CefrLevel.A2) -> Recommendation(RecommendationTitle.FOUNDATION, profile.focusSkills.firstOrNull())
    profile.currentCefrLevel == CefrLevel.B1 || profile.targetBand == TargetBand.NONE -> Recommendation(RecommendationTitle.GENERAL, profile.focusSkills.firstOrNull())
    else -> Recommendation(RecommendationTitle.IELTS_SKILL, profile.focusSkills.firstOrNull())
}
