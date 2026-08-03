package com.trungld.studyforielts.presentation.writing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trungld.studyforielts.R
import com.trungld.studyforielts.data.remote.model.WritingEvaluationDto
import com.trungld.studyforielts.ui.theme.Dimens

/**
 * Writing Practice screen.
 *
 * Two modes:
 *   - **Free Task 2** (no `lessonId`): the original flow — editable prompt
 *     card, plain essay input, single submit endpoint.
 *   - **Lesson-driven** (with `lessonId`): the prompt is locked read-only
 *     (sourced from the lesson), the Task 1 chart image is rendered via Coil
 *     if the lesson is a Task 1 lesson, and submit routes to either
 *     `/evaluate/task1/stream` (Task 1) or `/evaluate/stream` (Task 2).
 *
 * Layout (top to bottom, scrollable):
 *   1. Task prompt card (editable in free mode, read-only + lesson info in
 *      lesson mode; Task 1 also shows the chart image)
 *   2. Essay input TextField (multiline) + live word counter
 *   3. Submit button (disabled while empty / submitting)
 *   4. State-dependent area:
 *      - Idle       : nothing
 *      - Submitting : CircularProgressIndicator
 *      - Streaming  : shows running LLM text + spinner
 *      - Success    : Results card (band, feedback, vocab chips, rewrite)
 *      - Error      : error card with a Retry button
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WritingPracticeScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: WritingViewModel = hiltViewModel(),
) {
    val essay by viewModel.essayText.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lesson by viewModel.lesson.collectAsStateWithLifecycle()
    // Derive the image URL from the observed `lesson` state, NOT from
    // `viewModel.lessonImageUrl` — that getter reads `lesson.value` at
    // composition time and won't recompose when the lesson loads.
    val imageUrl = lesson?.takeIf { it.taskType == "task1" }?.let { l ->
        com.trungld.studyforielts.BuildConfig.YOUTUBE_BFF_BASE_URL.trimEnd('/') +
            "/writing/lessons/${l.id}/image"
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val currentLesson = lesson
                    Text(
                        if (currentLesson != null) {
                            if (currentLesson.taskType == "task1")
                                stringResource(R.string.writing_practice_top_bar_task1)
                            else
                                stringResource(R.string.writing_practice_top_bar_task2)
                        } else
                            stringResource(R.string.writing_practice_top_bar_default)
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ContentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

            // 1. Task prompt card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(Dimens.ContentPadding)) {
                    val currentLesson = lesson
                    Text(
                        text = if (currentLesson?.taskType == "task1")
                            stringResource(R.string.writing_practice_prompt_task1_label)
                        else
                            stringResource(R.string.writing_practice_prompt_task2_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = viewModel::onPromptChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        readOnly = viewModel.isPromptLocked,
                    )
                    // Task 1 chart image (Coil). The lesson image lives in
                    // GridFS and is served by the same BFF via
                    // /writing/lessons/{id}/image.
                    if (imageUrl != null) {
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm + Dimens.SpacingXs))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = Dimens.WritingChartMin, max = Dimens.WritingChartMax),
                            )
                        }
                    }
                }
            }

            // 2. Essay input
            OutlinedTextField(
                value = essay,
                onValueChange = viewModel::onEssayChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.WritingEssayMin),
                label = { Text(stringResource(R.string.writing_practice_essay_label)) },
                placeholder = {
                    Text(stringResource(R.string.writing_practice_essay_placeholder, WritingViewModel.MIN_ESSAY_WORDS))
                },
                minLines = 8,
                maxLines = 20,
                enabled = uiState !is WritingUiState.Submitting && uiState !is WritingUiState.Streaming,
            )

            // Word counter
            val wordCount = essay.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            Text(
                text = stringResource(R.string.writing_practice_word_count, wordCount, WritingViewModel.MIN_ESSAY_WORDS),
                style = MaterialTheme.typography.labelSmall,
                color = if (wordCount >= WritingViewModel.MIN_ESSAY_WORDS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.align(Alignment.End),
            )

            // 3. Submit
            Button(
                onClick = viewModel::submit,
                enabled = (uiState == WritingUiState.Idle || uiState is WritingUiState.Error)
                    && essay.isNotBlank()
                    && wordCount >= WritingViewModel.MIN_ESSAY_WORDS,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(stringResource(R.string.writing_practice_submit_button))
            }

            // 4. State-dependent area
            when (val state = uiState) {
                is WritingUiState.Idle -> Unit
                is WritingUiState.Submitting -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.ContentPaddingLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(Dimens.SpacingSm + Dimens.SpacingXs))
                            Text(
                                stringResource(R.string.writing_practice_submitting_text),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                is WritingUiState.Streaming -> StreamingCard(partialText = state.partialText)
                is WritingUiState.Success -> ResultsCard(
                    evaluation = state.evaluation,
                    onTryAgain = viewModel::reset,
                )
                is WritingUiState.Error -> ErrorCard(
                    message = state.message,
                    onRetry = viewModel::submit,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.ContentPadding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultsCard(
    evaluation: WritingEvaluationDto,
    onTryAgain: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Dimens.ContentPadding)) {
            Text(
                text = stringResource(R.string.writing_practice_results_estimated_band),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = "%.1f".format(evaluation.overallBand),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(Dimens.ContentPadding))
            HorizontalDivider()

            Spacer(modifier = Modifier.height(Dimens.SpacingSm + Dimens.SpacingXs))
            SectionLabel(stringResource(R.string.writing_practice_results_coherence))
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = evaluation.coherenceFeedback,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(Dimens.ContentPadding))
            SectionLabel(stringResource(R.string.writing_practice_results_vocab))
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
            ) {
                evaluation.vocabularySuggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { /* purely informational */ },
                        label = { Text(suggestion) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.ContentPadding))
            SectionLabel(stringResource(R.string.writing_practice_results_rewrite))
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = evaluation.simonStyleRewrite,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Dimens.SpacingSm + Dimens.SpacingXs),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.ContentPadding))
            TextButton(
                onClick = onTryAgain,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(stringResource(R.string.writing_practice_results_try_again))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Dimens.ContentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(
                    stringResource(R.string.writing_practice_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            TextButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(stringResource(R.string.writing_practice_error_retry))
            }
        }
    }
}

@Composable
private fun StreamingCard(partialText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Dimens.ContentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.height(Dimens.SpacingMd),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(
                    text = stringResource(R.string.writing_practice_streaming_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = partialText.ifBlank {
                        stringResource(R.string.writing_practice_streaming_waiting)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Dimens.SpacingSm + Dimens.SpacingXs),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}