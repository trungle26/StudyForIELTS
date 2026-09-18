package com.trungld.studyforielts.domain.model

data class GrammarExercise(
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val explanation: String,
)

data class GrammarLesson(
    val title: String,
    val explanation: String,
    val exercises: List<GrammarExercise>,
)

val DAILY_ROUTINES_PRESENT_SIMPLE_LESSON = GrammarLesson(
    title = "Present simple",
    explanation = "Use I, you, we, and they + the base verb. Use he and she + verb-s.",
    exercises = listOf(
        GrammarExercise("I ___ up every day.", listOf("wake", "wakes"), "wake", "Use the base verb after I."),
        GrammarExercise("She ___ breakfast in the morning.", listOf("eat", "eats"), "eats", "Use verb-s after she."),
        GrammarExercise("We ___ to school every day.", listOf("go", "goes"), "go", "Use the base verb after we."),
        GrammarExercise("He ___ at night.", listOf("sleep", "sleeps"), "sleeps", "Use verb-s after he."),
        GrammarExercise("They study ___.", listOf("at night", "every day"), "every day", "Every day tells us how often they study."),
    ),
)

fun normalizeGrammarAnswer(answer: String): String = answer.trim().lowercase()

fun isCorrectGrammarAnswer(exercise: GrammarExercise, answer: String): Boolean =
    normalizeGrammarAnswer(answer) == normalizeGrammarAnswer(exercise.answer)

data class GrammarProgress(val exerciseIndex: Int = 0, val completed: Boolean = false)

fun answerGrammarExercise(
    lesson: GrammarLesson,
    progress: GrammarProgress,
    answer: String,
): GrammarProgress {
    if (progress.completed || lesson.exercises.isEmpty()) return progress
    val exercise = lesson.exercises[progress.exerciseIndex]
    if (!isCorrectGrammarAnswer(exercise, answer)) return progress
    val next = progress.exerciseIndex + 1
    return if (next == lesson.exercises.size) {
        progress.copy(completed = true)
    } else {
        progress.copy(exerciseIndex = next)
    }
}
