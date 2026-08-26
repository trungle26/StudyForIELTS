package com.trungld.studyforielts.di

import com.trungld.studyforielts.data.repository.DictationRepositoryImpl
import com.trungld.studyforielts.data.repository.LessonRepositoryImpl
import com.trungld.studyforielts.data.repository.OnlineYoutubeDictationRepositoryImpl
import com.trungld.studyforielts.data.repository.RemoteDictationRepositoryImpl
import com.trungld.studyforielts.data.repository.RemoteVocabularyRepositoryImpl
import com.trungld.studyforielts.data.repository.SavedVocabularyRepositoryImpl
import com.trungld.studyforielts.data.repository.StrategyRepositoryImpl
import com.trungld.studyforielts.data.repository.StudyActivityRepositoryImpl
import com.trungld.studyforielts.data.repository.VocabularyRepositoryImpl
import com.trungld.studyforielts.domain.repository.DictationRepository
import com.trungld.studyforielts.domain.repository.LessonRepository
import com.trungld.studyforielts.domain.repository.OnlineYoutubeDictationRepository
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.domain.repository.RemoteVocabularyRepository
import com.trungld.studyforielts.domain.repository.SavedVocabularyRepository
import com.trungld.studyforielts.domain.repository.StrategyRepository
import com.trungld.studyforielts.domain.repository.StudyActivityRepository
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

    @Binds
    @Singleton
    abstract fun bindSavedVocabularyRepository(
        implementation: SavedVocabularyRepositoryImpl,
    ): SavedVocabularyRepository

    @Binds
    @Singleton
    abstract fun bindStudyActivityRepository(
        implementation: StudyActivityRepositoryImpl,
    ): StudyActivityRepository

    @Binds
    @Singleton
    abstract fun bindOnlineYoutubeDictationRepository(
        implementation: OnlineYoutubeDictationRepositoryImpl,
    ): OnlineYoutubeDictationRepository

    @Binds
    @Singleton
    abstract fun bindRemoteDictationRepository(
        implementation: RemoteDictationRepositoryImpl,
    ): RemoteDictationRepository

    @Binds
    @Singleton
    abstract fun bindRemoteVocabularyRepository(
        implementation: RemoteVocabularyRepositoryImpl,
    ): RemoteVocabularyRepository

    @Binds
    @Singleton
    abstract fun bindStrategyRepository(
        implementation: StrategyRepositoryImpl,
    ): StrategyRepository
}
