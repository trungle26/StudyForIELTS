package com.trungld.studyforielts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.trungld.studyforielts.data.bootstrap.DictationSampleSeeder
import com.trungld.studyforielts.presentation.dictation.DictationRoute
import com.trungld.studyforielts.presentation.dictation.DictationViewModel
import com.trungld.studyforielts.ui.theme.StudyForIELTSTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var dictationSampleSeeder: DictationSampleSeeder

    private val viewModel: DictationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            dictationSampleSeeder.seedIfNeeded()
            viewModel.loadLesson(DictationSampleSeeder.SAMPLE_LESSON_ID)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            StudyForIELTSTheme {
                DictationRoute(
                    uiState = uiState,
                    onDraftChanged = viewModel::onDraftChanged,
                    onPlaybackPositionChanged = viewModel::onPlaybackPositionChanged,
                    onPrimaryAction = viewModel::onPrimaryAction,
                    onNextSentence = viewModel::skipCurrentSentence,
                    onResetLesson = viewModel::resetLessonProgress,
                )
            }
        }
    }
}
