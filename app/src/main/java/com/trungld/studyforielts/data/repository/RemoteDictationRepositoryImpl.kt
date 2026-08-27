package com.trungld.studyforielts.data.repository

import androidx.room.withTransaction
import com.trungld.studyforielts.data.local.dao.RemoteDictationDao
import com.trungld.studyforielts.data.local.dao.RemoteDictationProgressDao
import com.trungld.studyforielts.data.local.dao.RemoteDictationSentenceProgressDao
import com.trungld.studyforielts.data.local.dao.RemoteVocabularyDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationProgressEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceProgressEntity
import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import com.trungld.studyforielts.data.local.entity.SentenceStatus
import com.trungld.studyforielts.data.local.model.RemoteDictationLessonSnapshot
import com.trungld.studyforielts.data.remote.api.DictationBffApi
import com.trungld.studyforielts.data.remote.model.DictationVocabularyDto
import com.trungld.studyforielts.data.remote.model.DictationLessonDto
import com.trungld.studyforielts.domain.model.CacheStatus
import com.trungld.studyforielts.domain.model.CachedRemoteDictationLesson
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.RemoteDictationLesson
import com.trungld.studyforielts.domain.model.RemoteDictationSentence
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.domain.repository.StudyActivityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Singleton
class RemoteDictationRepositoryImpl @Inject constructor(
    private val api: DictationBffApi,
    private val dao: RemoteDictationDao,
    private val progressDao: RemoteDictationProgressDao,
    private val sentenceProgressDao: RemoteDictationSentenceProgressDao,
    private val vocabularyDao: RemoteVocabularyDao,
    private val appDatabase: AppDatabase,
    private val studyActivityRepository: StudyActivityRepository,
) : RemoteDictationRepository {

    override fun observeLessons(level: String?): Flow<List<RemoteDictationLesson>> {
        val lessons = if (level == null) dao.observeAllLessons() else dao.observeLessonsByLevel(level)
        return lessons.map { entities -> entities.map { it.toDomain(emptyList()) } }
    }

    override fun observeCachedLessons(level: String?): Flow<List<CachedRemoteDictationLesson>> {
        val lessons = if (level == null) dao.observeAllLessons() else dao.observeLessonsByLevel(level)
        return combine(lessons, vocabularyDao.observeAllVocabularies()) { entities, vocabularies ->
            val vocabularyLessonIds = vocabularies.mapTo(mutableSetOf()) { it.lessonServerId }
            entities.map { entity ->
                entity.toCached(
                    now = System.currentTimeMillis(),
                    hasVocabulary = entity.serverId in vocabularyLessonIds,
                )
            }
        }
    }

    override suspend fun refreshLessonsCacheFirst(
        level: String?,
        now: Long,
    ): Result<List<RemoteDictationLesson>> = runCatching {
        // Cache is already serving the UI; refresh in the background and let the Flow re-emit.
        // We deliberately do not throw if the network is unavailable — cached data is still valid.
        refreshLessons(level = level).getOrElse { error ->
            // Swallow network errors: the list VM already shows the cached rows; failing here
            // would only produce a "refresh failed" banner for stale-but-still-usable data.
            if (error !is java.io.IOException) throw error
            emptyList<RemoteDictationLesson>()
        }
        cachedSnapshot(level)
    }

    override suspend fun touchLesson(serverId: String, now: Long) {
        dao.touchLastAccessedAt(serverId, now)
    }

    override suspend fun updateLocalAudio(serverId: String, path: String?, bytes: Long, now: Long) {
        dao.updateLocalAudio(serverId = serverId, path = path, bytes = bytes, timestamp = now)
    }

    override suspend fun downloadedLessonsByAccessTime(): List<RemoteDictationLessonEntity> {
        return dao.observeDownloadedLessonsByAccessTime()
    }

    /**
     * One-shot snapshot of cached lessons (the observe* APIs are cold Flows).
     * Used after a background refresh to surface the current cache contents.
     */
    private suspend fun cachedSnapshot(level: String?): List<RemoteDictationLesson> {
        // Bridge: collect the first emission of the Flow into a list.
        val flow = if (level == null) dao.observeAllLessons() else dao.observeLessonsByLevel(level)
        return flow.first().map { it.toDomain(emptyList()) }
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
        // Preserve offline cache metadata across REPLACE upserts so user downloads survive
        // a metadata refresh.
        val existingByServerId = dao.observeAllLessons().first().associateBy { it.serverId }
        val lessons = items.map { dto ->
            val existing = existingByServerId[dto.id]
            val fresh = dto.toLessonEntity()
            fresh.copy(
                lastAccessedAt = existing?.lastAccessedAt ?: fresh.cachedAt,
                localAudioPath = existing?.localAudioPath,
                localAudioBytes = existing?.localAudioBytes ?: 0L,
                audioDownloadedAt = existing?.audioDownloadedAt ?: 0L,
            )
        }
        val sentences = items.flatMap { lesson -> lesson.sentences.map { it.toSentenceEntity(lesson.id) } }
        appDatabase.withTransaction {
            dao.replaceAllLessons(lessons, sentences)
            items.forEach { dto ->
                vocabularyDao.replaceLessonVocabularies(
                    lessonServerId = dto.id,
                    vocabularies = dto.vocabularies.map { it.toVocabularyEntity(dto.id) },
                )
            }
        }
        items.map { it.toDomain() }
    }

    override suspend fun refreshLesson(lessonId: String): Result<RemoteDictationLesson> = runCatching {
        val dto = api.getLesson(lessonId).lesson
        appDatabase.withTransaction {
            // Preserve offline cache metadata across the REPLACE upsert.
            val existing = dao.observeAllLessons().first().firstOrNull { it.serverId == dto.id }
            val fresh = dto.toLessonEntity()
            val merged = fresh.copy(
                lastAccessedAt = existing?.lastAccessedAt ?: fresh.cachedAt,
                localAudioPath = existing?.localAudioPath,
                localAudioBytes = existing?.localAudioBytes ?: 0L,
                audioDownloadedAt = existing?.audioDownloadedAt ?: 0L,
            )
            dao.upsertLessons(listOf(merged))
            dao.deleteSentences(dto.id)
            dao.upsertSentences(dto.sentences.map { it.toSentenceEntity(dto.id) })
            vocabularyDao.replaceLessonVocabularies(
                lessonServerId = dto.id,
                vocabularies = dto.vocabularies.map { it.toVocabularyEntity(dto.id) },
            )
        }
        dto.toDomain()
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
        if (result.isCorrect) studyActivityRepository.recordToday()
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
        studyActivityRepository.recordToday()
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
        studyActivityRepository.recordToday()
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

    companion object {
        /**
         * Cache TTL for remote lesson metadata. After this window the entry is served from cache
         * but flagged [CacheStatus.STALE]; the list ViewModel refreshes it in the background.
         *
         * `ponytail:` ceiling = 24h is a sensible default for slow-changing content. Upgrade path:
         * make TTL per-lesson (server-driven) once the BFF exposes a `cacheControl.maxAge` field.
         */
        val CACHE_TTL: Duration = 24.hours
    }

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

    private fun DictationVocabularyDto.toVocabularyEntity(lessonId: String) =
        RemoteVocabularyEntity(
            lessonServerId = lessonId,
            word = word,
            phonetic = phonetic,
            meaning = meaning,
            exampleSentence = exampleSentence,
        )

    private fun com.trungld.studyforielts.data.remote.model.DictationSentenceDto.toSentenceEntity(lessonId: String) =
        RemoteDictationSentenceEntity(lessonId, orderIndex, text, startTimeMs, endTimeMs)

    private fun com.trungld.studyforielts.data.remote.model.DictationSentenceDto.toDomain() =
        RemoteDictationSentence(orderIndex, text, startTimeMs, endTimeMs)

    private fun RemoteDictationSentenceEntity.toDomain() =
        RemoteDictationSentence(orderIndex, text, startTimeMs, endTimeMs)
}

internal fun RemoteDictationLessonEntity.toCached(
    now: Long,
    hasVocabulary: Boolean = false,
): CachedRemoteDictationLesson {
    val age = now - cachedAt
    val status = when {
        age <= 0L -> CacheStatus.FRESH
        age < RemoteDictationRepositoryImpl.CACHE_TTL.inWholeMilliseconds -> CacheStatus.FRESH
        else -> CacheStatus.STALE
    }
    return CachedRemoteDictationLesson(
        lesson = toDomain(emptyList()),
        cacheStatus = status,
        hasLocalAudio = !localAudioPath.isNullOrBlank(),
        hasVocabulary = hasVocabulary,
        localAudioBytes = localAudioBytes,
    )
}

internal fun RemoteDictationLessonEntity.toDomain(sentences: List<RemoteDictationSentence>) =
    RemoteDictationLesson(serverId, title, level, source, audioUrl, durationSeconds, updatedAt, sentences)
