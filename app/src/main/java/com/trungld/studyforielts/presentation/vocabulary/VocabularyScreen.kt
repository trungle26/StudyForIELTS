package com.trungld.studyforielts.presentation.vocabulary

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.trungld.studyforielts.R
import com.trungld.studyforielts.data.local.entity.VocabularyEntity
import com.trungld.studyforielts.ui.theme.AppTheme
import com.trungld.studyforielts.ui.theme.Dimens
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    uiState: VocabularyUiState,
    onBackClick: () -> Unit,
    onMarkLearned: (VocabularyEntity) -> Unit,
    onRecycleToQueue: (VocabularyEntity) -> Unit,
    onPronounce: (String) -> Unit,
    onStartDictationClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lookupTarget by remember { mutableStateOf<VocabularyLookupTarget?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val contextSheetTitle = stringResource(R.string.vocabulary_sheet_context_title)
    val imageSheetTitle = stringResource(R.string.vocabulary_sheet_images_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.vocabulary_screen_title))
                },
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.ContentMaxWidth)
                .padding(horizontal = Dimens.ContentPadding + Dimens.SpacingXs, vertical = Dimens.SpacingSm + Dimens.SpacingXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.ContentPadding),
        ) {
            VocabularyHeader(uiState = uiState)

            if (uiState.totalCount == 0) {
                EmptyVocabularyState()
            } else if (uiState.isCompleted) {
                VocabularyCompletedState()
            } else {
                VocabularyCardStack(
                    visibleCards = uiState.visibleStack,
                    onSwipeRight = onMarkLearned,
                    onSwipeLeft = onRecycleToQueue,
                    onPronounce = onPronounce,
                    onOpenLookup = { word, mode ->
                        lookupTarget = VocabularyLookupTarget(
                            title = when (mode) {
                                VocabularyLookupMode.Context -> contextSheetTitle
                                VocabularyLookupMode.Images -> imageSheetTitle
                            },
                            url = when (mode) {
                                VocabularyLookupMode.Context -> buildContextLookupUrl(word)
                                VocabularyLookupMode.Images -> buildImageLookupUrl(word)
                            },
                        )
                    },
                )
            }

            Button(
                onClick = { onStartDictationClick(uiState.lessonId) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text(stringResource(R.string.start_dictation))
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
            VocabularyLookupBottomSheet(
                target = lookupTarget!!,
                onClose = { lookupTarget = null },
            )
        }
    }
}

@Composable
private fun VocabularyHeader(
    uiState: VocabularyUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingTiny)) {
        Text(
            text = stringResource(R.string.vocabulary_header_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.vocabulary_header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderStat(
                value = stringResource(
                    R.string.vocabulary_progress_summary,
                    uiState.learnedCount,
                    uiState.totalCount,
                ),
            )
            HeaderStat(
                value = stringResource(
                    R.string.vocabulary_remaining_summary,
                    uiState.remainingCount,
                ),
            )
        }
    }
}

