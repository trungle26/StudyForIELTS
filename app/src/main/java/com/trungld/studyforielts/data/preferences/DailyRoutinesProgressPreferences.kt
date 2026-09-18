package com.trungld.studyforielts.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trungld.studyforielts.domain.model.DailyRoutinesActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dailyRoutinesStore by preferencesDataStore("daily_routines_progress")

@Singleton
class DailyRoutinesProgressPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val completed: Flow<Set<DailyRoutinesActivity>> = context.dailyRoutinesStore.data.map { preferences ->
        preferences[COMPLETED].orEmpty().mapNotNull { value ->
            runCatching { DailyRoutinesActivity.valueOf(value) }.getOrNull()
        }.toSet()
    }

    suspend fun markCompleted(activity: DailyRoutinesActivity) {
        context.dailyRoutinesStore.edit { preferences ->
            preferences[COMPLETED] = preferences[COMPLETED].orEmpty() + activity.name
        }
    }

    private companion object {
        val COMPLETED = stringSetPreferencesKey("completed_activities")
    }
}
