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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * UI-process facade. It connects to MediaSession but never owns an audio player.
 *
 * PlaybackState is the only observable playback state owned by this client. All
 * derived playback values are read from that state by the higher UI layer.
 */
class PlaybackControllerClient(context: Context) {
    private val appContext = context.applicationContext
    private val database = OniDatabase.getDatabase(appContext)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val ready = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val stateMutex = Mutex()
    private val commandMutex = Mutex()
    private var lastAppliedRevision = 0L

    private val _state = MutableStateFlow(
        PlaybackState(
            currentSong = null,
            isPlaying = false,
            positionMs = 0,
            durationMs = 0,
            bufferedPositionMs = 0,
            beatEnergy = 0f,
            isPreparing = false,
            autoNextCountdownSeconds = null,
            shuffleEnabled = false,
            shuffleMode = ShuffleMode.RANDOM,
            repeatMode = RepeatMode.ALL,
            queue = emptyList(),
            shuffleType = ShuffleType.SONGS
        )
    )
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val c = controller ?: return
            val pos = c.currentPosition.coerceAtLeast(0)
            val isPlaying = c.isPlaying
            val isPreparing = c.playbackState == Player.STATE_BUFFERING
            val duration = c.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0
            val bufferedPos = c.bufferedPosition.coerceAtLeast(0)
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
                    commandMutex.withLock {
                        stateMutex.withLock {
                            if (revision > 0L && revision < lastAppliedRevision) return@withLock

                            val shuffle = args.getBoolean(
                                PlaybackController.SHUFFLE_ENABLED,
                                _state.value.shuffleEnabled
                            )
                            val shuffleModeOrdinal = args.getInt(
                                PlaybackController.SHUFFLE_MODE,
                                _state.value.shuffleMode.ordinal
                            )
                            val shuffleTypeOrdinal = args.getInt(
                                PlaybackController.SHUFFLE_TYPE,
                                _state.value.shuffleType.ordinal
                            )
                            val repeatModeOrdinal = args.getInt(
                                PlaybackController.REPEAT_MODE,
                                _state.value.repeatMode.ordinal
                            )
                            val isPlaying = args.getBoolean("is_playing", _state.value.isPlaying)
                            val isPreparing = args.getBoolean("is_preparing", _state.value.isPreparing)
                            val pos = args.getLong("position_ms", _state.value.positionMs)
                            val duration = args.getLong("duration_ms", _state.value.durationMs)
                            val bufferedPos = args.getLong(
                                "buffered_position_ms",
                                _state.value.bufferedPositionMs
                            )
                            val beatEnergy = args.getFloat("beat_energy", _state.value.beatEnergy)
                            val countdown = args.getInt("auto_next_countdown", -1)
                                .let { if (it == -1) null else it }
                            val currentId = args.getString("current_song_id")
                            val queueIds = args.getStringArrayList("queue_ids") ?: emptyList<String>()

                            val (currentSongEntity, queueSongs) = withContext(Dispatchers.IO) {
                                val dao = database.songDao()
                                val current = currentId?.let { dao.getSongById(it) }
                                val songs = if (queueIds == cachedQueueIds) {
                                    cachedQueueSongs.map {
                                        if (it.id == currentId && current != null) current else it
                                    }
                                } else {
                                    val byId = dao.getSongsByIds(queueIds).associateBy { it.id }
                                    val fetched = queueIds.mapNotNull { byId[it] }
                                    cachedQueueIds = queueIds
                                    cachedQueueSongs = fetched
                                    fetched
                                }
                                current to songs
                            }

                            if (revision > 0L && revision < lastAppliedRevision) return@withLock
                            if (revision > 0L) lastAppliedRevision = revision

                            val currentController = this@PlaybackControllerClient.controller

                            val controllerId = currentController?.currentMediaItem?.mediaId
                            val trackMatches = currentController == null || controllerId == currentId

                            val finalIsPlaying = currentController?.isPlaying ?: isPlaying
                            val finalPos = currentController?.currentPosition?.coerceAtLeast(0) ?: pos
                            val finalDuration = currentController?.duration
                                ?.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 }
                                ?: duration
                            val finalBufferedPos = currentController?.bufferedPosition?.coerceAtLeast(0)
                                ?: bufferedPos
                            val finalIsPreparing = if (currentController != null) {
                                currentController.playbackState == Player.STATE_BUFFERING
                            } else {
                                isPreparing
                            }

                            val resolvedSong = if (currentId != null) {
                                currentSongEntity ?: queueSongs.find { it.id == currentId }
                            } else {
                                null
                            }

                            _state.value = _state.value.copy(
                                currentSong = if (trackMatches) resolvedSong else _state.value.currentSong,
                                isPlaying = finalIsPlaying,
                                positionMs = finalPos,
                                durationMs = finalDuration,
                                bufferedPositionMs = finalBufferedPos,
                                beatEnergy = beatEnergy,
                                isPreparing = finalIsPreparing,
                                autoNextCountdownSeconds = countdown,
                                shuffleEnabled = shuffle,
                                shuffleMode = ShuffleMode.entries.getOrElse(shuffleModeOrdinal) {
                                    _state.value.shuffleMode
                                },
                                shuffleType = ShuffleType.entries.getOrElse(shuffleTypeOrdinal) {
                                    _state.value.shuffleType
                                },
                                repeatMode = RepeatMode.entries.getOrElse(repeatModeOrdinal) {
                                    _state.value.repeatMode
                                },
                                queue = if (trackMatches) queueSongs else _state.value.queue
                            )
                        }
                    }
                }
            }

            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.SessionResult(
                    androidx.media3.session.SessionResult.RESULT_SUCCESS
                )
            )
        }
    }

    init {
        val token = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java)
        )
        future = MediaController.Builder(appContext, token)
            .setListener(controllerListener)
            .buildAsync()
            .also { f ->
                f.addListener(
                    {
                        runCatching {
                            controller = f.get().also { it.addListener(playerListener) }
                            ready.complete(controller!!)
                            refreshState()
                        }
                    },
                    ContextCompat.getMainExecutor(appContext)
                )
            }

        scope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    val position = c.currentPosition.coerceAtLeast(0)
                    val duration = c.duration
                        .takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 }
                        ?: _state.value.durationMs
                    val bufferedPosition = c.bufferedPosition.coerceAtLeast(0)
                    _state.value = _state.value.copy(
                        positionMs = position,
                        durationMs = duration,
                        bufferedPositionMs = bufferedPosition
                    )
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    private var cachedQueueIds = emptyList<String>()
    private var cachedQueueSongs = emptyList<SongEntity>()

    private fun logicalRepeat(c: MediaController): RepeatMode =
        RepeatMode.reconcile(c.repeatMode, _state.value.repeatMode, keepAllWhenOff = true)

    private fun refreshState() {
        val c = controller ?: return
        val ids = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
        val currentId = c.currentMediaItem?.mediaId
        val posVal = c.currentPosition.coerceAtLeast(0)
        val durationVal = c.duration
            .takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 }
            ?: 0
        val bufferedVal = c.bufferedPosition.coerceAtLeast(0)

        if (ids == cachedQueueIds && cachedQueueSongs.isNotEmpty()) {
            val current = if (currentId != null) cachedQueueSongs.find { it.id == currentId } else null
            _state.value = _state.value.copy(
                currentSong = if (currentId != null) (current ?: _state.value.currentSong?.takeIf { it.id == currentId }) else null,
                isPlaying = c.isPlaying,
                positionMs = posVal,
                durationMs = durationVal,
                bufferedPositionMs = bufferedVal,
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = logicalRepeat(c),
                queue = cachedQueueSongs
            )
        }

        scope.launch {
            stateMutex.withLock {
                val (currentSongEntity, queueSongs) = withContext(Dispatchers.IO) {
                    val dao = database.songDao()
                    val current = currentId?.let { dao.getSongById(it) }
                    val songs = if (ids == cachedQueueIds && cachedQueueSongs.isNotEmpty()) {
                        cachedQueueSongs.map {
                            if (it.id == currentId && current != null) current else it
                        }
                    } else {
                        val byId = dao.getSongsByIds(ids).associateBy { it.id }
                        val fetched = ids.mapNotNull { byId[it] }
                        cachedQueueIds = ids
                        cachedQueueSongs = fetched
                        fetched
                    }
                    current to songs
                }

                val latest = controller ?: return@withLock
                val latestId = latest.currentMediaItem?.mediaId
                if (latestId != currentId) return@withLock

                val latestPosition = latest.currentPosition.coerceAtLeast(0)
                val latestDuration = latest.duration
                    .takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 }
                    ?: 0

                _state.value = _state.value.copy(
                    currentSong = if (currentId != null) (currentSongEntity ?: queueSongs.find { it.id == currentId }) else null,
                    isPlaying = latest.isPlaying,
                    positionMs = latestPosition,
                    durationMs = latestDuration,
                    bufferedPositionMs = latest.bufferedPosition.coerceAtLeast(0),
                    shuffleEnabled = latest.shuffleModeEnabled,
                    repeatMode = logicalRepeat(latest),
                    queue = queueSongs
                )
            }
        }
    }

    fun play(song: SongEntity) = command(
        PlaybackController.PLAY_SONG,
        Bundle().apply { putString(PlaybackController.SONG_ID, song.id) }
    )

    fun pause() = withController { it.pause() }
    fun resume() = withController { it.play() }
    fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms.coerceAtLeast(0)) }

    fun next() = command(PlaybackController.NEXT)
    fun previous() = command(PlaybackController.PREVIOUS)

    fun stop() = withController { it.stop() }
    fun clearCurrentSource() = stop()

    fun setQueue(songs: List<SongEntity>, startIndex: Int, playImmediately: Boolean) = command(
        PlaybackController.SET_QUEUE,
        Bundle().apply {
            putStringArrayList(
                PlaybackController.SONG_IDS,
                ArrayList(songs.map { it.id })
            )
            putInt(PlaybackController.START_INDEX, startIndex)
            putBoolean(PlaybackController.PLAY, playImmediately)
        }
    )

    fun setShuffle(enabled: Boolean, mode: ShuffleMode, type: ShuffleType = _state.value.shuffleType) = command(
        PlaybackController.SET_SHUFFLE,
        Bundle().apply {
            putBoolean(PlaybackController.ENABLED, enabled)
            putInt(PlaybackController.MODE, mode.ordinal)
            putInt(PlaybackController.SHUFFLE_TYPE, type.ordinal)
        }
    )

    fun addToQueue(song: SongEntity) = command(
        PlaybackController.ADD_TO_QUEUE,
        Bundle().apply { putString(PlaybackController.SONG_ID, song.id) }
    )

    fun playNext(song: SongEntity) = command(
        PlaybackController.PLAY_NEXT,
        Bundle().apply { putString(PlaybackController.SONG_ID, song.id) }
    )

    /**
     * Removes a song from the active queue. If the song is currently playing,
     * playback advances to the next track automatically.
     */
    fun removeFromQueue(song: SongEntity) = command(
        PlaybackController.REMOVE_FROM_QUEUE,
        Bundle().apply { putString(PlaybackController.SONG_ID, song.id) }
    )

    fun updateCurrentSongMetadata(song: SongEntity) = command(
        PlaybackController.UPDATE_SONG,
        Bundle().apply { putString(PlaybackController.SONG_ID, song.id) }
    )

    fun setSongWithoutPlaying(song: SongEntity) = updateCurrentSongMetadata(song)

    fun setRepeat(one: Boolean) = setRepeatMode(if (one) RepeatMode.ONE else RepeatMode.ALL)

    fun setRepeatMode(mode: RepeatMode) = command(
        PlaybackController.SET_REPEAT,
        Bundle().apply { putInt(PlaybackController.MODE, mode.ordinal) }
    )

    fun setBandGain(index: Int, gain: Float) = command(
        PlaybackController.EQ_BAND,
        Bundle().apply {
            putInt(PlaybackController.BAND, index)
            putFloat(PlaybackController.VALUE, gain)
        }
    )

    fun setBassBoost(level: Float) = command(
        PlaybackController.BASS,
        Bundle().apply { putFloat(PlaybackController.VALUE, level) }
    )

    fun setVirtualizer(level: Float) = command(
        PlaybackController.VIRTUALIZER,
        Bundle().apply { putFloat(PlaybackController.VALUE, level) }
    )

    fun setAutoNextDelay(seconds: Int) = command(
        PlaybackController.SET_DELAY,
        Bundle().apply { putInt(PlaybackController.DELAY_SECONDS, seconds) }
    )

    fun cancelPendingNext() = command(PlaybackController.CANCEL_DELAY)
    fun triggerAutoNextWithDelay() = command(PlaybackController.TRIGGER_DELAY)

    fun applyPreset(p: EqualizerPresetEntity) = command(
        PlaybackController.PRESET,
        Bundle().apply {
            putBundle(
                PlaybackController.PRESET_DATA,
                Bundle().apply {
                    putString("name", p.name)
                    putBoolean("isCustom", p.isCustom)
                    putFloat("band60Hz", p.band60Hz)
                    putFloat("band230Hz", p.band230Hz)
                    putFloat("band910Hz", p.band910Hz)
                    putFloat("band4kHz", p.band4kHz)
                    putFloat("band14kHz", p.band14kHz)
                    putFloat("bassBoost", p.bassBoost)
                    putFloat("virtualizer", p.virtualizer)
                }
            )
        }
    )

    private fun command(action: String, args: Bundle = Bundle()) {
        scope.launch {
            commandMutex.withLock {
                runCatching {
                    val c = ready.await()
                    val future = c.sendCustomCommand(
                        SessionCommand(action, Bundle.EMPTY),
                        args
                    )
                    suspendCancellableCoroutine<Unit> { continuation ->
                        future.addListener(
                            {
                                if (continuation.isActive) continuation.resume(Unit)
                            },
                            ContextCompat.getMainExecutor(appContext)
                        )
                        continuation.invokeOnCancellation { future.cancel(true) }
                    }
                }
            }
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch {
            commandMutex.withLock {
                runCatching { block(ready.await()) }
            }
        }
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        future?.cancel(true)
        controller = null
        future = null
        scope.cancel()
    }
}
