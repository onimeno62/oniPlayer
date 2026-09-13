package com.example.ui.widgets.updater

import android.content.Context
import android.os.SystemClock
import androidx.glance.appwidget.updateAll
import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import com.example.ui.widgets.glance.CompactPlayerGlanceWidget
import com.example.ui.widgets.glance.LyricsGlanceWidget
import com.example.ui.widgets.glance.NowPlayingGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps launcher widgets synchronized with meaningful playback changes without
 * running a continuous high-frequency polling loop.
 *
 * Lyrics are refreshed approximately once per second while playing so the
 * active line follows synchronized lyrics. Visual player widgets refresh less
 * frequently because their progress is only a snapshot in an AppWidget.
 */
object WidgetUpdateManager {
    private const val LYRICS_REFRESH_MS = 1_000L
    private const val PLAYER_REFRESH_MS = 2_000L

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var lastSongId: String? = null
    private var lastIsPlaying: Boolean? = null
    private var lastShuffle: Boolean? = null
    private var lastRepeat: Boolean? = null
    private var lastPositionMs: Long = Long.MIN_VALUE
    private var lastLyricsUpdateAt: Long = 0L
    private var lastPlayerUpdateAt: Long = 0L

    fun onPlaybackStateChanged(context: Context, state: PlaybackState) {
        val now = SystemClock.elapsedRealtime()
        val songChanged = state.currentSong?.id != lastSongId
        val playStateChanged = state.isPlaying != lastIsPlaying
        val shuffleChanged = state.shuffleEnabled != lastShuffle
        val repeatChanged = (state.repeatMode == RepeatMode.ONE) != lastRepeat
        val positionChanged = state.positionMs != lastPositionMs

        if (songChanged || playStateChanged || shuffleChanged || repeatChanged) {
            lastSongId = state.currentSong?.id
            lastIsPlaying = state.isPlaying
            lastShuffle = state.shuffleEnabled
            lastRepeat = state.repeatMode == RepeatMode.ONE
            lastPositionMs = state.positionMs
            lastLyricsUpdateAt = now
            lastPlayerUpdateAt = now

            scope.launch { updateAllWidgets(context) }
            return
        }

        if (!positionChanged || !state.isPlaying) return
        lastPositionMs = state.positionMs

        if (now - lastLyricsUpdateAt >= LYRICS_REFRESH_MS) {
            lastLyricsUpdateAt = now
            scope.launch {
                runCatching { LyricsGlanceWidget().updateAll(context) }
            }
        }

        if (now - lastPlayerUpdateAt >= PLAYER_REFRESH_MS) {
            lastPlayerUpdateAt = now
            scope.launch {
                runCatching {
                    NowPlayingGlanceWidget().updateAll(context)
                    CompactPlayerGlanceWidget().updateAll(context)
                }
            }
        }
    }

    private suspend fun updateAllWidgets(context: Context) {
        runCatching {
            NowPlayingGlanceWidget().updateAll(context)
            CompactPlayerGlanceWidget().updateAll(context)
            LyricsGlanceWidget().updateAll(context)
        }
    }
}