@Composable
private fun HeaderStat(
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.SurfaceAlpha + 0.05f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = Dimens.SpacingSm + Dimens.SpacingXs, vertical = Dimens.SpacingSm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyVocabularyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
    ) {
        Text(
            text = stringResource(R.string.vocabulary_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.vocabulary_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VocabularyCompletedState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ContentPaddingLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconTileSizeLarge),
            )
            Text(
                text = stringResource(R.string.vocabulary_completed_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.vocabulary_completed_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VocabularyCardStack(
    visibleCards: List<VocabularyEntity>,
    onSwipeRight: (VocabularyEntity) -> Unit,
    onSwipeLeft: (VocabularyEntity) -> Unit,
    onPronounce: (String) -> Unit,
    onOpenLookup: (String, VocabularyLookupMode) -> Unit,
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }
    var topSwipeProgress by remember(visibleCards.firstOrNull()?.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        contentAlignment = Alignment.Center,
    ) {
        visibleCards.asReversed().forEach { vocabulary ->
            key(vocabulary.id) {
                val stackIndex = visibleCards.indexOf(vocabulary)
                val topCard = stackIndex == 0
                val reactiveStackIndex = if (topCard) {
                    0f
                } else {
                    (stackIndex - topSwipeProgress).coerceAtLeast(0f)
                }
                val scale = 1f - (reactiveStackIndex * 0.04f)
                val stackOffsetYPx = with(density) { (reactiveStackIndex * 18).dp.toPx() }
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(durationMillis = if (topSwipeProgress > 0f) 80 else 170),
                    label = "VocabularyCardScale",
                )
                val animatedTranslationY by animateFloatAsState(
                    targetValue = stackOffsetYPx,
                    animationSpec = tween(durationMillis = if (topSwipeProgress > 0f) 80 else 170),
                    label = "VocabularyCardTranslationY",
                )
                val stackModifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        this.translationY = animatedTranslationY
                    }

                if (topCard) {
                    SwipeableVocabularyCard(
                        vocabulary = vocabulary,
                        modifier = stackModifier,
                        onSwipeRight = { onSwipeRight(vocabulary) },
                        onSwipeLeft = { onSwipeLeft(vocabulary) },
                        onSwipeProgressChanged = { progress ->
                            topSwipeProgress = progress
                        },
                        swipeThreshold = swipeThreshold,
                        onPronounce = { onPronounce(vocabulary.word) },
                        onOpenContext = { onOpenLookup(vocabulary.word, VocabularyLookupMode.Context) },
                        onOpenImages = { onOpenLookup(vocabulary.word, VocabularyLookupMode.Images) },
                    )
                } else {
                    StaticVocabularyCard(
                        vocabulary = vocabulary,
                        modifier = stackModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableVocabularyCard(
    vocabulary: VocabularyEntity,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeProgressChanged: (Float) -> Unit,
    swipeThreshold: Float,
    onPronounce: () -> Unit,
    onOpenContext: () -> Unit,
    onOpenImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetX = remember(vocabulary.id) { Animatable(0f) }
    val offsetY = remember(vocabulary.id) { Animatable(0f) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val dismissDistance = with(density) { 520.dp.toPx() }

    VocabularyCardFrame(
        vocabulary = vocabulary,
        modifier = modifier
            .offset {
                IntOffset(
                    x = offsetX.value.roundToInt(),
                    y = offsetY.value.roundToInt(),
                )
            }
            .graphicsLayer {
                rotationZ = offsetX.value / 28f
            }
            .pointerInput(vocabulary.id) {
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
                        val shouldDismissRight = offsetX.value > swipeThreshold
                        val shouldDismissLeft = offsetX.value < -swipeThreshold

                        when {
                            shouldDismissRight -> {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = dismissDistance,
                                    ) {
                                        onSwipeProgressChanged(
                                            (value.absoluteValue / swipeThreshold).coerceIn(0f, 1f),
                                        )
                                    }
                                    onSwipeRight()
                                    onSwipeProgressChanged(0f)
                                }
                            }

                            shouldDismissLeft -> {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = -dismissDistance,
                                    ) {
                                        onSwipeProgressChanged(
                                            (value.absoluteValue / swipeThreshold).coerceIn(0f, 1f),
                                        )
                                    }
                                    onSwipeLeft()
                                    onSwipeProgressChanged(0f)
                                }
                            }

                            else -> {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    ) {
                                        onSwipeProgressChanged(
                                            (value.absoluteValue / swipeThreshold).coerceIn(0f, 1f),
                                        )
                                    }
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                    onSwipeProgressChanged(0f)
                                }
                            }
                        }
                    },
                )
            },
        swipeOverlay = {
            SwipeHintOverlay(
                offsetX = offsetX.value,
                threshold = swipeThreshold,
            )
        },
        onPronounce = onPronounce,
        onOpenContext = onOpenContext,
        onOpenImages = onOpenImages,
    )
}

