package com.example.playback

import androidx.media3.common.Player
import androidx.media3.common.C
import com.example.data.entity.SongEntity

/** Song-weighting strategy applied whenever songs are shuffled. */
enum class ShuffleMode { RANDOM, DISCOVER, FAVORITES_BOOST }

/**
 * Poweramp-style repeat modes.
 *
 * Ordinals are persisted (PlaybackPersistence) and broadcast to the UI process, so new modes are
 * only ever appended. [ALL] = "Repeat list", [ONE] = "Repeat song", [OFF] = play the current list
 * once and stop, [SINGLE] = play the current song once and stop.
 */
enum class RepeatMode {
    ALL, ONE, OFF, SINGLE;

    val label: String
        get() = when (this) {
            OFF -> "Repeat off"
            ALL -> "Repeat list"
            ONE -> "Repeat song"
            SINGLE -> "Play single song"
        }

    val description: String
        get() = when (this) {
            OFF -> "Play the current list once, then stop"
            ALL -> "Start the current list again when it ends"
            ONE -> "Loop the current song"
            SINGLE -> "Stop after the current song"
        }

    /** Tap cycle, matching Poweramp: Off -> List -> Song -> Single song -> Off. */
    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> SINGLE
        SINGLE -> OFF
    }

    companion object {
        /**
         * Maps a Media3 repeat mode back onto the logical mode without losing modes Media3 cannot
         * represent (OFF and SINGLE both use REPEAT_MODE_OFF; ALL uses REPEAT_MODE_OFF while an
         * auto-next delay is armed).
         */
        fun reconcile(playerRepeatMode: Int, current: RepeatMode, keepAllWhenOff: Boolean): RepeatMode =
            when (playerRepeatMode) {
                Player.REPEAT_MODE_ONE -> ONE
                Player.REPEAT_MODE_ALL -> ALL
                else -> when {
                    current == OFF || current == SINGLE -> current
                    current == ALL && keepAllWhenOff -> ALL
                    else -> OFF
                }
            }
    }
}

/**
 * Poweramp-style shuffle scopes. A "category" in oniPlayer is the song's album.
 * Shuffle on/off is still carried by PlaybackState.shuffleEnabled; this only describes *how*.
 * Ordinals are persisted, append only.
 */
enum class ShuffleType {
    SONGS, CATEGORIES, SONGS_AND_CATEGORIES, ALL;

    val label: String
        get() = when (this) {
            SONGS -> "Shuffle songs"
            CATEGORIES -> "Shuffle albums"
            SONGS_AND_CATEGORIES -> "Shuffle songs & albums"
            ALL -> "Shuffle all songs"
        }

    val description: String
        get() = when (this) {
            SONGS -> "Random order inside the current list"
            CATEGORIES -> "Albums in random order, songs in album order"
            SONGS_AND_CATEGORIES -> "Albums in random order, songs shuffled too"
            ALL -> "Everything in your library, fully random"
        }

    companion object {
        /**
         * Tap cycle: Off -> Songs -> Albums -> Songs & albums -> All songs -> Off.
         * Returns null when the next state is "shuffle off".
         */
        fun nextAfter(shuffleEnabled: Boolean, current: ShuffleType): ShuffleType? {
            if (!shuffleEnabled) return SONGS
            return when (current) {
                SONGS -> CATEGORIES
                CATEGORIES -> SONGS_AND_CATEGORIES
                SONGS_AND_CATEGORIES -> ALL
                ALL -> null
            }
        }
    }
}

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
    val queue: List<SongEntity>,
    val shuffleType: ShuffleType = ShuffleType.SONGS,
    val audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
)
