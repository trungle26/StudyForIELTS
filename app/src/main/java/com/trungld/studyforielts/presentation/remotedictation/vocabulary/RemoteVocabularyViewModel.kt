package com.trungld.studyforielts.presentation.remotedictation.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import com.trungld.studyforielts.domain.repository.RemoteDictationRepository
import com.trungld.studyforielts.domain.repository.RemoteVocabularyRepository
import com.trungld.studyforielts.domain.repository.SavedVocabularyRepository
import com.trungld.studyforielts.presentation.vocabulary.VocabularyTtsManager
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
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val vocabularyTtsManager: VocabularyTtsManager,
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
            savedVocabularyRepository.removeVocabularyByWord(vocabulary.word)
        }
    }

    fun recycleVocabulary(vocabulary: RemoteVocabularyEntity) {
        // Dismiss from this lesson's deck while keeping a copy in the local "saved vocab" store
        // (unlearned words the user wants to review later, outside this lesson).
        queueIds.update { current -> current - vocabulary.word }
        viewModelScope.launch {
            remoteVocabularyRepository.updateVocabularyLearnedStatus(
                lessonServerId = lessonServerId,
                word = vocabulary.word,
                isLearned = true,
            )
            savedVocabularyRepository.saveVocabulary(
                word = vocabulary.word,
                phonetic = vocabulary.phonetic,
                meaning = vocabulary.meaning,
                exampleSentence = vocabulary.exampleSentence,
                sourceLessonId = "remote_$lessonServerId",
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