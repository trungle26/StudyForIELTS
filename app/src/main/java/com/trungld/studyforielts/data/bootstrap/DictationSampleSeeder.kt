package com.trungld.studyforielts.data.bootstrap

import androidx.room.withTransaction
import com.trungld.studyforielts.data.local.dao.LessonDao
import com.trungld.studyforielts.data.local.dao.ProgressDao
import com.trungld.studyforielts.data.local.dao.SentenceDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictationSampleSeeder @Inject constructor(
    private val appDatabase: AppDatabase,
    private val lessonDao: LessonDao,
    private val sentenceDao: SentenceDao,
    private val progressDao: ProgressDao,
) {

    suspend fun seedIfNeeded() {
        appDatabase.withTransaction {
            val lesson = lessonDao.getLessonById(SAMPLE_LESSON_ID)
            if (lesson == null) {
                lessonDao.insertLesson(
                    LessonEntity(
                        id = SAMPLE_LESSON_ID,
                        title = "Daily Routine Dictation",
                        level = "B1",
                        audioUrl = "",
                    )
                )
            }

            if (sentenceDao.getSentenceCountByLessonId(SAMPLE_LESSON_ID) == 0) {
                sentenceDao.insertSentences(sampleSentences())
            }

            if (progressDao.getProgressByLessonId(SAMPLE_LESSON_ID) == null) {
                progressDao.upsertProgress(
                    ProgressEntity(
                        lessonId = SAMPLE_LESSON_ID,
                        currentSentenceIndex = 0,
                        progressPercentage = 0f,
                        currentDraftText = "",
                        lastPlaybackPositionMs = 0L,
                        isLessonCompleted = false,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    private fun sampleSentences(): List<SentenceEntity> {
        return listOf(
            SentenceEntity(
                id = 101L,
                lessonId = SAMPLE_LESSON_ID,
                orderIndex = 0,
                correctText = "I usually wake up at six o'clock every morning.",
                startTime = 0L,
                endTime = 5_000L,
            ),
            SentenceEntity(
                id = 102L,
                lessonId = SAMPLE_LESSON_ID,
                orderIndex = 1,
                correctText = "After that, I make a cup of coffee and check my email.",
                startTime = 5_000L,
                endTime = 10_500L,
            ),
            SentenceEntity(
                id = 103L,
                lessonId = SAMPLE_LESSON_ID,
                orderIndex = 2,
                correctText = "My office is not far from home, so I often walk to work.",
                startTime = 10_500L,
                endTime = 16_000L,
            ),
            SentenceEntity(
                id = 104L,
                lessonId = SAMPLE_LESSON_ID,
                orderIndex = 3,
                correctText = "In the evening, I spend about an hour reviewing vocabulary.",
                startTime = 16_000L,
                endTime = 21_500L,
            ),
            SentenceEntity(
                id = 105L,
                lessonId = SAMPLE_LESSON_ID,
                orderIndex = 4,
                correctText = "This simple routine helps me stay consistent with my IELTS practice.",
                startTime = 21_500L,
                endTime = 27_500L,
            ),
        )
    }

    companion object {
        const val SAMPLE_LESSON_ID = 1L
    }
}
