package com.trungld.studyforielts.domain.model

data class WordComparison(
    val expectedWord: String?,
    val actualWord: String?,
    val status: WordComparisonStatus,
)
