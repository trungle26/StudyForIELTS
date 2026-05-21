package com.trungld.studyforielts.presentation.dictation

data class DictationAudioUiState(
    val isAvailable: Boolean = false,
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val segmentStartMs: Long = 0L,
    val segmentEndMs: Long = 0L,
    val errorMessage: String? = null,
)
