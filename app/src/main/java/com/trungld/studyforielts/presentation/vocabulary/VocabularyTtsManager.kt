package com.trungld.studyforielts.presentation.vocabulary

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.Locale
import javax.inject.Inject

@ViewModelScoped
class VocabularyTtsManager @Inject constructor(
    @ApplicationContext context: Context,
) : TextToSpeech.OnInitListener {

    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private var pendingWord: String? = null

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (!isReady) return

        val preferredLocale = when {
            textToSpeech.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE -> Locale.US
            textToSpeech.isLanguageAvailable(Locale.UK) >= TextToSpeech.LANG_AVAILABLE -> Locale.UK
            else -> Locale.getDefault()
        }

        textToSpeech.language = preferredLocale
        textToSpeech.setSpeechRate(0.92f)

        pendingWord?.let(::speak)
        pendingWord = null
    }

    fun speak(word: String) {
        if (!isReady) {
            pendingWord = word
            return
        }
        textToSpeech.speak(
            word,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "vocabulary_$word",
        )
    }

    fun shutdown() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
