package com.example.ui.widgets.updater

import android.content.Context
import android.os.SystemClock
import androidx.glance.appwidget.updateAll
import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import com.example.ui.widgets.glance.CompactPlayerGlanceWidget
import com.example.ui.widgets.glance.DynamicAlbumGlanceWidget
import com.example.ui.widgets.glance.LyricsGlanceWidget
import com.example.ui.widgets.glance.NowPlayingGlanceWidget
import com.example.ui.widgets.playback.WidgetPlaybackStateAdapter
import com.example.ui.widgets.settings.WidgetSettings
import com.example.ui.widgets.settings.WidgetSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Event-driven widget updates with user-configurable refresh policy. */
object WidgetUpdateManager {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var settings = WidgetSettings()
    private var settingsContext: Context? = null
    private var lastSongId: String? = null
    private var lastIsPlaying: Boolean? = null
    private var lastShuffle: Boolean? = null
    private var lastRepeat: Boolean? = null
    private var lastPositionMs: Long = Long.MIN_VALUE
    private var lastLyricsUpdateAt: Long = 0L
    private var lastPlayerUpdateAt: Long = 0L

    private fun ensureSettings(context: Context) {
        if (settingsContext != null) return
        settingsContext = context.applicationContext
        scope.launch {
            WidgetSettingsStore.settings(context.applicationContext).collect { settings = it }
        }
    }

    fun onPlaybackStateChanged(context: Context, state: PlaybackState) {
        ensureSettings(context)
        WidgetPlaybackStateAdapter.persistPlaybackState(context, state)
        if (!settings.widgetsEnabled) return

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

        if (settings.liveLyricsUpdates && settings.lyricsEnabled && now - lastLyricsUpdateAt >= settings.lyricsRefreshSeconds * 1_000L) {
            lastLyricsUpdateAt = now
            scope.launch { runCatching { LyricsGlanceWidget().updateAll(context) } }
        }

        if (now - lastPlayerUpdateAt >= settings.playerRefreshSeconds * 1_000L) {
            lastPlayerUpdateAt = now
            scope.launch {
                if (settings.nowPlayingEnabled) runCatching { NowPlayingGlanceWidget().updateAll(context) }
                if (settings.miniPlayerEnabled) runCatching { CompactPlayerGlanceWidget().updateAll(context) }
                if (settings.dynamicAlbumEnabled) runCatching { DynamicAlbumGlanceWidget().updateAll(context) }
            }
        }
    }

    private suspend fun updateAllWidgets(context: Context) {
        if (settings.nowPlayingEnabled) runCatching { NowPlayingGlanceWidget().updateAll(context) }
        if (settings.miniPlayerEnabled) runCatching { CompactPlayerGlanceWidget().updateAll(context) }
        if (settings.lyricsEnabled) runCatching { LyricsGlanceWidget().updateAll(context) }
        if (settings.dynamicAlbumEnabled) runCatching { DynamicAlbumGlanceWidget().updateAll(context) }
    }
}
