package com.trungld.studyforielts.presentation.youtube

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.model.YoutubeVideo
import com.trungld.studyforielts.domain.repository.OnlineYoutubeDictationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class YoutubePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OnlineYoutubeDictationRepository,
) : ViewModel() {

    private val videoId = checkNotNull(savedStateHandle.get<String>(VIDEO_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(YoutubePreviewUiState(videoId = videoId))
    val uiState: StateFlow<YoutubePreviewUiState> = _uiState.asStateFlow()

    private var hasRequestedTranscript = false

    init {
        observeCachedLesson()
    }

    fun retryTranscriptLoad() {
        val video = uiState.value.video ?: return
        hasRequestedTranscript = false
        loadTranscript(video)
    }

    private fun observeCachedLesson() {
        viewModelScope.launch {
            repository.observeSavedLesson(videoId).collect { lesson ->
                _uiState.update {
                    it.copy(
                        video = lesson?.video ?: it.video,
                        lesson = lesson?.takeIf { cachedLesson -> cachedLesson.sentences.isNotEmpty() } ?: it.lesson,
                        isLoadingVideo = lesson == null,
                        isLoadingTranscript = it.isLoadingTranscript && lesson?.sentences.isNullOrEmpty(),
                    )
                }

                when {
                    lesson == null -> Unit
                    lesson.sentences.isNotEmpty() -> {
                        _uiState.update {
                            it.copy(
                                lesson = lesson,
                                isLoadingTranscript = false,
                                errorMessage = null,
                            )
                        }
                    }

                    !hasRequestedTranscript -> loadTranscript(lesson.video)
                }
            }
        }
    }

    private fun loadTranscript(video: YoutubeVideo) {
        hasRequestedTranscript = true
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    video = video,
                    isLoadingVideo = false,
                    isLoadingTranscript = true,
                    errorMessage = null,
                )
            }

            repository.saveForOffline(video)
                .onSuccess { lesson ->
                    _uiState.update {
                        it.copy(
                            video = lesson.video,
                            lesson = lesson,
                            isLoadingTranscript = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingTranscript = false,
                            errorMessage = throwable.message ?: "Unable to load transcript.",
                        )
                    }
                }
        }
    }

    companion object {
        const val VIDEO_ID_ARGUMENT = "videoId"
    }
}
