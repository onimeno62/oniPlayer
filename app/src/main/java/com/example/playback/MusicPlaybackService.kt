package com.example.playback

import android.os.Bundle
import android.util.Log
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlaybackService : MediaSessionService() {
    private val TAG = "MusicPlaybackService"
    private var playbackController: PlaybackController? = null

    companion object {
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.d(TAG, "MusicPlaybackService Created")
        playbackController = PlaybackController(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playbackController?.mediaSession
    }

    override fun onDestroy() {
        isServiceRunning = false
        playbackController?.release()
        playbackController = null
        super.onDestroy()
        Log.d(TAG, "MusicPlaybackService Destroyed")
    }
}
