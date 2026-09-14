package com.trungld.studyforielts.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trungld.studyforielts.domain.model.CefrLevel
import com.trungld.studyforielts.domain.model.FocusSkill
import com.trungld.studyforielts.domain.model.LearnerProfile
import com.trungld.studyforielts.domain.model.TargetBand
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.learnerProfileStore by preferencesDataStore("learner_profile")

@Singleton
class LearnerProfilePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val profile: Flow<LearnerProfile> = context.learnerProfileStore.data.map { p ->
        LearnerProfile(
            onboardingCompleted = p[Keys.COMPLETED] ?: false,
            currentCefrLevel = p[Keys.CEFR]?.let { runCatching { CefrLevel.valueOf(it) }.getOrNull() } ?: CefrLevel.UNKNOWN,
            targetBand = p[Keys.BAND]?.let { value -> TargetBand.entries.firstOrNull { it.value == value } } ?: TargetBand.NONE,
            dailyGoalMinutes = p[Keys.MINUTES] ?: 10,
            focusSkills = p[Keys.SKILLS].orEmpty().split(",").mapNotNull { it.takeIf(String::isNotBlank)?.let { s -> runCatching { FocusSkill.valueOf(s) }.getOrNull() } }.toSet(),
            updatedAt = p[Keys.UPDATED_AT] ?: 0L,
        )
    }

    suspend fun save(profile: LearnerProfile) {
        context.learnerProfileStore.edit { p ->
            p[Keys.COMPLETED] = profile.onboardingCompleted
            p[Keys.CEFR] = profile.currentCefrLevel.name
            p[Keys.BAND] = profile.targetBand.value
            p[Keys.MINUTES] = profile.dailyGoalMinutes
            p[Keys.SKILLS] = profile.focusSkills.joinToString(",") { it.name }
            p[Keys.UPDATED_AT] = profile.updatedAt
        }
    }

    private object Keys {
        val COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CEFR = stringPreferencesKey("current_cefr")
        val BAND = stringPreferencesKey("target_band")
        val MINUTES = androidx.datastore.preferences.core.intPreferencesKey("daily_goal_minutes")
        val SKILLS = stringPreferencesKey("focus_skills")
        val UPDATED_AT = longPreferencesKey("updated_at")
    }
}
