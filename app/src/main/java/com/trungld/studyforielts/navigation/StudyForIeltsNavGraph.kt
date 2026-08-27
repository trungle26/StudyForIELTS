package com.trungld.studyforielts.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trungld.studyforielts.presentation.home.HomeScreen
import com.trungld.studyforielts.presentation.home.HomeViewModel
import com.trungld.studyforielts.presentation.dictation.DictationRoute
import com.trungld.studyforielts.presentation.dictation.DictationViewModel
import com.trungld.studyforielts.presentation.lesson.LessonListScreen
import com.trungld.studyforielts.presentation.lesson.LessonListViewModel
import com.trungld.studyforielts.presentation.level.LevelListScreen
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationListScreen
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationListViewModel
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationPlayerScreen
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationPlayerViewModel
import com.trungld.studyforielts.presentation.remotedictation.vocabulary.RemoteVocabularyScreen
import com.trungld.studyforielts.presentation.remotedictation.vocabulary.RemoteVocabularyViewModel
import com.trungld.studyforielts.presentation.strategy.StrategyDetailScreen
import com.trungld.studyforielts.presentation.strategy.StrategyDetailViewModel
import com.trungld.studyforielts.presentation.strategy.StrategyListScreen
import com.trungld.studyforielts.presentation.strategy.StrategyListViewModel
import com.trungld.studyforielts.presentation.vocabulary.VocabularyScreen
import com.trungld.studyforielts.presentation.vocabulary.VocabularyViewModel
import com.trungld.studyforielts.presentation.writing.WritingHomeScreen
import com.trungld.studyforielts.presentation.writing.WritingLessonListScreen
import com.trungld.studyforielts.presentation.writing.WritingLessonListViewModel
import com.trungld.studyforielts.presentation.writing.WritingPracticeScreen
import com.trungld.studyforielts.presentation.writing.WritingViewModel
import com.trungld.studyforielts.presentation.youtube.YoutubeBrowseScreen
import com.trungld.studyforielts.presentation.youtube.YoutubeBrowseViewModel
import com.trungld.studyforielts.presentation.youtube.YoutubeDictationScreen
import com.trungld.studyforielts.presentation.youtube.YoutubeDictationViewModel
import com.trungld.studyforielts.presentation.youtube.YoutubePreviewScreen
import com.trungld.studyforielts.presentation.youtube.YoutubePreviewViewModel

/**
 * Top-level scaffold: a [Scaffold] hosting one [NavHost] per bottom-nav tab.
 * Each tab owns its own [NavHostController] so the back stack is preserved when
 * the user switches tabs (per the 3.7 spec).
 */
@Composable
fun StudyForIeltsNavGraph() {
    val homeNavController = rememberNavController()
    val listeningNavController = rememberNavController()
    val writingNavController = rememberNavController()

    var currentTab by rememberSaveable { mutableStateOf(BottomNavItem.Home) }

    val currentNavController = currentTab.controller(homeNavController, listeningNavController, writingNavController)
    val currentBackStackEntry by currentNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Hide bottom navigation in immersive/focus modes (dictation player, youtube dictation, writing practice)
    val isBottomBarVisible = when {
        currentRoute == null -> true
        currentRoute.startsWith("listening/dictation/") -> false
        currentRoute.startsWith("listening/remote-dictation/player/") -> false
        currentRoute.startsWith("listening/youtube/dictation/") -> false
        currentRoute.startsWith("writing/practice") -> false
        else -> true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                StudyBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            BottomNavItem.entries.forEach { tab ->
                if (tab == currentTab) {
                    val navController = tab.controller(homeNavController, listeningNavController, writingNavController)
                    NavHost(
                        navController = navController,
                        startDestination = tab.startRoute(),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        registerGraph(tab, navController, onTabSelected = { currentTab = it })
                    }
                }
            }
        }
    }
}

private fun BottomNavItem.controller(
    home: NavHostController,
    listening: NavHostController,
    writing: NavHostController,
): NavHostController = when (this) {
    BottomNavItem.Home -> home
    BottomNavItem.Listening -> listening
    BottomNavItem.Writing -> writing
}

private fun BottomNavItem.startRoute(): String = when (this) {
    BottomNavItem.Home -> HomeDestination.Home.route
    BottomNavItem.Listening -> ListeningDestination.LevelList.route
    BottomNavItem.Writing -> WritingDestination.Home.route
}

