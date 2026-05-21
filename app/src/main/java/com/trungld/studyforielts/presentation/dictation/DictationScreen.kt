package com.trungld.studyforielts.presentation.dictation

import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trungld.studyforielts.data.local.entity.SentenceEntity
import com.trungld.studyforielts.domain.model.CheckResult
import com.trungld.studyforielts.domain.model.WordComparison
import com.trungld.studyforielts.domain.model.WordComparisonStatus
import kotlinx.coroutines.delay

@Composable
fun DictationRoute(
    uiState: DictationUiState,
    onDraftChanged: (String) -> Unit,
    onPlaybackPositionChanged: (Long) -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioController = rememberDictationAudioController(
        audioUrl = uiState.lesson?.audioUrl.orEmpty(),
        currentSentence = uiState.currentSentence,
        resumePositionMs = uiState.progress?.lastPlaybackPositionMs ?: 0L,
        shouldAutoPlay = uiState.step == DictationStep.INPUTTING,
        onPlaybackPositionChanged = onPlaybackPositionChanged,
    )

    DictationScreen(
        uiState = uiState,
        audioController = audioController,
        onDraftChanged = onDraftChanged,
        onPrimaryAction = onPrimaryAction,
        onNextSentence = onNextSentence,
        onResetLesson = onResetLesson,
        modifier = modifier,
    )
}

