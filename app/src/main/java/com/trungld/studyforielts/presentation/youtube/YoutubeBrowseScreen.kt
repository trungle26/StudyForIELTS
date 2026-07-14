package com.trungld.studyforielts.presentation.youtube

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trungld.studyforielts.domain.model.YoutubeVideo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeBrowseScreen(
    uiState: YoutubeBrowseUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onLevelSelected: (String) -> Unit,
    onRefreshFeed: () -> Unit,
    onClearSearch: () -> Unit,
    onVideoClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onWritingPracticeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val visibleVideos = if (uiState.hasSearched) uiState.searchResults else uiState.feedVideos
    val sectionTitle = if (uiState.hasSearched) "Search results" else "${uiState.selectedLevel} curated feed"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("YouTube dictation") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.TopCenter,
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pick a curated video by level, or search YouTube when you want something specific.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LevelChips(
                        selectedLevel = uiState.selectedLevel,
                        enabled = !uiState.isBusy,
                        onLevelSelected = onLevelSelected,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = onQueryChanged,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("IELTS listening practice") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        )
                        Button(
                            onClick = onSearch,
                            enabled = !uiState.isSearching,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Search")
                        }
                    }
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sectionTitle,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (uiState.hasSearched) {
                        TextButton(onClick = onClearSearch) {
                            Text("Feed")
                        }
                    } else {
                        TextButton(
                            onClick = onRefreshFeed,
                            enabled = !uiState.isLoadingFeed,
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onWritingPracticeClick),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Writing Practice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                text = "Get Band 9 feedback on your IELTS essay from an AI tutor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            if (uiState.isSearching || uiState.isLoadingFeed) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (visibleVideos.isEmpty()) {
                item {
                    EmptyYoutubeSection(
                        text = if (uiState.hasSearched) {
                            "No matching videos found."
                        } else {
                            "No curated videos for ${uiState.selectedLevel} yet. Try another level or refresh after adding videos."
                        },
                    )
                }
            } else {
                items(visibleVideos, key = { it.videoId }) { video ->
                    YoutubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video.videoId) },
                    )
                }
            }

            if (!uiState.hasSearched && uiState.savedVideos.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(uiState.savedVideos, key = { "saved-${it.videoId}" }) { video ->
                    YoutubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video.videoId) },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun LevelChips(
    selectedLevel: String,
    enabled: Boolean,
    onLevelSelected: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CEFR_LEVELS, key = { it }) { level ->
            FilterChip(
                selected = level == selectedLevel,
                onClick = { onLevelSelected(level) },
                enabled = enabled,
                label = { Text(level) },
            )
        }
    }
}

@Composable
private fun YoutubeVideoCard(
    video: YoutubeVideo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 132.dp, height = 74.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (video.level != null) {
                    Text(
                        text = listOfNotNull(
                            video.level,
                            video.channelTitle.takeIf { it.isNotBlank() },
                        ).joinToString(" | "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = video.title.ifBlank { video.videoId },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = video.subtitleText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun YoutubeVideo.subtitleText(): String {
    if (isSaved) return "Saved offline"

    val duration = durationSeconds?.let { seconds ->
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        "%d:%02d".format(minutes, remainingSeconds)
    }
    val tagsText = tags.take(2).joinToString(" #", prefix = "#").takeIf { it != "#" }

    return listOfNotNull(duration, tagsText, "Tap to prepare transcript").joinToString(" · ")
}

@Composable
private fun EmptyYoutubeSection(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 5f),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier.padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CEFR_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")
