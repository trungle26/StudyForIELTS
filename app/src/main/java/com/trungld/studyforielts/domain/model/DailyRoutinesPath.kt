package com.trungld.studyforielts.domain.model

const val DAILY_ROUTINES_TOPIC = "daily_routines"
const val DAILY_ROUTINES_STAGE = "beginner"
const val DAILY_ROUTINES_CEFR = "A1"

/** The deliberately small, fixed path for the first beginner topic. */
data class DailyRoutinesPath(
    val topic: String = DAILY_ROUTINES_TOPIC,
    val stage: String = DAILY_ROUTINES_STAGE,
    val cefrLevel: String = DAILY_ROUTINES_CEFR,
    val activities: List<DailyRoutinesActivity> = DailyRoutinesActivity.entries,
)

enum class DailyRoutinesActivity { VOCABULARY, READING, GRAMMAR, DICTATION, WRITING, REVIEW }

fun canSubmitDailyRoutinesAnswer(answer: String): Boolean = answer.isNotBlank()

fun shouldRecommendDailyRoutines(profile: LearnerProfile): Boolean =
    profile.currentCefrLevel == CefrLevel.UNKNOWN || profile.currentCefrLevel == CefrLevel.A1
