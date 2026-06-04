package com.trungld.studyforielts.presentation.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.repository.OnlineYoutubeDictationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class YoutubeBrowseViewModel @Inject constructor(
    private val repository: OnlineYoutubeDictationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(YoutubeBrowseUiState())
    val uiState: StateFlow<YoutubeBrowseUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeSavedVideos().collect { savedVideos ->
                _uiState.update { it.copy(savedVideos = savedVideos) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                errorMessage = null,
            )
        }
    }

    fun search() {
        val query = uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a search query first.") }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    hasSearched = true,
                    errorMessage = null,
                )
            }

            repository.searchVideos(query)
                .onSuccess { videos ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = videos,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = throwable.message ?: "Unable to search videos.",
                        )
                    }
                }
        }
    }
}
