package com.trungld.studyforielts.domain.model

import com.trungld.studyforielts.data.local.entity.VocabularyProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicVocabularyTest {

    @Test
    fun seedDataHasExpectedSize() {
        assertEquals(10, DAILY_ROUTINES_A1_VOCABULARY.size)
    }

    @Test
    fun seedDataIdsAreUnique() {
        val ids = DAILY_ROUTINES_A1_VOCABULARY.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun seedDataAllBelongToDailyRoutinesTopic() {
        assertTrue(DAILY_ROUTINES_A1_VOCABULARY.all { it.topic == DAILY_ROUTINES_TOPIC })
    }

    @Test
    fun seedDataAllA1Level() {
        assertTrue(DAILY_ROUTINES_A1_VOCABULARY.all { it.cefrLevel == DAILY_ROUTINES_CEFR })
    }

    @Test
    fun seedDataAllHaveVocabularySkill() {
        assertTrue(DAILY_ROUTINES_A1_VOCABULARY.all { it.skill == "vocabulary" })
    }

    @Test
    fun seedDataWordsAreNotBlank() {
        assertTrue(DAILY_ROUTINES_A1_VOCABULARY.all { it.word.isNotBlank() })
        assertTrue(DAILY_ROUTINES_A1_VOCABULARY.all { it.meaning.isNotBlank() })
        assertTrue(DAILY_ROUTINES_A1_VOCABULARY.all { it.exampleSentence.isNotBlank() })
    }

    @Test
    fun progressDefaultsToNotLearned() {
        val progress = VocabularyProgressEntity(vocabularyId = "test_id")
        assertEquals(false, progress.learned)
        assertEquals(0, progress.attempts)
        assertEquals(0L, progress.lastAttemptAt)
    }

    @Test
    fun progressLearnedCountMatchesFilter() {
        val progressList = listOf(
            VocabularyProgressEntity("a", learned = true, attempts = 1, lastAttemptAt = 1L),
            VocabularyProgressEntity("b", learned = false, attempts = 2, lastAttemptAt = 2L),
            VocabularyProgressEntity("c", learned = true, attempts = 1, lastAttemptAt = 3L),
        )
        assertEquals(2, progressList.count { it.learned })
    }

    @Test
    fun reviewFilterReturnsOnlyLearnedWords() {
        val vocab = DAILY_ROUTINES_A1_VOCABULARY
        val progressMap = mapOf(
            vocab[0].id to VocabularyProgressEntity(vocab[0].id, learned = true, attempts = 1, lastAttemptAt = 1L),
            vocab[1].id to VocabularyProgressEntity(vocab[1].id, learned = false, attempts = 1, lastAttemptAt = 2L),
        )
        val learnedWords = vocab.filter { progressMap[it.id]?.learned == true }
        assertEquals(1, learnedWords.size)
        assertEquals(vocab[0].word, learnedWords[0].word)
    }
}
