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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
    onPronounce: (String) -> Unit,
    onStartDictationClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lookupTarget by remember { mutableStateOf<VocabularyLookupTarget?>(null) }
    var remainingCards by remember(uiState.lessonId, uiState.vocabularies) {
        mutableStateOf(uiState.vocabularies)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val contextSheetTitle = stringResource(R.string.vocabulary_sheet_context_title)
    val imageSheetTitle = stringResource(R.string.vocabulary_sheet_images_title)
    val totalCount = uiState.totalCount
    val reviewedCount = totalCount - remainingCards.size
    val isCompleted = totalCount > 0 && remainingCards.isEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.vocabulary_screen_title)) },
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
            VocabularyHeader(
                reviewedCount = reviewedCount,
                totalCount = totalCount,
                remainingCount = remainingCards.size,
            )

            when {
                totalCount == 0 -> EmptyVocabularyState()
                isCompleted -> VocabularyCompletedState()
                else -> {
                    VocabularyCardStack(
                        visibleCards = remainingCards.take(3),
                        onDismissCard = { vocabulary ->
                            remainingCards = remainingCards.filterNot { it.id == vocabulary.id }
                        },
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
    reviewedCount: Int,
    totalCount: Int,
    remainingCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.vocabulary_header_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.vocabulary_header_subtitle_simple),
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
                    reviewedCount,
                    totalCount,
                ),
            )
            HeaderStat(
                value = stringResource(
                    R.string.vocabulary_remaining_summary,
                    remainingCount,
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
                text = stringResource(R.string.vocabulary_completed_subtitle_simple),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VocabularyCardStack(
    visibleCards: List<VocabularyEntity>,
    onDismissCard: (VocabularyEntity) -> Unit,
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
        visibleCards.asReversed().forEachIndexed { reversedIndex, vocabulary ->
            val stackIndex = visibleCards.lastIndex - reversedIndex
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
                            translationY = with(density) { stackOffsetY.toPx() }
                        },
                    onDismissCard = onDismissCard,
                    onPronounce = { onPronounce(vocabulary.word) },
                    onOpenContext = {
                        onOpenLookup(vocabulary.word, VocabularyLookupMode.Context)
                    },
                    onOpenImages = {
                        onOpenLookup(vocabulary.word, VocabularyLookupMode.Images)
                    },
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
                            translationY = with(density) { stackOffsetY.toPx() }
                        },
                )
            }
        }
    }
}

@Composable
private fun SwipeableVocabularyCard(
    vocabulary: VocabularyEntity,
    onDismissCard: (VocabularyEntity) -> Unit,
    onPronounce: () -> Unit,
    onOpenContext: () -> Unit,
    onOpenImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember(vocabulary.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(vocabulary.id) { mutableFloatStateOf(0f) }
    var isAnimatingOut by remember(vocabulary.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 120.dp.toPx() }
    val dismissDistance = with(density) { 640.dp.toPx() }
    val dismissYOffset = with(density) { 36.dp.toPx() }
    val latestOnDismissCard by rememberUpdatedState(onDismissCard)

    VocabularyCardFrame(
        vocabulary = vocabulary,
        modifier = modifier
            .offset {
                IntOffset(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt(),
                )
            }
            .graphicsLayer {
                rotationZ = offsetX / 28f
            }
            .pointerInput(vocabulary.id, isAnimatingOut) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (isAnimatingOut) {
                            return@detectDragGestures
                        }
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y * 0.22f
                    },
                    onDragEnd = {
                        if (isAnimatingOut) {
                            return@detectDragGestures
                        }
                        when {
                            offsetX.absoluteValue > swipeThreshold -> {
                                val horizontalTarget = if (offsetX > 0f) {
                                    dismissDistance
                                } else {
                                    -dismissDistance
                                }
                                isAnimatingOut = true
                                val currentX = offsetX
                                val currentY = offsetY
                                val animatedX = Animatable(currentX)
                                val animatedY = Animatable(currentY)
                                coroutineScope.launch {
                                    launch {
                                        animatedX.animateTo(
                                            targetValue = horizontalTarget,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                        ) {
                                            offsetX = value
                                        }
                                    }
                                    launch {
                                        animatedY.animateTo(
                                            targetValue = currentY + dismissYOffset,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessVeryLow,
                                            ),
                                        ) {
                                            offsetY = value
                                        }
                                    }
                                }.invokeOnCompletion {
                                    latestOnDismissCard(vocabulary)
                                    offsetX = 0f
                                    offsetY = 0f
                                    isAnimatingOut = false
                                }
                            }

                            else -> {
                                val currentX = offsetX
                                val currentY = offsetY
                                val animatedX = Animatable(currentX)
                                val animatedY = Animatable(currentY)
                                coroutineScope.launch {
                                    launch {
                                        animatedX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        ) {
                                            offsetX = value
                                        }
                                    }
                                    launch {
                                        animatedY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        ) {
                                            offsetY = value
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            },
        swipeOverlay = {
            SwipeHintOverlay(
                offsetX = offsetX,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
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
                                text = stringResource(R.string.vocabulary_swipe_any_side),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }

            swipeOverlay()
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

    Box(
        modifier = Modifier
            .align(if (offsetX > 0f) Alignment.TopStart else Alignment.TopEnd)
            .padding(20.dp)
            .background(Color(0xFF16A34A).copy(alpha = 0.92f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .alpha(progress),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
            )
            Text(
                text = stringResource(R.string.swipe_mark_learned),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

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
