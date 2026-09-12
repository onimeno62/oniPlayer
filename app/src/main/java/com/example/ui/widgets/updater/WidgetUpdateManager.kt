package com.example.ui.widgets.updater

import android.content.Context
import com.example.playback.PlaybackState
import com.example.ui.widgets.glance.CompactPlayerGlanceWidget
import com.example.ui.widgets.glance.LyricsGlanceWidget
import com.example.ui.widgets.glance.NowPlayingGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages low-frequency, event-driven updates for Glance widgets on meaningful playback changes.
 * Avoids any battery-draining continuous polling loop.
 */
object WidgetUpdateManager {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var lastSongId: String? = null
    private var lastIsPlaying: Boolean? = null
    private var lastShuffle: Boolean? = null
    private var lastRepeat: Boolean? = null

    fun onPlaybackStateChanged(context: Context, state: PlaybackState) {
        val songChanged = state.currentSong?.id != lastSongId
        val playStateChanged = state.isPlaying != lastIsPlaying
        val shuffleChanged = state.shuffleEnabled != lastShuffle
        val repeatChanged = (state.repeatMode == com.example.playback.RepeatMode.ONE) != lastRepeat

        if (songChanged || playStateChanged || shuffleChanged || repeatChanged) {
            lastSongId = state.currentSong?.id
            lastIsPlaying = state.isPlaying
            lastShuffle = state.shuffleEnabled
            lastRepeat = state.repeatMode == com.example.playback.RepeatMode.ONE

            scope.launch {
                try {
                    NowPlayingGlanceWidget().updateAll(context)
                    CompactPlayerGlanceWidget().updateAll(context)
                    LyricsGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    // Safe handling during background execution
                }
            }
        }
    }
}
