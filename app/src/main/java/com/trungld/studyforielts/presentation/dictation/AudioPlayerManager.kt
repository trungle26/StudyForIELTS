package com.trungld.studyforielts.presentation.dictation

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@ViewModelScoped
class AudioPlayerManager @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listener = DictationPlayerListener()

    private val _audioState = MutableStateFlow(DictationAudioUiState())
    val audioState: StateFlow<DictationAudioUiState> = _audioState.asStateFlow()

    private var currentAudioUrl: String? = null
    private var currentSegment: AudioSegment? = null
    private var pendingSegment: AudioSegment? = null

    init {
        player.addListener(listener)
        scope.launch {
            monitorLoopWindow()
        }
    }

    fun configureSegment(
        audioUrl: String,
        startMs: Long,
        endMs: Long,
        resumePositionMs: Long,
        shouldAutoPlay: Boolean,
    ) {
        if (audioUrl.isBlank()) {
            clearSegment()
            _audioState.update {
                DictationAudioUiState(
                    isAvailable = false,
                    errorMessage = "Missing audio source for this lesson.",
                )
            }
            return
        }

        val segment = AudioSegment(
            startMs = startMs,
            endMs = endMs,
            resumePositionMs = resumePositionMs,
            shouldAutoPlay = shouldAutoPlay,
        )

        pendingSegment = segment
        if (currentAudioUrl != audioUrl) {
            currentAudioUrl = audioUrl
            currentSegment = null
            _audioState.update {
                it.copy(
                    isAvailable = true,
                    isPrepared = false,
                    isPlaying = false,
                    errorMessage = null,
                    segmentStartMs = startMs,
                    segmentEndMs = endMs,
                )
            }
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            player.prepare()
            return
        }

        if (player.playbackState == Player.STATE_IDLE) {
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            player.prepare()
            return
        }

        if (_audioState.value.isPrepared) {
            applyPendingSegment()
        }
    }

    fun clearSegment() {
        pendingSegment = null
        currentSegment = null
        if (player.isPlaying) {
            player.pause()
        }
        _audioState.update {
            it.copy(
                isPlaying = false,
                currentPositionMs = 0L,
                segmentStartMs = 0L,
                segmentEndMs = 0L,
            )
        }
    }

    fun togglePlayback() {
        val segment = currentSegment ?: pendingSegment ?: return
        if (!_audioState.value.isPrepared) return

        if (player.isPlaying) {
            player.pause()
            updatePlaybackState()
            return
        }

        val safePosition = player.currentPosition.coerceIn(segment.startMs, segment.endMs)
        player.seekTo(safePosition)
        player.play()
        updatePlaybackState()
    }

    fun replaySegment() {
        val segment = currentSegment ?: pendingSegment ?: return
        if (!_audioState.value.isPrepared) return
        player.seekTo(segment.startMs)
        if (!player.isPlaying) {
            player.play()
        }
        updatePlaybackState()
    }

    fun pause() {
        if (player.isPlaying) {
            player.pause()
            updatePlaybackState()
        }
    }

    fun release() {
        pendingSegment = null
        currentSegment = null
        player.removeListener(listener)
        player.release()
        scope.cancel()
    }

    private suspend fun monitorLoopWindow() {
        while (true) {
            val segment = currentSegment
            if (segment != null && _audioState.value.isPrepared) {
                val position = player.currentPosition
                if (player.isPlaying && position >= segment.endMs) {
                    player.seekTo(segment.startMs)
                    player.play()
                }
                _audioState.update {
                    it.copy(
                        isPlaying = player.isPlaying,
                        currentPositionMs = player.currentPosition,
                    )
                }
            }
            delay(150)
        }
    }

    private fun applyPendingSegment() {
        val segment = pendingSegment ?: return
        currentSegment = segment
        pendingSegment = null

        val targetPosition = if (segment.resumePositionMs in segment.startMs..segment.endMs) {
            segment.resumePositionMs
        } else {
            segment.startMs
        }

        player.seekTo(targetPosition)
        if (segment.shouldAutoPlay) {
            player.play()
        } else {
            player.pause()
        }

        _audioState.update {
            it.copy(
                isAvailable = true,
                isPrepared = true,
                isPlaying = player.isPlaying,
                currentPositionMs = targetPosition,
                segmentStartMs = segment.startMs,
                segmentEndMs = segment.endMs,
                errorMessage = null,
            )
        }
    }

    private fun updatePlaybackState() {
        _audioState.update {
            it.copy(
                isAvailable = currentAudioUrl != null,
                isPrepared = player.playbackState == Player.STATE_READY,
                isPlaying = player.isPlaying,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                errorMessage = null,
            )
        }
    }

    private inner class DictationPlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                applyPendingSegment()
            } else {
                updatePlaybackState()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _audioState.update {
                it.copy(
                    isAvailable = currentAudioUrl != null,
                    isPrepared = false,
                    isPlaying = false,
                    errorMessage = error.errorCodeName,
                )
            }
        }
    }

    private data class AudioSegment(
        val startMs: Long,
        val endMs: Long,
        val resumePositionMs: Long,
        val shouldAutoPlay: Boolean,
    )
}
