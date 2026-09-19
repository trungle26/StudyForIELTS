package com.trungld.studyforielts.presentation.dailyroutines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trungld.studyforielts.R
import com.trungld.studyforielts.data.local.entity.TopicVocabularyEntity
import com.trungld.studyforielts.data.local.entity.VocabularyProgressEntity
import com.trungld.studyforielts.domain.model.DAILY_ROUTINES_A1_DICTATION
import com.trungld.studyforielts.domain.model.DAILY_ROUTINES_PRESENT_SIMPLE_LESSON
import com.trungld.studyforielts.domain.model.DailyRoutinesActivity
import com.trungld.studyforielts.domain.model.DictationProgress
import com.trungld.studyforielts.domain.model.GrammarProgress
import com.trungld.studyforielts.domain.model.answerDictationSentence
import com.trungld.studyforielts.domain.model.answerGrammarExercise
import com.trungld.studyforielts.domain.model.checkDailyRoutinesAnswer
import com.trungld.studyforielts.presentation.vocabulary.VocabularyTtsManager

@Composable
fun DailyRoutinesActivityScreen(
    activity: DailyRoutinesActivity,
    vocabulary: List<TopicVocabularyEntity>,
    vocabProgress: List<VocabularyProgressEntity>,
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
    onMarkWord: (String, Boolean) -> Unit,
    tts: VocabularyTtsManager,
) {
    when (activity) {
        DailyRoutinesActivity.VOCABULARY -> VocabularyActivityContent(
            vocabulary = vocabulary,
            progress = vocabProgress,
            onBackClick = onBackClick,
            onCompleted = onCompleted,
            onMarkWord = onMarkWord,
        )
        DailyRoutinesActivity.REVIEW -> ReviewActivityContent(
            vocabulary = vocabulary,
            progress = vocabProgress,
            onBackClick = onBackClick,
            onCompleted = onCompleted,
            onMarkWord = onMarkWord,
        )
        DailyRoutinesActivity.GRAMMAR -> GrammarActivityContent(
            onBackClick = onBackClick,
            onCompleted = onCompleted,
        )
        DailyRoutinesActivity.DICTATION -> DictationActivityContent(
            onBackClick = onBackClick,
            onCompleted = onCompleted,
            tts = tts,
        )
        else -> AnswerActivityContent(
            activity = activity,
            onBackClick = onBackClick,
            onCompleted = onCompleted,
        )
    }
}

@Composable
private fun VocabularyActivityContent(
    vocabulary: List<TopicVocabularyEntity>,
    progress: List<VocabularyProgressEntity>,
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
    onMarkWord: (String, Boolean) -> Unit,
) {
    val progressMap = remember(progress) { progress.associateBy { it.vocabularyId } }
    var currentIndex by remember { mutableIntStateOf(0) }
    val learnedCount = progress.count { it.learned }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.daily_routines_back)) }
        Text(stringResource(R.string.daily_routines_vocabulary), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.daily_routines_vocabulary_prompt), style = MaterialTheme.typography.bodyLarge)

        if (vocabulary.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Loading vocabulary" },
            )
        } else {
            Text(
                stringResource(R.string.topic_vocab_progress, learnedCount, vocabulary.size),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (learnedCount == vocabulary.size) {
                Text(
                    stringResource(R.string.topic_vocab_all_learned),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = { onCompleted(); onBackClick() }) {
                    Text(stringResource(R.string.daily_routines_done))
                }
            } else {
                val safeIndex = currentIndex.coerceIn(0, vocabulary.size - 1)
                val word = vocabulary[safeIndex]
                val wordProgress = progressMap[word.id]

                VocabularyCard(word = word, isLearned = wordProgress?.learned == true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            onMarkWord(word.id, false)
                            if (safeIndex < vocabulary.size - 1) currentIndex = safeIndex + 1
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.topic_vocab_still_learning)) }
                    Button(
                        onClick = {
                            onMarkWord(word.id, true)
                            if (safeIndex < vocabulary.size - 1) currentIndex = safeIndex + 1
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.topic_vocab_know_it)) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = { currentIndex = (safeIndex - 1).coerceAtLeast(0) },
                        enabled = safeIndex > 0,
                    ) { Text(stringResource(R.string.topic_vocab_previous)) }
                    Text(
                        "${safeIndex + 1} / ${vocabulary.size}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    TextButton(
                        onClick = { currentIndex = (safeIndex + 1).coerceAtMost(vocabulary.size - 1) },
                        enabled = safeIndex < vocabulary.size - 1,
                    ) { Text(stringResource(R.string.topic_vocab_next)) }
                }
            }
        }
    }
}

