package com.example.ui.widgets.defaultpack.shared

import com.example.ui.widgets.core.OniWidgetPlaybackState

/** What a lyric-first widget should show for the current playback snapshot. */
data class OniLyricFrame(
    val previous: String?,
    val current: String,
    val next: String?,
    /** True when [current] is a real lyric line rather than a state message. */
    val isLyric: Boolean
)

/**
 * Pure, testable text decisions shared by widget renderers.
 * Keeps empty/paused/no-lyrics copy consistent across families.
 */
object OniWidgetCopy {
    fun title(state: OniWidgetPlaybackState): String =
        if (state.isBlank) "Nothing playing" else state.title

    fun artist(state: OniWidgetPlaybackState): String =
        if (state.isBlank) "Tap to open oniPlayer" else state.artist

    fun artistAlbum(state: OniWidgetPlaybackState): String = when {
        state.isBlank -> artist(state)
        state.album.isBlank() -> state.artist
        else -> "${state.artist} · ${state.album}"
    }

    /** Metadata line that also carries paused state in text, not colour alone. */
    fun statusMeta(state: OniWidgetPlaybackState): String = when {
        state.isBlank -> "oniPlayer"
        state.isPlaying -> "${state.title} · ${state.artist}"
        else -> "Paused · ${state.title}"
    }

    fun artworkDescription(state: OniWidgetPlaybackState): String =
        if (state.isBlank) "oniPlayer artwork" else "Album artwork for ${state.title}"

    fun progressDescription(state: OniWidgetPlaybackState): String =
        if (state.durationMs <= 0L) "Playback progress unavailable"
        else "${time(state.positionMs)} of ${time(state.durationMs)}"

    /** Locale-independent m:ss / h:mm:ss so digits never shift width between updates. */
    fun time(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        val h = total / 3600L
        val m = (total % 3600L) / 60L
        val s = total % 60L
        val ss = s.toString().padStart(2, '0')
        return if (h > 0L) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
    }

    fun lyricFrame(state: OniWidgetPlaybackState): OniLyricFrame = when {
        state.isBlank -> OniLyricFrame(null, "Nothing playing", "Start a song to see lyrics here", false)
        !state.hasLyrics -> OniLyricFrame(null, "No lyrics for this song", artistLine(state), false)
        state.activeLyric.isNullOrBlank() -> OniLyricFrame(
            state.previousLyric?.takeIf { it.isNotBlank() },
            "♪  ♪  ♪",
            state.nextLyric?.takeIf { it.isNotBlank() },
            false
        )
        else -> OniLyricFrame(
            state.previousLyric?.takeIf { it.isNotBlank() },
            state.activeLyric.orEmpty(),
            state.nextLyric?.takeIf { it.isNotBlank() },
            true
        )
    }

    private fun artistLine(state: OniWidgetPlaybackState): String = "${state.title} · ${state.artist}"
}

/** Pure progress geometry so tiny fractions stay visible at any width. */
object OniWidgetProgressMath {
    fun fillWidth(fraction: Float, available: Float, minVisible: Float, hasDuration: Boolean): Float {
        if (!hasDuration || available <= 0f) return 0f
        val f = fraction.coerceIn(0f, 1f)
        if (f <= 0f) return 0f
        return (available * f).coerceIn(minOf(minVisible, available), available)
    }
}
