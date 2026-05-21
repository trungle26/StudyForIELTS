package com.trungld.studyforielts.domain.model

data class LessonOverview(
    val lessonId: Long,
    val title: String,
    val level: String,
    val progressPercentage: Float,
)
