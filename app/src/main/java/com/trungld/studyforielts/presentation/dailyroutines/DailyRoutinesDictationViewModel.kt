package com.trungld.studyforielts.presentation.dailyroutines

import androidx.lifecycle.ViewModel
import com.trungld.studyforielts.presentation.vocabulary.VocabularyTtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DailyRoutinesDictationViewModel @Inject constructor(
    val tts: VocabularyTtsManager,
) : ViewModel() {
    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}
