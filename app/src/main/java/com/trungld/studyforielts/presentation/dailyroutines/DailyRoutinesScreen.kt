package com.trungld.studyforielts.presentation.dailyroutines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trungld.studyforielts.R
import com.trungld.studyforielts.domain.model.DailyRoutinesActivity
import com.trungld.studyforielts.domain.model.DailyRoutinesPath

@Composable
fun DailyRoutinesScreen(
    completed: Set<DailyRoutinesActivity>,
    onBackClick: () -> Unit,
    onActivityClick: (DailyRoutinesActivity) -> Unit,
) {
    val path = DailyRoutinesPath()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.daily_routines_back)) }
        Text(stringResource(R.string.daily_routines_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.daily_routines_context), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.daily_routines_goal), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.daily_routines_progress, completed.size, path.activities.size))
        if (completed.size == path.activities.size) {
            Text(stringResource(R.string.daily_routines_all_complete), style = MaterialTheme.typography.titleMedium)
        }
        path.activities.forEach { activity ->
            Button(onClick = { onActivityClick(activity) }) {
                Text(stringResource(if (activity in completed) R.string.daily_routines_completed_button else activity.labelRes()))
            }
        }
        Text(stringResource(R.string.daily_routines_completion), style = MaterialTheme.typography.bodySmall)
    }
}

private fun DailyRoutinesActivity.labelRes(): Int = when (this) {
    DailyRoutinesActivity.VOCABULARY -> R.string.daily_routines_vocabulary
    DailyRoutinesActivity.READING -> R.string.daily_routines_reading
    DailyRoutinesActivity.GRAMMAR -> R.string.daily_routines_grammar
    DailyRoutinesActivity.DICTATION -> R.string.daily_routines_dictation
    DailyRoutinesActivity.WRITING -> R.string.daily_routines_writing
    DailyRoutinesActivity.REVIEW -> R.string.daily_routines_review
}
