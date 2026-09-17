package com.example.ui.widgets.updater

import android.content.Context
import android.os.SystemClock
import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates event-driven Glance updates.
 *
 * The playback service remains the source of truth. This manager persists the
 * latest snapshot, coalesces bursts of playback callbacks, serializes Glance
 * update work, and performs a delayed follow-up for late metadata/artwork.
 */
object WidgetUpdateManager {
    private const val TAG = "OniWidgetUpdates"
    private const val FORCED_DEBOUNCE_MS = 150L
    private const val NORMAL_DEBOUNCE_MS = 350L
    private const val FOLLOW_UP_DELAY_MS = 500L
    private const val POSITION_DRIFT_THRESHOLD_MS = 2_000L

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val updateMutex = Mutex()

    @Volatile
    private var settings = WidgetSettings()
    @Volatile
    private var settingsContext: Context? = null
    private var settingsJob: Job? = null
    private var pendingUpdateJob: Job? = null
    private var followUpJob: Job? = null

    private var lastSongId: String? = null
    private var lastIsPlaying: Boolean? = null
    private var lastShuffle: Boolean? = null
    private var lastRepeat: Boolean? = null
    private var lastPositionMs: Long = Long.MIN_VALUE
    private var lastPublishedPositionMs: Long = Long.MIN_VALUE

    private fun ensureSettings(context: Context) {
        if (settingsContext != null) return
        settingsContext = context.applicationContext
        settingsJob = scope.launch {
            WidgetSettingsStore.settings(context.applicationContext).collect { settings = it }
        }
    }

    /** Called by the playback service for every playback-state emission. */
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
        val positionDrift = if (lastPublishedPositionMs == Long.MIN_VALUE) {
            Long.MAX_VALUE
        } else {
            kotlin.math.abs(state.positionMs - lastPublishedPositionMs)
        }

        lastSongId = state.currentSong?.id
        lastIsPlaying = state.isPlaying
        lastShuffle = state.shuffleEnabled
        lastRepeat = state.repeatMode == RepeatMode.ONE
        lastPositionMs = state.positionMs

        when {
            songChanged || playStateChanged || shuffleChanged || repeatChanged -> {
                requestUpdate(context, force = true, followUp = songChanged)
            }
            positionChanged && state.isPlaying && positionDrift >= POSITION_DRIFT_THRESHOLD_MS -> {
                requestUpdate(context, force = false, followUp = false)
            }
        }

        if (now < 0L) {
            Log.w(TAG, "Unexpected elapsed realtime value: $now")
        }
    }

    /** Forces an update and optionally schedules a second pass for late artwork. */
    fun requestWithFollowUp(context: Context) {
        ensureSettings(context)
        if (!settings.widgetsEnabled) return
        requestUpdate(context, force = true, followUp = true)
    }

    /** Cancels pending work. Call this when the playback service is destroyed. */
    fun cancel() {
        pendingUpdateJob?.cancel()
        followUpJob?.cancel()
        settingsJob?.cancel()
    }

    private fun requestUpdate(context: Context, force: Boolean, followUp: Boolean) {
        pendingUpdateJob?.cancel()
        pendingUpdateJob = scope.launch {
            delay(if (force) FORCED_DEBOUNCE_MS else NORMAL_DEBOUNCE_MS)
            publishWidgets(context.applicationContext)
        }

        if (followUp) {
            followUpJob?.cancel()
            followUpJob = scope.launch {
                delay(FOLLOW_UP_DELAY_MS)
                publishWidgets(context.applicationContext)
            }
        }
    }

    private suspend fun publishWidgets(context: Context) {
        updateMutex.withLock {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                if (settings.nowPlayingEnabled) {
                    NowPlayingGlanceWidget().updateAll(context)
                }
                if (settings.miniPlayerEnabled) {
                    CompactPlayerGlanceWidget().updateAll(context)
                }
                if (settings.lyricsEnabled) {
                    LyricsGlanceWidget().updateAll(context)
                }
                if (settings.dynamicAlbumEnabled) {
                    DynamicAlbumGlanceWidget().updateAll(context)
                }
                lastPublishedPositionMs = lastPositionMs
                Log.d(TAG, "Published widget updates in ${SystemClock.elapsedRealtime() - startedAt}ms")
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to publish widget updates", error)
            }
        }
    }
}
