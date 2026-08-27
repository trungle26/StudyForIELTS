package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.model.RemoteDictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CachedRemoteDictationLesson
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.RemoteDictationLesson
import kotlinx.coroutines.flow.Flow

interface RemoteDictationRepository {

    fun observeLessons(level: String? = null): Flow<List<RemoteDictationLesson>>

    /** Same as [observeLessons] but enriches each lesson with cache + offline-audio metadata. */
    fun observeCachedLessons(level: String? = null): Flow<List<CachedRemoteDictationLesson>>

    fun observeLesson(lessonId: String): Flow<RemoteDictationLesson?>

    fun observeLessonSnapshot(lessonId: String): Flow<RemoteDictationLessonSnapshot?>

    suspend fun refreshLessons(
        level: String? = null,
        page: Int = 1,
        limit: Int = 100,
    ): Result<List<RemoteDictationLesson>>

    /** Cache-first refresh: returns cached rows immediately, refreshes in background. */
    suspend fun refreshLessonsCacheFirst(
        level: String? = null,
        now: Long = System.currentTimeMillis(),
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

    /** Records that the user opened this lesson; updates the LRU timestamp used for eviction. */
    suspend fun touchLesson(serverId: String, now: Long = System.currentTimeMillis())

    /** Persists the path/size of a downloaded audio file (or clears them with `path = null`). */
    suspend fun updateLocalAudio(
        serverId: String,
        path: String?,
        bytes: Long,
        now: Long = System.currentTimeMillis(),
    )

    /** All lessons that currently have a downloaded audio file, ordered LRU first. */
    suspend fun downloadedLessonsByAccessTime(): List<RemoteDictationLessonEntity>
}

