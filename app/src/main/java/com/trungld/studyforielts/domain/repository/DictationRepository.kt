package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.model.DictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CheckResult
import kotlinx.coroutines.flow.Flow

interface DictationRepository {

    fun observeLessonSnapshot(lessonId: Long): Flow<DictationLessonSnapshot?>

    suspend fun ensureLessonProgress(lessonId: Long)

    suspend fun saveDraft(lessonId: Long, draft: String)

    suspend fun updatePlaybackPosition(lessonId: Long, playbackPositionMs: Long)

    suspend fun submitSentenceAnswer(
        lessonId: Long,
        sentence: SentenceEntity,
        userAnswer: String,
        result: CheckResult,
    )

    suspend fun continueAfterReview(
        lessonId: Long,
        sentence: SentenceEntity,
    )

    suspend fun skipSentence(
        lessonId: Long,
        sentence: SentenceEntity,
        draft: String,
    )

    suspend fun resetLessonProgress(lessonId: Long)
}
