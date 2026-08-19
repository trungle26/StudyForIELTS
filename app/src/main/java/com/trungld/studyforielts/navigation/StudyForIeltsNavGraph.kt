package com.trungld.studyforielts.navigation

import android.net.Uri
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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trungld.studyforielts.presentation.dictation.DictationRoute
import com.trungld.studyforielts.presentation.dictation.DictationViewModel
import com.trungld.studyforielts.presentation.lesson.LessonListScreen
import com.trungld.studyforielts.presentation.lesson.LessonListViewModel
import com.trungld.studyforielts.presentation.level.LevelListScreen
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationListScreen
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationListViewModel
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationPlayerScreen
import com.trungld.studyforielts.presentation.remotedictation.RemoteDictationPlayerViewModel
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            StudyBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Only the active tab's NavHost is composed; inactive tabs rely on
            // rememberNavController's saveable state to restore their back stack
            // when the user comes back. This matches the standard pattern from
            // the navigation-compose samples.
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
    BottomNavItem.Home -> HomeDestination.LevelList.route
    BottomNavItem.Listening -> ListeningDestination.YoutubeBrowse.route
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
    composable(route = HomeDestination.LevelList.route) {
        LevelListScreen(
            onLevelClick = { level ->
                // ponytail: revert to LessonList when offline/local lessons are
                // the primary entry point; restore when bundled lessons need
                // surfacing alongside remote dictation.
                navController.navigate(HomeDestination.RemoteDictationList.createRoute(level))
            },
            // "Online YouTube Dictation" is the entry point into the Listening
            // tab; switching tabs brings YouTubeBrowse to the foreground as its
            // own back stack.
            onOnlineYoutubeClick = { onTabSelected(BottomNavItem.Listening) },
        )
    }

    composable(
        route = HomeDestination.LessonList.route,
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
                navController.navigate(HomeDestination.Vocabulary.createRoute(lessonId))
            },
        )
    }

    composable(
        route = HomeDestination.RemoteDictationList.route,
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
                navController.navigate(HomeDestination.RemoteDictationPlayer.createRoute(lessonId))
            },
        )
    }

    composable(
        route = HomeDestination.RemoteDictationPlayer.route,
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
        route = HomeDestination.Vocabulary.route,
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
                navController.navigate(HomeDestination.Dictation.createRoute(lessonId))
            },
        )
    }

    composable(
        route = HomeDestination.Dictation.route,
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
}

sealed class HomeDestination(val route: String) {
    data object LevelList : HomeDestination("home/levels")

    data object LessonList : HomeDestination("home/lessons/{${LessonListViewModel.LEVEL_ARGUMENT}}") {
        fun createRoute(level: String): String = "home/lessons/$level"
    }

    data object Vocabulary : HomeDestination("home/vocabulary/{${VocabularyViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "home/vocabulary/$lessonId"
    }

    data object RemoteDictationList : HomeDestination("home/remote-dictation/{${RemoteDictationListViewModel.LEVEL_ARGUMENT}}") {
        fun createRoute(level: String): String = "home/remote-dictation/$level"
    }

    data object RemoteDictationPlayer : HomeDestination("home/remote-dictation/player/{${RemoteDictationPlayerViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: String): String = "home/remote-dictation/player/${android.net.Uri.encode(lessonId)}"
    }

    data object Dictation : HomeDestination("home/dictation/{${DictationViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "home/dictation/$lessonId"
    }
}

// --- Listening tab ----------------------------------------------------

private fun NavGraphBuilder.registerListeningGraph(
    navController: NavHostController,
    onTabSelected: (BottomNavItem) -> Unit,
) {
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
            // Tab root: the back arrow on YouTubeBrowse has nothing to pop in
            // this tab's back stack, so route it back to the Home tab.
            onBackClick = { onTabSelected(BottomNavItem.Home) },
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
