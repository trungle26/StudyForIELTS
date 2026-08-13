package com.trungld.studyforielts.data.repository

import com.trungld.studyforielts.data.local.dao.RemoteDictationDao
import com.trungld.studyforielts.data.local.entity.RemoteDictationLessonEntity
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.data.remote.api.DictationBffApi
import com.trungld.studyforielts.data.remote.model.DictationLessonDto
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
