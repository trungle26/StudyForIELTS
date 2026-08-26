package com.trungld.studyforielts.di

import com.trungld.studyforielts.BuildConfig
import com.trungld.studyforielts.data.remote.api.DictationBffApi
import com.trungld.studyforielts.data.remote.api.WritingApi
import com.trungld.studyforielts.data.remote.api.YoutubeBffApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Timeouts: LLM responses can be slow (long essay + few-shot prompt),
        // so we bump read/call/write to 180 s. OkHttp already streams response
        // bodies by default; the bigger window is the main timeout fix.
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS) // 0 = no overall call timeout; we rely on read timeout
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.YOUTUBE_BFF_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideYoutubeBffApi(retrofit: Retrofit): YoutubeBffApi {
        return retrofit.create(YoutubeBffApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDictationBffApi(retrofit: Retrofit): DictationBffApi =
        retrofit.create(DictationBffApi::class.java)

    @Provides
    @Singleton
    fun provideWritingApi(retrofit: Retrofit): WritingApi {
        return retrofit.create(WritingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStrategyApi(retrofit: Retrofit): com.trungld.studyforielts.data.remote.api.StrategyApi {
        return retrofit.create(com.trungld.studyforielts.data.remote.api.StrategyApi::class.java)
    }
}
