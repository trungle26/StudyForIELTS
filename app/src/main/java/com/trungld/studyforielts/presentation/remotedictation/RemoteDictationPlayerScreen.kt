package com.trungld.studyforielts.presentation.remotedictation

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.trungld.studyforielts.data.local.entity.RemoteDictationSentenceEntity
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.WordComparison
import com.trungld.studyforielts.domain.model.WordComparisonStatus
import com.trungld.studyforielts.presentation.dictation.DictationAudioUiState
import com.trungld.studyforielts.presentation.dictation.DictationStep
import com.trungld.studyforielts.ui.theme.AppTheme
import com.trungld.studyforielts.ui.theme.Dimens

@Composable
fun RemoteDictationPlayerRoute(
    uiState: RemoteDictationPlayerUiState,
    onDraftChanged: (String) -> Unit,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemoteDictationPlayerScreen(
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
fun RemoteDictationPlayerScreen(
    uiState: RemoteDictationPlayerUiState,
    onDraftChanged: (String) -> Unit,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val handlePrimaryAction = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onPrimaryAction()
    }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ContentPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header & Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.lesson?.title.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Text(
                                            text = uiState.lesson?.level.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                    Text(
                                        text = if (uiState.step == DictationStep.COMPLETED) {
                                            stringResource(R.string.dictation_completed)
                                        } else {
                                            "Sentence ${currentIndex + 1} of $totalSentences"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Accuracy badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = "${uiState.progressPercentage.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (uiState.progressPercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Audio Player Console
                ModernAudioPlayerConsole(
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
                        if (uiState.step == DictationStep.REVIEWING) {
                            FeedbackSection(
                                step = uiState.step,
                                feedback = uiState.feedback,
                                sentence = uiState.currentSentence,
                            )

                            Button(
                                onClick = handlePrimaryAction,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.dictation_continue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            ModernInputSection(
                                draft = uiState.currentDraft,
                                step = uiState.step,
                                onDraftChanged = onDraftChanged,
                                onPrimaryAction = handlePrimaryAction,
                            )
                        } else {
                            ModernInputSection(
                                draft = uiState.currentDraft,
                                step = uiState.step,
                                onDraftChanged = onDraftChanged,
                                onPrimaryAction = handlePrimaryAction,
                            )

                            FeedbackSection(
                                step = uiState.step,
                                feedback = uiState.feedback,
                                sentence = uiState.currentSentence,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ModernAudioPlayerConsole(
    audioState: DictationAudioUiState,
    step: DictationStep,
    onTogglePlayback: () -> Unit,
    onReplay: () -> Unit,
    onNextSentence: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Loop info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Loop Interval",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = "${formatMillis(audioState.segmentStartMs)} - ${formatMillis(audioState.segmentEndMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Player control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Replay
                IconButton(
                    onClick = onReplay,
                    enabled = audioState.isAvailable && audioState.isPrepared && step != DictationStep.COMPLETED,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay segment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Play / Pause Hero Button
                IconButton(
                    onClick = onTogglePlayback,
                    enabled = audioState.isAvailable && audioState.isPrepared && step != DictationStep.COMPLETED,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }

                // Skip Next
                IconButton(
                    onClick = onNextSentence,
                    enabled = step != DictationStep.COMPLETED,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip to next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            if (audioState.errorMessage != null) {
                Text(
                    text = audioState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ModernInputSection(
    draft: String,
    step: DictationStep,
    onDraftChanged: (String) -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (step == DictationStep.REVIEWING) "Your Submission" else "Your Transcription",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (step == DictationStep.REVIEWING) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "Reviewed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            enabled = step != DictationStep.COMPLETED && step != DictationStep.REVIEWING,
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onPrimaryAction() }),
            placeholder = {
                Text("Listen carefully and type the words you hear...")
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        )

        if (step != DictationStep.REVIEWING) {
            Button(
                onClick = onPrimaryAction,
                enabled = step == DictationStep.INPUTTING,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dictation_check),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    step: DictationStep,
    feedback: CheckResult?,
    sentence: RemoteDictationSentenceEntity,
) {
    val sectionTitle = if (step == DictationStep.REVIEWING) {
        stringResource(R.string.dictation_review_title)
    } else {
        stringResource(R.string.dictation_reference_title)
    }
    val hasFeedback = feedback != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        if (!hasFeedback) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Listen attentively to the audio loop and transcribe accurately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.dictation_expected),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = buildExpectedAnnotatedString(
                        comparisons = feedback.wordComparisons,
                        neutralColor = MaterialTheme.colorScheme.onSurface,
                        wrongColor = AppTheme.colors.wrongAmber,
                        missingColor = AppTheme.colors.missingRed,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                )

                Text(
                    text = stringResource(R.string.dictation_actual),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildActualAnnotatedString(
                        comparisons = feedback.wordComparisons,
                        neutralColor = MaterialTheme.colorScheme.onSurface,
                        wrongColor = AppTheme.colors.wrongAmber,
                        extraColor = AppTheme.colors.extraBlue,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
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
                label = "Missing: ${missingWords.joinToString()}",
                background = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        if (wrongWords.isNotEmpty()) {
            FeedbackChip(
                label = "Incorrect: ${wrongWords.joinToString()}",
                background = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (extraWords.isNotEmpty()) {
            FeedbackChip(
                label = "Extra: ${extraWords.joinToString()}",
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(R.string.dictation_complete_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.dictation_complete_summary,
                    completedCount,
                    totalSentences,
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onResetLesson,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.dictation_restart))
            }
        }
    }
}

@Composable
private fun EmptyLessonSection() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = "No sentences available in this lesson.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
    }
}

private fun buildExpectedAnnotatedString(
    comparisons: List<WordComparison>,
    neutralColor: Color,
    wrongColor: Color,
    missingColor: Color,
) = buildAnnotatedString {
    comparisons.forEachIndexed { index, comparison ->
        val color = when (comparison.status) {
            WordComparisonStatus.CORRECT -> neutralColor
            WordComparisonStatus.WRONG -> wrongColor
            WordComparisonStatus.MISSING -> missingColor
            WordComparisonStatus.EXTRA -> null
        }

        if (color != null && comparison.expectedWord != null) {
            withStyle(
                SpanStyle(
                    color = color,
                    fontWeight = if (comparison.status == WordComparisonStatus.CORRECT) FontWeight.Normal else FontWeight.Bold,
                ),
            ) {
                append(comparison.expectedWord)
            }
            if (index < comparisons.lastIndex) {
                append(" ")
            }
        }
    }
}

private fun buildActualAnnotatedString(
    comparisons: List<WordComparison>,
    neutralColor: Color,
    wrongColor: Color,
    extraColor: Color,
) = buildAnnotatedString {
    comparisons.forEachIndexed { index, comparison ->
        val color = when (comparison.status) {
            WordComparisonStatus.CORRECT -> neutralColor
            WordComparisonStatus.WRONG -> wrongColor
            WordComparisonStatus.EXTRA -> extraColor
            WordComparisonStatus.MISSING -> null
        }

        if (color != null && comparison.actualWord != null) {
            withStyle(
                SpanStyle(
                    color = color,
                    fontWeight = if (comparison.status == WordComparisonStatus.CORRECT) FontWeight.Normal else FontWeight.Bold,
                ),
            ) {
                append(comparison.actualWord)
            }
            if (index < comparisons.lastIndex) {
                append(" ")
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
