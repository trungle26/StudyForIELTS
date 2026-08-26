package com.trungld.studyforielts.presentation.strategy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.domain.model.StrategyGuide
import com.trungld.studyforielts.domain.repository.StrategyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StrategyDetailUiState(
    val strategy: StrategyGuide? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class StrategyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strategyRepository: StrategyRepository,
) : ViewModel() {

    companion object {
        const val STRATEGY_ID_ARGUMENT = "strategyId"
    }

    private val strategyId: String = checkNotNull(savedStateHandle[STRATEGY_ID_ARGUMENT])

    private val _uiState = MutableStateFlow(StrategyDetailUiState())
    val uiState: StateFlow<StrategyDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val item = strategyRepository.getStrategyById(strategyId)
            _uiState.value = StrategyDetailUiState(strategy = item, isLoading = false)
        }
    }
}
