package com.trungld.studyforielts.presentation.vocabulary

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import com.trungld.studyforielts.data.local.entity.VocabularyEntity
import com.trungld.studyforielts.ui.theme.AeroButton
import com.trungld.studyforielts.ui.theme.AeroButtonStyle
import com.trungld.studyforielts.ui.theme.AeroCard
import com.trungld.studyforielts.ui.theme.AppTheme
import com.trungld.studyforielts.ui.theme.Dimens
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.vocabulary_screen_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.vocabulary_mastered_count, uiState.learnedCount, uiState.totalCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .padding(horizontal = Dimens.ContentPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VocabularyProgressBar(uiState = uiState)

                when {
                    uiState.totalCount == 0 -> EmptyVocabularyState()
                    uiState.isCompleted -> VocabularyCompletedState(
                        onStartDictation = { onStartDictationClick(uiState.lessonId) },
                    )
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
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

                        // Bottom Action CTA
                        AeroButton(
                            onClick = { onStartDictationClick(uiState.lessonId) },
                            style = AeroButtonStyle.AERO_BLUE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
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
private fun VocabularyProgressBar(uiState: VocabularyUiState) {
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
                    text = stringResource(R.string.vocabulary_deck),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.vocabulary_words_remaining, uiState.remainingCount),
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
private fun VocabularyCompletedState(onStartDictation: () -> Unit) {
    AeroCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(24.dp),
        accentGlow = Color(0x664CAF50),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AeroCard(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                accentGlow = Color(0x664CAF50),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
            AeroButton(
                onClick = onStartDictation,
                style = AeroButtonStyle.NATURE_EMERALD,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
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
            .fillMaxSize(),
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
                val stackOffsetYPx = with(density) { (reactiveStackIndex * 14).dp.toPx() }
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
                    .fillMaxSize()
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
    onManualSwipeRight: () -> Unit = {},
    onManualSwipeLeft: () -> Unit = {},
) {
    AeroCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Word and Details container (tight spacing to fit without scrolling)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Header row: Word + Audio pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vocabulary.word,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (vocabulary.phonetic.isNotBlank()) {
                                Text(
                                    text = vocabulary.phonetic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        AeroButton(
                            onClick = onPronounce,
                            enabled = enabled,
                            style = AeroButtonStyle.FROSTED_GLASS,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = stringResource(R.string.vocabulary_pronounce),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    // Meaning Card
                    AeroCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isGlass = true,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Definition",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = vocabulary.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp,
                            )
                        }
                    }

                    // Example Sentence Card
                    if (vocabulary.exampleSentence.isNotBlank()) {
                        AeroCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            isGlass = true,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Example in IELTS Context",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "\"${vocabulary.exampleSentence}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Quick Actions & Swipe Bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AeroButton(
                            onClick = onOpenContext,
                            modifier = Modifier.weight(1f).height(42.dp),
                            enabled = enabled,
                            style = AeroButtonStyle.FROSTED_GLASS,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.vocabulary_context_in_sentence),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        AeroButton(
                            onClick = onOpenImages,
                            modifier = Modifier.weight(1f).height(42.dp),
                            enabled = enabled,
                            style = AeroButtonStyle.FROSTED_GLASS,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.vocabulary_images),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Direct Tapping Controls (Accessibility & Faster One-Handed UI)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AeroButton(
                            onClick = onManualSwipeLeft,
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            style = AeroButtonStyle.WARM_AMBER,
                            modifier = Modifier.weight(1f).height(44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.swipe_mark_review),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        AeroButton(
                            onClick = onManualSwipeRight,
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            style = AeroButtonStyle.NATURE_EMERALD,
                            modifier = Modifier.weight(1f).height(44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
            .matchParentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor.copy(alpha = (progress * 0.9f).coerceIn(0f, 0.92f)))
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
private fun VocabularyLookupBottomSheet(
    target: VocabularyLookupTarget,
    onClose: () -> Unit,
) {
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
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}

private data class VocabularyLookupTarget(
    val title: String,
    val url: String,
)

private enum class VocabularyLookupMode {
    Context,
    Images,
}

