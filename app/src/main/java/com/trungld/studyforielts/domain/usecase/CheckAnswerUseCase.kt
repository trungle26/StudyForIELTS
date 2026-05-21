package com.trungld.studyforielts.domain.usecase

import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.WordComparison
import com.trungld.studyforielts.domain.model.WordComparisonStatus
import javax.inject.Inject

class CheckAnswerUseCase @Inject constructor() {

    operator fun invoke(
        expectedText: String,
        userText: String,
    ): CheckResult {
        val expectedWords = tokenize(expectedText)
        val actualWords = tokenize(userText)
        val comparisons = buildComparisons(expectedWords, actualWords)

        return CheckResult(
            isCorrect = comparisons.all { it.status == WordComparisonStatus.CORRECT } &&
                expectedWords.size == actualWords.size,
            expectedText = expectedText,
            userText = userText,
            missingWords = comparisons
                .filter { it.status == WordComparisonStatus.MISSING }
                .mapNotNull { it.expectedWord },
            wrongWords = comparisons
                .filter { it.status == WordComparisonStatus.WRONG }
                .mapNotNull { it.actualWord },
            extraWords = comparisons
                .filter { it.status == WordComparisonStatus.EXTRA }
                .mapNotNull { it.actualWord },
            wordComparisons = comparisons,
        )
    }

    private fun buildComparisons(
        expectedWords: List<String>,
        actualWords: List<String>,
    ): List<WordComparison> {
        if (expectedWords.isEmpty() && actualWords.isEmpty()) return emptyList()

        val normalizedExpected = expectedWords.map(::normalizeWord)
        val normalizedActual = actualWords.map(::normalizeWord)
        val lcsTable = buildLcsTable(normalizedExpected, normalizedActual)
        val rawOperations = buildRawOperations(
            expectedWords = expectedWords,
            actualWords = actualWords,
            normalizedExpected = normalizedExpected,
            normalizedActual = normalizedActual,
            lcsTable = lcsTable,
        )

        return collapseOperations(rawOperations)
    }

    private fun buildLcsTable(
        expectedWords: List<String>,
        actualWords: List<String>,
    ): Array<IntArray> {
        val table = Array(expectedWords.size + 1) { IntArray(actualWords.size + 1) }
        for (expectedIndex in expectedWords.indices.reversed()) {
            for (actualIndex in actualWords.indices.reversed()) {
                table[expectedIndex][actualIndex] =
                    if (expectedWords[expectedIndex] == actualWords[actualIndex]) {
                        table[expectedIndex + 1][actualIndex + 1] + 1
                    } else {
                        maxOf(
                            table[expectedIndex + 1][actualIndex],
                            table[expectedIndex][actualIndex + 1],
                        )
                    }
            }
        }
        return table
    }

    private fun buildRawOperations(
        expectedWords: List<String>,
        actualWords: List<String>,
        normalizedExpected: List<String>,
        normalizedActual: List<String>,
        lcsTable: Array<IntArray>,
    ): List<WordComparison> {
        val operations = mutableListOf<WordComparison>()
        var expectedIndex = 0
        var actualIndex = 0

        while (expectedIndex < expectedWords.size && actualIndex < actualWords.size) {
            if (normalizedExpected[expectedIndex] == normalizedActual[actualIndex]) {
                operations += WordComparison(
                    expectedWord = expectedWords[expectedIndex],
                    actualWord = actualWords[actualIndex],
                    status = WordComparisonStatus.CORRECT,
                )
                expectedIndex++
                actualIndex++
            } else if (lcsTable[expectedIndex + 1][actualIndex] >= lcsTable[expectedIndex][actualIndex + 1]) {
                operations += WordComparison(
                    expectedWord = expectedWords[expectedIndex],
                    actualWord = null,
                    status = WordComparisonStatus.MISSING,
                )
                expectedIndex++
            } else {
                operations += WordComparison(
                    expectedWord = null,
                    actualWord = actualWords[actualIndex],
                    status = WordComparisonStatus.EXTRA,
                )
                actualIndex++
            }
        }

        while (expectedIndex < expectedWords.size) {
            operations += WordComparison(
                expectedWord = expectedWords[expectedIndex],
                actualWord = null,
                status = WordComparisonStatus.MISSING,
            )
            expectedIndex++
        }

        while (actualIndex < actualWords.size) {
            operations += WordComparison(
                expectedWord = null,
                actualWord = actualWords[actualIndex],
                status = WordComparisonStatus.EXTRA,
            )
            actualIndex++
        }

        return operations
    }

    private fun collapseOperations(
        operations: List<WordComparison>,
    ): List<WordComparison> {
        val collapsed = mutableListOf<WordComparison>()
        var index = 0

        while (index < operations.size) {
            val current = operations[index]
            if (current.status == WordComparisonStatus.CORRECT) {
                collapsed += current
                index++
                continue
            }

            val missingBlock = mutableListOf<String>()
            val extraBlock = mutableListOf<String>()

            while (index < operations.size && operations[index].status != WordComparisonStatus.CORRECT) {
                when (operations[index].status) {
                    WordComparisonStatus.MISSING -> operations[index].expectedWord?.let(missingBlock::add)
                    WordComparisonStatus.EXTRA -> operations[index].actualWord?.let(extraBlock::add)
                    else -> Unit
                }
                index++
            }

            val wrongCount = minOf(missingBlock.size, extraBlock.size)
            repeat(wrongCount) { blockIndex ->
                collapsed += WordComparison(
                    expectedWord = missingBlock[blockIndex],
                    actualWord = extraBlock[blockIndex],
                    status = WordComparisonStatus.WRONG,
                )
            }

            missingBlock.drop(wrongCount).forEach { missingWord ->
                collapsed += WordComparison(
                    expectedWord = missingWord,
                    actualWord = null,
                    status = WordComparisonStatus.MISSING,
                )
            }

            extraBlock.drop(wrongCount).forEach { extraWord ->
                collapsed += WordComparison(
                    expectedWord = null,
                    actualWord = extraWord,
                    status = WordComparisonStatus.EXTRA,
                )
            }
        }

        return collapsed
    }

    private fun tokenize(text: String): List<String> {
        return text.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    private fun normalizeWord(word: String): String {
        return word.lowercase()
            .replace(Regex("^[\\p{Punct}]+|[\\p{Punct}]+$"), "")
    }
}
