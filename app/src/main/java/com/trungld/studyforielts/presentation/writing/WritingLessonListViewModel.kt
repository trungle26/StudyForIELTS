package com.trungld.studyforielts.presentation.writing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.remote.api.WritingApi
import com.trungld.studyforielts.data.remote.model.WritingLessonDto
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * ViewModel for the lesson-list screen.
 *
 * Fetches paginated published lessons from `GET /writing/lessons?task_type=...`.
 * The first page is loaded eagerly in [init]; subsequent pages are pulled on
 * demand by [loadMore] when the user scrolls.
 *
 * Task type ("task1" or "task2") is passed in as a nav argument; the same
 * ViewModel is reused for both task lists.
 */
@HiltViewModel
class WritingLessonListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val writingApi: WritingApi,
) : ViewModel() {

    private val taskType: String = checkNotNull(savedStateHandle.get<String>(TASK_TYPE_ARGUMENT)) {
        "WritingLessonListViewModel requires a '$TASK_TYPE_ARGUMENT' nav argument"
    }

    /** Exposed so the navigation layer can choose the right screen chrome. */
    val taskTypeOrDefault: String get() = taskType

    private val _uiState = MutableStateFlow<WritingLessonListUiState>(WritingLessonListUiState.Loading)
    val uiState: StateFlow<WritingLessonListUiState> = _uiState.asStateFlow()

    init {
        loadPage(1, replace = true)
    }

    /**
     * Pull a new page. If [replace] is true the existing list is dropped
     * (initial load + manual retry); otherwise the new items are appended
     * (infinite scroll).
     */
    fun loadPage(page: Int, replace: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            if (!replace && current is WritingLessonListUiState.Loaded && current.loadingMore) return@launch
            if (replace) {
                _uiState.value = WritingLessonListUiState.Loading
            } else if (current is WritingLessonListUiState.Loaded) {
                _uiState.value = current.copy(loadingMore = true)
            }
            try {
                val response = writingApi.listLessons(taskType = taskType, page = page, limit = PAGE_SIZE)
                val existing = if (replace) emptyList() else (current as? WritingLessonListUiState.Loaded)?.items ?: emptyList()
                _uiState.value = WritingLessonListUiState.Loaded(
                    items = existing + response.items,
                    page = response.page,
                    totalPages = response.totalPages,
                    loadingMore = false,
                )
            } catch (e: HttpException) {
                android.util.Log.e("WritingLessonListVM", "HTTP error loading lessons", e)
                _uiState.value = WritingLessonListUiState.Error(
                    "Server error: ${e.code()}. Please try again."
                )
            } catch (e: IOException) {
                android.util.Log.e("WritingLessonListVM", "Network error loading lessons", e)
                _uiState.value = WritingLessonListUiState.Error(
                    "Network error: please check your connection and try again."
                )
            } catch (e: Exception) {
                android.util.Log.e("WritingLessonListVM", "Unexpected error loading lessons", e)
                _uiState.value = WritingLessonListUiState.Error(
                    e.message ?: "Unexpected error."
                )
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value as? WritingLessonListUiState.Loaded ?: return
        if (current.loadingMore) return
        if (current.page >= current.totalPages) return
        loadPage(current.page + 1, replace = false)
    }

    fun retry() = loadPage(1, replace = true)

    companion object {
        const val TASK_TYPE_ARGUMENT = "taskType"
        const val PAGE_SIZE = 20
    }
}

/** UI state for the lesson-list screen. */
sealed interface WritingLessonListUiState {
    data object Loading : WritingLessonListUiState

    data class Loaded(
        val items: List<WritingLessonDto>,
        val page: Int,
        val totalPages: Int,
        val loadingMore: Boolean,
    ) : WritingLessonListUiState

    data class Error(val message: String) : WritingLessonListUiState
}
