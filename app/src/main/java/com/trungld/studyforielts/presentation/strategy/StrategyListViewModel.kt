package com.trungld.studyforielts.presentation.strategy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.model.IeltsSkillType
import com.trungld.studyforielts.domain.model.StrategyGuide
import com.trungld.studyforielts.domain.repository.StrategyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StrategyListUiState(
    val skill: IeltsSkillType = IeltsSkillType.LISTENING,
    val selectedQuestionType: String? = null,
    val questionTypes: List<Pair<String, String>> = emptyList(), // (key, label)
    val strategies: List<StrategyGuide> = emptyList(),
    val filteredStrategies: List<StrategyGuide> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class StrategyListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strategyRepository: StrategyRepository,
) : ViewModel() {

    companion object {
        const val SKILL_ARGUMENT = "skill"
    }

    private val skillKey: String = checkNotNull(savedStateHandle[SKILL_ARGUMENT])
    val skill: IeltsSkillType = IeltsSkillType.fromKey(skillKey)

    private val _selectedQuestionType = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StrategyListUiState> = combine(
        strategyRepository.observeStrategies(skill),
        _selectedQuestionType,
    ) { allItems, selectedType ->
        val types = allItems.map { it.questionType to it.questionTypeLabel }.distinct()
        val filtered = if (selectedType == null) {
            allItems
        } else {
            allItems.filter { it.questionType == selectedType }
        }
        StrategyListUiState(
            skill = skill,
            selectedQuestionType = selectedType,
            questionTypes = types,
            strategies = allItems,
            filteredStrategies = filtered,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StrategyListUiState(skill = skill, isLoading = true),
    )

    init {
        viewModelScope.launch {
            strategyRepository.refreshStrategies()
        }
    }

    fun onQuestionTypeSelected(type: String?) {
        _selectedQuestionType.value = type
    }
}
