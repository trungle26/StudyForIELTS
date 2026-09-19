package com.trungld.studyforielts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRoutinesReadingTest {
    @Test
    fun correctAnswersAdvanceAndComplete() {
        val questions = DAILY_ROUTINES_READING_QUESTIONS
        val progress = questions.indices.fold(ReadingProgress()) { state, index ->
            answerReadingQuestion(state, questions, questions[index].answer)
        }

        assertTrue(progress.completed)
        assertTrue(progress.missed.isEmpty())
    }

    @Test
    fun incorrectAnswerIsRetainedForRetry() {
        val questions = DAILY_ROUTINES_READING_QUESTIONS
        val afterWrong = answerReadingQuestion(ReadingProgress(), questions, "wrong")
        val complete = (1 until questions.size).fold(afterWrong) { state, index ->
            answerReadingQuestion(state, questions, questions[index].answer)
        }

        assertEquals(setOf(0), complete.missed)
        assertTrue(complete.completed)
        val retry = retryReading(complete, questions)
        assertEquals(0, retry.questionIndex)
        val corrected = answerReadingQuestion(retry, questions, questions[0].answer)
        val finished = (1 until questions.size).fold(corrected) { state, index ->
            answerReadingQuestion(state, questions, questions[index].answer)
        }
        assertTrue(finished.completed)
        assertTrue(finished.missed.isEmpty())
    }
}
