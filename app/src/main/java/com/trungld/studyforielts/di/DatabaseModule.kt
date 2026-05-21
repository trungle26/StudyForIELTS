package com.trungld.studyforielts.di

import android.content.Context
import androidx.room.Room
import com.trungld.studyforielts.data.local.dao.LessonDao
import com.trungld.studyforielts.data.local.dao.ProgressDao
import com.trungld.studyforielts.data.local.dao.SentenceDao
import com.trungld.studyforielts.data.local.dao.DictationDao
import com.trungld.studyforielts.data.local.dao.SentenceProgressDao
import com.trungld.studyforielts.data.local.dao.VocabularyDao
import com.trungld.studyforielts.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
        )
            .build()
    }

    @Provides
    fun provideDictationDao(database: AppDatabase): DictationDao = database.dictationDao()

    @Provides
    fun provideLessonDao(database: AppDatabase): LessonDao = database.lessonDao()

    @Provides
    fun provideSentenceDao(database: AppDatabase): SentenceDao = database.sentenceDao()

    @Provides
    fun provideProgressDao(database: AppDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideSentenceProgressDao(database: AppDatabase): SentenceProgressDao =
        database.sentenceProgressDao()

    @Provides
    fun provideVocabularyDao(database: AppDatabase): VocabularyDao = database.vocabularyDao()
}
