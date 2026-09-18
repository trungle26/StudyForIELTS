package com.trungld.studyforielts.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRoutinesRulesTest {
    @Test fun grammarAcceptsPresentSimpleThirdPersonForm() {
        assertTrue(checkDailyRoutinesAnswer(DailyRoutinesActivity.GRAMMAR, "  EATS "))
        assertFalse(checkDailyRoutinesAnswer(DailyRoutinesActivity.GRAMMAR, "eat"))
    }

    @Test fun writingRequiresAShortSentence() {
        assertFalse(checkDailyRoutinesAnswer(DailyRoutinesActivity.WRITING, "sleep"))
        assertTrue(checkDailyRoutinesAnswer(DailyRoutinesActivity.WRITING, "I sleep at night"))
    }
}
