package com.trungld.studyforielts.domain.model

data class CheckResult(
    val isCorrect: Boolean,
    val expectedText: String,
    val userText: String,
    val missingWords: List<String>,
    val wrongWords: List<String>,
    val extraWords: List<String>,
    val wordComparisons: List<WordComparison>,
)