@Composable
private fun ReviewActivityContent(
    vocabulary: List<TopicVocabularyEntity>,
    progress: List<VocabularyProgressEntity>,
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
    onMarkWord: (String, Boolean) -> Unit,
) {
    val progressMap = remember(progress) { progress.associateBy { it.vocabularyId } }
    val learnedWords = remember(vocabulary, progress) {
        vocabulary.filter { progressMap[it.id]?.learned == true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.daily_routines_back)) }
        Text(stringResource(R.string.daily_routines_review), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.daily_routines_review_prompt), style = MaterialTheme.typography.bodyLarge)

        if (vocabulary.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Loading review" },
            )
        } else if (learnedWords.isEmpty()) {
            Text(
                stringResource(R.string.topic_vocab_review_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                stringResource(R.string.topic_vocab_review_count, learnedWords.size),
                style = MaterialTheme.typography.bodyMedium,
            )
            learnedWords.forEach { word ->
                VocabularyCard(word = word, isLearned = true)
                OutlinedButton(onClick = { onMarkWord(word.id, false) }) {
                    Text(stringResource(R.string.topic_vocab_mark_for_review))
                }
            }
            Button(onClick = { onCompleted(); onBackClick() }) {
                Text(stringResource(R.string.daily_routines_done))
            }
        }
    }
}