@Composable
fun DictationScreen(
    uiState: DictationUiState,
    audioController: DictationAudioControllerState,
    onDraftChanged: (String) -> Unit,
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
                        text = "Level ${uiState.lesson?.level.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (uiState.step == DictationStep.COMPLETED) {
                            "Completed"
                        } else {
                            "Sentence ${currentIndex + 1} / $totalSentences"
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryMetric(label = "Done", value = completedCount.toString())
                    SummaryMetric(label = "Progress", value = "${uiState.progressPercentage.toInt()}%")
                    SummaryMetric(label = "Resume", value = formatMillis(uiState.progress?.lastPlaybackPositionMs ?: 0L))
                }
            }

            MediaControlSection(
                audioController = audioController,
                step = uiState.step,
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
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    audioController: DictationAudioControllerState,
    step: DictationStep,
    onNextSentence: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Playback",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = audioController::togglePlayback,
                enabled = audioController.isAvailable && audioController.isPrepared && step != DictationStep.COMPLETED,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (audioController.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (audioController.isPlaying) "Pause" else "Play")
            }
            FilledTonalButton(
                onClick = audioController::replayLastThreeSeconds,
                enabled = audioController.isAvailable && audioController.isPrepared && step != DictationStep.COMPLETED,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Replay 3s")
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
                Spacer(modifier = Modifier.size(8.dp))
                Text("Next")
            }
        }

        when {
            audioController.errorMessage != null -> {
                Text(
                    text = audioController.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            !audioController.isAvailable -> {
                Text(
                    text = "Audio source is not configured for this lesson yet. UI and dictation flow are active, playback is disabled.",
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
            text = "Type what you hear",
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
                Text("Type the sentence here")
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
            Text(if (step == DictationStep.REVIEWING) "Continue" else "Check")
        }
    }
}

@Composable
private fun FeedbackSection(
    step: DictationStep,
    feedback: CheckResult?,
    sentence: SentenceEntity,
) {
    val sectionTitle = if (step == DictationStep.REVIEWING) "Review" else "Reference"
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
                Text(
                    text = "Current segment: ${formatMillis(sentence.startTime)} - ${formatMillis(sentence.endTime)}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    text = "Expected",
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
                    text = "Your answer",
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
                label = "Missing: ${missingWords.joinToString()}",
                background = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        if (wrongWords.isNotEmpty()) {
            FeedbackChip(
                label = "Wrong: ${wrongWords.joinToString()}",
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
                text = "Lesson completed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "You answered $completedCount of $totalSentences sentences correctly.",
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
                    Text("Restart lesson")
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
            text = "No sentence data is available for this lesson.",
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

@Stable
class DictationAudioControllerState internal constructor(
    val isAvailable: Boolean,
    val isPrepared: Boolean,
    val isPlaying: Boolean,
    val errorMessage: String?,
    private val onTogglePlayback: () -> Unit,
    private val onReplayLastThreeSeconds: () -> Unit,
) {
    fun togglePlayback() = onTogglePlayback()

    fun replayLastThreeSeconds() = onReplayLastThreeSeconds()
}

@Composable
private fun rememberDictationAudioController(
    audioUrl: String,
    currentSentence: SentenceEntity?,
    resumePositionMs: Long,
    shouldAutoPlay: Boolean,
    onPlaybackPositionChanged: (Long) -> Unit,
): DictationAudioControllerState {
    var mediaPlayer by remember(audioUrl) { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember(audioUrl) { mutableStateOf(false) }
    var isPlaying by remember(audioUrl) { mutableStateOf(false) }
    var errorMessage by remember(audioUrl) { mutableStateOf<String?>(null) }
    var currentPositionMs by remember(audioUrl) { mutableLongStateOf(0L) }
    var lastPersistedPositionMs by remember(audioUrl) { mutableLongStateOf(0L) }

    val hasAudio = audioUrl.isNotBlank()

    DisposableEffect(audioUrl) {
        if (!hasAudio) {
            onDispose { }
        } else {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setOnPreparedListener {
                isPrepared = true
                errorMessage = null
            }
            player.setOnCompletionListener {
                isPlaying = false
            }
            player.setOnErrorListener { _, _, _ ->
                errorMessage = "Playback failed for this lesson audio."
                isPlaying = false
                true
            }

            runCatching {
                player.setDataSource(audioUrl)
                player.prepareAsync()
            }.onFailure {
                errorMessage = "Audio source could not be opened."
            }

            onDispose {
                runCatching {
                    player.stop()
                }
                player.release()
                mediaPlayer = null
                isPrepared = false
                isPlaying = false
            }
        }
    }

    LaunchedEffect(currentSentence?.id, isPrepared, shouldAutoPlay) {
        val player = mediaPlayer ?: return@LaunchedEffect
        val sentence = currentSentence ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect

        val targetPosition = when {
            resumePositionMs in sentence.startTime..sentence.endTime -> resumePositionMs
            else -> sentence.startTime
        }

        runCatching {
            player.seekTo(targetPosition.toInt())
            currentPositionMs = targetPosition
            if (shouldAutoPlay) {
                player.start()
                isPlaying = true
            } else {
                player.pause()
                isPlaying = false
            }
        }
    }

    LaunchedEffect(mediaPlayer, isPrepared, currentSentence?.id, isPlaying) {
        val player = mediaPlayer ?: return@LaunchedEffect
        val sentence = currentSentence ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect

        while (isPlaying) {
            val position = player.currentPosition.toLong()
            currentPositionMs = position
            if (position >= sentence.endTime) {
                player.seekTo(sentence.startTime.toInt())
                player.start()
                currentPositionMs = sentence.startTime
            }
            if (kotlin.math.abs(position - lastPersistedPositionMs) >= 1_000L) {
                lastPersistedPositionMs = position
                onPlaybackPositionChanged(position)
            }
            delay(200)
            isPlaying = player.isPlaying
        }
    }

    return remember(hasAudio, isPrepared, isPlaying, errorMessage, currentSentence?.id, resumePositionMs) {
        DictationAudioControllerState(
            isAvailable = hasAudio,
            isPrepared = isPrepared,
            isPlaying = isPlaying,
            errorMessage = errorMessage,
            onTogglePlayback = {
                val player = mediaPlayer
                val sentence = currentSentence
                if (player != null && sentence != null && isPrepared) {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                        onPlaybackPositionChanged(player.currentPosition.toLong())
                    } else {
                        val position = player.currentPosition.toLong()
                        val safePosition = position.coerceIn(sentence.startTime, sentence.endTime)
                        player.seekTo(safePosition.toInt())
                        player.start()
                        isPlaying = true
                    }
                }
            },
            onReplayLastThreeSeconds = {
                val player = mediaPlayer
                val sentence = currentSentence
                if (player != null && sentence != null && isPrepared) {
                    val rewindTarget = (currentPositionMs - 3_000L).coerceAtLeast(sentence.startTime)
                    player.seekTo(rewindTarget.toInt())
                    currentPositionMs = rewindTarget
                    if (!player.isPlaying) {
                        player.start()
                        isPlaying = true
                    }
                }
            },
        )
    }
}

private fun formatMillis(value: Long): String {
    val totalSeconds = (value / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
