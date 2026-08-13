package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.domain.model.RemoteDictationLesson
import kotlinx.coroutines.flow.Flow

interface RemoteDictationRepository {

    fun observeLessons(level: String? = null): Flow<List<RemoteDictationLesson>>

    fun observeLesson(lessonId: String): Flow<RemoteDictationLesson?>

    suspend fun refreshLessons(
        level: String? = null,
        page: Int = 1,
        limit: Int = 100,
    ): Result<List<RemoteDictationLesson>>

    suspend fun refreshLesson(lessonId: String): Result<RemoteDictationLesson>
}
