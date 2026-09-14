package com.trungld.studyforielts.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trungld.studyforielts.data.preferences.LearnerProfilePreferences
import com.trungld.studyforielts.domain.model.CefrLevel
import com.trungld.studyforielts.domain.model.FocusSkill
import com.trungld.studyforielts.domain.model.LearnerProfile
import com.trungld.studyforielts.domain.model.TargetBand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val preferences: LearnerProfilePreferences) : ViewModel() {
    val profile: StateFlow<LearnerProfile> = preferences.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnerProfile())
    fun save(profile: LearnerProfile) = viewModelScope.launch { preferences.save(profile.copy(onboardingCompleted = true, updatedAt = System.currentTimeMillis())) }
}
