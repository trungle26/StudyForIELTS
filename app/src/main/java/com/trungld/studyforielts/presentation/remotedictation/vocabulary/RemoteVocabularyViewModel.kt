package com.trungld.studyforielts.presentation.remotedictation.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.domain.repository.RemoteVocabularyRepository
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
class RemoteVocabularyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val remoteVocabularyRepository: RemoteVocabularyRepository,
    private val remoteDictationRepository: RemoteDictationRepository,
) : ViewModel() {

    private val lessonServerId: String =
        checkNotNull(savedStateHandle.get<String>(LESSON_ID_ARGUMENT))

    private val allVocabularies = MutableStateFlow<List<RemoteVocabularyEntity>>(emptyList())
    private val queueIds = MutableStateFlow<Set<String>>(emptySet())
    private val isLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RemoteVocabularyUiState> = combine(
        allVocabularies,
        queueIds,
        isLoading,
        error,
    ) { vocabularies, queue, loading, errorMessage ->
        val queueList = vocabularies.filter { it.word in queue }
        RemoteVocabularyUiState(
            lessonServerId = lessonServerId,
            isLoading = loading,
            allVocabularies = vocabularies,
            queueVocabularies = queueList,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemoteVocabularyUiState(lessonServerId = lessonServerId),
    )

    init {
        refresh()
        observeVocabularies()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            val result = remoteDictationRepository.refreshLesson(lessonServerId)
            if (result.isFailure) {
                error.value = result.exceptionOrNull()?.message
            }
            // isLoading flips false inside observeVocabularies when the first emission arrives.
        }
    }

    fun markVocabularyLearned(vocabulary: RemoteVocabularyEntity) {
        queueIds.update { current -> current - vocabulary.word }
        viewModelScope.launch {
            remoteVocabularyRepository.updateVocabularyLearnedStatus(
                lessonServerId = lessonServerId,
                word = vocabulary.word,
                isLearned = true,
            )
        }
    }

    fun recycleVocabulary(vocabulary: RemoteVocabularyEntity) {
        queueIds.update { current -> current + vocabulary.word }
        viewModelScope.launch {
            remoteVocabularyRepository.updateVocabularyLearnedStatus(
                lessonServerId = lessonServerId,
                word = vocabulary.word,
                isLearned = false,
            )
        }
    }

    private fun observeVocabularies() {
        viewModelScope.launch {
            remoteVocabularyRepository
                .observeVocabulariesByLessonServerId(lessonServerId)
                .collect { vocabularies ->
                    allVocabularies.value = vocabularies
                    isLoading.value = false
                    syncQueue(vocabularies)
                }
        }
    }

    private fun syncQueue(vocabularies: List<RemoteVocabularyEntity>) {
        val unlearnedWords = vocabularies
            .filterNot(RemoteVocabularyEntity::isLearned)
            .map(RemoteVocabularyEntity::word)
            .toSet()
        queueIds.update { current ->
            val retained = current intersect unlearnedWords
            val missing = unlearnedWords - current
            retained + missing
        }
    }

    companion object {
        const val LESSON_ID_ARGUMENT = "lessonId"
    }
}