package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.database.OniDatabase
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** UI-process facade. It connects to MediaSession but never owns an audio player. */
class PlaybackControllerClient(context: Context) {
    private val appContext = context.applicationContext
    private val database = OniDatabase.getDatabase(appContext)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val ready = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    private val stateMutex = Mutex()
    private var lastAppliedRevision = 0L

    private val _state = MutableStateFlow(PlaybackState(null, false, 0, 0, 0, 0f, false, null, false, ShuffleMode.RANDOM, RepeatMode.ALL, emptyList()))
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    val currentSong = state.map { it.currentSong }.stateIn(scope, SharingStarted.Eagerly, null)
    val isPlaying = state.map { it.isPlaying }.stateIn(scope, SharingStarted.Eagerly, false)
    
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()
    
    val duration = state.map { it.durationMs }.stateIn(scope, SharingStarted.Eagerly, 0L)
    val bufferedPosition = state.map { it.bufferedPositionMs }.stateIn(scope, SharingStarted.Eagerly, 0L)
    val beatEnergy = state.map { it.beatEnergy }.stateIn(scope, SharingStarted.Eagerly, 0f)
    val isPreparing = state.map { it.isPreparing }.stateIn(scope, SharingStarted.Eagerly, false)
    val autoNextCountdown = state.map { it.autoNextCountdownSeconds }.stateIn(scope, SharingStarted.Eagerly, null)
    val queue = state.map { it.queue }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val isShuffle = state.map { it.shuffleEnabled }.stateIn(scope, SharingStarted.Eagerly, false)
    val shuffleMode = state.map { it.shuffleMode }.stateIn(scope, SharingStarted.Eagerly, ShuffleMode.RANDOM)
    val repeatMode = state.map { it.repeatMode }.stateIn(scope, SharingStarted.Eagerly, RepeatMode.ALL)

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val c = controller ?: return
            
            // 1. High frequency: position, buffered position, duration, isPlaying, buffering state
            val pos = c.currentPosition.coerceAtLeast(0)
            _position.value = pos
            val isPlaying = c.isPlaying
            val isPreparing = c.playbackState == Player.STATE_BUFFERING
            val duration = c.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0
            val bufferedPos = c.bufferedPosition.coerceAtLeast(0)
            
