package com.trungld.studyforielts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRoutinesPresentSimpleTest {
    @Test fun answersIgnoreCaseAndSurroundingWhitespace() {
        val exercise = DAILY_ROUTINES_PRESENT_SIMPLE_LESSON.exercises.first()
        assertTrue(isCorrectGrammarAnswer(exercise, "  WAKE "))
        assertEquals("wake", normalizeGrammarAnswer("  WAKE "))
    }

    @Test fun wrongAnswerDoesNotAdvanceAndRetryCanSucceed() {
        val lesson = DAILY_ROUTINES_PRESENT_SIMPLE_LESSON
        val start = GrammarProgress()
        assertEquals(start, answerGrammarExercise(lesson, start, "wakes"))
        assertEquals(GrammarProgress(1), answerGrammarExercise(lesson, start, "wake"))
    }

    @Test fun seededLessonCoversRequiredFormsAndProgressesToCompletion() {
        val lesson = DAILY_ROUTINES_PRESENT_SIMPLE_LESSON
        assertTrue(lesson.exercises.size >= 5)
        assertTrue(lesson.exercises.any { it.prompt.startsWith("I ") })
        assertTrue(lesson.exercises.any { it.prompt.startsWith("She ") })
        assertTrue(lesson.exercises.any { it.prompt.contains("every day") })
        assertTrue(lesson.exercises.any { it.prompt.contains("morning") })
        assertTrue(lesson.exercises.any { it.prompt.contains("night") })

        var progress = GrammarProgress()
        lesson.exercises.forEach { exercise ->
            progress = answerGrammarExercise(lesson, progress, exercise.answer)
        }
        assertTrue(progress.completed)
        assertFalse(answerGrammarExercise(lesson, progress, "wrong").exerciseIndex != progress.exerciseIndex)
    }
}
