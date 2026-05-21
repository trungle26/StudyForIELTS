package com.trungld.studyforielts.di

import com.trungld.studyforielts.data.repository.DictationRepositoryImpl
import com.trungld.studyforielts.domain.repository.DictationRepository
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
}
