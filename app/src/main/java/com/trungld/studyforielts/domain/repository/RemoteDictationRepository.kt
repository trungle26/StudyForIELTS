package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.model.RemoteDictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.RemoteDictationLesson
import kotlinx.coroutines.flow.Flow

interface RemoteDictationRepository {

    fun observeLessons(level: String? = null): Flow<List<RemoteDictationLesson>>

    fun observeLesson(lessonId: String): Flow<RemoteDictationLesson?>

    fun observeLessonSnapshot(lessonId: String): Flow<RemoteDictationLessonSnapshot?>

    suspend fun refreshLessons(
        level: String? = null,
        page: Int = 1,
        limit: Int = 100,
    ): Result<List<RemoteDictationLesson>>

    suspend fun refreshLesson(lessonId: String): Result<RemoteDictationLesson>

    suspend fun ensureLessonProgress(lessonId: String)

    suspend fun saveDraft(lessonId: String, draft: String)

    suspend fun updatePlaybackPosition(lessonId: String, playbackPositionMs: Long)

    suspend fun submitSentenceAnswer(
        lessonId: String,
        sentence: RemoteDictationSentenceEntity,
        userAnswer: String,
        result: CheckResult,
    )

    suspend fun continueAfterReview(
        lessonId: String,
        sentence: RemoteDictationSentenceEntity,
    )

    suspend fun skipSentence(
        lessonId: String,
        sentence: RemoteDictationSentenceEntity,
        draft: String,
    )

    suspend fun resetLessonProgress(lessonId: String)
}
