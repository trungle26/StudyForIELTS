package com.trungld.studyforielts.presentation.youtube

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.trungld.studyforielts.R
import com.trungld.studyforielts.domain.model.YoutubeSentence
import com.trungld.studyforielts.presentation.dictation.DictationStep
import com.trungld.studyforielts.ui.theme.AeroButton
import com.trungld.studyforielts.ui.theme.AeroButtonStyle
import com.trungld.studyforielts.ui.theme.AeroCard
import com.trungld.studyforielts.ui.theme.Dimens
import kotlinx.coroutines.flow.SharedFlow

// Responsive breakpoint: when the available width is at least this value,
// show a two-pane layout (player + progress on the left, controls on the right).
private val ExpandedBreakpoint = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeDictationScreen(
    uiState: YoutubeDictationUiState,
    playerCommands: SharedFlow<YoutubePlayerCommand>,
    onPlayerReady: () -> Unit,
    onCurrentSecond: (Float) -> Unit,
    onDraftChanged: (String) -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetSession: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.youtube_dictation_title)) },
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimens.ContentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                YoutubeDictationContent(
                    uiState = uiState,
                    playerCommands = playerCommands,
                    onPlayerReady = onPlayerReady,
                    onCurrentSecond = onCurrentSecond,
                    onDraftChanged = onDraftChanged,
                    onReplay = onReplay,
                    onPrimaryAction = onPrimaryAction,
                    onNextSentence = onNextSentence,
                    onResetSession = onResetSession,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun YoutubeDictationContent(
    uiState: YoutubeDictationUiState,
    playerCommands: SharedFlow<YoutubePlayerCommand>,
    onPlayerReady: () -> Unit,
    onCurrentSecond: (Float) -> Unit,
    onDraftChanged: (String) -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompleted = uiState.step == DictationStep.COMPLETED

    BoxWithConstraints(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        val isExpanded = maxWidth >= ExpandedBreakpoint

        if (isExpanded) {
            // Two-pane: player on the left, dictation controls on the right.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.ContentPadding, vertical = Dimens.ContentPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ContentPadding),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ContentPaddingLarge - Dimens.SpacingXs),
                ) {
                    YoutubePlayerSection(
                        videoId = uiState.videoId,
                        playerCommands = playerCommands,
                        onPlayerReady = onPlayerReady,
                        onCurrentSecond = onCurrentSecond,
                    )
                    ProgressHeader(uiState = uiState)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ContentPaddingLarge - Dimens.SpacingXs),
                ) {
                    DictationInteractionSection(
                        uiState = uiState,
                        isCompleted = isCompleted,
                        onDraftChanged = onDraftChanged,
                        onReplay = onReplay,
                        onPrimaryAction = onPrimaryAction,
                        onNextSentence = onNextSentence,
                        onResetSession = onResetSession,
                    )
                }
            }
        } else {
            // Compact (phone / narrow): single scrollable column, capped width.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = Dimens.ContentMaxWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.ContentPadding, vertical = Dimens.ContentPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ContentPaddingLarge - Dimens.SpacingXs),
                ) {
                    YoutubePlayerSection(
                        videoId = uiState.videoId,
                        playerCommands = playerCommands,
                        onPlayerReady = onPlayerReady,
                        onCurrentSecond = onCurrentSecond,
                    )
                    ProgressHeader(uiState = uiState)
                    DictationInteractionSection(
                        uiState = uiState,
                        isCompleted = isCompleted,
                        onDraftChanged = onDraftChanged,
                        onReplay = onReplay,
                        onPrimaryAction = onPrimaryAction,
                        onNextSentence = onNextSentence,
                        onResetSession = onResetSession,
                    )
                }
            }
        }
    }
}

@Composable
private fun DictationInteractionSection(
    uiState: YoutubeDictationUiState,
    isCompleted: Boolean,
    onDraftChanged: (String) -> Unit,
    onReplay: () -> Unit,
    onPrimaryAction: () -> Unit,
    onNextSentence: () -> Unit,
    onResetSession: () -> Unit,
) {
    if (isCompleted) {
        CompletionCard(
            sentenceCount = uiState.sentences.size,
            onResetSession = onResetSession,
        )
        return
    }

    ControlsRow(
        onReplay = onReplay,
        onNextSentence = onNextSentence,
    )
    InputCard(
        draft = uiState.currentDraft,
        step = uiState.step,
        onDraftChanged = onDraftChanged,
        onPrimaryAction = onPrimaryAction,
    )
    ReferenceCard(
        sentence = uiState.currentSentence,
        currentSecond = uiState.currentSecond,
        step = uiState.step,
        feedbackText = uiState.feedback?.expectedText,
    )
}

