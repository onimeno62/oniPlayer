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
import kotlinx.coroutines.SharingStarted
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

/** UI-process facade. It connects to MediaSession but never owns an audio player. */
class PlaybackControllerClient(context: Context) {
    private val appContext = context.applicationContext
    private val database = OniDatabase.getDatabase(appContext)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val ready = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    private val _state = MutableStateFlow(PlaybackState(null, false, 0, 0, 0, 0f, false, null, false, ShuffleMode.RANDOM, RepeatMode.ALL, emptyList()))
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    val currentSong = state.map { it.currentSong }.stateIn(scope, SharingStarted.Eagerly, null)
    val isPlaying = state.map { it.isPlaying }.stateIn(scope, SharingStarted.Eagerly, false)
    val position = state.map { it.positionMs }.stateIn(scope, SharingStarted.Eagerly, 0L)
    val duration = state.map { it.durationMs }.stateIn(scope, SharingStarted.Eagerly, 0L)
    val bufferedPosition = state.map { it.bufferedPositionMs }.stateIn(scope, SharingStarted.Eagerly, 0L)
    val beatEnergy = state.map { it.beatEnergy }.stateIn(scope, SharingStarted.Eagerly, 0f)
    val isPreparing = state.map { it.isPreparing }.stateIn(scope, SharingStarted.Eagerly, false)
    val autoNextCountdown = state.map { it.autoNextCountdownSeconds }.stateIn(scope, SharingStarted.Eagerly, null)
    val queue = state.map { it.queue }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val isShuffle = state.map { it.shuffleEnabled }.stateIn(scope, SharingStarted.Eagerly, false)
    val shuffleMode = state.map { it.shuffleMode }.stateIn(scope, SharingStarted.Eagerly, ShuffleMode.RANDOM)
    val repeatMode = state.map { it.repeatMode }.stateIn(scope, SharingStarted.Eagerly, RepeatMode.ALL)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refreshState()
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java))
        future = MediaController.Builder(appContext, token).buildAsync().also { f ->
            f.addListener({
                runCatching {
                    controller = f.get().also { it.addListener(listener) }
                    ready.complete(controller!!)
                    refreshState()
                }
            }, ContextCompat.getMainExecutor(appContext))
        }
        scope.launch { while (isActive) { refreshPosition(); delay(500) } }
    }

    private fun refreshPosition() {
        val c = controller ?: return
        _state.value = _state.value.copy(isPlaying = c.isPlaying, positionMs = c.currentPosition.coerceAtLeast(0), durationMs = c.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0, bufferedPositionMs = c.bufferedPosition.coerceAtLeast(0))
    }

    private fun refreshState() {
        val c = controller ?: return
        scope.launch {
            val ids = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
            val byId = database.songDao().getSongsByIds(ids).associateBy { it.id }
            val currentId = c.currentMediaItem?.mediaId
            val extras = c.currentMediaItem?.mediaMetadata?.extras
            val logicalRepeat = if ((extras?.getInt(PlaybackController.REPEAT_MODE, RepeatMode.ALL.ordinal) ?: RepeatMode.ALL.ordinal) == RepeatMode.ONE.ordinal) RepeatMode.ONE else RepeatMode.ALL
            val logicalShuffle = extras?.getBoolean(PlaybackController.SHUFFLE_ENABLED) ?: _state.value.shuffleEnabled
            val logicalShuffleMode = ShuffleMode.entries.getOrElse(extras?.getInt(PlaybackController.SHUFFLE_MODE, _state.value.shuffleMode.ordinal) ?: _state.value.shuffleMode.ordinal) { _state.value.shuffleMode }
            _state.value = _state.value.copy(
                currentSong = currentId?.let { byId[it] ?: database.songDao().getSongById(it) },
                isPlaying = c.isPlaying,
                positionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = c.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0,
                bufferedPositionMs = c.bufferedPosition.coerceAtLeast(0),
                shuffleEnabled = logicalShuffle,
                shuffleMode = logicalShuffleMode,
                repeatMode = logicalRepeat,
                queue = ids.mapNotNull { byId[it] }
            )
        }
    }

    fun play(song: SongEntity) = command(PlaybackController.PLAY_SONG, Bundle().apply { putString(PlaybackController.SONG_ID, song.id) })
    fun pause() = withController { it.pause() }
    fun resume() = withController { it.play() }
    fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms.coerceAtLeast(0)) }
    fun next() = command(PlaybackController.NEXT)
    fun previous() = command(PlaybackController.PREVIOUS)
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
    fun release() { controller?.removeListener(listener); controller?.release(); future?.cancel(true); controller = null; future = null; scope.cancel() }
}
