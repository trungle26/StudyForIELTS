package com.trungld.studyforielts.data.remote.api

import com.trungld.studyforielts.data.remote.model.DictationLessonDetailDto
import com.trungld.studyforielts.data.remote.model.DictationLessonListDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DictationBffApi {

    @GET("dictation/lessons")
    suspend fun listLessons(
        @Query("level") level: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): DictationLessonListDto

    @GET("dictation/lessons/{lessonId}")
    suspend fun getLesson(
        @Path("lessonId") lessonId: String,
    ): DictationLessonDetailDto
}
