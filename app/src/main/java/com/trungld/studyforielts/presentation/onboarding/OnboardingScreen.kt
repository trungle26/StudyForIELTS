package com.trungld.studyforielts.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trungld.studyforielts.R
import com.trungld.studyforielts.domain.model.CefrLevel
import com.trungld.studyforielts.domain.model.FocusSkill
import com.trungld.studyforielts.domain.model.LearnerProfile
import com.trungld.studyforielts.domain.model.TargetBand

@Composable
fun OnboardingScreen(initial: LearnerProfile, onSave: (LearnerProfile) -> Unit, onSkip: () -> Unit) {
    var cefr by remember { mutableStateOf(initial.currentCefrLevel) }
    var band by remember { mutableStateOf(initial.targetBand) }
    var minutes by remember { mutableIntStateOf(initial.dailyGoalMinutes) }
    var skills by remember { mutableStateOf(initial.focusSkills) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = 1f, modifier = Modifier.fillMaxWidth())
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.onboarding_cefr_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.onboarding_cefr_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            CefrLevel.entries.forEach { level -> RowChoice(cefrLabel(level), level == cefr) { cefr = level } }
            Text(stringResource(R.string.onboarding_band_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.onboarding_band_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TargetBand.entries.forEach { target -> RowChoice(if (target == TargetBand.NONE) stringResource(R.string.onboarding_none) else target.value, target == band) { band = target } }
            Text(stringResource(R.string.onboarding_minutes_title), style = MaterialTheme.typography.titleLarge)
            listOf(10, 20, 30, 45, 60).forEach { value -> FilterChip(selected = minutes == value, onClick = { minutes = value }, label = { Text(value.toString()) }) }
            Text(stringResource(R.string.onboarding_skills_title), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusSkill.entries.take(3).forEach { skill -> FilterChip(selected = skill in skills, onClick = { skills = if (skill in skills) skills - skill else skills + skill }, label = { Text(skill.name.lowercase().replaceFirstChar(Char::uppercase)) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusSkill.entries.drop(3).forEach { skill -> FilterChip(selected = skill in skills, onClick = { skills = if (skill in skills) skills - skill else skills + skill }, label = { Text(skill.name.lowercase().replaceFirstChar(Char::uppercase)) }) }
            }
            Button(onClick = { onSave(LearnerProfile(true, cefr, band, minutes, skills)) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_save)) }
            OutlinedButton(onClick = onSkip, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_skip)) }
        }
    }
}

@Composable
private fun RowChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected, onClick)
        Text(label, Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun cefrLabel(level: CefrLevel): String = when (level) {
    CefrLevel.UNKNOWN -> stringResource(R.string.onboarding_unknown)
    else -> level.name
}
