package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.StudyActivityDao
import com.trungld.studyforielts.data.local.entity.StudyActivityEntity
import com.trungld.studyforielts.domain.model.StreakSummary
import com.trungld.studyforielts.domain.repository.StudyActivityRepository
import com.trungld.studyforielts.domain.usecase.ComputeStreakUseCase
import com.trungld.studyforielts.util.LocalDateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyActivityRepositoryImpl @Inject constructor(
    private val studyActivityDao: StudyActivityDao,
    private val localDateProvider: LocalDateProvider,
) : StudyActivityRepository {

    override fun observeStreak(): Flow<StreakSummary> {
        val today = localDateProvider.today()
        return studyActivityDao.observeAll().map { rows ->
            ComputeStreakUseCase(
                activityDates = rows.mapNotNull { runCatching {
                    java.time.LocalDate.parse(it.activityDate)
                }.getOrNull() },
                today = today,
            )
        }
    }

    override suspend fun recordToday() {
        val today = localDateProvider.today()
        studyActivityDao.insertIfAbsent(
            StudyActivityEntity(activityDate = today.toString())
        )
    }
}
