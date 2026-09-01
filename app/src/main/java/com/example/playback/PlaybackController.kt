package com.example.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.data.database.OniDatabase
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Service-owned playback engine. The Activity and ViewModels never own ExoPlayer. */
class PlaybackController(private val service: MediaSessionService) {
    companion object {
        const val SET_QUEUE = "com.example.oniplayer.SET_QUEUE"
        const val PLAY_SONG = "com.example.oniplayer.PLAY_SONG"
        const val SET_SHUFFLE = "com.example.oniplayer.SET_SHUFFLE"
        const val ADD_TO_QUEUE = "com.example.oniplayer.ADD_TO_QUEUE"
        const val PLAY_NEXT = "com.example.oniplayer.PLAY_NEXT"
        const val UPDATE_SONG = "com.example.oniplayer.UPDATE_SONG"
        const val TOGGLE_FAVORITE = "com.example.oniplayer.TOGGLE_FAVORITE"
        const val EQ_BAND = "com.example.oniplayer.EQ_BAND"
        const val BASS = "com.example.oniplayer.BASS"
        const val VIRTUALIZER = "com.example.oniplayer.VIRTUALIZER"
        const val PRESET = "com.example.oniplayer.PRESET"
        const val SET_DELAY = "com.example.oniplayer.SET_DELAY"
        const val CANCEL_DELAY = "com.example.oniplayer.CANCEL_DELAY"
        const val TRIGGER_DELAY = "com.example.oniplayer.TRIGGER_DELAY"
        const val NEXT = "com.example.oniplayer.NEXT"
        const val PREVIOUS = "com.example.oniplayer.PREVIOUS"

        const val SONG_IDS = "song_ids"
        const val SONG_ID = "song_id"
        const val START_INDEX = "start_index"
        const val PLAY = "play"
        const val ENABLED = "enabled"
        const val MODE = "mode"
        const val VALUE = "value"
        const val BAND = "band"
        const val DELAY_SECONDS = "delay_seconds"
        const val PRESET_DATA = "preset"
        const val SHUFFLE_ENABLED = "shuffle_enabled"
        const val SHUFFLE_MODE = "shuffle_mode"
        const val REPEAT_MODE = "repeat_mode"
        const val SHUFFLE_ORDER = "shuffle_order"
    }

    private val context: Context = service.applicationContext
    private val dao = OniDatabase.getDatabase(context).songDao()
    private val persistence = PlaybackPersistence(context)
    private val audioEffectsController = AudioEffectsController(context)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val commandMutex = Mutex()
    private val playbackDelayController = PlaybackDelayController(
        scope = scope,
        onDelayFinished = {
            player.seekToNextMediaItem()
            player.play()
        }
    )

    val player = ExoPlayer.Builder(context).setHandleAudioBecomingNoisy(true).build()
    val mediaSession = MediaSession.Builder(service, player).setCallback(SessionCallback()).build()

