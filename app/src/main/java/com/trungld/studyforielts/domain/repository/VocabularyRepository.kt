package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {

    fun observeVocabulariesByLessonId(lessonId: Long): Flow<List<VocabularyEntity>>
}
