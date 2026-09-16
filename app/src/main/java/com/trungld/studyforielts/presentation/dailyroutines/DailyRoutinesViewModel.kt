package com.trungld.studyforielts.presentation.dailyroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.local.entity.TopicVocabularyEntity
import com.trungld.studyforielts.data.local.entity.VocabularyProgressEntity
import com.trungld.studyforielts.data.preferences.DailyRoutinesProgressPreferences
import com.trungld.studyforielts.domain.model.DAILY_ROUTINES_A1_VOCABULARY
import com.trungld.studyforielts.domain.model.DAILY_ROUTINES_CEFR
import com.trungld.studyforielts.domain.model.DAILY_ROUTINES_TOPIC
import com.trungld.studyforielts.domain.model.DailyRoutinesActivity
import com.trungld.studyforielts.domain.repository.TopicVocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DailyRoutinesViewModel @Inject constructor(
    private val progress: DailyRoutinesProgressPreferences,
    private val vocabRepo: TopicVocabularyRepository,
) : ViewModel() {
    val completed: StateFlow<Set<DailyRoutinesActivity>> = progress.completed.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )

    val vocabulary: StateFlow<List<TopicVocabularyEntity>> =
        vocabRepo.observeVocabulary(DAILY_ROUTINES_TOPIC, DAILY_ROUTINES_CEFR).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
        )

    val vocabProgress: StateFlow<List<VocabularyProgressEntity>> =
        vocabRepo.observeProgress(DAILY_ROUTINES_TOPIC, DAILY_ROUTINES_CEFR).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
        )

    init {
        viewModelScope.launch {
            vocabRepo.seedIfEmpty(DAILY_ROUTINES_TOPIC, DAILY_ROUTINES_CEFR, DAILY_ROUTINES_A1_VOCABULARY)
        }
    }

    fun markCompleted(activity: DailyRoutinesActivity) {
        viewModelScope.launch { progress.markCompleted(activity) }
    }

    fun markWordLearned(vocabularyId: String, learned: Boolean) {
        viewModelScope.launch { vocabRepo.recordAttempt(vocabularyId, learned) }
    }
}
