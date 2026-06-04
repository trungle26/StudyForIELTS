package com.trungld.studyforielts.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.trungld.studyforielts.presentation.vocabulary.VocabularyScreen
import com.trungld.studyforielts.presentation.vocabulary.VocabularyViewModel
import com.trungld.studyforielts.presentation.youtube.YoutubeBrowseScreen
import com.trungld.studyforielts.presentation.youtube.YoutubeBrowseViewModel
import com.trungld.studyforielts.presentation.youtube.YoutubeDictationScreen
import com.trungld.studyforielts.presentation.youtube.YoutubeDictationViewModel
import com.trungld.studyforielts.presentation.youtube.YoutubePreviewScreen
import com.trungld.studyforielts.presentation.youtube.YoutubePreviewViewModel

@Composable
fun StudyForIeltsNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = StudyDestination.LevelList.route,
    ) {
        composable(route = StudyDestination.LevelList.route) {
            LevelListScreen(
                onLevelClick = { level ->
                    navController.navigate(StudyDestination.LessonList.createRoute(level))
                },
                onOnlineYoutubeClick = {
                    navController.navigate(StudyDestination.YoutubeBrowse.route)
                },
            )
        }

        composable(
            route = StudyDestination.LessonList.route,
            arguments = listOf(
                navArgument(LessonListViewModel.LEVEL_ARGUMENT) {
                    type = NavType.StringType
                },
            ),
        ) {
            val viewModel: LessonListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LessonListScreen(
                uiState = uiState,
                onBackClick = navController::popBackStack,
                onLessonClick = { lessonId ->
                    navController.navigate(StudyDestination.Vocabulary.createRoute(lessonId))
                },
            )
        }

        composable(
            route = StudyDestination.Vocabulary.route,
            arguments = listOf(
                navArgument(VocabularyViewModel.LESSON_ID_ARGUMENT) {
                    type = NavType.LongType
                },
            ),
        ) {
            val viewModel: VocabularyViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            VocabularyScreen(
                uiState = uiState,
                onBackClick = navController::popBackStack,
                onMarkLearned = viewModel::markVocabularyLearned,
                onRecycleToQueue = viewModel::recycleVocabulary,
                onPronounce = viewModel::pronounce,
                onStartDictationClick = { lessonId ->
                    navController.navigate(StudyDestination.Dictation.createRoute(lessonId))
                },
            )
        }

        composable(
            route = StudyDestination.Dictation.route,
            arguments = listOf(
                navArgument(DictationViewModel.LESSON_ID_ARGUMENT) {
                    type = NavType.LongType
                },
            ),
        ) {
            val viewModel: DictationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

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

        composable(route = StudyDestination.YoutubeBrowse.route) {
            val viewModel: YoutubeBrowseViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            YoutubeBrowseScreen(
                uiState = uiState,
                onQueryChanged = viewModel::onQueryChanged,
                onSearch = viewModel::search,
                onVideoClick = { videoId ->
                    navController.navigate(StudyDestination.YoutubePreview.createRoute(videoId))
                },
                onBackClick = navController::popBackStack,
            )
        }

        composable(
            route = StudyDestination.YoutubePreview.route,
            arguments = listOf(
                navArgument(YoutubePreviewViewModel.VIDEO_ID_ARGUMENT) {
                    type = NavType.StringType
                },
            ),
        ) {
            val viewModel: YoutubePreviewViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            YoutubePreviewScreen(
                uiState = uiState,
                onBackClick = navController::popBackStack,
                onRetryClick = viewModel::retryTranscriptLoad,
                onStartDictationClick = { videoId ->
                    navController.navigate(StudyDestination.YoutubeDictation.createRoute(videoId))
                },
            )
        }

        composable(
            route = StudyDestination.YoutubeDictation.route,
            arguments = listOf(
                navArgument(YoutubeDictationViewModel.VIDEO_ID_ARGUMENT) {
                    type = NavType.StringType
                },
            ),
        ) {
            val viewModel: YoutubeDictationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

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
}

sealed class StudyDestination(val route: String) {
    data object LevelList : StudyDestination("levels")

    data object LessonList : StudyDestination("lessons/{${LessonListViewModel.LEVEL_ARGUMENT}}") {
        fun createRoute(level: String): String = "lessons/$level"
    }

    data object Vocabulary : StudyDestination("vocabulary/{${VocabularyViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "vocabulary/$lessonId"
    }

    data object Dictation : StudyDestination("dictation/{${DictationViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "dictation/$lessonId"
    }

    data object YoutubeBrowse : StudyDestination("youtube")

    data object YoutubePreview : StudyDestination(
        "youtube/preview/{${YoutubePreviewViewModel.VIDEO_ID_ARGUMENT}}",
    ) {
        fun createRoute(videoId: String): String {
            return "youtube/preview/${Uri.encode(videoId)}"
        }
    }

    data object YoutubeDictation : StudyDestination(
        "youtube/dictation/{${YoutubeDictationViewModel.VIDEO_ID_ARGUMENT}}",
    ) {
        fun createRoute(videoId: String): String {
            return "youtube/dictation/${Uri.encode(videoId)}"
        }
    }
}
