package com.trungld.studyforielts.presentation.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vocabularyRepository: VocabularyRepository,
    private val vocabularyTtsManager: VocabularyTtsManager,
) : ViewModel() {

    private val lessonId = checkNotNull(savedStateHandle.get<Long>(LESSON_ID_ARGUMENT))
    val uiState: StateFlow<VocabularyUiState> = vocabularyRepository
        .observeVocabulariesByLessonId(lessonId)
        .map { vocabularies ->
            VocabularyUiState(
                lessonId = lessonId,
                vocabularies = vocabularies,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VocabularyUiState(lessonId = lessonId),
        )

    fun pronounce(word: String) {
        vocabularyTtsManager.speak(word)
    }

    override fun onCleared() {
        vocabularyTtsManager.shutdown()
        super.onCleared()
    }

    companion object {
        const val LESSON_ID_ARGUMENT = "lessonId"
    }
}
