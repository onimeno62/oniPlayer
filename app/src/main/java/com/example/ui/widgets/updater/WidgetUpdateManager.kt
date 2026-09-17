package com.example.ui.widgets.updater

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
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

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val updateMutex = Mutex()

    @Volatile
    private var settings = WidgetSettings()
    @Volatile
    private var settingsContext: Context? = null
    private var settingsJob: Job? = null
    private var pendingUpdateJob: Job? = null
    private var followUpJob: Job? = null

    private var pendingPlayerUpdate = false
    private var pendingLyricsUpdate = false
    private var lastSongId: String? = null
    private var lastIsPlaying: Boolean? = null
    private var lastShuffle: Boolean? = null
    private var lastRepeat: Boolean? = null
    private var lastPositionMs: Long = Long.MIN_VALUE
    private var lastPlayerPublishedPositionMs: Long = Long.MIN_VALUE
    private var lastLyricsPublishedPositionMs: Long = Long.MIN_VALUE

    private fun ensureSettings(context: Context) {
        if (settingsContext != null) return
        settingsContext = context.applicationContext
        settingsJob = scope.launch {
            WidgetSettingsStore.settings(context.applicationContext).collect { next ->
                val wasEnabled = settings.widgetsEnabled
                settings = next
                if (!wasEnabled && next.widgetsEnabled) {
                    requestUpdate(
                        context = context.applicationContext,
                        force = true,
                        player = true,
                        lyrics = next.lyricsEnabled && next.liveLyricsUpdates,
                        followUp = false
                    )
                }
            }
        }
    }

    /** Called by the single playback-state source for every state emission. */
    fun onPlaybackStateChanged(context: Context, state: PlaybackState) {
        ensureSettings(context)
        if (!settings.widgetsEnabled) return

        val songChanged = state.currentSong?.id != lastSongId
        val playStateChanged = state.isPlaying != lastIsPlaying
        val shuffleChanged = state.shuffleEnabled != lastShuffle
        val repeatChanged = (state.repeatMode == RepeatMode.ONE) != lastRepeat
        val positionChanged = state.positionMs != lastPositionMs

        lastSongId = state.currentSong?.id
        lastIsPlaying = state.isPlaying
        lastShuffle = state.shuffleEnabled
        lastRepeat = state.repeatMode == RepeatMode.ONE
        lastPositionMs = state.positionMs

        if (songChanged || playStateChanged || shuffleChanged || repeatChanged) {
            requestUpdate(
                context = context.applicationContext,
                force = true,
                player = true,
                lyrics = settings.lyricsEnabled && settings.liveLyricsUpdates,
                followUp = songChanged
            )
            return
        }

        if (!positionChanged || !state.isPlaying) return

        val playerIntervalMs = settings.playerRefreshSeconds.coerceIn(1, 10) * 1_000L
        val lyricsIntervalMs = settings.lyricsRefreshSeconds.coerceIn(1, 10) * 1_000L

        val playerDrift = if (lastPlayerPublishedPositionMs == Long.MIN_VALUE) {
            Long.MAX_VALUE
        } else {
            kotlin.math.abs(state.positionMs - lastPlayerPublishedPositionMs)
        }

        val lyricsDrift = if (lastLyricsPublishedPositionMs == Long.MIN_VALUE) {
            Long.MAX_VALUE
        } else {
            kotlin.math.abs(state.positionMs - lastLyricsPublishedPositionMs)
        }

        val playerDue = playerDrift >= playerIntervalMs
        val lyricsDue =
            settings.lyricsEnabled &&
                settings.liveLyricsUpdates &&
                lyricsDrift >= lyricsIntervalMs

        if (playerDue || lyricsDue) {
            requestUpdate(
                context = context.applicationContext,
                force = false,
                player = playerDue,
                lyrics = lyricsDue,
                followUp = false
            )
        }
    }

    /** Forces publication and optionally schedules a second artwork/metadata pass. */
    fun requestWithFollowUp(context: Context) {
        ensureSettings(context)
        if (!settings.widgetsEnabled) return
        requestUpdate(
            context = context.applicationContext,
            force = true,
            player = true,
            lyrics = settings.lyricsEnabled,
            followUp = true
        )
    }

    fun cancel() {
        pendingUpdateJob?.cancel()
        followUpJob?.cancel()
        settingsJob?.cancel()
        pendingUpdateJob = null
        followUpJob = null
        pendingPlayerUpdate = false
        pendingLyricsUpdate = false
    }

    private fun requestUpdate(
        context: Context,
        force: Boolean,
        player: Boolean,
        lyrics: Boolean,
        followUp: Boolean
    ) {
        pendingPlayerUpdate = pendingPlayerUpdate || player
        pendingLyricsUpdate = pendingLyricsUpdate || lyrics

        if (force) {
            pendingUpdateJob?.cancel()
            pendingUpdateJob = scope.launch {
                delay(FORCED_DEBOUNCE_MS)
                publishPending(context.applicationContext)
            }
        } else if (pendingUpdateJob?.isActive != true) {
            // Keep one trailing update alive. Playback can emit more frequently than
            // the widget refresh interval; restarting this delay would starve updates.
            pendingUpdateJob = scope.launch {
                delay(NORMAL_DEBOUNCE_MS)
                publishPending(context.applicationContext)
            }
        }

        if (followUp) {
            followUpJob?.cancel()
            followUpJob = scope.launch {
                delay(FOLLOW_UP_DELAY_MS)
                publishNow(
                    context = context.applicationContext,
                    player = true,
                    lyrics = settings.lyricsEnabled
                )
            }
        }
    }

    private suspend fun publishPending(context: Context) {
        val player = pendingPlayerUpdate
        val lyrics = pendingLyricsUpdate
        pendingPlayerUpdate = false
        pendingLyricsUpdate = false

        if (!player && !lyrics) return

        publishNow(context, player, lyrics)
    }

    private suspend fun publishNow(
        context: Context,
        player: Boolean,
        lyrics: Boolean
    ) {
        updateMutex.withLock {
            val widgetState = WidgetPlaybackStateAdapter.fromPlaybackState(
                OniAudioEngine.getInstance(context).state.value
            )
            val startedAt = SystemClock.elapsedRealtime()

            try {
                val manager = GlanceAppWidgetManager(context)

                if (player) {
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
                    if (settings.dynamicAlbumEnabled) {
                        publishForProvider(
                            manager,
                            context,
                            DynamicAlbumGlanceWidget::class.java,
                            widgetState
                        )
                    }
                    lastPlayerPublishedPositionMs = widgetState.positionMs
                }

                if (lyrics && settings.lyricsEnabled) {
                    publishForProvider(
                        manager,
                        context,
                        LyricsGlanceWidget::class.java,
                        widgetState
                    )
                    lastLyricsPublishedPositionMs = widgetState.positionMs
                }

                Log.d(
                    TAG,
                    "Published player=$player lyrics=$lyrics in " +
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
                else -> Unit
            }
        }
    }
}
