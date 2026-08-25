package com.trungld.studyforielts.presentation.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.SavedVocabularyEntity
import com.trungld.studyforielts.domain.repository.SavedVocabularyRepository
import com.trungld.studyforielts.presentation.vocabulary.VocabularyTtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelListUiState(
    val savedVocabularies: List<SavedVocabularyEntity> = emptyList(),
)

@HiltViewModel
class LevelListViewModel @Inject constructor(
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val vocabularyTtsManager: VocabularyTtsManager,
) : ViewModel() {

    val uiState: StateFlow<LevelListUiState> = savedVocabularyRepository
        .observeSavedVocabularies()
        .map { list -> LevelListUiState(savedVocabularies = list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LevelListUiState(),
        )

    fun pronounce(word: String) {
        vocabularyTtsManager.speak(word)
    }

    fun removeSavedVocabulary(id: Long) {
        viewModelScope.launch {
            savedVocabularyRepository.removeVocabulary(id)
        }
    }

    override fun onCleared() {
        vocabularyTtsManager.shutdown()
        super.onCleared()
    }
}
