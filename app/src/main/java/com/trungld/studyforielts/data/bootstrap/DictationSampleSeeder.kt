package com.trungld.studyforielts.data.bootstrap

import android.content.Context
import androidx.room.withTransaction
import com.trungld.studyforielts.data.local.dao.LessonDao
import com.trungld.studyforielts.data.local.dao.ProgressDao
import com.trungld.studyforielts.data.local.dao.SentenceDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictationSampleSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val lessonDao: LessonDao,
    private val sentenceDao: SentenceDao,
    private val progressDao: ProgressDao,
) {

    suspend fun seedIfNeeded() {
        appDatabase.withTransaction {
            val sampleAudioUri = buildSampleAudioUri()
            val lessons = sampleLessons(sampleAudioUri)

            lessons.forEach { lesson ->
                val existingLesson = lessonDao.getLessonById(lesson.id)
                if (existingLesson == null) {
                    lessonDao.insertLesson(lesson)
                } else if (existingLesson != lesson) {
                    lessonDao.updateLesson(lesson)
                }

                if (sentenceDao.getSentenceCountByLessonId(lesson.id) == 0) {
                    sentenceDao.insertSentences(sampleSentencesForLesson(lesson.id))
                }
                if (progressDao.getProgressByLessonId(lesson.id) == null) {
                    progressDao.upsertProgress(defaultProgress(lesson.id))
                }
            }
        }
    }

    private fun sampleLessons(audioUrl: String): List<LessonEntity> {
        return listOf(
            LessonEntity(
                id = LESSON_ID_B1,
                title = "Daily Routine Dictation",
                level = "B1",
                audioUrl = audioUrl,
            ),
            LessonEntity(
                id = LESSON_ID_B2,
                title = "Workplace Communication Dictation",
                level = "B2",
                audioUrl = audioUrl,
            ),
            LessonEntity(
                id = LESSON_ID_C1,
                title = "Academic Discussion Dictation",
                level = "C1",
                audioUrl = audioUrl,
            ),
        )
    }

    private fun sampleSentencesForLesson(lessonId: Long): List<SentenceEntity> {
        val sentences = when (lessonId) {
            LESSON_ID_B1 -> listOf(
                "I usually wake up at six o'clock every morning.",
                "After that, I make a cup of coffee and check my email.",
                "My office is not far from home, so I often walk to work.",
                "In the evening, I spend about an hour reviewing vocabulary.",
                "This simple routine helps me stay consistent with my IELTS practice.",
            )

            LESSON_ID_B2 -> listOf(
                "The project manager asked everyone to submit feedback before Friday afternoon.",
                "During the meeting, several team members suggested a more flexible schedule.",
                "We need to clarify the budget constraints before we finalize the proposal.",
                "Although the deadline is tight, the team believes the target is still realistic.",
                "Clear communication often prevents minor misunderstandings from becoming serious problems.",
            )

            else -> listOf(
                "Researchers increasingly emphasize the role of attention in long term language retention.",
                "A well designed study routine can improve both fluency and critical listening skills.",
                "Many candidates underestimate how much consistency matters in advanced exam preparation.",
                "From an academic perspective, reflection is just as valuable as repeated exposure.",
                "Sustained progress usually depends on deliberate practice rather than short bursts of motivation.",
            )
        }

        return sentences.mapIndexed { index, sentence ->
            val startTime = index * SEGMENT_DURATION_MS
            SentenceEntity(
                id = lessonId * 100L + index + 1L,
                lessonId = lessonId,
                orderIndex = index,
                correctText = sentence,
                startTime = startTime,
                endTime = startTime + SEGMENT_DURATION_MS,
            )
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
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun buildSampleAudioUri(): String {
        return "android.resource://${context.packageName}/raw/$SAMPLE_AUDIO_FILE_NAME"
    }

    companion object {
        const val LESSON_ID_B1 = 1L
        const val LESSON_ID_B2 = 2L
        const val LESSON_ID_C1 = 3L
        const val SAMPLE_AUDIO_FILE_NAME = "dictation_sample"
        private const val SEGMENT_DURATION_MS = 5_500L
    }
}
