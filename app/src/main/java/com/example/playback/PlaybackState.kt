package com.example.playback

import com.example.data.entity.SongEntity

enum class ShuffleMode { RANDOM, DISCOVER, FAVORITES_BOOST }
enum class RepeatMode { ALL, ONE }

data class PlaybackState(
    val currentSong: SongEntity?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedPositionMs: Long,
    val beatEnergy: Float,
    val isPreparing: Boolean,
    val autoNextCountdownSeconds: Int?,
    val shuffleEnabled: Boolean,
    val shuffleMode: ShuffleMode,
    val repeatMode: RepeatMode,
    val queue: List<SongEntity>
)

sealed interface PlaybackCommand {
    data class Play(val song: SongEntity) : PlaybackCommand
    data object Pause : PlaybackCommand
    data object Resume : PlaybackCommand
    data class SeekTo(val positionMs: Long) : PlaybackCommand
    data object Next : PlaybackCommand
    data object Previous : PlaybackCommand
    data object TogglePlayPause : PlaybackCommand
    data class SetQueue(val songs: List<SongEntity>, val startIndex: Int) : PlaybackCommand
    data class SetShuffle(val enabled: Boolean, val mode: ShuffleMode) : PlaybackCommand
    data class SetRepeat(val mode: RepeatMode) : PlaybackCommand
}