@Composable
private fun VocabularyCard(word: TopicVocabularyEntity, isLearned: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${word.word}: ${word.meaning}" },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(word.word, style = MaterialTheme.typography.titleLarge)
            if (word.phonetic.isNotBlank()) {
                Text(word.phonetic, style = MaterialTheme.typography.bodyMedium)
            }
            Text(word.meaning, style = MaterialTheme.typography.bodyLarge)
            Text(word.exampleSentence, style = MaterialTheme.typography.bodyMedium)
            if (isLearned) {
                Text(
                    stringResource(R.string.topic_vocab_learned_badge),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun GrammarActivityContent(
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
) {
    val lesson = DAILY_ROUTINES_PRESENT_SIMPLE_LESSON
    var progress by remember { mutableStateOf(GrammarProgress()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var feedbackCorrect by remember { mutableStateOf<Boolean?>(null) }
    val exercise = lesson.exercises[progress.exerciseIndex.coerceAtMost(lesson.exercises.lastIndex)]

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.daily_routines_back)) }
        Text(stringResource(R.string.daily_routines_grammar), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.daily_routines_grammar_explanation), style = MaterialTheme.typography.bodyLarge)
        if (progress.completed) {
            Text(stringResource(R.string.daily_routines_grammar_complete), style = MaterialTheme.typography.titleMedium)
            Button(onClick = { onCompleted(); onBackClick() }) { Text(stringResource(R.string.daily_routines_done)) }
        } else {
            Text(stringResource(R.string.daily_routines_grammar_progress, progress.exerciseIndex + 1, lesson.exercises.size))
            Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = exercise.prompt }) {
                Text(exercise.prompt, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
            exercise.options.forEach { option ->
                OutlinedButton(
                    onClick = {
                        selectedAnswer = option
                        feedbackCorrect = option.trim().equals(exercise.answer.trim(), ignoreCase = true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(option) }
            }
            feedbackCorrect?.let { correct ->
                Text(
                    stringResource(if (correct) R.string.daily_routines_grammar_correct else R.string.daily_routines_grammar_try_again),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (correct) {
                    Text(exercise.explanation, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        progress = answerGrammarExercise(lesson, progress, selectedAnswer.orEmpty())
                        selectedAnswer = null
                        feedbackCorrect = null
                    }) { Text(stringResource(if (progress.exerciseIndex == lesson.exercises.lastIndex) R.string.daily_routines_done else R.string.daily_routines_grammar_next)) }
                } else {
                    Text(exercise.explanation, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { selectedAnswer = null; feedbackCorrect = null }) {
                        Text(stringResource(R.string.daily_routines_grammar_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerActivityContent(
    activity: DailyRoutinesActivity,
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var correct by remember { mutableStateOf(false) }
    val prompt = when (activity) {
        DailyRoutinesActivity.GRAMMAR -> R.string.daily_routines_grammar_prompt
        DailyRoutinesActivity.DICTATION -> R.string.daily_routines_dictation_prompt
        DailyRoutinesActivity.WRITING -> R.string.daily_routines_writing_prompt
        else -> R.string.daily_routines_vocabulary_prompt
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.daily_routines_back)) }
        Text(stringResource(activity.titleRes()), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(prompt), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; submitted = false; correct = false },
            label = { Text(stringResource(R.string.daily_routines_answer_label)) },
            modifier = Modifier.fillMaxSize(),
        )
        Button(onClick = {
            submitted = true
            correct = checkDailyRoutinesAnswer(activity, answer)
        }, enabled = answer.isNotBlank()) {
            Text(stringResource(R.string.daily_routines_check))
        }
        if (submitted) {
            Text(stringResource(if (correct) R.string.daily_routines_answer_correct else R.string.daily_routines_answer_try_again))
            if (correct) Button(onClick = { onCompleted(); onBackClick() }) { Text(stringResource(R.string.daily_routines_done)) }
        }
    }
}

private fun DailyRoutinesActivity.titleRes(): Int = when (this) {
    DailyRoutinesActivity.GRAMMAR -> R.string.daily_routines_grammar
    DailyRoutinesActivity.WRITING -> R.string.daily_routines_writing
    DailyRoutinesActivity.REVIEW -> R.string.daily_routines_review
    DailyRoutinesActivity.VOCABULARY -> R.string.daily_routines_vocabulary
    DailyRoutinesActivity.DICTATION -> R.string.daily_routines_dictation
}

private enum class DictationFeedback { CORRECT, INCORRECT }

@Composable
private fun DictationActivityContent(
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
    tts: VocabularyTtsManager,
) {
    var progress by remember { mutableStateOf(DictationProgress()) }
    var answer by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<DictationFeedback?>(null) }
    val sentences = DAILY_ROUTINES_A1_DICTATION
    val sentence = sentences.getOrNull(progress.sentenceIndex)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.daily_routines_back)) }
        Text(stringResource(R.string.daily_routines_dictation), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.daily_routines_dictation_prompt), style = MaterialTheme.typography.bodyLarge)
        if (progress.completed) {
            Text(stringResource(R.string.daily_routines_dictation_complete), style = MaterialTheme.typography.titleMedium)
            Button(onClick = { onCompleted(); onBackClick() }) { Text(stringResource(R.string.daily_routines_done)) }
        } else if (sentence != null) {
            val playLabel = "${stringResource(R.string.daily_routines_dictation_play)} ${progress.sentenceIndex + 1}"
            Text(stringResource(R.string.daily_routines_dictation_progress, progress.sentenceIndex + 1, sentences.size))
            OutlinedButton(
                onClick = { tts.speak(sentence) },
                modifier = Modifier.semantics { contentDescription = playLabel },
            ) { Text(stringResource(R.string.daily_routines_dictation_play)) }
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it; feedback = null },
                label = { Text(stringResource(R.string.daily_routines_answer_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(onClick = {
                val next = answerDictationSentence(progress, sentences, answer)
                feedback = if (next == progress) DictationFeedback.INCORRECT else DictationFeedback.CORRECT
                if (next != progress) { progress = next; answer = "" }
            }, enabled = answer.isNotBlank()) { Text(stringResource(R.string.daily_routines_check)) }
            feedback?.let { result ->
                val feedbackText = stringResource(if (result == DictationFeedback.CORRECT) R.string.daily_routines_dictation_correct else R.string.daily_routines_dictation_try_again)
                Text(feedbackText, modifier = Modifier.semantics { contentDescription = feedbackText })
            }
        }
    }
}
