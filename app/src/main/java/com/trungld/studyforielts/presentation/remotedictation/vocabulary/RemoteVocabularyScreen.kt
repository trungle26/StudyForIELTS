package com.trungld.studyforielts.presentation.remotedictation.vocabulary

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.trungld.studyforielts.R
import com.trungld.studyforielts.data.local.entity.RemoteVocabularyEntity
import com.trungld.studyforielts.presentation.vocabulary.buildContextLookupUrl
import com.trungld.studyforielts.presentation.vocabulary.buildImageLookupUrl
import com.trungld.studyforielts.ui.theme.AppTheme
import com.trungld.studyforielts.ui.theme.Dimens
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteVocabularyScreen(
    uiState: RemoteVocabularyUiState,
    onBackClick: () -> Unit,
    onMarkLearned: (RemoteVocabularyEntity) -> Unit,
    onRecycleToQueue: (RemoteVocabularyEntity) -> Unit,
    onStartDictationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lookupTarget by remember { mutableStateOf<RemoteLookupTarget?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val contextSheetTitle = stringResource(R.string.vocabulary_sheet_context_title)
    val imageSheetTitle = stringResource(R.string.vocabulary_sheet_images_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.vocabulary_screen_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!uiState.isLoading && uiState.totalCount > 0) {
                            Text(
                                text = "${uiState.learnedCount}/${uiState.totalCount} Mastered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = Dimens.ContentMaxWidth)
                        .padding(horizontal = Dimens.ContentPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RemoteVocabularyProgressBar(uiState = uiState)

                    when {
                        uiState.isEmpty -> EmptyVocabularyState()
                        uiState.isCompleted -> RemoteVocabularyCompletedState(
                            onStartDictation = { onStartDictationClick(uiState.lessonServerId) },
                        )
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                RemoteVocabularyCardStack(
                                    visibleCards = uiState.visibleStack,
                                    onSwipeRight = onMarkLearned,
                                    onSwipeLeft = onRecycleToQueue,
                                    onOpenLookup = { word, mode ->
                                        lookupTarget = RemoteLookupTarget(
                                            title = when (mode) {
                                                RemoteLookupMode.Context -> contextSheetTitle
                                                RemoteLookupMode.Images -> imageSheetTitle
                                            },
                                            url = when (mode) {
                                                RemoteLookupMode.Context -> buildContextLookupUrl(word)
                                                RemoteLookupMode.Images -> buildImageLookupUrl(word)
                                            },
                                        )
                                    },
                                )
                            }

                            Button(
                                onClick = { onStartDictationClick(uiState.lessonServerId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.start_dictation),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (lookupTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { lookupTarget = null },
            sheetState = sheetState,
            dragHandle = null,
        ) {
            RemoteVocabularyLookupBottomSheet(
                target = lookupTarget!!,
                onClose = { lookupTarget = null },
            )
        }
    }
}

@Composable
private fun RemoteVocabularyProgressBar(uiState: RemoteVocabularyUiState) {
    val progress = if (uiState.totalCount > 0) {
        uiState.learnedCount.toFloat() / uiState.totalCount.toFloat()
    } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Vocabulary Deck",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${uiState.remainingCount} words remaining",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyVocabularyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.vocabulary_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.vocabulary_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemoteVocabularyCompletedState(onStartDictation: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.vocabulary_completed_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.vocabulary_completed_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStartDictation,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.start_dictation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RemoteVocabularyCardStack(
    visibleCards: List<RemoteVocabularyEntity>,
    onSwipeRight: (RemoteVocabularyEntity) -> Unit,
    onSwipeLeft: (RemoteVocabularyEntity) -> Unit,
    onOpenLookup: (String, RemoteLookupMode) -> Unit,
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }
    var topSwipeProgress by remember(visibleCards.firstOrNull()?.word) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        visibleCards.asReversed().forEach { vocab ->
            key(vocab.word) {
                val stackIndex = visibleCards.indexOf(vocab)
                val topCard = stackIndex == 0
                val reactiveStackIndex = if (topCard) {
                    0f
                } else {
                    (stackIndex - topSwipeProgress).coerceAtLeast(0f)
                }
                val scale = 1f - (reactiveStackIndex * 0.04f)
                val stackOffsetYPx = with(density) { (reactiveStackIndex * 14).dp.toPx() }
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(durationMillis = if (topSwipeProgress > 0f) 80 else 170),
                    label = "RemoteVocabCardScale",
                )
                val animatedTranslationY by animateFloatAsState(
                    targetValue = stackOffsetYPx,
                    animationSpec = tween(durationMillis = if (topSwipeProgress > 0f) 80 else 170),
                    label = "RemoteVocabCardTranslationY",
                )
                val stackModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        this.translationY = animatedTranslationY
                    }

                if (topCard) {
                    SwipeableRemoteVocabularyCard(
                        vocabulary = vocab,
                        modifier = stackModifier,
                        onSwipeRight = { onSwipeRight(vocab) },
                        onSwipeLeft = { onSwipeLeft(vocab) },
                        onSwipeProgressChanged = { topSwipeProgress = it },
                        swipeThreshold = swipeThreshold,
                        onOpenContext = { onOpenLookup(vocab.word, RemoteLookupMode.Context) },
                        onOpenImages = { onOpenLookup(vocab.word, RemoteLookupMode.Images) },
                    )
                } else {
                    StaticRemoteVocabularyCard(vocab, stackModifier)
                }
            }
        }
    }
}

@Composable
private fun SwipeableRemoteVocabularyCard(
    vocabulary: RemoteVocabularyEntity,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeProgressChanged: (Float) -> Unit,
    swipeThreshold: Float,
    onOpenContext: () -> Unit,
    onOpenImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetX = remember(vocabulary.word) { Animatable(0f) }
    val offsetY = remember(vocabulary.word) { Animatable(0f) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val dismissDistance = with(density) { 520.dp.toPx() }

    RemoteVocabularyCardFrame(
        word = vocabulary.word,
        phonetic = vocabulary.phonetic,
        meaning = vocabulary.meaning,
        exampleSentence = vocabulary.exampleSentence,
        modifier = modifier
            .offset {
                IntOffset(
                    x = offsetX.value.roundToInt(),
                    y = offsetY.value.roundToInt(),
                )
            }
            .graphicsLayer { rotationZ = offsetX.value / 28f }
            .pointerInput(vocabulary.word) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val nextOffsetX = offsetX.value + dragAmount.x
                            offsetX.snapTo(nextOffsetX)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.22f)
                            onSwipeProgressChanged(
                                (nextOffsetX.absoluteValue / swipeThreshold).coerceIn(0f, 1f),
                            )
                        }
                    },
                    onDragEnd = {
                        val right = offsetX.value > swipeThreshold
                        val left = offsetX.value < -swipeThreshold
                        when {
                            right -> coroutineScope.launch {
                                offsetX.animateTo(dismissDistance) {
                                    onSwipeProgressChanged((value.absoluteValue / swipeThreshold).coerceIn(0f, 1f))
                                }
                                onSwipeRight()
                                onSwipeProgressChanged(0f)
                            }
                            left -> coroutineScope.launch {
                                offsetX.animateTo(-dismissDistance) {
                                    onSwipeProgressChanged((value.absoluteValue / swipeThreshold).coerceIn(0f, 1f))
                                }
                                onSwipeLeft()
                                onSwipeProgressChanged(0f)
                            }
                            else -> coroutineScope.launch {
                                offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) {
                                    onSwipeProgressChanged((value.absoluteValue / swipeThreshold).coerceIn(0f, 1f))
                                }
                                offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                                onSwipeProgressChanged(0f)
                            }
                        }
                    },
                )
            },
        swipeOverlay = {
            RemoteSwipeHintOverlay(offsetX.value, swipeThreshold)
        },
        onOpenContext = onOpenContext,
        onOpenImages = onOpenImages,
        onManualSwipeRight = {
            coroutineScope.launch {
                offsetX.animateTo(dismissDistance)
                onSwipeRight()
            }
        },
        onManualSwipeLeft = {
            coroutineScope.launch {
                offsetX.animateTo(-dismissDistance)
                onSwipeLeft()
            }
        },
    )
}

