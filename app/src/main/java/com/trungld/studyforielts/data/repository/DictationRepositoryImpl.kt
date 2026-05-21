package com.trungld.studyforielts.data.repository

import androidx.room.withTransaction
import com.trungld.studyforielts.data.local.dao.DictationDao
import com.trungld.studyforielts.data.local.dao.ProgressDao
import com.trungld.studyforielts.data.local.dao.SentenceDao
import com.trungld.studyforielts.data.local.dao.SentenceProgressDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.entity.SentenceProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceStatus
import com.trungld.studyforielts.data.local.model.DictationLessonSnapshot
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.repository.DictationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictationRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dictationDao: DictationDao,
    private val sentenceDao: SentenceDao,
    private val progressDao: ProgressDao,
    private val sentenceProgressDao: SentenceProgressDao,
) : DictationRepository {

    override fun observeLessonSnapshot(lessonId: Long): Flow<DictationLessonSnapshot?> {
        return dictationDao.observeLessonSnapshot(lessonId)
    }

    override suspend fun ensureLessonProgress(lessonId: Long) {
        appDatabase.withTransaction {
            val existingProgress = progressDao.getProgressByLessonId(lessonId)
            if (existingProgress == null) {
                progressDao.upsertProgress(defaultProgress(lessonId = lessonId))
            }
        }
    }

    override suspend fun saveDraft(lessonId: Long, draft: String) {
        appDatabase.withTransaction {
            val currentProgress = progressDao.getProgressByLessonId(lessonId)
                ?: defaultProgress(lessonId = lessonId)
            progressDao.upsertProgress(
                currentProgress.copy(
                    currentDraftText = draft,
                    updatedAt = now(),
                )
            )
        }
    }

    override suspend fun updatePlaybackPosition(
        lessonId: Long,
        playbackPositionMs: Long,
    ) {
        appDatabase.withTransaction {
            val currentProgress = progressDao.getProgressByLessonId(lessonId)
                ?: defaultProgress(lessonId = lessonId)
            progressDao.upsertProgress(
                currentProgress.copy(
                    lastPlaybackPositionMs = playbackPositionMs,
                    updatedAt = now(),
                )
            )
        }
    }

    override suspend fun submitSentenceAnswer(
        lessonId: Long,
        sentence: SentenceEntity,
        userAnswer: String,
        result: CheckResult,
    ) {
        appDatabase.withTransaction {
            val currentProgress = progressDao.getProgressByLessonId(lessonId)
                ?: defaultProgress(lessonId = lessonId)
            val existingSentenceProgress = sentenceProgressDao.getSentenceProgressByIds(
                lessonId = lessonId,
                sentenceId = sentence.id,
            )
            val totalSentences = sentenceDao.getSentenceCountByLessonId(lessonId)
            val attemptsCount = (existingSentenceProgress?.attemptsCount ?: 0) + 1
            val checkedAt = now()

            sentenceProgressDao.upsertSentenceProgress(
                SentenceProgressEntity(
                    lessonId = lessonId,
                    sentenceId = sentence.id,
                    userAnswer = userAnswer,
                    isCorrect = result.isCorrect,
                    attemptsCount = attemptsCount,
                    status = if (result.isCorrect) SentenceStatus.CORRECT else SentenceStatus.IN_PROGRESS,
                    lastCheckedAt = checkedAt,
                )
            )

            if (result.isCorrect) {
                val nextSentenceIndex = (sentence.orderIndex + 1).coerceAtMost(totalSentences)
                val nextSentence = sentenceDao.getSentenceByLessonIdAndOrderIndex(
                    lessonId = lessonId,
                    orderIndex = nextSentenceIndex,
                )
                progressDao.upsertProgress(
                    currentProgress.copy(
                        currentSentenceIndex = nextSentenceIndex,
                        progressPercentage = calculateProgressPercentage(
                            currentSentenceIndex = nextSentenceIndex,
                            totalSentences = totalSentences,
                        ),
                        currentDraftText = "",
                        lastPlaybackPositionMs = nextSentence?.startTime ?: sentence.endTime,
                        isLessonCompleted = nextSentenceIndex >= totalSentences,
                        updatedAt = checkedAt,
                    )
                )
            } else {
                progressDao.upsertProgress(
                    currentProgress.copy(
                        currentSentenceIndex = sentence.orderIndex,
                        currentDraftText = userAnswer,
                        lastPlaybackPositionMs = sentence.startTime,
                        isLessonCompleted = false,
                        updatedAt = checkedAt,
                    )
                )
            }
        }
    }

    override suspend fun continueAfterReview(
        lessonId: Long,
        sentence: SentenceEntity,
    ) {
        appDatabase.withTransaction {
            val currentProgress = progressDao.getProgressByLessonId(lessonId)
                ?: defaultProgress(lessonId = lessonId)
            val existingSentenceProgress = sentenceProgressDao.getSentenceProgressByIds(
                lessonId = lessonId,
                sentenceId = sentence.id,
            )
            val totalSentences = sentenceDao.getSentenceCountByLessonId(lessonId)
            val nextSentenceIndex = (sentence.orderIndex + 1).coerceAtMost(totalSentences)
            val nextSentence = sentenceDao.getSentenceByLessonIdAndOrderIndex(
                lessonId = lessonId,
                orderIndex = nextSentenceIndex,
            )
            val checkedAt = now()

            if (existingSentenceProgress != null && !existingSentenceProgress.isCorrect) {
                sentenceProgressDao.upsertSentenceProgress(
                    existingSentenceProgress.copy(
                        status = SentenceStatus.SKIPPED,
                        lastCheckedAt = checkedAt,
                    )
                )
            }

            progressDao.upsertProgress(
                currentProgress.copy(
                    currentSentenceIndex = nextSentenceIndex,
                    progressPercentage = calculateProgressPercentage(
                        currentSentenceIndex = nextSentenceIndex,
                        totalSentences = totalSentences,
                    ),
                    currentDraftText = "",
                    lastPlaybackPositionMs = nextSentence?.startTime ?: sentence.endTime,
                    isLessonCompleted = nextSentenceIndex >= totalSentences,
                    updatedAt = checkedAt,
                )
            )
        }
    }

    override suspend fun skipSentence(
        lessonId: Long,
        sentence: SentenceEntity,
        draft: String,
    ) {
        appDatabase.withTransaction {
            val currentProgress = progressDao.getProgressByLessonId(lessonId)
                ?: defaultProgress(lessonId = lessonId)
            val existingSentenceProgress = sentenceProgressDao.getSentenceProgressByIds(
                lessonId = lessonId,
                sentenceId = sentence.id,
            )
            val totalSentences = sentenceDao.getSentenceCountByLessonId(lessonId)
            val nextSentenceIndex = (sentence.orderIndex + 1).coerceAtMost(totalSentences)
            val nextSentence = sentenceDao.getSentenceByLessonIdAndOrderIndex(
                lessonId = lessonId,
                orderIndex = nextSentenceIndex,
            )
            val checkedAt = now()

            sentenceProgressDao.upsertSentenceProgress(
                SentenceProgressEntity(
                    lessonId = lessonId,
                    sentenceId = sentence.id,
                    userAnswer = draft,
                    isCorrect = false,
                    attemptsCount = existingSentenceProgress?.attemptsCount ?: 0,
                    status = SentenceStatus.SKIPPED,
                    lastCheckedAt = checkedAt,
                )
            )

            progressDao.upsertProgress(
                currentProgress.copy(
                    currentSentenceIndex = nextSentenceIndex,
                    progressPercentage = calculateProgressPercentage(
                        currentSentenceIndex = nextSentenceIndex,
                        totalSentences = totalSentences,
                    ),
                    currentDraftText = "",
                    lastPlaybackPositionMs = nextSentence?.startTime ?: sentence.endTime,
                    isLessonCompleted = nextSentenceIndex >= totalSentences,
                    updatedAt = checkedAt,
                )
            )
        }
    }

    override suspend fun resetLessonProgress(lessonId: Long) {
        appDatabase.withTransaction {
            sentenceProgressDao.deleteSentenceProgressByLessonId(lessonId)
            progressDao.upsertProgress(defaultProgress(lessonId = lessonId))
        }
    }

    private fun defaultProgress(lessonId: Long): ProgressEntity {
        return ProgressEntity(
            lessonId = lessonId,
            currentSentenceIndex = 0,
            progressPercentage = 0f,
            currentDraftText = "",
            lastPlaybackPositionMs = 0L,
            isLessonCompleted = false,
            updatedAt = now(),
        )
    }

    private fun calculateProgressPercentage(
        currentSentenceIndex: Int,
        totalSentences: Int,
    ): Float {
        if (totalSentences <= 0) return 0f
        return (currentSentenceIndex.toFloat() / totalSentences.toFloat()) * 100f
    }

    private fun now(): Long = System.currentTimeMillis()
}
