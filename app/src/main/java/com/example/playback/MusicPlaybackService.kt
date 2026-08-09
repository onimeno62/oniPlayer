package com.example.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlaybackService : MediaSessionService() {
    private lateinit var playbackController: PlaybackController

    override fun onCreate() {
        super.onCreate()
        playbackController = PlaybackController(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = playbackController.mediaSession

    override fun onDestroy() {
        playbackController.release()
        super.onDestroy()
    }
}
