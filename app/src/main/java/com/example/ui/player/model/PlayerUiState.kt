package com.example.ui.player.model

import com.example.data.entity.SongEntity

/**
 * Immutable UI state for the oniPlayer Player screen.
 * Owned by [com.example.ui.viewmodel.MusicPlayerViewModel].
 *
 * Establishes a clean presentation boundary between playback engine / ViewModel
 * and presentation composables, avoiding direct engine references in the UI layer.
 */
data class PlayerUiState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val isFavorite: Boolean = false,
    val queue: List<SongEntity> = emptyList(),
    val floatingLyricsEnabled: Boolean = false,
    val playbackDelayCountdown: Int? = null,
    val sleepTimerMinutesLeft: Int = 0,
    val isSleepTimerRunning: Boolean = false,
    val isFetchingLyrics: Boolean = false
) {
    val progressFraction: Float
        get() = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val hasSynchronizedLyrics: Boolean
        get() = com.example.ui.lyrics.LyricsHelper.isSynced(currentSong?.lyrics)
}
