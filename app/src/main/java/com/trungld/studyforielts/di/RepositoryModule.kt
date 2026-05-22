package com.trungld.studyforielts.di

import com.trungld.studyforielts.data.repository.DictationRepositoryImpl
import com.trungld.studyforielts.data.repository.LessonRepositoryImpl
import com.trungld.studyforielts.data.repository.VocabularyRepositoryImpl
import com.trungld.studyforielts.domain.repository.DictationRepository
import com.trungld.studyforielts.domain.repository.LessonRepository
import com.trungld.studyforielts.domain.repository.VocabularyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDictationRepository(
        implementation: DictationRepositoryImpl,
    ): DictationRepository

    @Binds
    @Singleton
    abstract fun bindLessonRepository(
        implementation: LessonRepositoryImpl,
    ): LessonRepository

    @Binds
    @Singleton
    abstract fun bindVocabularyRepository(
        implementation: VocabularyRepositoryImpl,
    ): VocabularyRepository
}