            // Check if structural/low frequency state changed
            val structuralChanged = events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_PLAYLIST_METADATA_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_REPEAT_MODE_CHANGED
            )
            
            if (structuralChanged || _state.value.currentSong == null || _state.value.queue.isEmpty()) {
                refreshState()
            } else {
                _state.value = _state.value.copy(
                    isPlaying = isPlaying,
                    positionMs = pos,
                    durationMs = duration,
                    bufferedPositionMs = bufferedPos,
                    isPreparing = isPreparing
                )
            }
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<androidx.media3.session.SessionResult> {
            if (command.customAction == "STATE_CHANGED") {
                val revision = args.getLong("state_revision", 0L)
                scope.launch {
                    stateMutex.withLock {
                        if (revision > 0L && revision < lastAppliedRevision) {
                            return@withLock
                        }
                        val shuffle = args.getBoolean(PlaybackController.SHUFFLE_ENABLED, _state.value.shuffleEnabled)
                        val shuffleModeOrdinal = args.getInt(PlaybackController.SHUFFLE_MODE, _state.value.shuffleMode.ordinal)
                        val repeatModeOrdinal = args.getInt(PlaybackController.REPEAT_MODE, _state.value.repeatMode.ordinal)
                        val isPlaying = args.getBoolean("is_playing", _state.value.isPlaying)
                        val isPreparing = args.getBoolean("is_preparing", _state.value.isPreparing)
                        val pos = args.getLong("position_ms", _state.value.positionMs)
                        val duration = args.getLong("duration_ms", _state.value.durationMs)
                        val bufferedPos = args.getLong("buffered_position_ms", _state.value.bufferedPositionMs)
                        val beatEnergy = args.getFloat("beat_energy", _state.value.beatEnergy)
                        val countdown = args.getInt("auto_next_countdown", -1).let { if (it == -1) null else it }
                        val currentId = args.getString("current_song_id")
                        val queueIds = args.getStringArrayList("queue_ids") ?: emptyList<String>()

                        val currentSongEntity = currentId?.let { database.songDao().getSongById(it) }

                        val queueSongs = if (queueIds == cachedQueueIds) {
                            cachedQueueSongs.map { if (it.id == currentId && currentSongEntity != null) currentSongEntity else it }
                        } else {
                            val byId = database.songDao().getSongsByIds(queueIds).associateBy { it.id }
                            val fetched = queueIds.mapNotNull { byId[it] ?: database.songDao().getSongById(it) }
                            cachedQueueIds = queueIds
                            cachedQueueSongs = fetched
                            fetched
                        }

                        if (revision > 0L && revision < lastAppliedRevision) {
                            return@withLock
                        }
                        if (revision > 0L) {
                            lastAppliedRevision = revision
                        }

                        val currentController = this@PlaybackControllerClient.controller
                        val finalIsPlaying = currentController?.isPlaying ?: isPlaying
                        val finalPos = currentController?.currentPosition?.coerceAtLeast(0) ?: pos
                        val finalDuration = currentController?.duration?.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: duration
                        val finalBufferedPos = currentController?.bufferedPosition?.coerceAtLeast(0) ?: bufferedPos
                        val finalIsPreparing = if (currentController != null) currentController.playbackState == Player.STATE_BUFFERING else isPreparing

                        _state.value = _state.value.copy(
                            currentSong = currentSongEntity ?: queueSongs.find { it.id == currentId },
                            isPlaying = finalIsPlaying,
                            positionMs = finalPos,
                            durationMs = finalDuration,
                            bufferedPositionMs = finalBufferedPos,
                            beatEnergy = beatEnergy,
                            isPreparing = finalIsPreparing,
                            autoNextCountdownSeconds = countdown,
                            shuffleEnabled = shuffle,
                            shuffleMode = ShuffleMode.entries.getOrElse(shuffleModeOrdinal) { _state.value.shuffleMode },
                            repeatMode = RepeatMode.entries.getOrElse(repeatModeOrdinal) { _state.value.repeatMode },
                            queue = queueSongs
                        )
                        _position.value = finalPos
                    }
                }
            }
            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
            )
        }
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java))
        future = MediaController.Builder(appContext, token)
            .setListener(controllerListener)
            .buildAsync().also { f ->
                f.addListener({
                    runCatching {
                        controller = f.get().also { it.addListener(playerListener) }
                        ready.complete(controller!!)
                        refreshState()
                    }
                }, ContextCompat.getMainExecutor(appContext))
            }
        scope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _position.value = c.currentPosition.coerceAtLeast(0)
                }
                delay(200)
            }
        }
    }

    private fun refreshPosition() {
        val c = controller ?: return
        val pos = c.currentPosition.coerceAtLeast(0)
        _position.value = pos
        _state.value = _state.value.copy(
            isPlaying = c.isPlaying,
            positionMs = pos,
            durationMs = c.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0,
            bufferedPositionMs = c.bufferedPosition.coerceAtLeast(0)
        )
    }

    private var cachedQueueIds = emptyList<String>()
    private var cachedQueueSongs = emptyList<SongEntity>()

    private fun refreshState() {
        val c = controller ?: return
        scope.launch {
            stateMutex.withLock {
                val ids = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
                val currentId = c.currentMediaItem?.mediaId
                
                // Always fetch the current playing song freshly from the database to guarantee updated lyrics/metadata
                val currentSongEntity = currentId?.let { database.songDao().getSongById(it) }

                val queueSongs = if (ids == cachedQueueIds) {
                    cachedQueueSongs.map { if (it.id == currentId && currentSongEntity != null) currentSongEntity else it }
                } else {
                    val byId = database.songDao().getSongsByIds(ids).associateBy { it.id }
                    val fetched = ids.mapNotNull { byId[it] ?: database.songDao().getSongById(it) }
                    cachedQueueIds = ids
                    cachedQueueSongs = fetched
                    fetched
                }

                val logicalRepeat = if (c.repeatMode == Player.REPEAT_MODE_ONE) RepeatMode.ONE else RepeatMode.ALL
                val logicalShuffle = _state.value.shuffleEnabled
                val logicalShuffleMode = _state.value.shuffleMode

                val isPlayingVal = c.isPlaying
                val posVal = c.currentPosition.coerceAtLeast(0)
                val durationVal = c.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0
                val bufferedVal = c.bufferedPosition.coerceAtLeast(0)

                _state.value = _state.value.copy(
                    currentSong = currentSongEntity ?: queueSongs.find { it.id == currentId },
                    isPlaying = isPlayingVal,
                    positionMs = posVal,
                    durationMs = durationVal,
                    bufferedPositionMs = bufferedVal,
                    shuffleEnabled = logicalShuffle,
                    shuffleMode = logicalShuffleMode,
                    repeatMode = logicalRepeat,
                    queue = queueSongs
                )
                _position.value = posVal
            }
        }
    }

    fun play(song: SongEntity) = command(PlaybackController.PLAY_SONG, Bundle().apply { putString(PlaybackController.SONG_ID, song.id) })
    fun pause() = withController { it.pause() }
    fun resume() = withController { it.play() }
    fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms.coerceAtLeast(0)) }
    fun next() = withController { it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPreviousMediaItem() }
    fun stop() = withController { it.stop() }
    fun clearCurrentSource() = stop()

    fun setQueue(songs: List<SongEntity>, startIndex: Int, playImmediately: Boolean) = command(PlaybackController.SET_QUEUE, Bundle().apply {
        putStringArrayList(PlaybackController.SONG_IDS, ArrayList(songs.map { it.id }))
        putInt(PlaybackController.START_INDEX, startIndex)
        putBoolean(PlaybackController.PLAY, playImmediately)
    })
    fun setShuffle(enabled: Boolean, mode: ShuffleMode) = command(PlaybackController.SET_SHUFFLE, Bundle().apply { putBoolean(PlaybackController.ENABLED, enabled); putInt(PlaybackController.MODE, mode.ordinal) })
    fun addToQueue(song: SongEntity) = command(PlaybackController.ADD_TO_QUEUE, Bundle().apply { putString(PlaybackController.SONG_ID, song.id) })
    fun playNext(song: SongEntity) = command(PlaybackController.PLAY_NEXT, Bundle().apply { putString(PlaybackController.SONG_ID, song.id) })
    fun updateCurrentSongMetadata(song: SongEntity) = command(PlaybackController.UPDATE_SONG, Bundle().apply { putString(PlaybackController.SONG_ID, song.id) })
    fun setSongWithoutPlaying(song: SongEntity) = updateCurrentSongMetadata(song)
    fun setRepeat(one: Boolean) = withController { it.setRepeatMode(if (one) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL) }
    fun setBandGain(index: Int, gain: Float) = command(PlaybackController.EQ_BAND, Bundle().apply { putInt(PlaybackController.BAND, index); putFloat(PlaybackController.VALUE, gain) })
    fun setBassBoost(level: Float) = command(PlaybackController.BASS, Bundle().apply { putFloat(PlaybackController.VALUE, level) })
    fun setVirtualizer(level: Float) = command(PlaybackController.VIRTUALIZER, Bundle().apply { putFloat(PlaybackController.VALUE, level) })
    fun setAutoNextDelay(seconds: Int) = command(PlaybackController.SET_DELAY, Bundle().apply { putInt(PlaybackController.DELAY_SECONDS, seconds) })
    fun cancelPendingNext() = command(PlaybackController.CANCEL_DELAY)
    fun triggerAutoNextWithDelay() = command(PlaybackController.TRIGGER_DELAY)
    fun applyPreset(p: EqualizerPresetEntity) = command(PlaybackController.PRESET, Bundle().apply { putBundle(PlaybackController.PRESET_DATA, Bundle().apply { putString("name", p.name); putBoolean("isCustom", p.isCustom); putFloat("band60Hz", p.band60Hz); putFloat("band230Hz", p.band230Hz); putFloat("band910Hz", p.band910Hz); putFloat("band4kHz", p.band4kHz); putFloat("band14kHz", p.band14kHz); putFloat("bassBoost", p.bassBoost); putFloat("virtualizer", p.virtualizer) }) })

    private fun command(action: String, args: Bundle = Bundle()) {
        scope.launch { runCatching { ready.await().sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args) } }
    }
    private fun withController(block: (MediaController) -> Unit) { scope.launch { runCatching { block(ready.await()) } } }
    fun release() { controller?.removeListener(playerListener); controller?.release(); future?.cancel(true); controller = null; future = null; scope.cancel() }
}
