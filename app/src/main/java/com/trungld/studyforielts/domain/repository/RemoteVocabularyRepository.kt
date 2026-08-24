package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import kotlinx.coroutines.flow.Flow

interface RemoteVocabularyRepository {

    fun observeVocabulariesByLessonServerId(lessonServerId: String): Flow<List<RemoteVocabularyEntity>>

    suspend fun updateVocabularyLearnedStatus(
        lessonServerId: String,
        word: String,
        isLearned: Boolean,
    )
}