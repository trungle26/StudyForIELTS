package com.trungld.studyforielts.domain.model

data class RemoteDictationLesson(
    val id: String,
    val title: String,
    val level: String,
    val source: String,
    val audioUrl: String,
    val contentType: String = "dictation",
    val skill: String? = "listening",
    val cefrLevel: String? = null,
    val sourceUrl: String? = null,
    val licenseNote: String? = null,
    val tags: List<String> = emptyList(),
    val durationSeconds: Int?,
    val updatedAt: String?,
    val sentences: List<RemoteDictationSentence> = emptyList(),
)

data class RemoteDictationSentence(
    val orderIndex: Int,
    val text: String,
    val startTimeMs: Int,
    val endTimeMs: Int,
)
