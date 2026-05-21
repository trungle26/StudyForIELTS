package com.trungld.studyforielts.domain.repository

import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.domain.model.LessonOverview
import kotlinx.coroutines.flow.Flow

interface LessonRepository {

    fun getLessonsByLevel(level: String): Flow<List<LessonEntity>>

    fun observeLessonOverviewsByLevel(level: String): Flow<List<LessonOverview>>
}
