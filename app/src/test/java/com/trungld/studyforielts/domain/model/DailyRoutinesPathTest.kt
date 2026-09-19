package com.trungld.studyforielts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyRoutinesPathTest {
    @Test fun everyActivityHasAStableRouteKey() {
        assertEquals(listOf("VOCABULARY", "READING", "GRAMMAR", "DICTATION", "WRITING", "REVIEW"), DailyRoutinesActivity.entries.map { it.name })
    }

    @Test fun blankAnswersCannotBeSubmitted() {
        assertEquals(false, canSubmitDailyRoutinesAnswer("   "))
        assertEquals(true, canSubmitDailyRoutinesAnswer("I wake up early."))
    }
}
