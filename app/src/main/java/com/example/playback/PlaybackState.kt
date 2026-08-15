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
