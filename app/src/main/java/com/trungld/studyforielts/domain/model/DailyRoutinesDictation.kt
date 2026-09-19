package com.trungld.studyforielts.domain.model

/** Fixed A1 seed for the daily-routines dictation activity. */
val DAILY_ROUTINES_A1_DICTATION = listOf(
    "I wake up early.",
    "She eats breakfast.",
    "We go to school.",
    "He studies every day.",
    "They sleep at night.",
)

fun normalizeDictationAnswer(answer: String): String =
    answer.trim().trimEnd('.', '!', '?').trim().lowercase()

data class DictationProgress(val sentenceIndex: Int = 0, val completed: Boolean = false)

fun answerDictationSentence(
    progress: DictationProgress,
    sentences: List<String>,
    answer: String,
): DictationProgress {
    if (progress.completed || sentences.isEmpty()) return progress
    val index = progress.sentenceIndex.coerceIn(0, sentences.lastIndex)
    if (normalizeDictationAnswer(answer) != normalizeDictationAnswer(sentences[index])) return progress
    return if (index == sentences.lastIndex) {
        progress.copy(sentenceIndex = sentences.size, completed = true)
    } else {
        progress.copy(sentenceIndex = index + 1)
    }
}
