package com.trungld.studyforielts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRoutinesDictationTest {
    @Test fun normalizationIgnoresCaseWhitespaceAndEndingPunctuation() {
        assertEquals("i wake up early", normalizeDictationAnswer("  I WAKE UP EARLY..! "))
        assertEquals("she eats breakfast", normalizeDictationAnswer(" She eats breakfast. "))
    }

    @Test fun wrongAttemptStaysOnSentenceAndCorrectAttemptAdvances() {
        val start = DictationProgress()
        assertEquals(start, answerDictationSentence(start, DAILY_ROUTINES_A1_DICTATION, "I wake late"))
        assertEquals(DictationProgress(1), answerDictationSentence(start, DAILY_ROUTINES_A1_DICTATION, " i wake up early. "))
    }

    @Test fun finalCorrectAttemptCompletesLesson() {
        var progress = DictationProgress()
        DAILY_ROUTINES_A1_DICTATION.forEach { sentence ->
            progress = answerDictationSentence(progress, DAILY_ROUTINES_A1_DICTATION, sentence)
        }
        assertTrue(progress.completed)
        assertEquals(DAILY_ROUTINES_A1_DICTATION.size, progress.sentenceIndex)
    }
}
