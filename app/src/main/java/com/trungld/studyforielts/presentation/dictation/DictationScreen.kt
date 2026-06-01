package com.trungld.studyforielts.presentation.dictation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trungld.studyforielts.R
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.WordComparison
import com.trungld.studyforielts.domain.model.WordComparisonStatus

@Composable
fun DictationRoute(
    uiState: DictationUiState,
    onDraftChanged: (String) -> Unit,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DictationScreen(
        uiState = uiState,
        onDraftChanged = onDraftChanged,
        onTogglePlayback = onTogglePlayback,
        onReplay = onReplay,
        onPrimaryAction = onPrimaryAction,
        onNextSentence = onNextSentence,
        onResetLesson = onResetLesson,
        modifier = modifier,
    )
}

@Composable
fun DictationScreen(
    uiState: DictationUiState,
    onDraftChanged: (String) -> Unit,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val totalSentences = uiState.sentences.size
        val currentIndex = uiState.currentSentence?.orderIndex ?: totalSentences
        val completedCount = uiState.sentenceProgresses.values.count { it.isCorrect }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = uiState.lesson?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.dictation_level,
                            uiState.lesson?.level.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (uiState.step == DictationStep.COMPLETED) {
                            stringResource(R.string.dictation_completed)
                        } else {
                            stringResource(
                                R.string.dictation_sentence_index,
                                currentIndex + 1,
                                totalSentences,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { (uiState.progressPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.dictation_metric_done),
                        value = completedCount.toString(),
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.dictation_metric_progress),
                        value = "${uiState.progressPercentage.toInt()}%",
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.dictation_metric_player),
                        value = formatMillis(uiState.audioState.currentPositionMs),
                    )
                }
            }

            MediaControlSection(
                audioState = uiState.audioState,
                step = uiState.step,
                onTogglePlayback = onTogglePlayback,
                onReplay = onReplay,
                onNextSentence = onNextSentence,
            )

            when {
                uiState.step == DictationStep.COMPLETED -> {
                    CompletionSection(
                        completedCount = completedCount,
                        totalSentences = totalSentences,
                        onResetLesson = onResetLesson,
                    )
                }

                uiState.currentSentence == null -> {
                    EmptyLessonSection()
                }

                else -> {
                    InputSection(
                        draft = uiState.currentDraft,
                        step = uiState.step,
                        onDraftChanged = onDraftChanged,
                        onPrimaryAction = onPrimaryAction,
                    )
                    FeedbackSection(
                        step = uiState.step,
                        feedback = uiState.feedback,
                        sentence = uiState.currentSentence,
                        currentPlaybackPositionMs = uiState.audioState.currentPositionMs,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MediaControlSection(
    audioState: DictationAudioUiState,
    step: DictationStep,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onNextSentence: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.dictation_playback_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = onTogglePlayback,
                enabled = audioState.isAvailable && audioState.isPrepared && step != DictationStep.COMPLETED,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
            }
            FilledTonalButton(
                onClick = onReplay,
                enabled = audioState.isAvailable && audioState.isPrepared && step != DictationStep.COMPLETED,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                )
            }
            FilledTonalButton(
                onClick = onNextSentence,
                enabled = step != DictationStep.COMPLETED,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                )
            }
        }

        when {
            audioState.errorMessage != null -> {
                Text(
                    text = audioState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            !audioState.isAvailable -> {
                Text(
                    text = stringResource(R.string.dictation_audio_missing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            else -> {
                Text(
                    text = stringResource(
                        R.string.dictation_looping,
                        formatMillis(audioState.segmentStartMs),
                        formatMillis(audioState.segmentEndMs),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun InputSection(
    draft: String,
    step: DictationStep,
    onDraftChanged: (String) -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.dictation_input_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            enabled = step != DictationStep.COMPLETED,
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onPrimaryAction() }),
            placeholder = {
                Text(stringResource(R.string.dictation_input_placeholder))
            },
        )
        Button(
            onClick = onPrimaryAction,
            enabled = step == DictationStep.INPUTTING || step == DictationStep.REVIEWING,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (step == DictationStep.REVIEWING) Icons.Default.SkipNext else Icons.Default.CheckCircle,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                if (step == DictationStep.REVIEWING) {
                    stringResource(R.string.dictation_continue)
                } else {
                    stringResource(R.string.dictation_check)
                },
            )
        }
    }
}

@Composable
private fun FeedbackSection(
    step: DictationStep,
    feedback: CheckResult?,
    sentence: SentenceEntity,
    currentPlaybackPositionMs: Long,
) {
    val sectionTitle = if (step == DictationStep.REVIEWING) {
        stringResource(R.string.dictation_review_title)
    } else {
        stringResource(R.string.dictation_reference_title)
    }
    val hasFeedback = feedback != null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (!hasFeedback) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.dictation_current_segment,
                            formatMillis(sentence.startTime),
                            formatMillis(sentence.endTime),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.dictation_current_playback,
                            formatMillis(currentPlaybackPositionMs),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.dictation_expected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildExpectedAnnotatedString(
                        comparisons = feedback.wordComparisons,
                        neutralColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = stringResource(R.string.dictation_actual),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildActualAnnotatedString(
                        comparisons = feedback.wordComparisons,
                        neutralColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )

                ErrorSummaryRow(
                    missingWords = feedback.missingWords,
                    wrongWords = feedback.wrongWords,
                    extraWords = feedback.extraWords,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ErrorSummaryRow(
    missingWords: List<String>,
    wrongWords: List<String>,
    extraWords: List<String>,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (missingWords.isNotEmpty()) {
            FeedbackChip(
                label = stringResource(
                    R.string.dictation_missing_words,
                    missingWords.joinToString(),
                ),
                background = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        if (wrongWords.isNotEmpty()) {
            FeedbackChip(
                label = stringResource(
                    R.string.dictation_wrong_words,
                    wrongWords.joinToString(),
                ),
                background = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (extraWords.isNotEmpty()) {
            FeedbackChip(
                label = stringResource(
                    R.string.dictation_extra_words,
                    extraWords.joinToString(),
                ),
                background = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun FeedbackChip(
    label: String,
    background: Color,
    content: Color,
) {
    Box(
        modifier = Modifier
            .background(background, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CompletionSection(
    completedCount: Int,
    totalSentences: Int,
    onResetLesson: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.dictation_complete_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.dictation_complete_summary,
                    completedCount,
                    totalSentences,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onResetLesson,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.dictation_restart))
                }
            }
        }
    }
}

@Composable
private fun EmptyLessonSection() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = stringResource(R.string.dictation_empty_lesson),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildExpectedAnnotatedString(
    comparisons: List<WordComparison>,
    neutralColor: Color,
) = buildAnnotatedString {
    comparisons.forEachIndexed { index, comparison ->
        if (index > 0) append(" ")
        val color = when (comparison.status) {
            WordComparisonStatus.CORRECT -> Color.Unspecified
            WordComparisonStatus.WRONG -> Color(0xFFD97706)
            WordComparisonStatus.MISSING -> Color(0xFFB3261E)
            WordComparisonStatus.EXTRA -> neutralColor
        }
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) {
            append(comparison.expectedWord ?: "[]")
        }
    }
}

private fun buildActualAnnotatedString(
    comparisons: List<WordComparison>,
    neutralColor: Color,
) = buildAnnotatedString {
    comparisons.forEachIndexed { index, comparison ->
        if (index > 0) append(" ")
        val color = when (comparison.status) {
            WordComparisonStatus.CORRECT -> Color.Unspecified
            WordComparisonStatus.WRONG -> Color(0xFFD97706)
            WordComparisonStatus.MISSING -> neutralColor
            WordComparisonStatus.EXTRA -> Color(0xFF1D4ED8)
        }
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) {
            append(comparison.actualWord ?: "[]")
        }
    }
}

private fun formatMillis(value: Long): String {
    val totalSeconds = (value / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
