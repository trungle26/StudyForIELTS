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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trungld.studyforielts.R
import com.trungld.studyforielts.domain.model.YoutubeVideo
import com.trungld.studyforielts.ui.theme.Dimens

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
    val sectionTitle = if (uiState.hasSearched) {
        stringResource(R.string.youtube_section_search_results)
    } else {
        stringResource(R.string.youtube_section_curated_feed, uiState.selectedLevel)
    }
    val savedOfflineLabel = stringResource(R.string.youtube_subtitle_saved_offline)
    val tapToPrepareLabel = stringResource(R.string.youtube_subtitle_tap_to_prepare)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.youtube_browse_title)) },
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
                .widthIn(max = Dimens.ContentMaxWidth)
                .padding(horizontal = Dimens.ContentPadding, vertical = Dimens.SpacingSm + Dimens.SpacingXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + 2.dp)) {
                    Text(
                        text = stringResource(R.string.youtube_browse_subtitle),
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
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = onQueryChanged,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.youtube_search_placeholder)) },
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
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(stringResource(R.string.youtube_search_action))
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
                            Text(stringResource(R.string.youtube_action_feed))
                        }
                    } else {
                        TextButton(
                            onClick = onRefreshFeed,
                            enabled = !uiState.isLoadingFeed,
                        ) {
                            Text(stringResource(R.string.youtube_action_refresh))
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
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.ContentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.youtube_writing_practice_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                text = stringResource(R.string.youtube_writing_practice_subtitle),
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
                            .padding(vertical = Dimens.ContentPaddingLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (visibleVideos.isEmpty()) {
                item {
                    EmptyYoutubeSection(
                        text = if (uiState.hasSearched) {
                            stringResource(R.string.youtube_empty_search)
                        } else {
                            stringResource(R.string.youtube_empty_feed, uiState.selectedLevel)
                        },
                    )
                }
            } else {
                items(visibleVideos, key = { it.videoId }) { video ->
                    YoutubeVideoCard(
                        video = video,
                        savedOfflineLabel = savedOfflineLabel,
                        tapToPrepareLabel = tapToPrepareLabel,
                        onClick = { onVideoClick(video.videoId) },
                    )
                }
            }

            if (!uiState.hasSearched && uiState.savedVideos.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.youtube_saved_offline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(uiState.savedVideos, key = { "saved-${it.videoId}" }) { video ->
                    YoutubeVideoCard(
                        video = video,
                        savedOfflineLabel = savedOfflineLabel,
                        tapToPrepareLabel = tapToPrepareLabel,
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
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
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
    savedOfflineLabel: String,
    tapToPrepareLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.SurfaceAlpha),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.SpacingSm + Dimens.SpacingXs),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd - 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 132.dp, height = 74.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs + 2.dp),
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
                    text = video.subtitleText(savedOfflineLabel, tapToPrepareLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun YoutubeVideo.subtitleText(savedOfflineLabel: String, tapToPrepareLabel: String): String {
    if (isSaved) return savedOfflineLabel

    val duration = durationSeconds?.let { seconds ->
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        "%d:%02d".format(minutes, remainingSeconds)
    }
    val tagsText = tags.take(2).joinToString(" #", prefix = "#").takeIf { it != "#" }

    return listOfNotNull(duration, tagsText, tapToPrepareLabel).joinToString(" · ")
}

@Composable
private fun EmptyYoutubeSection(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 5f),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.SurfaceAlpha),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier.padding(Dimens.ContentPadding + 2.dp),
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