@Composable
private fun StudyBottomBar(
    currentTab: BottomNavItem,
    onTabSelected: (BottomNavItem) -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val barBackground = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xF0183144),
                Color(0xEB112433),
                Color(0xE60C1B27),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF2FFFFFF),
                Color(0xD9E1F0FA),
                Color(0xD2D2E8F7),
            )
        )
    }

    val barBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0x8080D8FF),
                Color(0x3340C4FF),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xEEFFFFFF),
                Color(0x8081D4FA),
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ambientColor = if (isDark) Color(0x6600E5FF) else Color(0x330288D1),
                spotColor = if (isDark) Color(0x4D00B0FF) else Color(0x2B01579B),
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(barBackground)
                .border(
                    width = 1.2.dp,
                    brush = barBorder,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                )
                .drawWithContent {
                    drawContent()
                    // Top glassy specular sheen line
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to if (isDark) Color(0x44FFFFFF) else Color(0x88FFFFFF),
                            0.45f to if (isDark) Color(0x11FFFFFF) else Color(0x22FFFFFF),
                            0.46f to Color(0x00FFFFFF),
                            1.0f to Color(0x00FFFFFF),
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height * 0.48f),
                    )
                }
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                BottomNavItem.entries.forEach { tab ->
                    val selected = tab == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                stringResource(tab.labelRes),
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun NavGraphBuilder.registerGraph(
    tab: BottomNavItem,
    navController: NavHostController,
    onTabSelected: (BottomNavItem) -> Unit,
) {
    when (tab) {
        BottomNavItem.Home -> registerHomeGraph(navController, onTabSelected)
        BottomNavItem.Listening -> registerListeningGraph(navController, onTabSelected)
        BottomNavItem.Writing -> registerWritingGraph(navController)
    }
}

// --- Home tab ---------------------------------------------------------

private fun NavGraphBuilder.registerHomeGraph(
    navController: NavHostController,
    onTabSelected: (BottomNavItem) -> Unit,
) {
    composable(route = HomeDestination.Home.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HomeScreen(
            uiState = uiState,
            onListeningTabClick = { onTabSelected(BottomNavItem.Listening) },
            onStrategyClick = { strategyId ->
                navController.navigate(HomeDestination.StrategyDetail.createRoute(strategyId))
            },
            onSkillClick = { skill ->
                navController.navigate(HomeDestination.StrategyList.createRoute(skill.key))
            },
            onPronounce = viewModel::pronounce,
            onRemoveSavedVocabulary = viewModel::removeSavedVocabulary,
        )
    }

    composable(
        route = HomeDestination.StrategyList.route,
        arguments = listOf(
            navArgument(StrategyListViewModel.SKILL_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: StrategyListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        StrategyListScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
            onStrategyClick = { strategyId ->
                navController.navigate(HomeDestination.StrategyDetail.createRoute(strategyId))
            },
            onFilterSelected = viewModel::onQuestionTypeSelected,
        )
    }

    composable(
        route = HomeDestination.StrategyDetail.route,
        arguments = listOf(
            navArgument(StrategyDetailViewModel.STRATEGY_ID_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: StrategyDetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        StrategyDetailScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
        )
    }
}

sealed class HomeDestination(val route: String) {
    data object Home : HomeDestination("home/main")

    data object StrategyList : HomeDestination("home/strategies/{${StrategyListViewModel.SKILL_ARGUMENT}}") {
        fun createRoute(skill: String): String = "home/strategies/$skill"
    }

    data object StrategyDetail : HomeDestination("home/strategy/{${StrategyDetailViewModel.STRATEGY_ID_ARGUMENT}}") {
        fun createRoute(strategyId: String): String = "home/strategy/${Uri.encode(strategyId)}"
    }
}

// --- Listening tab ----------------------------------------------------

private fun NavGraphBuilder.registerListeningGraph(
    navController: NavHostController,
    onTabSelected: (BottomNavItem) -> Unit,
) {
    composable(route = ListeningDestination.LevelList.route) {
        LevelListScreen(
            onLevelClick = { level ->
                navController.navigate(ListeningDestination.RemoteDictationList.createRoute(level))
            },
            onOnlineYoutubeClick = {
                navController.navigate(ListeningDestination.YoutubeBrowse.route)
            },
        )
    }

    composable(
        route = ListeningDestination.LessonList.route,
        arguments = listOf(
            navArgument(LessonListViewModel.LEVEL_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: LessonListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LessonListScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
            onLessonClick = { lessonId ->
                navController.navigate(ListeningDestination.Vocabulary.createRoute(lessonId))
            },
        )
    }

    composable(
        route = ListeningDestination.RemoteDictationList.route,
        arguments = listOf(
            navArgument(RemoteDictationListViewModel.LEVEL_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: RemoteDictationListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        RemoteDictationListScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
            onLessonClick = { lessonId ->
                navController.navigate(ListeningDestination.RemoteDictationVocabulary.createRoute(lessonId))
            },
        )
    }

    composable(
        route = ListeningDestination.RemoteDictationPlayer.route,
        arguments = listOf(
            navArgument(RemoteDictationPlayerViewModel.LESSON_ID_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: RemoteDictationPlayerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        RemoteDictationPlayerScreen(
            uiState = uiState,
            onDraftChanged = viewModel::onDraftChanged,
            onTogglePlayback = viewModel::onTogglePlayback,
            onReplay = viewModel::onReplaySegment,
            onPrimaryAction = viewModel::onPrimaryAction,
            onNextSentence = viewModel::skipCurrentSentence,
            onResetLesson = viewModel::resetLessonProgress,
        )
    }

    composable(
        route = ListeningDestination.RemoteDictationVocabulary.route,
        arguments = listOf(
            navArgument(RemoteVocabularyViewModel.LESSON_ID_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: RemoteVocabularyViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        RemoteVocabularyScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
            onMarkLearned = viewModel::markVocabularyLearned,
            onRecycleToQueue = viewModel::recycleVocabulary,
            onPronounce = viewModel::pronounce,
            onStartDictationClick = { lessonServerId ->
                navController.navigate(ListeningDestination.RemoteDictationPlayer.createRoute(lessonServerId))
            },
        )
    }

    composable(
        route = ListeningDestination.Vocabulary.route,
        arguments = listOf(
            navArgument(VocabularyViewModel.LESSON_ID_ARGUMENT) { type = NavType.LongType },
        ),
    ) {
        val viewModel: VocabularyViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        VocabularyScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
            onMarkLearned = viewModel::markVocabularyLearned,
            onRecycleToQueue = viewModel::recycleVocabulary,
            onPronounce = viewModel::pronounce,
            onStartDictationClick = { lessonId ->
                navController.navigate(ListeningDestination.Dictation.createRoute(lessonId))
            },
        )
    }

    composable(
        route = ListeningDestination.Dictation.route,
        arguments = listOf(
            navArgument(DictationViewModel.LESSON_ID_ARGUMENT) { type = NavType.LongType },
        ),
    ) {
        val viewModel: DictationViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DictationRoute(
            uiState = uiState,
            onDraftChanged = viewModel::onDraftChanged,
            onTogglePlayback = viewModel::onTogglePlayback,
            onReplay = viewModel::onReplaySegment,
            onPrimaryAction = viewModel::onPrimaryAction,
            onNextSentence = viewModel::skipCurrentSentence,
            onResetLesson = viewModel::resetLessonProgress,
        )
    }

    composable(route = ListeningDestination.YoutubeBrowse.route) {
        val viewModel: YoutubeBrowseViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        YoutubeBrowseScreen(
            uiState = uiState,
            onQueryChanged = viewModel::onQueryChanged,
            onSearch = viewModel::search,
            onLevelSelected = viewModel::onLevelSelected,
            onRefreshFeed = viewModel::refreshFeed,
            onClearSearch = viewModel::clearSearch,
            onVideoClick = { videoId ->
                navController.navigate(ListeningDestination.YoutubePreview.createRoute(videoId))
            },
            onBackClick = { navController.popBackStack() },
            onWritingPracticeClick = { onTabSelected(BottomNavItem.Writing) },
        )
    }

    composable(
        route = ListeningDestination.YoutubePreview.route,
        arguments = listOf(
            navArgument(YoutubePreviewViewModel.VIDEO_ID_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: YoutubePreviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        YoutubePreviewScreen(
            uiState = uiState,
            onBackClick = navController::popBackStack,
            onRetryClick = viewModel::retryTranscriptLoad,
            onStartDictationClick = { videoId ->
                navController.navigate(ListeningDestination.YoutubeDictation.createRoute(videoId))
            },
        )
    }

    composable(
        route = ListeningDestination.YoutubeDictation.route,
        arguments = listOf(
            navArgument(YoutubeDictationViewModel.VIDEO_ID_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: YoutubeDictationViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        YoutubeDictationScreen(
            uiState = uiState,
            playerCommands = viewModel.playerCommands,
            onPlayerReady = viewModel::onPlayerReady,
            onCurrentSecond = viewModel::onCurrentSecond,
            onDraftChanged = viewModel::onDraftChanged,
            onReplay = viewModel::onReplaySentence,
            onPrimaryAction = viewModel::onPrimaryAction,
            onNextSentence = viewModel::skipCurrentSentence,
            onResetSession = viewModel::resetSession,
            onBackClick = navController::popBackStack,
        )
    }
}

sealed class ListeningDestination(val route: String) {
    data object LevelList : ListeningDestination("listening/levels")

    data object LessonList : ListeningDestination("listening/lessons/{${LessonListViewModel.LEVEL_ARGUMENT}}") {
        fun createRoute(level: String): String = "listening/lessons/$level"
    }

    data object Vocabulary : ListeningDestination("listening/vocabulary/{${VocabularyViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "listening/vocabulary/$lessonId"
    }

    data object RemoteDictationList : ListeningDestination("listening/remote-dictation/{${RemoteDictationListViewModel.LEVEL_ARGUMENT}}") {
        fun createRoute(level: String): String = "listening/remote-dictation/$level"
    }

    data object RemoteDictationPlayer : ListeningDestination("listening/remote-dictation/player/{${RemoteDictationPlayerViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: String): String = "listening/remote-dictation/player/${Uri.encode(lessonId)}"
    }

    data object RemoteDictationVocabulary : ListeningDestination(
        "listening/remote-dictation/vocabulary/{${RemoteVocabularyViewModel.LESSON_ID_ARGUMENT}}",
    ) {
        fun createRoute(lessonServerId: String): String =
            "listening/remote-dictation/vocabulary/${Uri.encode(lessonServerId)}"
    }

    data object Dictation : ListeningDestination("listening/dictation/{${DictationViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "listening/dictation/$lessonId"
    }

    data object YoutubeBrowse : ListeningDestination("listening/youtube")

    data object YoutubePreview : ListeningDestination(
        "listening/youtube/preview/{${YoutubePreviewViewModel.VIDEO_ID_ARGUMENT}}",
    ) {
        fun createRoute(videoId: String): String =
            "listening/youtube/preview/${Uri.encode(videoId)}"
    }

    data object YoutubeDictation : ListeningDestination(
        "listening/youtube/dictation/{${YoutubeDictationViewModel.VIDEO_ID_ARGUMENT}}",
    ) {
        fun createRoute(videoId: String): String =
            "listening/youtube/dictation/${Uri.encode(videoId)}"
    }
}

// --- Writing tab ------------------------------------------------------

private fun NavGraphBuilder.registerWritingGraph(
    navController: NavHostController,
) {
    composable(route = WritingDestination.Home.route) {
        WritingHomeScreen(
            onTask1Click = {
                navController.navigate(WritingDestination.LessonList.createRoute("task1"))
            },
            onTask2Click = {
                navController.navigate(WritingDestination.LessonList.createRoute("task2"))
            },
        )
    }

    composable(
        route = WritingDestination.LessonList.route,
        arguments = listOf(
            navArgument(WritingLessonListViewModel.TASK_TYPE_ARGUMENT) { type = NavType.StringType },
        ),
    ) {
        val viewModel: WritingLessonListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val taskType = viewModel.taskTypeOrDefault
        val (label, showChart) = when (taskType) {
            "task1" -> "Task 1 - Bài học" to true
            else -> "Task 2 - Bài học" to false
        }
        WritingLessonListScreen(
            uiState = uiState,
            taskTypeLabel = label,
            showChartThumbnails = showChart,
            onBackClick = navController::popBackStack,
            onLessonClick = { lesson ->
                navController.navigate(WritingDestination.Practice.createRoute(lesson.id))
            },
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
        )
    }

    composable(
        route = WritingDestination.Practice.route,
        arguments = listOf(
            navArgument(WritingViewModel.LESSON_ID_ARGUMENT) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        WritingPracticeScreen(
            // Wire the back arrow to navigateUp; if we land here as the
            // start destination (rare path), the arrow stays hidden because
            // the screen falls back to onBackClick = null when there is
            // nothing to pop. We always provide a handler here for safety.
            onBackClick = { navController.navigateUp() },
        )
    }
}

sealed class WritingDestination(val route: String) {
    data object Home : WritingDestination("writing/home")

    data object LessonList : WritingDestination(
        "writing/lessons/{${WritingLessonListViewModel.TASK_TYPE_ARGUMENT}}",
    ) {
        fun createRoute(taskType: String): String = "writing/lessons/$taskType"
    }

    // Practice accepts an optional `lessonId` query argument. When the user
    // picks "Free Practice" on the home screen we navigate with no query,
    // and Compose Navigation fills the argument with the configured default
    // (null). When a lesson row is picked we pass the lesson id so the
    // screen can fetch + display the lesson and route the submit call.
    data object Practice : WritingDestination(
        "writing/practice?${WritingViewModel.LESSON_ID_ARGUMENT}={${WritingViewModel.LESSON_ID_ARGUMENT}}",
    ) {
        fun createRoute(lessonId: String): String =
            "writing/practice?${WritingViewModel.LESSON_ID_ARGUMENT}=$lessonId"
    }
}
