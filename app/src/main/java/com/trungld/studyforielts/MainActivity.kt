package com.trungld.studyforielts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.trungld.studyforielts.data.bootstrap.DictationSampleSeeder
import com.trungld.studyforielts.navigation.StudyForIeltsNavGraph
import com.trungld.studyforielts.ui.theme.StudyForIELTSTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dictationSampleSeeder: DictationSampleSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            dictationSampleSeeder.seedIfNeeded()
        }

        setContent {
            StudyForIELTSTheme {
                StudyForIeltsNavGraph()
            }
        }
    }
}
