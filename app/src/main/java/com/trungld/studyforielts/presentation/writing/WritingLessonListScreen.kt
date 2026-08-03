package com.trungld.studyforielts.presentation.writing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.trungld.studyforielts.R
import com.trungld.studyforielts.data.remote.model.WritingLessonDto
import com.trungld.studyforielts.ui.theme.Dimens

/**
 * Paginated list of published writing lessons.
 *
 * One [WritingLessonListScreen] is mounted per task type; the [taskTypeLabel]
 * is rendered in the top app bar so the user can tell Task 1 / Task 2 apart.
 *
 * Tap a lesson to navigate to the practice screen with the lesson id; the
 * caller wires the [onLessonClick] to `navController.navigate(...)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingLessonListScreen(
    uiState: WritingLessonListUiState,
    taskTypeLabel: String,
    showChartThumbnails: Boolean,
    onBackClick: () -> Unit,
    onLessonClick: (WritingLessonDto) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Trigger pagination when the user scrolls within 4 items of the bottom.
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMore, uiState) {
        if (shouldLoadMore && uiState is WritingLessonListUiState.Loaded && !uiState.loadingMore) {
            onLoadMore()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(taskTypeLabel) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is WritingLessonListUiState.Loading -> CenteredLoading(padding)
            is WritingLessonListUiState.Error -> CenteredError(state.message, onRetry, padding)
            is WritingLessonListUiState.Loaded -> {
                if (state.items.isEmpty()) {
                    EmptyState(padding)
                    return@Scaffold
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Dimens.ContentPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Dimens.SpacingSm + Dimens.SpacingXs),
                ) {
                    items(state.items, key = { it.id }) { lesson ->
                        LessonRow(
                            lesson = lesson,
                            showChartThumbnails = showChartThumbnails,
                            onClick = { onLessonClick(lesson) },
                        )
                    }
                    if (state.loadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.ContentPadding),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(Dimens.SmallSpinnerSize)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: WritingLessonDto,
    showChartThumbnails: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingSm + Dimens.SpacingXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showChartThumbnails) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(Dimens.IconTileSize),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(Dimens.SpacingSm + Dimens.SpacingXs))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.taskPrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    lesson.difficulty?.let { difficulty ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(difficulty.replaceFirstChar { it.uppercase() }) },
                        )
                        Spacer(modifier = Modifier.size(Dimens.SpacingSm))
                    }
                    Text(
                        text = stringResource(R.string.writing_lesson_list_tips_count, lesson.tips.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredLoading(padding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator() }
}

@Composable
private fun CenteredError(
    message: String,
    onRetry: () -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(Dimens.ContentPaddingLarge),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm + Dimens.SpacingXs))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.writing_lesson_list_retry)) }
        }
    }
}

@Composable
private fun EmptyState(padding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(Dimens.ContentPaddingLarge),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.writing_lesson_list_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
