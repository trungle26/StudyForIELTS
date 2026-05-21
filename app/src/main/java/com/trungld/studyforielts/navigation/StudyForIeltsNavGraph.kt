package com.trungld.studyforielts.navigation

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
                onReplayLastThreeSeconds = viewModel::onReplayLastThreeSeconds,
                onPrimaryAction = viewModel::onPrimaryAction,
                onNextSentence = viewModel::skipCurrentSentence,
                onResetLesson = viewModel::resetLessonProgress,
            )
        }
    }
}

sealed class StudyDestination(val route: String) {
    data object LevelList : StudyDestination("levels")

    data object LessonList : StudyDestination("lessons/{${LessonListViewModel.LEVEL_ARGUMENT}}") {
        fun createRoute(level: String): String = "lessons/$level"
    }

    data object Dictation : StudyDestination("dictation/{${DictationViewModel.LESSON_ID_ARGUMENT}}") {
        fun createRoute(lessonId: Long): String = "dictation/$lessonId"
    }
}
