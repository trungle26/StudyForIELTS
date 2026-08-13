package com.trungld.studyforielts.presentation.remotedictation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemoteDictationListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RemoteDictationRepository,
) : ViewModel() {

    val level: String = checkNotNull(savedStateHandle[LEVEL_ARGUMENT])

    private val refreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<RemoteDictationListUiState> = combine(
        repository.observeLessons(level), refreshing, error,
    ) { lessons, isRefreshing, errorMessage ->
        RemoteDictationListUiState(level, lessons, isRefreshing, errorMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoteDictationListUiState(level))

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = repository.refreshLessons(level).exceptionOrNull()?.message
            refreshing.value = false
        }
    }

    companion object { const val LEVEL_ARGUMENT = "level" }
}
