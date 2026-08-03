package com.trungld.studyforielts.presentation.youtube

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trungld.studyforielts.R
import com.trungld.studyforielts.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubePreviewScreen(
    uiState: YoutubePreviewUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onStartDictationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.youtube_preview_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.ContentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ContentPadding, vertical = Dimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ContentPaddingLarge - Dimens.SpacingXs),
        ) {
            if (uiState.isLoadingVideo && uiState.video == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.ContentPaddingLarge * 2),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val video = uiState.video
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.SurfaceAlpha),
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.ContentPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ContentPadding - Dimens.SpacingXs),
                ) {
                    AsyncImage(
                        model = video?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = video?.title?.ifBlank { uiState.videoId } ?: uiState.videoId,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.youtube_preview_video_id, uiState.videoId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.ContentPadding + 2.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.youtube_preview_transcript),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            uiState.isLoadingTranscript -> stringResource(R.string.youtube_preview_transcript_loading)
                            uiState.canStartDictation -> stringResource(
                                R.string.youtube_preview_transcript_ready,
                                uiState.lesson?.sentences?.size.orZero(),
                            )
                            uiState.errorMessage != null -> uiState.errorMessage
                            else -> stringResource(R.string.youtube_preview_transcript_waiting)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.errorMessage == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    if (uiState.isLoadingTranscript) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.errorMessage != null) {
                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.youtube_preview_retry))
                }
            }

            Button(
                onClick = { onStartDictationClick(uiState.videoId) },
                enabled = uiState.canStartDictation,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.youtube_preview_start_dictation))
            }
        }
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0
