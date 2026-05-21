package com.trungld.studyforielts.presentation.lesson

import com.trungld.studyforielts.domain.model.LessonOverview

data class LessonListUiState(
    val level: String = "",
    val lessons: List<LessonOverview> = emptyList(),
)