@Composable
private fun YoutubePlayerSection(
    videoId: String,
    playerCommands: SharedFlow<YoutubePlayerCommand>,
    onPlayerReady: () -> Unit,
    onCurrentSecond: (Float) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    val currentOnPlayerReady by rememberUpdatedState(onPlayerReady)
    val currentOnCurrentSecond by rememberUpdatedState(onCurrentSecond)

    var playerView by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var activePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = {
            YouTubePlayerView(it).also { view ->
                playerView = view
                lifecycleOwner.lifecycle.addObserver(view)
                view.addYouTubePlayerListener(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            activePlayer = youTubePlayer
                            currentOnPlayerReady()
                        }

                        override fun onCurrentSecond(
                            youTubePlayer: YouTubePlayer,
                            second: Float,
                        ) {
                            currentOnCurrentSecond(second)
                        }
                    },
                )
            }
        },
        update = { view ->
            if (playerView !== view) {
                playerView = view
            }
        },
    )

    LaunchedEffect(playerCommands, activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        playerCommands.collect { command ->
            when (command) {
                is YoutubePlayerCommand.LoadVideo -> player.loadVideo(command.videoId, command.startSeconds)
                is YoutubePlayerCommand.SeekTo -> {
                    player.seekTo(command.seconds)
                    if (command.play) player.play()
                }

                YoutubePlayerCommand.Pause -> player.pause()
                YoutubePlayerCommand.Play -> player.play()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            playerView?.let { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            }
            playerView = null
            activePlayer = null
        }
    }
}

@Composable
private fun ProgressHeader(uiState: YoutubeDictationUiState) {
    AeroCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = uiState.lesson?.video?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (uiState.step == DictationStep.COMPLETED) {
                        stringResource(R.string.youtube_dictation_completed)
                    } else {
                        stringResource(
                            R.string.youtube_dictation_sentence_progress,
                            uiState.currentSentenceIndex + 1,
                            uiState.sentences.size,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AeroCard(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    isGlass = true,
                ) {
                    Text(
                        text = stringResource(R.string.youtube_dictation_percent, uiState.progressPercentage.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (uiState.progressPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ControlsRow(
    onReplay: () -> Unit,
    onNextSentence: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AeroButton(
            onClick = onReplay,
            modifier = Modifier.weight(1f).height(44.dp),
            style = AeroButtonStyle.FROSTED_GLASS,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.youtube_dictation_replay),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AeroButton(
            onClick = onNextSentence,
            modifier = Modifier.weight(1f).height(44.dp),
            style = AeroButtonStyle.FROSTED_GLASS,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.youtube_dictation_skip),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InputCard(
    draft: String,
    step: DictationStep,
    onDraftChanged: (String) -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.youtube_dictation_input_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onPrimaryAction() }),
            placeholder = { Text(stringResource(R.string.youtube_dictation_input_placeholder)) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        )
        AeroButton(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            style = AeroButtonStyle.AERO_BLUE,
        ) {
            Icon(
                imageVector = if (step == DictationStep.REVIEWING) Icons.Default.SkipNext else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (step == DictationStep.REVIEWING) stringResource(R.string.youtube_dictation_continue)
                else stringResource(R.string.youtube_dictation_check),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReferenceCard(
    sentence: YoutubeSentence?,
    currentSecond: Float,
    step: DictationStep,
    feedbackText: String?,
) {
    AeroCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        isGlass = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (step == DictationStep.REVIEWING) stringResource(R.string.youtube_dictation_review)
                else stringResource(R.string.youtube_dictation_loop_window),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = sentence?.let {
                    stringResource(
                        R.string.youtube_dictation_timestamp_range,
                        formatMillis(it.startTimeMs),
                        formatMillis(it.endTimeMs),
                        formatSeconds(currentSecond),
                    )
                }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (step == DictationStep.REVIEWING && feedbackText != null) {
                Text(
                    text = feedbackText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CompletionCard(
    sentenceCount: Int,
    onResetSession: () -> Unit,
) {
    AeroCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.youtube_dictation_session_completed),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.youtube_dictation_session_completed_summary, sentenceCount),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AeroButton(
                onClick = onResetSession,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                style = AeroButtonStyle.AERO_BLUE,
                modifier = Modifier.height(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.youtube_dictation_restart))
            }
        }
    }
}

private fun Context.findLifecycleOwner(): LifecycleOwner {
    return when (this) {
        is LifecycleOwner -> this
        is android.content.ContextWrapper -> baseContext.findLifecycleOwner()
        else -> error("Expected a LifecycleOwner context for YouTubePlayerView.")
    }
}

private fun formatMillis(value: Long): String {
    val totalSeconds = (value / 1_000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatSeconds(value: Float): String {
    return formatMillis((value * 1_000f).toLong())
}
