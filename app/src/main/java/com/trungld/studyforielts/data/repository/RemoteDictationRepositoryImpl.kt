package com.trungld.studyforielts.data.repository

import androidx.room.withTransaction
import com.trungld.studyforielts.data.local.dao.RemoteDictationDao
import com.trungld.studyforielts.data.local.dao.RemoteDictationProgressDao
import com.trungld.studyforielts.data.local.dao.RemoteDictationSentenceProgressDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationProgressEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceStatus
import com.trungld.studyforielts.data.local.model.RemoteDictationLessonSnapshot
import com.trungld.studyforielts.data.remote.api.DictationBffApi
import com.trungld.studyforielts.data.remote.model.DictationLessonDto
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.RemoteDictationLesson
import com.trungld.studyforielts.domain.model.RemoteDictationSentence
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class RemoteDictationRepositoryImpl @Inject constructor(
    private val api: DictationBffApi,
    private val dao: RemoteDictationDao,
    private val progressDao: RemoteDictationProgressDao,
    private val sentenceProgressDao: RemoteDictationSentenceProgressDao,
    private val appDatabase: AppDatabase,
) : RemoteDictationRepository {

    override fun observeLessons(level: String?): Flow<List<RemoteDictationLesson>> {
        val lessons = if (level == null) dao.observeAllLessons() else dao.observeLessonsByLevel(level)
        return lessons.map { entities -> entities.map { it.toDomain(emptyList()) } }
    }

    override fun observeLesson(lessonId: String): Flow<RemoteDictationLesson?> {
        return combine(dao.observeLesson(lessonId), dao.observeSentences(lessonId)) { lesson, sentences ->
            lesson?.toDomain(sentences.map { it.toDomain() })
        }
    }

    override fun observeLessonSnapshot(lessonId: String): Flow<RemoteDictationLessonSnapshot?> {
        return dao.observeLessonSnapshot(lessonId)
    }

    override suspend fun refreshLessons(
        level: String?,
        page: Int,
        limit: Int,
    ): Result<List<RemoteDictationLesson>> = runCatching {
        val items = api.listLessons(level, page, limit).items
        val lessons = items.map { it.toLessonEntity() }
        val sentences = items.flatMap { lesson -> lesson.sentences.map { it.toSentenceEntity(lesson.id) } }
        dao.replaceAllLessons(lessons, sentences)
        items.map { it.toDomain() }
    }

    override suspend fun refreshLesson(lessonId: String): Result<RemoteDictationLesson> = runCatching {
        val lesson = api.getLesson(lessonId).lesson
        dao.upsertLessons(listOf(lesson.toLessonEntity()))
        dao.deleteSentences(lesson.id)
        dao.upsertSentences(lesson.sentences.map { it.toSentenceEntity(lesson.id) })
        lesson.toDomain()
    }

    override suspend fun ensureLessonProgress(lessonId: String) {
        appDatabase.withTransaction {
            val existing = progressDao.getProgressByLessonServerId(lessonId)
            if (existing == null) {
                progressDao.upsertProgress(defaultProgress(lessonId))
            }
        }
    }

    override suspend fun saveDraft(lessonId: String, draft: String) {
        appDatabase.withTransaction {
            val current = progressDao.getProgressByLessonServerId(lessonId)
                ?: defaultProgress(lessonId)
            progressDao.upsertProgress(
                current.copy(
                    currentDraftText = draft,
                    updatedAt = now(),
                )
            )
        }
    }

    override suspend fun updatePlaybackPosition(lessonId: String, playbackPositionMs: Long) {
        appDatabase.withTransaction {
            val current = progressDao.getProgressByLessonServerId(lessonId)
                ?: defaultProgress(lessonId)
            progressDao.upsertProgress(
                current.copy(
                    lastPlaybackPositionMs = playbackPositionMs,
                    updatedAt = now(),
                )
            )
        }
    }

    override suspend fun submitSentenceAnswer(
        lessonId: String,
        sentence: RemoteDictationSentenceEntity,
        userAnswer: String,
        result: CheckResult,
    ) {
        appDatabase.withTransaction {
            val current = progressDao.getProgressByLessonServerId(lessonId)
                ?: defaultProgress(lessonId)
            val existingSentenceProgress = sentenceProgressDao.getSentenceProgressByOrderIndex(
                lessonServerId = lessonId,
                orderIndex = sentence.orderIndex,
            )
            val totalSentences = dao.observeSentencesCount(lessonId)
            val attemptsCount = (existingSentenceProgress?.attemptsCount ?: 0) + 1
            val checkedAt = now()

            sentenceProgressDao.upsertSentenceProgress(
                RemoteDictationSentenceProgressEntity(
                    lessonServerId = lessonId,
                    orderIndex = sentence.orderIndex,
                    userAnswer = userAnswer,
                    isCorrect = result.isCorrect,
                    attemptsCount = attemptsCount,
                    status = if (result.isCorrect) SentenceStatus.CORRECT else SentenceStatus.IN_PROGRESS,
                    lastCheckedAt = checkedAt,
                )
            )

            if (result.isCorrect) {
                val nextSentenceIndex = (sentence.orderIndex + 1).coerceAtMost(totalSentences)
                val nextSentence = dao.getSentenceByOrderIndex(lessonId, nextSentenceIndex)
                progressDao.upsertProgress(
                    current.copy(
                        currentSentenceIndex = nextSentenceIndex,
                        progressPercentage = calculateProgressPercentage(nextSentenceIndex, totalSentences),
                        currentDraftText = "",
                        lastPlaybackPositionMs = nextSentence?.startTimeMs?.toLong() ?: sentence.endTimeMs.toLong(),
                        isLessonCompleted = nextSentenceIndex >= totalSentences,
                        updatedAt = checkedAt,
                    )
                )
            } else {
                progressDao.upsertProgress(
                    current.copy(
                        currentSentenceIndex = sentence.orderIndex,
                        currentDraftText = userAnswer,
                        lastPlaybackPositionMs = sentence.startTimeMs.toLong(),
                        isLessonCompleted = false,
                        updatedAt = checkedAt,
                    )
                )
            }
        }
    }

    override suspend fun continueAfterReview(
        lessonId: String,
        sentence: RemoteDictationSentenceEntity,
    ) {
        appDatabase.withTransaction {
            val current = progressDao.getProgressByLessonServerId(lessonId)
                ?: defaultProgress(lessonId)
            val existingSentenceProgress = sentenceProgressDao.getSentenceProgressByOrderIndex(
                lessonServerId = lessonId,
                orderIndex = sentence.orderIndex,
            )
            val totalSentences = dao.observeSentencesCount(lessonId)
            val nextSentenceIndex = (sentence.orderIndex + 1).coerceAtMost(totalSentences)
            val nextSentence = dao.getSentenceByOrderIndex(lessonId, nextSentenceIndex)
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
                current.copy(
                    currentSentenceIndex = nextSentenceIndex,
                    progressPercentage = calculateProgressPercentage(nextSentenceIndex, totalSentences),
                    currentDraftText = "",
                    lastPlaybackPositionMs = nextSentence?.startTimeMs?.toLong() ?: sentence.endTimeMs.toLong(),
                    isLessonCompleted = nextSentenceIndex >= totalSentences,
                    updatedAt = checkedAt,
                )
            )
        }
    }

    override suspend fun skipSentence(
        lessonId: String,
        sentence: RemoteDictationSentenceEntity,
        draft: String,
    ) {
        appDatabase.withTransaction {
            val current = progressDao.getProgressByLessonServerId(lessonId)
                ?: defaultProgress(lessonId)
            val existingSentenceProgress = sentenceProgressDao.getSentenceProgressByOrderIndex(
                lessonServerId = lessonId,
                orderIndex = sentence.orderIndex,
            )
            val totalSentences = dao.observeSentencesCount(lessonId)
            val nextSentenceIndex = (sentence.orderIndex + 1).coerceAtMost(totalSentences)
            val nextSentence = dao.getSentenceByOrderIndex(lessonId, nextSentenceIndex)
            val checkedAt = now()

            sentenceProgressDao.upsertSentenceProgress(
                RemoteDictationSentenceProgressEntity(
                    lessonServerId = lessonId,
                    orderIndex = sentence.orderIndex,
                    userAnswer = draft,
                    isCorrect = false,
                    attemptsCount = existingSentenceProgress?.attemptsCount ?: 0,
                    status = SentenceStatus.SKIPPED,
                    lastCheckedAt = checkedAt,
                )
            )

            progressDao.upsertProgress(
                current.copy(
                    currentSentenceIndex = nextSentenceIndex,
                    progressPercentage = calculateProgressPercentage(nextSentenceIndex, totalSentences),
                    currentDraftText = "",
                    lastPlaybackPositionMs = nextSentence?.startTimeMs?.toLong() ?: sentence.endTimeMs.toLong(),
                    isLessonCompleted = nextSentenceIndex >= totalSentences,
                    updatedAt = checkedAt,
                )
            )
        }
    }

    override suspend fun resetLessonProgress(lessonId: String) {
        appDatabase.withTransaction {
            sentenceProgressDao.deleteSentenceProgressByLessonServerId(lessonId)
            progressDao.upsertProgress(defaultProgress(lessonId))
        }
    }

    private fun defaultProgress(lessonId: String): RemoteDictationProgressEntity =
        RemoteDictationProgressEntity(
            lessonServerId = lessonId,
            currentSentenceIndex = 0,
            progressPercentage = 0f,
            currentDraftText = "",
            lastPlaybackPositionMs = 0L,
            isLessonCompleted = false,
            updatedAt = now(),
        )

    private fun calculateProgressPercentage(currentSentenceIndex: Int, totalSentences: Int): Float {
        if (totalSentences <= 0) return 0f
        return (currentSentenceIndex.toFloat() / totalSentences.toFloat()) * 100f
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun DictationLessonDto.toLessonEntity() = RemoteDictationLessonEntity(
        serverId = id,
        title = title,
        level = level,
        source = source,
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        updatedAt = updatedAt,
    )

    private fun DictationLessonDto.toDomain() = RemoteDictationLesson(
        id = id,
        title = title,
        level = level,
        source = source,
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        updatedAt = updatedAt,
        sentences = sentences.map { it.toDomain() },
    )

    private fun com.trungld.studyforielts.data.remote.model.DictationSentenceDto.toSentenceEntity(lessonId: String) =
        RemoteDictationSentenceEntity(lessonId, orderIndex, text, startTimeMs, endTimeMs)

    private fun com.trungld.studyforielts.data.remote.model.DictationSentenceDto.toDomain() =
        RemoteDictationSentence(orderIndex, text, startTimeMs, endTimeMs)

    private fun RemoteDictationLessonEntity.toDomain(sentences: List<RemoteDictationSentence>) =
        RemoteDictationLesson(serverId, title, level, source, audioUrl, durationSeconds, updatedAt, sentences)

    private fun RemoteDictationSentenceEntity.toDomain() =
        RemoteDictationSentence(orderIndex, text, startTimeMs, endTimeMs)
}
