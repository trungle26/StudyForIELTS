package com.trungld.studyforielts.presentation.remotedictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trungld.studyforielts.presentation.dictation.DictationAudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDictationPlayerScreen(
    uiState: RemoteDictationPlayerUiState,
    onBackClick: () -> Unit,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onNextSentence: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(uiState.title) }) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${uiState.level} · sentence ${uiState.currentIndex + 1}/${uiState.sentences.size}")
            Text(uiState.currentSentence?.text.orEmpty(), style = MaterialTheme.typography.titleLarge)
            AudioControls(uiState.audioState, onTogglePlayback, onReplay, onNextSentence)
        }
    }
}

@Composable
private fun AudioControls(
    audioState: DictationAudioUiState,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onNextSentence: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onTogglePlayback) {
            Icon(if (audioState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
        }
        Button(onClick = onReplay) { Icon(Icons.Filled.Replay, contentDescription = null) }
        Button(onClick = onNextSentence) { Icon(Icons.Filled.SkipNext, contentDescription = null) }
    }
}