package com.trungld.studyforielts.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.trungld.studyforielts.data.local.dao.DictationDao
import com.trungld.studyforielts.data.local.dao.LessonDao
import com.trungld.studyforielts.data.local.dao.ProgressDao
import com.trungld.studyforielts.data.local.dao.SentenceDao
import com.trungld.studyforielts.data.local.dao.VocabularyDao
import com.trungld.studyforielts.data.local.entity.LessonEntity
import com.trungld.studyforielts.data.local.entity.ProgressEntity
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.data.local.entity.SentenceProgressEntity
import com.trungld.studyforielts.data.local.entity.VocabularyEntity

@Database(
    entities = [
        LessonEntity::class,
        SentenceEntity::class,
        ProgressEntity::class,
        SentenceProgressEntity::class,
        VocabularyEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dictationDao(): DictationDao

    abstract fun lessonDao(): LessonDao

    abstract fun sentenceDao(): SentenceDao

    abstract fun progressDao(): ProgressDao

    abstract fun sentenceProgressDao(): com.trungld.studyforielts.data.local.dao.SentenceProgressDao

    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        const val DATABASE_NAME = "study_for_ielts.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE sentences
                    ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE progress
                    ADD COLUMN currentDraftText TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE progress
                    ADD COLUMN lastPlaybackPositionMs INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE progress
                    ADD COLUMN isLessonCompleted INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE progress
                    ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sentence_progress (
                        lessonId INTEGER NOT NULL,
                        sentenceId INTEGER NOT NULL,
                        userAnswer TEXT NOT NULL,
                        isCorrect INTEGER NOT NULL,
                        attemptsCount INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        lastCheckedAt INTEGER NOT NULL,
                        PRIMARY KEY(lessonId, sentenceId),
                        FOREIGN KEY(lessonId) REFERENCES lessons(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(sentenceId) REFERENCES sentences(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sentence_progress_lessonId ON sentence_progress(lessonId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sentence_progress_sentenceId ON sentence_progress(sentenceId)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vocabularies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        lessonId INTEGER NOT NULL,
                        word TEXT NOT NULL,
                        phonetic TEXT NOT NULL,
                        meaning TEXT NOT NULL,
                        exampleSentence TEXT NOT NULL,
                        isLearned INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(lessonId) REFERENCES lessons(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_vocabularies_lessonId ON vocabularies(lessonId)"
                )
            }
        }
    }
}
