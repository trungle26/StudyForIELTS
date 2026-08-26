package com.trungld.studyforielts.domain.model

/**
 * Snapshot of the user's current streak. `currentDays` is the number of
 * consecutive local calendar days, ending today or yesterday, with at least
 * one recorded meaningful dictation action.
 */
data class StreakSummary(
    val currentDays: Int,
)
