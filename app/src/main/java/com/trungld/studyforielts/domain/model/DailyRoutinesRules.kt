package com.trungld.studyforielts.domain.model

fun checkDailyRoutinesAnswer(activity: DailyRoutinesActivity, answer: String): Boolean {
    val normalized = answer.trim().lowercase().replace(Regex("\\s+"), " ")
    return when (activity) {
        DailyRoutinesActivity.GRAMMAR -> normalized == "eats"
        DailyRoutinesActivity.DICTATION -> normalized == "i wake up every morning."
        DailyRoutinesActivity.WRITING -> normalized.isNotBlank() && normalized.split(' ').size >= 3
        DailyRoutinesActivity.VOCABULARY,
        DailyRoutinesActivity.REVIEW -> true
    }
}
