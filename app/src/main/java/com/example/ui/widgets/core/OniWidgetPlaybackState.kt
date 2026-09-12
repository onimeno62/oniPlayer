package com.example.ui.widgets.core

/**
 * Read-only, immutable widget playback state tailored for widget rendering.
 * Does not depend on ViewModels or ExoPlayer directly.
 */
data class OniWidgetPlaybackState(
    val songId: String? = null,
    val title: String = "No track playing",
    val artist: String = "oniPlayer",
    val album: String = "",
    val albumArtworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val activeLyric: String? = null,
    val previousLyric: String? = null,
    val nextLyric: String? = null,
    val hasLyrics: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isBlank: Boolean
        get() = songId == null
}
