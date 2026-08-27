package com.trungld.studyforielts.presentation.remotedictation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.presentation.common.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemoteDictationListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RemoteDictationRepository,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {

    val level: String = checkNotNull(savedStateHandle[LEVEL_ARGUMENT])

    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val isOnline = MutableStateFlow(connectivity.isOnline())

    init {
        observeConnectivity()
        refresh()
    }

    val uiState: StateFlow<RemoteDictationListUiState> = combine(
        repository.observeCachedLessons(level),
        refreshing,
        error,
        isOnline,
    ) { lessons, isRefreshing, errorMessage, online ->
        RemoteDictationListUiState(
            level = level,
            lessons = lessons,
            // First emission from Room flips this false; refreshing shows the spinner overlay.
            isLoading = isRefreshing && lessons.isEmpty(),
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
            isOffline = !online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoteDictationListUiState(level))

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = repository.refreshLessons(level).exceptionOrNull()?.message
            refreshing.value = false
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivity.observe().collect { isOnline.value = it }
        }
    }

    companion object { const val LEVEL_ARGUMENT = "level" }
}
