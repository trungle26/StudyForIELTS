package com.trungld.studyforielts.domain.model

enum class IeltsSkillType(val key: String, val displayName: String) {
    LISTENING("listening", "Listening"),
    READING("reading", "Reading"),
    WRITING_TASK1("writing_task1", "Writing Task 1"),
    WRITING_TASK2("writing_task2", "Writing Task 2"),
    SPEAKING("speaking", "Speaking");

    companion object {
        fun fromKey(key: String): IeltsSkillType =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: LISTENING
    }
}

data class StrategyStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
)

data class StrategyGuide(
    val id: String,
    val skill: IeltsSkillType,
    val questionType: String,
    val questionTypeLabel: String,
    val title: String,
    val summary: String,
    val targetBand: String = "7.0+",
    val timeBudgetMin: Int = 20,
    val isSpotlight: Boolean = false,
    val overview: String,
    val steps: List<StrategyStep> = emptyList(),
    val dos: List<String> = emptyList(),
    val donts: List<String> = emptyList(),
    val commonTraps: List<String> = emptyList(),
    val goldenRule: String? = null,
)
