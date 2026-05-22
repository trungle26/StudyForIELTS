package com.trungld.studyforielts.presentation.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.VocabularyEntity
import com.trungld.studyforielts.domain.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vocabularyRepository: VocabularyRepository,
    private val vocabularyTtsManager: VocabularyTtsManager,
) : ViewModel() {

    private val lessonId = checkNotNull(savedStateHandle.get<Long>(LESSON_ID_ARGUMENT))
    private val allVocabularies = MutableStateFlow<List<VocabularyEntity>>(emptyList())
    private val queueIds = MutableStateFlow<List<Long>>(emptyList())

    val uiState: StateFlow<VocabularyUiState> = combine(
        allVocabularies,
        queueIds,
    ) { vocabularies, queue ->
        val vocabularyMap = vocabularies.associateBy(VocabularyEntity::id)
        VocabularyUiState(
            lessonId = lessonId,
            allVocabularies = vocabularies,
            queueVocabularies = queue.mapNotNull(vocabularyMap::get),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VocabularyUiState(lessonId = lessonId),
    )

    init {
        observeVocabularies()
    }

    fun markVocabularyLearned(vocabulary: VocabularyEntity) {
        queueIds.update { currentQueue ->
            currentQueue.filterNot { it == vocabulary.id }
        }
        viewModelScope.launch {
            vocabularyRepository.updateVocabularyLearnedStatus(
                vocabId = vocabulary.id,
                isLearned = true,
            )
        }
    }

    fun recycleVocabulary(vocabulary: VocabularyEntity) {
        queueIds.update { currentQueue ->
            currentQueue.filterNot { it == vocabulary.id } + vocabulary.id
        }
        viewModelScope.launch {
            vocabularyRepository.updateVocabularyLearnedStatus(
                vocabId = vocabulary.id,
                isLearned = false,
            )
        }
    }

    fun pronounce(word: String) {
        vocabularyTtsManager.speak(word)
    }

    override fun onCleared() {
        vocabularyTtsManager.shutdown()
        super.onCleared()
    }

    private fun observeVocabularies() {
        viewModelScope.launch {
            vocabularyRepository.observeVocabulariesByLessonId(lessonId).collect { vocabularies ->
                allVocabularies.value = vocabularies
                syncQueue(vocabularies)
            }
        }
    }

    private fun syncQueue(vocabularies: List<VocabularyEntity>) {
        val unlearnedIds = vocabularies
            .filterNot(VocabularyEntity::isLearned)
            .map(VocabularyEntity::id)

        queueIds.update { currentQueue ->
            val retained = currentQueue.filter { it in unlearnedIds }
            val missing = unlearnedIds.filterNot { it in retained }
            retained + missing
        }
    }

    companion object {
        const val LESSON_ID_ARGUMENT = "lessonId"
    }
}