@Composable
private fun StaticRemoteVocabularyCard(vocabulary: RemoteVocabularyEntity, modifier: Modifier) {
    RemoteVocabularyCardFrame(
        word = vocabulary.word,
        phonetic = vocabulary.phonetic,
        meaning = vocabulary.meaning,
        exampleSentence = vocabulary.exampleSentence,
        modifier = modifier,
        swipeOverlay = {},
        onOpenContext = {},
        onOpenImages = {},
        enabled = false,
    )
}

@Composable
private fun RemoteVocabularyCardFrame(
    word: String,
    phonetic: String,
    meaning: String,
    exampleSentence: String,
    modifier: Modifier,
    swipeOverlay: @Composable BoxScope.() -> Unit,
    onOpenContext: () -> Unit,
    onOpenImages: () -> Unit,
    enabled: Boolean = true,
    onManualSwipeRight: () -> Unit = {},
    onManualSwipeLeft: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Word and Details container (tight spacing to fit without scrolling)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (phonetic.isNotBlank()) {
                            Text(
                                text = phonetic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Meaning Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Definition",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp,
                            )
                        }
                    }

                    // Example Sentence Card
                    if (exampleSentence.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Example in IELTS Context",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "\"$exampleSentence\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Actions & Controls
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenContext,
                            modifier = Modifier.weight(1f).height(38.dp),
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.vocabulary_context_in_sentence),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenImages,
                            modifier = Modifier.weight(1f).height(38.dp),
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.vocabulary_images),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    // Direct Tapping Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onManualSwipeLeft,
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.swipeReviewContainer,
                                contentColor = AppTheme.colors.swipeReview,
                            ),
                            modifier = Modifier.weight(1f).height(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.swipe_mark_review),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Button(
                            onClick = onManualSwipeRight,
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.swipeLearnedContainer,
                                contentColor = AppTheme.colors.swipeLearned,
                            ),
                            modifier = Modifier.weight(1f).height(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.swipe_mark_learned),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            swipeOverlay()
        }
    }
}

@Composable
private fun BoxScope.RemoteSwipeHintOverlay(offsetX: Float, threshold: Float) {
    val progress = (offsetX.absoluteValue / threshold).coerceIn(0f, 1f)
    if (progress <= 0f) return

    val isRight = offsetX > 0f
    val bg = if (isRight) AppTheme.colors.swipeLearned else AppTheme.colors.swipeReview
    val icon = if (isRight) Icons.Default.Check else Icons.AutoMirrored.Filled.Undo
    val label = if (isRight) stringResource(R.string.vocabulary_status_learned) else stringResource(R.string.vocabulary_status_review)

    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(bg.copy(alpha = (progress * 0.9f).coerceIn(0f, 0.92f)))
            .alpha(progress),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteVocabularyLookupBottomSheet(target: RemoteLookupTarget, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ContentPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.vocabulary_lookup_close))
            }
        }
        RemoteWebLookupView(
            url = target.url,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RemoteWebLookupView(url: String, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
                loadUrl(url)
            }
        },
        update = { webView -> if (webView.url != url) webView.loadUrl(url) },
        modifier = modifier.fillMaxSize(),
    )
}

private data class RemoteLookupTarget(val title: String, val url: String)

private enum class RemoteLookupMode { Context, Images }
