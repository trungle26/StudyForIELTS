package com.trungld.studyforielts.presentation.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LessonListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    lessonRepository: LessonRepository,
) : ViewModel() {

    private val level = checkNotNull(savedStateHandle.get<String>(LEVEL_ARGUMENT))

    val uiState: StateFlow<LessonListUiState> = lessonRepository.observeLessonOverviewsByLevel(level)
        .map { lessons ->
            LessonListUiState(
                level = level,
                lessons = lessons,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LessonListUiState(level = level),
        )

    companion object {
        const val LEVEL_ARGUMENT = "level"
    }
}
