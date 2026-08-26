package com.trungld.studyforielts.domain.usecase

import com.trungld.studyforielts.domain.model.StreakSummary
import java.time.LocalDate

/**
 * Pure function for calculating the streak. Given a set of recorded activity
 * dates (any string parseable as `LocalDate`) and "today", returns the number
 * of consecutive days ending today or yesterday.
 *
 * Days without activity before the latest contiguous run are ignored. If the
 * most recent recorded date is older than yesterday, the current streak is 0.
 * Duplicates are tolerated; the set is the caller's responsibility.
 */
object ComputeStreakUseCase {
    operator fun invoke(
        activityDates: Collection<LocalDate>,
        today: LocalDate,
    ): StreakSummary {
        if (activityDates.isEmpty()) return StreakSummary(currentDays = 0)

        val dates = activityDates.toHashSet()
        var cursor = today
        var count = 0

        // Allow the streak to lag by one day: if no activity today but activity
        // yesterday, count yesterday as the anchor so the user doesn't lose
        // their streak before practicing later in the day.
        if (cursor !in dates) {
            val yesterday = today.minusDays(1)
            if (yesterday !in dates) return StreakSummary(currentDays = 0)
            cursor = yesterday
        }

        while (cursor in dates) {
            count++
            cursor = cursor.minusDays(1)
        }

        return StreakSummary(currentDays = count)
    }
}
