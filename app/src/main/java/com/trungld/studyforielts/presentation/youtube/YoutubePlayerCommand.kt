package com.trungld.studyforielts.presentation.youtube

sealed interface YoutubePlayerCommand {
    data class LoadVideo(
        val videoId: String,
        val startSeconds: Float,
    ) : YoutubePlayerCommand

    data class SeekTo(
        val seconds: Float,
        val play: Boolean = true,
    ) : YoutubePlayerCommand

    data object Play : YoutubePlayerCommand

    data object Pause : YoutubePlayerCommand
}
