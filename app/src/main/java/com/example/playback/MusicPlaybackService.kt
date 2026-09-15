package com.example.playback

import android.util.Log
import androidx.media3.common.C
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.ui.widgets.updater.WidgetUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaSessionService() {
    private val TAG = "MusicPlaybackService"
    private var playbackController: PlaybackController? = null
    private val widgetUpdateScope = CoroutineScope(Dispatchers.Default + Job())

    companion object {
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.d(TAG, "MusicPlaybackService Created")
        playbackController = PlaybackController(this)

        // Feed the widget adapter directly from the service-owned playback source of truth.
        // The manager deduplicates structural changes and throttles progress/lyrics updates,
        // so this observer stays responsive without causing high-frequency AppWidget renders.
        widgetUpdateScope.launch {
            while (isActive) {
                playbackController?.let { controller ->
                    val player = controller.player
                    val current = controller.state.value
                    val duration = player.duration
                        .takeIf { it != C.TIME_UNSET && it >= 0L }
                        ?: current.durationMs
                    val snapshot = current.copy(
                        isPlaying = player.isPlaying,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = duration,
                        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                    )
                    WidgetUpdateManager.onPlaybackStateChanged(this@MusicPlaybackService, snapshot)
                }
                delay(250L)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playbackController?.mediaSession
    }

    override fun onDestroy() {
        isServiceRunning = false
        widgetUpdateScope.cancel()
        playbackController?.release()
        playbackController = null
        super.onDestroy()
        Log.d(TAG, "MusicPlaybackService Destroyed")
    }
}
