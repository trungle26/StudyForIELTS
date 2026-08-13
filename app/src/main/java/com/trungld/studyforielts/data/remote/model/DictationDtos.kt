package com.trungld.studyforielts.data.remote.model

import com.google.gson.annotations.SerializedName

data class DictationLessonListDto(
    @SerializedName("level") val level: String?,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("items") val items: List<DictationLessonDto>,
)

data class DictationLessonDetailDto(
    @SerializedName("lesson") val lesson: DictationLessonDto,
)

data class DictationLessonDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("level") val level: String,
    @SerializedName("source") val source: String = "",
    @SerializedName("audioUrl") val audioUrl: String,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("sentences") val sentences: List<DictationSentenceDto> = emptyList(),
    @SerializedName("vocabularies") val vocabularies: List<DictationVocabularyDto> = emptyList(),
    @SerializedName("updatedAt") val updatedAt: String? = null,
)

data class DictationSentenceDto(
    @SerializedName("orderIndex") val orderIndex: Int,
    @SerializedName("text") val text: String,
    @SerializedName("startTimeMs") val startTimeMs: Int,
    @SerializedName("endTimeMs") val endTimeMs: Int,
)

data class DictationVocabularyDto(
    @SerializedName("word") val word: String,
    @SerializedName("phonetic") val phonetic: String = "",
    @SerializedName("meaning") val meaning: String = "",
    @SerializedName("exampleSentence") val exampleSentence: String = "",
)
