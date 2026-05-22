package com.trungld.studyforielts.presentation.vocabulary

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                )
                Box(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.start_dictation))
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyVocabularyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        contentAlignment = Alignment.Center,
    ) {
        visibleCards.asReversed().forEach { vocabulary ->
            val stackIndex = visibleCards.indexOf(vocabulary)
            val topCard = stackIndex == 0
            val scale = 1f - (stackIndex * 0.04f)
            val stackOffsetY = (stackIndex * 18).dp
            val alpha = 1f - (stackIndex * 0.12f)

            if (topCard) {
                SwipeableVocabularyCard(
                    vocabulary = vocabulary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.translationY = with(density) { stackOffsetY.toPx() }
                        },
                    onSwipeRight = { onSwipeRight(vocabulary) },
                    onSwipeLeft = { onSwipeLeft(vocabulary) },
                    onPronounce = { onPronounce(vocabulary.word) },
                    onOpenContext = { onOpenLookup(vocabulary.word, VocabularyLookupMode.Context) },
                    onOpenImages = { onOpenLookup(vocabulary.word, VocabularyLookupMode.Images) },
                )
            } else {
                StaticVocabularyCard(
                    vocabulary = vocabulary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.translationY = with(density) { stackOffsetY.toPx() }
                        },
                )
            }
        }
    }
}

@Composable
private fun SwipeableVocabularyCard(
    vocabulary: VocabularyEntity,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onPronounce: () -> Unit,
    onOpenContext: () -> Unit,
    onOpenImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetX = remember(vocabulary.id) { Animatable(0f) }
    val offsetY = remember(vocabulary.id) { Animatable(0f) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 120.dp.toPx() }
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
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.22f)
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
                                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    )
                                    onSwipeRight()
                                }
                            }

                            shouldDismissLeft -> {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = -dismissDistance,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    )
                                    onSwipeLeft()
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
                                    )
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
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
        modifier = modifier.alpha(0.94f),
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (vocabulary.isLearned) 0.45f else 0.78f,
            ),
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = vocabulary.word,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(
                                    if (vocabulary.isLearned) {
                                        R.string.vocabulary_status_learned
                                    } else {
                                        R.string.vocabulary_status_review
                                    },
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (vocabulary.isLearned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onOpenContext,
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                            Box(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.vocabulary_context_in_sentence))
                        }
                        Button(
                            onClick = onOpenImages,
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                            )
                            Box(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.vocabulary_images))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HintChip(
                            text = stringResource(R.string.swipe_mark_review),
                            background = Color(0xFFFFEDD5),
                            contentColor = Color(0xFF9A3412),
                            icon = Icons.AutoMirrored.Filled.Undo,
                        )
                        HintChip(
                            text = stringResource(R.string.swipe_mark_learned),
                            background = Color(0xFFDCFCE7),
                            contentColor = Color(0xFF166534),
                            icon = Icons.Default.Check,
                        )
                    }
                }
            }

            swipeOverlay()
        }
    }
}

@Composable
private fun HintChip(
    text: String,
    background: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
    val backgroundColor = if (isRightSwipe) Color(0xFF16A34A) else Color(0xFFEA580C)
    val icon = if (isRightSwipe) Icons.Default.Check else Icons.AutoMirrored.Filled.Undo
    val label = if (isRightSwipe) {
        stringResource(R.string.vocabulary_status_learned)
    } else {
        stringResource(R.string.vocabulary_status_review)
    }

    Box(
        modifier = Modifier
            .align(if (isRightSwipe) Alignment.TopStart else Alignment.TopEnd)
            .padding(20.dp)
            .background(backgroundColor.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .alpha(progress),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
            )
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                shape = RoundedCornerShape(14.dp),
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
