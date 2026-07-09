package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import coil.Coil
import coil.request.ImageRequest
import coil.target.Target
import com.example.MainActivity
import com.example.R
import com.example.data.entity.SongEntity
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

data class PlaybackStateData(
    val song: SongEntity?,
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long,
    val isShuffle: Boolean,
    val isRepeat: Boolean,
    val isFavorite: Boolean
)

class MusicPlaybackService : Service() {
    private val TAG = "MusicPlaybackService"
    private val NOTIFICATION_ID = 2026
    private val CHANNEL_ID = "oni_player_playback_channel"

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isForeground = false

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.ACTION_PREVIOUS"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.ACTION_TOGGLE_REPEAT"
        const val ACTION_TOGGLE_FAVORITE = "com.example.ACTION_TOGGLE_FAVORITE"
        const val ACTION_STOP = "com.example.ACTION_STOP"

        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.d(TAG, "MusicPlaybackService Created")
        createNotificationChannel()
        setupMediaSession()
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            Log.d(TAG, "onStartCommand Action: $action")
            val viewModel = MusicPlayerViewModel.activeInstance
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    viewModel?.togglePlayPause()
                }
                ACTION_NEXT -> {
                    viewModel?.skipNext()
                }
                ACTION_PREVIOUS -> {
                    viewModel?.skipPrevious()
                }
                ACTION_TOGGLE_SHUFFLE -> {
                    viewModel?.toggleShuffle()
                }
                ACTION_TOGGLE_REPEAT -> {
                    viewModel?.toggleRepeat()
                }
                ACTION_TOGGLE_FAVORITE -> {
                    viewModel?.let { vm ->
                        vm.audioEngine.currentSong.value?.let { song ->
                            vm.toggleFavorite(song.id)
                        }
                    }
                }
                ACTION_STOP -> {
                    viewModel?.audioEngine?.stop()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Oni Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and status of the current playing track"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OniPlayerMediaSession").apply {
            isActive = true
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    MusicPlayerViewModel.activeInstance?.togglePlayPause()
                }

                override fun onPause() {
                    MusicPlayerViewModel.activeInstance?.togglePlayPause()
                }

                override fun onSkipToNext() {
                    MusicPlayerViewModel.activeInstance?.skipNext()
                }

                override fun onSkipToPrevious() {
                    MusicPlayerViewModel.activeInstance?.skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    MusicPlayerViewModel.activeInstance?.audioEngine?.seekTo(pos)
                }
            })
        }
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            var viewModel = MusicPlayerViewModel.activeInstance
            while (viewModel == null) {
                delay(500)
                viewModel = MusicPlayerViewModel.activeInstance
            }

            val engine = viewModel.audioEngine

            combine(
                engine.currentSong,
                engine.isPlaying,
                engine.position,
                engine.duration,
                viewModel.isShuffle,
                viewModel.isRepeat,
                viewModel.favoriteSongs
            ) { array ->
                val currentSong = array[0] as? SongEntity
                val isPlaying = array[1] as Boolean
                val position = array[2] as Long
                val duration = array[3] as Long
                val isShuffle = array[4] as Boolean
                val isRepeat = array[5] as Boolean
                @Suppress("UNCHECKED_CAST")
                val favoriteSongs = array[6] as List<SongEntity>
                val isFav = currentSong?.let { song -> favoriteSongs.any { it.id == song.id } } ?: false
                PlaybackStateData(currentSong, isPlaying, position, duration, isShuffle, isRepeat, isFav)
            }.collectLatest { stateData ->
                updateNotification(stateData)
            }
        }
    }

    private fun updateNotification(state: PlaybackStateData) {
        val song = state.song
        if (song == null) {
            stopForeground(true)
            isForeground = false
            OniWidgetUpdater.updateAllWidgets(this, null, false, 0L)
            return
        }

        // Update sleek widgets in real time
        OniWidgetUpdater.updateAllWidgets(this, song, state.isPlaying, state.position)

        // Update MediaSession state
        val playbackStateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )
            .setState(
                if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                state.position,
                1.0f
            )

        mediaSession?.setPlaybackState(playbackStateBuilder.build())

        // Update MediaSession metadata
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.customTitle ?: song.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.customArtist ?: song.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.customAlbum ?: song.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)

        mediaSession?.setMetadata(metadataBuilder.build())

        // Load album art bitmap asynchronously or use default
        val artUri = song.albumArtUri
        if (!artUri.isNullOrEmpty()) {
            val request = ImageRequest.Builder(this)
                .data(artUri)
                .allowHardware(false)
                .target(object : Target {
                    override fun onSuccess(result: Drawable) {
                        val bitmap = (result as? BitmapDrawable)?.bitmap
                        buildAndShowNotification(state, bitmap)
                    }

                    override fun onError(error: Drawable?) {
                        buildAndShowNotification(state, null)
                    }
                })
                .build()
            Coil.imageLoader(this).enqueue(request)
        } else {
            buildAndShowNotification(state, null)
        }
    }

    private fun buildAndShowNotification(state: PlaybackStateData, albumArtBitmap: Bitmap?) {
        val song = state.song ?: return

        // PendingIntent to launch MainActivity
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Pending intents for media buttons
        val prevPendingIntent = PendingIntent.getService(
            this, 1, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPausePendingIntent = PendingIntent.getService(
            this, 2, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextPendingIntent = PendingIntent.getService(
            this, 3, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val shufflePendingIntent = PendingIntent.getService(
            this, 4, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_SHUFFLE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val favoritePendingIntent = PendingIntent.getService(
            this, 5, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_FAVORITE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPendingIntent = PendingIntent.getService(
            this, 6, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build native notification
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setSmallIcon(R.drawable.ic_play_arrow)
            .setContentTitle(song.customTitle ?: song.title)
            .setContentText(song.customArtist ?: song.artist)
            .setSubText(song.customAlbum ?: song.album)
            .setLargeIcon(albumArtBitmap)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)

        // Set native MediaStyle on native builder
        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(1, 2, 3) // Previous, Play/Pause, Next

        builder.setStyle(mediaStyle)

        // Add action buttons in order: Shuffle, Previous, Play/Pause, Next, Favorite
        // 0. Shuffle Action
        val shuffleIcon = R.drawable.ic_shuffle
        builder.addAction(Notification.Action.Builder(shuffleIcon, "Shuffle", shufflePendingIntent).build())

        // 1. Previous Action
        builder.addAction(Notification.Action.Builder(R.drawable.ic_skip_previous, "Previous", prevPendingIntent).build())

        // 2. Play/Pause Action
        val playPauseIcon = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseText = if (state.isPlaying) "Pause" else "Play"
        builder.addAction(Notification.Action.Builder(playPauseIcon, playPauseText, playPausePendingIntent).build())

        // 3. Next Action
        builder.addAction(Notification.Action.Builder(R.drawable.ic_skip_next, "Next", nextPendingIntent).build())

        // 4. Favorite Action
        val favIcon = if (state.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        builder.addAction(Notification.Action.Builder(favIcon, "Favorite", favoritePendingIntent).build())

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForeground = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        mediaSession?.release()
        serviceScope.cancel()
        Log.d(TAG, "MusicPlaybackService Destroyed")
    }
}
