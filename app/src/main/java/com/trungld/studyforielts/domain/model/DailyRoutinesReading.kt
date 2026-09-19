package com.trungld.studyforielts.domain.model

const val DAILY_ROUTINES_READING_CEFR = "A1"
const val DAILY_ROUTINES_READING_SKILL = "reading"
const val DAILY_ROUTINES_READING_STAGE = "foundation"

/** Original, short A1 passage and deterministic checks for the daily-routines path. */
data class ReadingQuestion(
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val explanation: String,
    val evidence: String,
)

data class ReadingProgress(
    val questionIndex: Int = 0,
    val missed: Set<Int> = emptySet(),
    val completed: Boolean = false,
)

val DAILY_ROUTINES_READING_PASSAGE =
    "Mina wakes up at seven o'clock every morning. She opens the window and drinks water. " +
        "Then she eats breakfast with her brother. Mina walks to work because the office is near her home. " +
        "At lunch, she talks with her friends. In the afternoon, she finishes her work and goes home. " +
        "After dinner, Mina reads a short book and prepares her clothes for the next day. She goes to bed at ten o'clock."

val DAILY_ROUTINES_READING_VOCABULARY = listOf(
    "window" to "an opening in a wall that lets in light",
    "near" to "not far away",
    "finishes" to "stops doing something because it is complete",
    "prepares" to "gets something ready",
    "clothes" to "things that people wear",
)

val DAILY_ROUTINES_READING_QUESTIONS = listOf(
    ReadingQuestion(
        prompt = "What is the passage mainly about?",
        options = listOf("Mina's daily routine", "Mina's holiday", "Mina's new house"),
        answer = "Mina's daily routine",
        explanation = "The passage describes Mina's activities from morning to bedtime.",
        evidence = "It tells us what Mina does every morning, afternoon, and evening.",
    ),
    ReadingQuestion(
        prompt = "True, false, or not stated: Mina walks to work.",
        options = listOf("True", "False", "Not stated"),
        answer = "True",
        explanation = "The passage directly says that Mina walks to work.",
        evidence = "Mina walks to work because the office is near her home.",
    ),
    ReadingQuestion(
        prompt = "What does Mina do after dinner?",
        options = listOf("She reads a short book", "She goes to work", "She drinks water"),
        answer = "She reads a short book",
        explanation = "Reading comes after dinner in Mina's evening routine.",
        evidence = "After dinner, Mina reads a short book.",
    ),
    ReadingQuestion(
        prompt = "In the passage, what does 'near' mean?",
        options = listOf("Not far away", "Very expensive", "Open all night"),
        answer = "Not far away",
        explanation = "The office is near her home, so Mina can walk there.",
        evidence = "Mina walks to work because the office is near her home.",
    ),
)

fun answerReadingQuestion(
    progress: ReadingProgress,
    questions: List<ReadingQuestion>,
    answer: String,
): ReadingProgress {
    if (progress.completed || questions.isEmpty()) return progress
    val index = progress.questionIndex.coerceIn(0, questions.lastIndex)
    val updatedMissed = if (answer == questions[index].answer) progress.missed - index else progress.missed + index
    return if (index == questions.lastIndex) {
        progress.copy(questionIndex = questions.size, missed = updatedMissed, completed = true)
    } else {
        progress.copy(questionIndex = index + 1, missed = updatedMissed)
    }
}

fun retryReading(progress: ReadingProgress, questions: List<ReadingQuestion>): ReadingProgress {
    val next = progress.missed.minOrNull() ?: return progress.copy(completed = true)
    return progress.copy(questionIndex = next, completed = false)
}
