package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.LessonDao
import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.domain.model.LessonOverview
import com.trungld.studyforielts.domain.repository.LessonRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LessonRepositoryImpl @Inject constructor(
    private val lessonDao: LessonDao,
) : LessonRepository {

    override fun getLessonsByLevel(level: String): Flow<List<LessonEntity>> {
        return lessonDao.observeLessonsByLevel(level)
    }

    override fun observeLessonOverviewsByLevel(level: String): Flow<List<LessonOverview>> {
        return lessonDao.observeLessonOverviewsByLevel(level).map { overviews ->
            overviews.map { overview ->
                LessonOverview(
                    lessonId = overview.lessonId,
                    title = overview.title,
                    level = overview.level,
                    progressPercentage = overview.progressPercentage,
                )
            }
        }
    }
}
