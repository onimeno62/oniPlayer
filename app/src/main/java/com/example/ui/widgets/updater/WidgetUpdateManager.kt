package com.example.ui.widgets.updater

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAppWidgetState
import com.example.playback.OniAudioEngine
import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import com.example.ui.widgets.core.OniWidgetPlaybackState
import com.example.ui.widgets.glance.CompactPlayerGlanceWidget
import com.example.ui.widgets.glance.DynamicAlbumGlanceWidget
import com.example.ui.widgets.glance.LyricsGlanceWidget
import com.example.ui.widgets.glance.NowPlayingGlanceWidget
import com.example.ui.widgets.glance.WidgetGlanceState
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
 * Single coordinator between playback state and all placed Glance widget instances.
 *
 * PlaybackController/OniAudioEngine remains the only source of truth. This
 * coordinator converts that state once, writes it into each widget instance's
 * Glance Preferences state, and requests an exact-id render.
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
            WidgetSettingsStore.settings(context.applicationContext).collect {
                settings = it
            }
        }
    }

    fun onPlaybackStateChanged(context: Context, state: PlaybackState) {
        ensureSettings(context)
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
                requestUpdate(context, state, force = true, followUp = songChanged)
            }
            positionChanged && state.isPlaying && positionDrift >= POSITION_DRIFT_THRESHOLD_MS -> {
                requestUpdate(context, state, force = false, followUp = false)
            }
        }

        if (now < 0L) {
            Log.w(TAG, "Unexpected elapsed realtime value: $now")
        }
    }

    fun requestWithFollowUp(context: Context) {
        ensureSettings(context)
        if (!settings.widgetsEnabled) return
        val state = OniAudioEngine.getInstance(context).state.value
        requestUpdate(context, state, force = true, followUp = true)
    }

    fun cancel() {
        pendingUpdateJob?.cancel()
        followUpJob?.cancel()
        settingsJob?.cancel()
    }

    private fun requestUpdate(
        context: Context,
        state: PlaybackState,
        force: Boolean,
        followUp: Boolean
    ) {
        if (force) {
            pendingUpdateJob?.cancel()
            pendingUpdateJob = scope.launch {
                delay(FORCED_DEBOUNCE_MS)
                publishWidgets(
                    context.applicationContext,
                    OniAudioEngine.getInstance(context).state.value
                )
            }
        } else if (pendingUpdateJob?.isActive != true) {
            // Keep one trailing position update alive. Playback emits frequently;
            // restarting the delay for every emission would starve the update forever.
            pendingUpdateJob = scope.launch {
                delay(NORMAL_DEBOUNCE_MS)
                publishWidgets(
                    context.applicationContext,
                    OniAudioEngine.getInstance(context).state.value
                )
            }
        }

        if (followUp) {
            followUpJob?.cancel()
            followUpJob = scope.launch {
                delay(FOLLOW_UP_DELAY_MS)
                publishWidgets(
                    context.applicationContext,
                    OniAudioEngine.getInstance(context).state.value
                )
            }
        }
    }

    private suspend fun publishWidgets(context: Context, state: PlaybackState) {
        updateMutex.withLock {
            val widgetState = WidgetPlaybackStateAdapter.fromPlaybackState(state)
            val startedAt = SystemClock.elapsedRealtime()
            try {
                val manager = GlanceAppWidgetManager(context)
                if (settings.nowPlayingEnabled) {
                    publishForProvider(
                        manager,
                        context,
                        NowPlayingGlanceWidget::class.java,
                        widgetState
                    )
                }
                if (settings.miniPlayerEnabled) {
                    publishForProvider(
                        manager,
                        context,
                        CompactPlayerGlanceWidget::class.java,
                        widgetState
                    )
                }
                if (settings.lyricsEnabled) {
                    publishForProvider(
                        manager,
                        context,
                        LyricsGlanceWidget::class.java,
                        widgetState
                    )
                }
                if (settings.dynamicAlbumEnabled) {
                    publishForProvider(
                        manager,
                        context,
                        DynamicAlbumGlanceWidget::class.java,
                        widgetState
                    )
                }

                lastPublishedPositionMs = lastPositionMs
                Log.d(
                    TAG,
                    "Published exact widget state in " +
                        (SystemClock.elapsedRealtime() - startedAt) + "ms"
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to publish widget state", error)
            }
        }
    }

    private suspend fun <T : GlanceAppWidget> publishForProvider(
        manager: GlanceAppWidgetManager,
        context: Context,
        provider: Class<T>,
        state: OniWidgetPlaybackState
    ) {
        val glanceIds = manager.getGlanceIds(provider)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { preferences ->
                WidgetGlanceState.writeTo(preferences, state)
            }
            when (provider) {
                NowPlayingGlanceWidget::class.java ->
                    NowPlayingGlanceWidget().update(context, glanceId)
                CompactPlayerGlanceWidget::class.java ->
                    CompactPlayerGlanceWidget().update(context, glanceId)
                LyricsGlanceWidget::class.java ->
                    LyricsGlanceWidget().update(context, glanceId)
                DynamicAlbumGlanceWidget::class.java ->
                    DynamicAlbumGlanceWidget().update(context, glanceId)
            }
        }
    }
}