@Composable
private fun StaticVocabularyCard(
    vocabulary: VocabularyEntity,
    modifier: Modifier = Modifier,
) {
    VocabularyCardFrame(
        vocabulary = vocabulary,
        modifier = modifier,
        swipeOverlay = {},
        onPronounce = {},
        onOpenContext = {},
        onOpenImages = {},
        enabled = false,
    )
}

@Composable
private fun VocabularyCardFrame(
    vocabulary: VocabularyEntity,
    modifier: Modifier,
    swipeOverlay: @Composable BoxScope.() -> Unit,
    onPronounce: () -> Unit,
    onOpenContext: () -> Unit,
    onOpenImages: () -> Unit,
    enabled: Boolean = true,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.ContentPaddingLarge),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
                            Text(
                                text = vocabulary.word,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(
                            onClick = onPronounce,
                            enabled = enabled,
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = stringResource(R.string.vocabulary_pronounce),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.phonetic_label, vocabulary.phonetic),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.meaning_label, vocabulary.meaning),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.example_label, vocabulary.exampleSentence),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.vocabulary_stack_helper),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
                    ) {
                        Button(
                            onClick = onOpenContext,
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(stringResource(R.string.vocabulary_context_in_sentence))
                        }
                        Button(
                            onClick = onOpenImages,
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(stringResource(R.string.vocabulary_images))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
                    ) {
                        SwipeLegendItem(
                            text = stringResource(R.string.swipe_mark_review),
                            background = AppTheme.colors.swipeReviewContainer,
                            contentColor = AppTheme.colors.swipeReview,
                            icon = Icons.AutoMirrored.Filled.Undo,
                            modifier = Modifier.weight(1f),
                        )
                        SwipeLegendItem(
                            text = stringResource(R.string.swipe_mark_learned),
                            background = AppTheme.colors.swipeLearnedContainer,
                            contentColor = AppTheme.colors.swipeLearned,
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            swipeOverlay()
        }
    }
}

@Composable
private fun SwipeLegendItem(
    text: String,
    background: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = background,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingSm + Dimens.SpacingXs, vertical = Dimens.SpacingSm + Dimens.SpacingTiny),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingTiny),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
            )
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BoxScope.SwipeHintOverlay(
    offsetX: Float,
    threshold: Float,
) {
    val progress = (offsetX.absoluteValue / threshold).coerceIn(0f, 1f)
    if (progress <= 0f) return

    val isRightSwipe = offsetX > 0f
    val backgroundColor = if (isRightSwipe) AppTheme.colors.swipeLearned else AppTheme.colors.swipeReview
    val icon = if (isRightSwipe) Icons.Default.Check else Icons.AutoMirrored.Filled.Undo
    val label = if (isRightSwipe) {
        stringResource(R.string.vocabulary_status_learned)
    } else {
        stringResource(R.string.vocabulary_status_review)
    }

    Box(
        modifier = Modifier
            .align(if (isRightSwipe) Alignment.CenterStart else Alignment.CenterEnd)
            .alpha(progress),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.94f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyLookupBottomSheet(
    target: VocabularyLookupTarget,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(horizontal = Dimens.ContentPadding, vertical = Dimens.SpacingSm + Dimens.SpacingXs),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm + Dimens.SpacingXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Button(
                onClick = onClose,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.vocabulary_lookup_close))
            }
        }
        WebLookupView(
            url = target.url,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebLookupView(
    url: String,
    modifier: Modifier = Modifier,
) {
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
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier,
    )
}

private fun buildContextLookupUrl(word: String): String {
    val encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.toString())
    return "https://www.google.com/search?q=how+to+use+$encodedWord+in+a+sentence"
}

private fun buildImageLookupUrl(word: String): String {
    val encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.toString())
    return "https://www.google.com/search?tbm=isch&q=$encodedWord"
}

private data class VocabularyLookupTarget(
    val title: String,
    val url: String,
)

private enum class VocabularyLookupMode {
    Context,
    Images,
}