    private val _state = MutableStateFlow(PlaybackState(null, false, 0, 0, 0, 0f, false, null, false, ShuffleMode.RANDOM, RepeatMode.ALL, emptyList()))
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var baseQueue = emptyList<SongEntity>()
    private var shuffleEnabled = false
    private var shuffleMode = ShuffleMode.RANDOM
    private var repeatMode = RepeatMode.ALL
    private var preparing = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { publish(); savePlaybackState() }
            override fun onPlaybackStateChanged(state: Int) {
                preparing = state == Player.STATE_BUFFERING
                publish()
                if (state == Player.STATE_ENDED && repeatMode == RepeatMode.ALL && playbackDelayController.hasDelay) {
                    playbackDelayController.startDelay()
                }
                savePlaybackState()
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) cancelDelay()
                publish()
                if (item != null && player.playWhenReady && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    scope.launch { recordPlay(item.mediaId) }
                }
                savePlaybackState()
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                publish()
                savePlaybackState()
            }
            override fun onAudioSessionIdChanged(id: Int) { audioEffectsController.attachToAudioSession(id) }
            override fun onRepeatModeChanged(playerRepeatMode: Int) {
                repeatMode = if (playerRepeatMode == Player.REPEAT_MODE_ONE) RepeatMode.ONE else RepeatMode.ALL
                publish(); savePlaybackState()
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                this@PlaybackController.shuffleEnabled = shuffleModeEnabled
                publish(); savePlaybackState()
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                preparing = false
                Log.e("PlaybackController", "ExoPlayer error: ${error.errorCodeName}", error)
                publish()
            }
        })
        scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    withContext(Dispatchers.IO) {
                        persistence.savePosition(pos)
                    }
                }
                delay(5000)
            }
        }
        scope.launch {
            playbackDelayController.countdown.collect {
                publish()
            }
        }
        restorePlaybackState()
    }

    suspend fun setQueue(ids: List<String>, startIndex: Int, playImmediately: Boolean) {
        val songs = withContext(Dispatchers.IO) {
            val byId = dao.getSongsByIds(ids).associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }
        if (songs.isEmpty()) return
        baseQueue = songs
        val sourceIndex = startIndex.coerceIn(0, songs.lastIndex)
        preparing = true
        player.setMediaItems(songs.map(::mediaItem), sourceIndex, 0L)
        if (shuffleEnabled) {
            player.setShuffleOrder(buildShuffleOrder(songs, songs[sourceIndex].id))
        } else {
            player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(songs.size))
        }
        player.setShuffleModeEnabled(shuffleEnabled)
        applyRepeatMode()
        player.prepare()
        if (playImmediately) {
            player.play()
            scope.launch { recordPlay(songs[sourceIndex].id) }
        } else player.pause()
        publish(songs)
    }

    suspend fun playSong(id: String) {
        val currentItem = player.currentMediaItem
        if (currentItem?.mediaId == id) {
            if (!player.isPlaying) player.play()
            return
        }
        val index = indexOf(id)
        if (index >= 0) {
            player.seekTo(index, 0L)
            player.play()
            scope.launch { recordPlay(id) }
            return
        }
        val song = withContext(Dispatchers.IO) { dao.getSongById(id) } ?: return
        val insertIndex = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItem(insertIndex, mediaItem(song))
        baseQueue = baseQueue + song
        player.seekTo(insertIndex, 0L)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
        scope.launch { recordPlay(id) }
        publish()
    }

    fun pause() = player.pause()
    fun resume() = player.play()
    fun toggle() = if (player.isPlaying) player.pause() else player.play()
    fun seek(positionMs: Long) = player.seekTo(positionMs.coerceAtLeast(0))
    fun next() { cancelDelay(); player.seekToNextMediaItem(); player.play() }
    fun previous() { cancelDelay(); player.seekToPreviousMediaItem(); player.play() }
    fun stop() = player.stop()

    suspend fun setShuffle(enabled: Boolean, mode: ShuffleMode, suppliedOrder: IntArray? = null) {
        shuffleEnabled = enabled
        shuffleMode = mode
        if (player.mediaItemCount > 0) {
            val currentId = player.currentMediaItem?.mediaId
            val shuffleOrder = if (enabled) {
                if (suppliedOrder != null) {
                    ShuffleOrder.DefaultShuffleOrder(suppliedOrder, System.nanoTime())
                } else {
                    buildShuffleOrder(baseQueue, currentId)
                }
            } else {
                ShuffleOrder.UnshuffledShuffleOrder(player.mediaItemCount)
            }
            player.setShuffleOrder(shuffleOrder)
            player.setShuffleModeEnabled(enabled)
        }
        publish(currentQueue())
        savePlaybackState()
    }

    suspend fun addToQueue(id: String) {
        val song = withContext(Dispatchers.IO) { dao.getSongById(id) } ?: return
        if (indexOf(id) >= 0) return
        player.addMediaItem(mediaItem(song))
        baseQueue = baseQueue + song
        publish(currentQueue()); savePlaybackState()
    }

    suspend fun playNext(id: String) {
        if (id == player.currentMediaItem?.mediaId) return
        val song = withContext(Dispatchers.IO) { dao.getSongById(id) } ?: return
        val old = indexOf(id)
        if (old >= 0 && old != player.currentMediaItemIndex) player.removeMediaItem(old)
        val insert = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItem(insert, mediaItem(song))
        baseQueue = baseQueue.filterNot { it.id == id }
        val currentId = player.currentMediaItem?.mediaId
        val baseIndex = baseQueue.indexOfFirst { it.id == currentId }
        baseQueue = if (baseIndex >= 0) baseQueue.toMutableList().apply { add(baseIndex + 1, song) } else baseQueue + song
        publish(currentQueue()); savePlaybackState()
    }

    suspend fun updateSong(id: String) {
        val song = withContext(Dispatchers.IO) { dao.getSongById(id) } ?: return
        val index = indexOf(id)
        if (index >= 0) player.replaceMediaItem(index, mediaItem(song))
        baseQueue = baseQueue.map { if (it.id == id) song else it }
        if (player.currentMediaItem?.mediaId == id) _state.value = _state.value.copy(currentSong = song)
        publish(currentQueue())
    }

    suspend fun toggleFavorite() {
        val id = player.currentMediaItem?.mediaId ?: return
        val song = withContext(Dispatchers.IO) { dao.getSongById(id) } ?: return
        withContext(Dispatchers.IO) { dao.updateSong(song.copy(isFavorite = !song.isFavorite)) }
        updateSong(id)
    }

    fun setRepeat(one: Boolean) {
        repeatMode = if (one) RepeatMode.ONE else RepeatMode.ALL
        applyRepeatMode()
        publish()
        savePlaybackState()
    }

    fun setDelay(seconds: Int) {
        playbackDelayController.setDelay(seconds)
        applyRepeatMode()
        publish()
    }

    fun cancelDelay() {
        playbackDelayController.cancelDelay()
        publish()
    }

    fun triggerDelay() {
        if (playbackDelayController.hasDelay) playbackDelayController.startDelay() else next()
    }

    fun setBand(index: Int, gain: Float) = audioEffectsController.setBand(index, gain)
    fun setBass(level: Float) = audioEffectsController.setBass(level)
    fun setVirtualizer(level: Float) = audioEffectsController.setVirtualizer(level)
    fun applyPreset(p: EqualizerPresetEntity) = audioEffectsController.applyPreset(p)
    fun getBandGains(): FloatArray = audioEffectsController.getBandGains()
    fun getBass(): Float = audioEffectsController.getBass()
    fun getVirtualizer(): Float = audioEffectsController.getVirtualizer()

    private suspend fun recordPlay(id: String) {
        val song = withContext(Dispatchers.IO) { dao.getSongById(id) } ?: return
        withContext(Dispatchers.IO) { dao.updateSong(song.copy(playCount = song.playCount + 1, lastPlayedTimestamp = System.currentTimeMillis())) }
        // Keep the transition path hot. Do not replace/reprepare the current MediaItem after a track changes.
    }

    private fun buildShuffleOrder(queue: List<SongEntity>, currentId: String?): ShuffleOrder {
        if (queue.isEmpty()) return ShuffleOrder.UnshuffledShuffleOrder(0)
        val indices = ShuffleCalculator.calculateShuffleIndices(queue, currentId, shuffleMode)
        return ShuffleOrder.DefaultShuffleOrder(indices, System.nanoTime())
    }

    private fun indexOf(id: String?) = if (id == null) -1 else (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == id } ?: -1
    private fun currentQueue(): List<SongEntity> {
        val byId = _state.value.queue.associateBy { it.id }
        return (0 until player.mediaItemCount).mapNotNull { i -> byId[player.getMediaItemAt(i).mediaId] }
    }
    private fun applyRepeatMode() { player.repeatMode = if (repeatMode == RepeatMode.ONE) Player.REPEAT_MODE_ONE else if (playbackDelayController.hasDelay) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL }

    private fun updateCurrentMetadata() {
        val i = player.currentMediaItemIndex
        val song = _state.value.currentSong ?: baseQueue.find { it.id == player.currentMediaItem?.mediaId } ?: return
        if (i in 0 until player.mediaItemCount) player.replaceMediaItem(i, mediaItem(song))
    }

    private fun mediaItem(song: SongEntity) = MediaItem.Builder().setMediaId(song.id).setUri(Uri.parse(song.filePath)).setMediaMetadata(
        MediaMetadata.Builder().setTitle(song.displayTitle).setArtist(song.displayArtist).setAlbumTitle(song.displayAlbum).setArtworkUri(song.albumArtUri?.let(Uri::parse))
            .setExtras(Bundle().apply { putBoolean(SHUFFLE_ENABLED, shuffleEnabled); putInt(SHUFFLE_MODE, shuffleMode.ordinal); putInt(REPEAT_MODE, repeatMode.ordinal) }).build()
    ).build()

    private fun savePlaybackState() {
        val currentId = player.currentMediaItem?.mediaId
        val currentPosition = player.currentPosition
        val isPlaying = player.isPlaying
        val queueIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val currentShuffleEnabled = shuffleEnabled
        val currentShuffleMode = shuffleMode
        val currentRepeatMode = repeatMode
        scope.launch(Dispatchers.IO) {
            persistence.save(
                PersistedPlaybackState(
                    currentSongId = currentId,
                    positionMs = currentPosition,
                    isPlaying = isPlaying,
                    shuffleEnabled = currentShuffleEnabled,
                    shuffleMode = currentShuffleMode,
                    repeatMode = currentRepeatMode,
                    queueIds = queueIds
                )
            )
        }
    }

    private fun restorePlaybackState() {
        scope.launch {
            val persisted = withContext(Dispatchers.IO) { persistence.load() } ?: return@launch
            val currentSongId = persisted.currentSongId ?: return@launch
            if (currentSongId.isEmpty()) return@launch
            val queueIds = persisted.queueIds.ifEmpty { listOf(currentSongId) }
            val songs = withContext(Dispatchers.IO) {
                val byId = dao.getSongsByIds(queueIds).associateBy { it.id }
                queueIds.mapNotNull { byId[it] }
            }
            if (songs.isEmpty()) return@launch
            baseQueue = songs
            shuffleEnabled = persisted.shuffleEnabled
            shuffleMode = persisted.shuffleMode
            repeatMode = persisted.repeatMode
            val currentIndex = songs.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
            preparing = true
            player.setMediaItems(songs.map(::mediaItem), currentIndex, persisted.positionMs)
            if (shuffleEnabled) {
                player.setShuffleOrder(buildShuffleOrder(songs, currentSongId))
            } else {
                player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(songs.size))
            }
            player.setShuffleModeEnabled(shuffleEnabled)
            applyRepeatMode()
            player.prepare()
            player.pause()
            publish(songs)
        }
    }

    private var nextRevision = 1L
    private var lastPublishedRevision = 0L

    private data class PlaybackSnapshot(
        val revision: Long,
        val currentSong: SongEntity?,
        val currentSongId: String?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val bufferedPositionMs: Long,
        val beatEnergy: Float,
        val isPreparing: Boolean,
        val autoNextCountdownSeconds: Int?,
        val shuffleEnabled: Boolean,
        val shuffleMode: ShuffleMode,
        val repeatMode: RepeatMode,
        val queue: List<SongEntity>
    )

    private fun publish(queueOverride: List<SongEntity>? = null) {
        val q = queueOverride ?: currentQueue()
        val revision = nextRevision++
        if (revision < lastPublishedRevision) return
        val currentId = player.currentMediaItem?.mediaId
        val currentSong = q.find { it.id == currentId } ?: _state.value.currentSong
        val currentBeatEnergy = audioEffectsController.beatEnergy.value
        val isPlaying = player.isPlaying
        val positionMs = player.currentPosition.coerceAtLeast(0)
        val durationMs = player.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0
        val bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0)
        val isPreparing = preparing
        val countdownSeconds = playbackDelayController.countdown.value
        val currentShuffleEnabled = shuffleEnabled
        val currentShuffleMode = shuffleMode
        val currentRepeatMode = repeatMode

        val snapshot = PlaybackSnapshot(
            revision = revision,
            currentSong = currentSong,
            currentSongId = currentId,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPositionMs,
            beatEnergy = currentBeatEnergy,
            isPreparing = isPreparing,
            autoNextCountdownSeconds = countdownSeconds,
            shuffleEnabled = currentShuffleEnabled,
            shuffleMode = currentShuffleMode,
            repeatMode = currentRepeatMode,
            queue = q
        )

        lastPublishedRevision = snapshot.revision
        _state.value = _state.value.copy(
            currentSong = snapshot.currentSong,
            isPlaying = snapshot.isPlaying,
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
            bufferedPositionMs = snapshot.bufferedPositionMs,
            beatEnergy = snapshot.beatEnergy,
            isPreparing = snapshot.isPreparing,
            autoNextCountdownSeconds = snapshot.autoNextCountdownSeconds,
            shuffleEnabled = snapshot.shuffleEnabled,
            shuffleMode = snapshot.shuffleMode,
            repeatMode = snapshot.repeatMode,
            queue = snapshot.queue
        )
        val bundle = Bundle().apply {
            putLong("state_revision", snapshot.revision)
            putString("current_song_id", snapshot.currentSongId)
            putBoolean("is_playing", snapshot.isPlaying)
            putLong("position_ms", snapshot.positionMs)
            putLong("duration_ms", snapshot.durationMs)
            putLong("buffered_position_ms", snapshot.bufferedPositionMs)
            putFloat("beat_energy", snapshot.beatEnergy)
            putBoolean("is_preparing", snapshot.isPreparing)
            putInt("auto_next_countdown", snapshot.autoNextCountdownSeconds ?: -1)
            putBoolean(SHUFFLE_ENABLED, snapshot.shuffleEnabled)
            putInt(SHUFFLE_MODE, snapshot.shuffleMode.ordinal)
            putInt(REPEAT_MODE, snapshot.repeatMode.ordinal)
            putStringArrayList("queue_ids", ArrayList(snapshot.queue.map { it.id }))
        }
        mediaSession.broadcastCustomCommand(SessionCommand("STATE_CHANGED", Bundle.EMPTY), bundle)
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(SET_QUEUE, Bundle.EMPTY)).add(SessionCommand(PLAY_SONG, Bundle.EMPTY)).add(SessionCommand(SET_SHUFFLE, Bundle.EMPTY))
                .add(SessionCommand(ADD_TO_QUEUE, Bundle.EMPTY)).add(SessionCommand(PLAY_NEXT, Bundle.EMPTY)).add(SessionCommand(UPDATE_SONG, Bundle.EMPTY))
                .add(SessionCommand(TOGGLE_FAVORITE, Bundle.EMPTY)).add(SessionCommand(EQ_BAND, Bundle.EMPTY)).add(SessionCommand(BASS, Bundle.EMPTY))
                .add(SessionCommand(VIRTUALIZER, Bundle.EMPTY)).add(SessionCommand(PRESET, Bundle.EMPTY)).add(SessionCommand(SET_DELAY, Bundle.EMPTY))
                .add(SessionCommand(CANCEL_DELAY, Bundle.EMPTY)).add(SessionCommand(TRIGGER_DELAY, Bundle.EMPTY)).add(SessionCommand(NEXT, Bundle.EMPTY)).add(SessionCommand(PREVIOUS, Bundle.EMPTY)).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands).build()
        }

        override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            val result = SettableFuture.create<SessionResult>()
            scope.launch {
                try {
                    commandMutex.withLock {
                        when (command.customAction) {
                            SET_QUEUE -> setQueue(args.getStringArrayList(SONG_IDS).orEmpty(), args.getInt(START_INDEX), args.getBoolean(PLAY, true))
                            PLAY_SONG -> playSong(args.getString(SONG_ID) ?: return@withLock)
                            SET_SHUFFLE -> setShuffle(args.getBoolean(ENABLED), ShuffleMode.entries.getOrElse(args.getInt(MODE)) { shuffleMode }, args.getIntArray(SHUFFLE_ORDER))
                            ADD_TO_QUEUE -> addToQueue(args.getString(SONG_ID) ?: return@withLock)
                            PLAY_NEXT -> playNext(args.getString(SONG_ID) ?: return@withLock)
                            UPDATE_SONG -> updateSong(args.getString(SONG_ID) ?: return@withLock)
                            TOGGLE_FAVORITE -> toggleFavorite()
                            EQ_BAND -> setBand(args.getInt(BAND), args.getFloat(VALUE))
                            BASS -> setBass(args.getFloat(VALUE))
                            VIRTUALIZER -> setVirtualizer(args.getFloat(VALUE))
                            SET_DELAY -> setDelay(args.getInt(DELAY_SECONDS))
                            CANCEL_DELAY -> cancelDelay()
                            TRIGGER_DELAY -> triggerDelay()
                            NEXT -> next()
                            PREVIOUS -> previous()
                            PRESET -> args.getBundle(PRESET_DATA)?.let { p -> applyPreset(EqualizerPresetEntity(p.getString("name", "Custom"), p.getBoolean("isCustom"), p.getFloat("band60Hz"), p.getFloat("band230Hz"), p.getFloat("band910Hz"), p.getFloat("band4kHz"), p.getFloat("band14kHz"), p.getFloat("bassBoost"), p.getFloat("virtualizer"))) }
                        }
                    }
                    result.set(SessionResult(SessionResult.RESULT_SUCCESS))
                } catch (t: Throwable) {
                    result.setException(t)
                }
            }
            return result
        }
    }

    fun release() { playbackDelayController.release(); audioEffectsController.release(); mediaSession.release(); player.release(); scope.cancel() }
}
