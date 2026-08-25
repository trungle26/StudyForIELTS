package com.trungld.studyforielts.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        contentWindowInsets = WindowInsets.navigationBars,
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BottomNavItem.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
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
            onPronounce = viewModel::pronounce,
            onRemoveSavedVocabulary = viewModel::removeSavedVocabulary,
        )
    }
}

sealed class HomeDestination(val route: String) {
    data object Home : HomeDestination("home/main")
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
            onFreePracticeClick = {
                navController.navigate(WritingDestination.Practice.route)
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
