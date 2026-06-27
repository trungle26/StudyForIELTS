package com.trungld.studyforielts.data.repository

import androidx.room.withTransaction
import com.trungld.studyforielts.data.local.dao.YoutubeDictationDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import com.trungld.studyforielts.data.local.entity.YoutubeSentenceEntity
import com.trungld.studyforielts.data.local.entity.YoutubeVideoEntity
import com.trungld.studyforielts.data.local.model.YoutubeVideoWithSentences
import com.trungld.studyforielts.data.remote.api.YoutubeBffApi
import com.trungld.studyforielts.data.remote.model.YoutubeFeedItemDto
import com.trungld.studyforielts.data.remote.model.YoutubeSearchResultDto
import com.trungld.studyforielts.data.remote.model.YoutubeTranscriptResponseDto
import com.trungld.studyforielts.domain.model.YoutubeDictationLesson
import com.trungld.studyforielts.domain.model.YoutubeSentence
import com.trungld.studyforielts.domain.model.YoutubeVideo
import com.trungld.studyforielts.domain.repository.OnlineYoutubeDictationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OnlineYoutubeDictationRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val youtubeDictationDao: YoutubeDictationDao,
    private val youtubeBffApi: YoutubeBffApi,
) : OnlineYoutubeDictationRepository {

    override fun observeSavedVideos(): Flow<List<YoutubeVideo>> {
        return youtubeDictationDao.observeSavedVideos().map { videos ->
            videos.map { it.toDomainVideo() }
        }
    }

    override fun observeSavedLesson(videoId: String): Flow<YoutubeDictationLesson?> {
        return youtubeDictationDao.observeVideoWithSentences(videoId).map { local ->
            local?.toDomainLesson()
        }
    }

    override suspend fun fetchFeed(
        level: String,
        page: Int,
        limit: Int,
    ): Result<List<YoutubeVideo>> {
        return runCatching {
            val response = youtubeBffApi.getFeed(
                level = level,
                page = page,
                limit = limit,
            )
            val videos = response.items.map { it.toDomainVideo() }
            cacheFeedResults(response.items)
            videos
        }
    }

    override suspend fun searchVideos(
        query: String,
        limit: Int,
    ): Result<List<YoutubeVideo>> {
        return runCatching {
            val response = youtubeBffApi.searchVideos(
                query = query,
                limit = limit,
            )
            val videos = response.results.map { it.toDomainVideo() }
            cacheSearchResults(response.results)
            videos
        }
    }

    override suspend fun cacheTranscript(
        video: YoutubeVideo,
        language: String,
    ): Result<YoutubeDictationLesson> {
        return fetchAndCacheTranscript(
            video = video,
            language = language,
            markSaved = false,
        )
    }

    override suspend fun saveForOffline(
        video: YoutubeVideo,
        language: String,
    ): Result<YoutubeDictationLesson> {
        return fetchAndCacheTranscript(
            video = video,
            language = language,
            markSaved = true,
        )
    }

    private suspend fun cacheSearchResults(results: List<YoutubeSearchResultDto>) {
        val cachedAt = now()
        val entities = results.map { result ->
            val existingVideo = youtubeDictationDao.getVideo(result.videoId)
            YoutubeVideoEntity(
                videoId = result.videoId,
                title = result.title,
                thumbnailUrl = result.bestThumbnailUrl(),
                transcriptLanguage = existingVideo?.transcriptLanguage,
                transcriptLanguageCode = existingVideo?.transcriptLanguageCode,
                isTranscriptGenerated = existingVideo?.isTranscriptGenerated,
                isSaved = existingVideo?.isSaved ?: false,
                cachedAt = cachedAt,
            )
        }

        if (entities.isNotEmpty()) {
            youtubeDictationDao.insertVideos(entities)
        }
    }

    private suspend fun cacheFeedResults(results: List<YoutubeFeedItemDto>) {
        val cachedAt = now()
        val entities = results.map { result ->
            val existingVideo = youtubeDictationDao.getVideo(result.videoId)
            YoutubeVideoEntity(
                videoId = result.videoId,
                title = result.title,
                thumbnailUrl = result.thumbnailUrl.orEmpty(),
                transcriptLanguage = existingVideo?.transcriptLanguage,
                transcriptLanguageCode = existingVideo?.transcriptLanguageCode,
                isTranscriptGenerated = existingVideo?.isTranscriptGenerated,
                isSaved = existingVideo?.isSaved ?: false,
                cachedAt = cachedAt,
            )
        }

        if (entities.isNotEmpty()) {
            youtubeDictationDao.insertVideos(entities)
        }
    }

    private suspend fun fetchAndCacheTranscript(
        video: YoutubeVideo,
        language: String,
        markSaved: Boolean,
    ): Result<YoutubeDictationLesson> {
        return runCatching {
            val transcript = youtubeBffApi.getTranscript(
                videoId = video.videoId,
                language = language,
            )
            val existingVideo = youtubeDictationDao.getVideo(video.videoId)
            val shouldSave = markSaved || video.isSaved || existingVideo?.isSaved == true

            appDatabase.withTransaction {
                youtubeDictationDao.deleteSentencesByVideoId(video.videoId)
                youtubeDictationDao.insertVideo(
                    transcript.toVideoEntity(
                        video = video,
                        existingVideo = existingVideo,
                        isSaved = shouldSave,
                    )
                )
                youtubeDictationDao.insertSentences(transcript.toSentenceEntities())
            }

            transcript.toDomainLesson(
                video = video.copy(isSaved = shouldSave),
            )
        }
    }

    private fun YoutubeSearchResultDto.toDomainVideo(): YoutubeVideo {
        return YoutubeVideo(
            videoId = videoId,
            title = title,
            thumbnailUrl = bestThumbnailUrl(),
        )
    }

    private fun YoutubeFeedItemDto.toDomainVideo(): YoutubeVideo {
        return YoutubeVideo(
            videoId = videoId,
            title = title,
            thumbnailUrl = thumbnailUrl.orEmpty(),
            channelTitle = channelTitle.orEmpty(),
            level = level,
            durationSeconds = durationSeconds,
            tags = tags,
        )
    }

    private fun YoutubeSearchResultDto.bestThumbnailUrl(): String {
        return thumbnails
            .maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
            ?.url
            ?: thumbnail
            .orEmpty()
    }

    private fun YoutubeTranscriptResponseDto.toVideoEntity(
        video: YoutubeVideo,
        existingVideo: YoutubeVideoEntity?,
        isSaved: Boolean,
    ): YoutubeVideoEntity {
        return YoutubeVideoEntity(
            videoId = videoId,
            title = video.title.ifBlank { existingVideo?.title.orEmpty() },
            thumbnailUrl = video.thumbnailUrl.ifBlank { existingVideo?.thumbnailUrl.orEmpty() },
            transcriptLanguage = language,
            transcriptLanguageCode = languageCode,
            isTranscriptGenerated = isGenerated,
            isSaved = isSaved,
            cachedAt = now(),
        )
    }

    private fun YoutubeTranscriptResponseDto.toSentenceEntities(): List<YoutubeSentenceEntity> {
        return segments.mapIndexed { index, segment ->
            YoutubeSentenceEntity(
                videoId = videoId,
                orderIndex = index,
                startTimeMs = (segment.startTime * MILLIS_PER_SECOND).roundToLong(),
                endTimeMs = (segment.endTime * MILLIS_PER_SECOND).roundToLong(),
                text = segment.text,
            )
        }
    }

    private fun YoutubeTranscriptResponseDto.toDomainLesson(
        video: YoutubeVideo,
    ): YoutubeDictationLesson {
        return YoutubeDictationLesson(
            video = video,
            language = language,
            languageCode = languageCode,
            isGenerated = isGenerated,
            sentences = toSentenceEntities().map { it.toDomainSentence() },
        )
    }

    private fun YoutubeVideoWithSentences.toDomainLesson(): YoutubeDictationLesson {
        return YoutubeDictationLesson(
            video = video.toDomainVideo(),
            language = video.transcriptLanguage,
            languageCode = video.transcriptLanguageCode,
            isGenerated = video.isTranscriptGenerated,
            sentences = sentences
                .sortedBy { it.orderIndex }
                .map { it.toDomainSentence() },
        )
    }

    private fun YoutubeVideoEntity.toDomainVideo(): YoutubeVideo {
        return YoutubeVideo(
            videoId = videoId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            isSaved = isSaved,
        )
    }

    private fun YoutubeSentenceEntity.toDomainSentence(): YoutubeSentence {
        return YoutubeSentence(
            orderIndex = orderIndex,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            text = text,
        )
    }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
