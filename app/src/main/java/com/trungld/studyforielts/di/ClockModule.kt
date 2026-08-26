package com.trungld.studyforielts.di

import com.trungld.studyforielts.util.LocalDateProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun provideLocalDateProvider(): LocalDateProvider = LocalDateProvider.System
}
