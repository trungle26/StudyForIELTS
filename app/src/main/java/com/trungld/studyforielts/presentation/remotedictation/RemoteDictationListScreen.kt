package com.trungld.studyforielts.presentation.remotedictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trungld.studyforielts.domain.model.RemoteDictationLesson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDictationListScreen(
    uiState: RemoteDictationListUiState,
    onBackClick: () -> Unit,
    onLessonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dictation ${uiState.level}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.lessons, key = { it.id }) { lesson ->
                RemoteDictationLessonItem(lesson, onClick = { onLessonClick(lesson.id) })
            }
        }
    }
}

@Composable
private fun RemoteDictationLessonItem(lesson: RemoteDictationLesson, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            Text("${lesson.level} · ${lesson.source}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
