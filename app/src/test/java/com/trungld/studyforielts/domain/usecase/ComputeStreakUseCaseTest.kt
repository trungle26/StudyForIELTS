package com.trungld.studyforielts.domain.usecase

import com.trungld.studyforielts.domain.model.StreakSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ComputeStreakUseCaseTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 26)

    @Test
    fun `no activity returns zero streak`() {
        val result = ComputeStreakUseCase(emptyList(), today)
        assertEquals(StreakSummary(currentDays = 0), result)
    }

    @Test
    fun `activity today returns streak of one`() {
        val result = ComputeStreakUseCase(listOf(today), today)
        assertEquals(StreakSummary(currentDays = 1), result)
    }

    @Test
    fun `three consecutive days ending today returns three`() {
        val dates = listOf(
            today,
            today.minusDays(1),
            today.minusDays(2),
        )
        val result = ComputeStreakUseCase(dates, today)
        assertEquals(StreakSummary(currentDays = 3), result)
    }

    @Test
    fun `gap before the last run breaks the streak`() {
        val dates = listOf(
            today,
            today.minusDays(1),
            // missing day-2
            today.minusDays(3),
        )
        val result = ComputeStreakUseCase(dates, today)
        assertEquals(StreakSummary(currentDays = 2), result)
    }

    @Test
    fun `activity only yesterday still counts as streak of one`() {
        val result = ComputeStreakUseCase(listOf(today.minusDays(1)), today)
        assertEquals(StreakSummary(currentDays = 1), result)
    }

    @Test
    fun `activity two days ago resets streak to zero`() {
        val result = ComputeStreakUseCase(listOf(today.minusDays(2)), today)
        assertEquals(StreakSummary(currentDays = 0), result)
    }

    @Test
    fun `duplicates do not inflate the streak`() {
        val dates = listOf(today, today, today.minusDays(1), today.minusDays(1))
        val result = ComputeStreakUseCase(dates, today)
        assertEquals(StreakSummary(currentDays = 2), result)
    }
}
