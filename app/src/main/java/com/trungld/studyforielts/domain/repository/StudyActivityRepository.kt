package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.domain.model.StreakSummary
import kotlinx.coroutines.flow.Flow

interface StudyActivityRepository {
    fun observeStreak(): Flow<StreakSummary>

    /**
     * Record today's local date as a study day. Idempotent: repeated calls on
     * the same calendar day do not change the streak.
     */
    suspend fun recordToday()
}
