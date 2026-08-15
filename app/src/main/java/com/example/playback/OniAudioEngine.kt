package com.example.playback

import android.content.Context
import com.example.data.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI-process playback facade.
 *
 * PlaybackController owns ExoPlayer and MediaSession in the playback service.
 * This class deliberately does not maintain a second playback state. All
 * observable playback state is derived from PlaybackControllerClient.state.
 */
class OniAudioEngine private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: OniAudioEngine? = null

        fun getInstance(context: Context): OniAudioEngine {
            return instance ?: synchronized(this) {
                instance ?: OniAudioEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val client = PlaybackControllerClient(context)

    /** Single playback state exposed to the UI process. */
    val state: StateFlow<PlaybackState> = client.state

    val isPlaying: StateFlow<Boolean> = state
        .map { it.isPlaying }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val beatEnergy: StateFlow<Float> = state
        .map { it.beatEnergy }
        .stateIn(scope, SharingStarted.Eagerly, 0f)

    val isPreparing: StateFlow<Boolean> = state
        .map { it.isPreparing }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val currentSong: StateFlow<SongEntity?> = state
        .map { it.currentSong }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val position: StateFlow<Long> = state
        .map { it.positionMs }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    val duration: StateFlow<Long> = state
        .map { it.durationMs }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    val currentPlaylist: StateFlow<List<SongEntity>> = state
        .map { it.queue }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val isShuffle: StateFlow<Boolean> = state
        .map { it.shuffleEnabled }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val shuffleMode: StateFlow<ShuffleMode> = state
        .map { it.shuffleMode }
        .stateIn(scope, SharingStarted.Eagerly, ShuffleMode.RANDOM)

    val repeatMode: StateFlow<RepeatMode> = state
        .map { it.repeatMode }
        .stateIn(scope, SharingStarted.Eagerly, RepeatMode.ALL)

    val isRepeat: StateFlow<Boolean> = state
        .map { it.repeatMode == RepeatMode.ONE }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _floatingLyricsEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    val floatingLyricsEnabled: StateFlow<Boolean> = _floatingLyricsEnabled.asStateFlow()

    // Local UI-process cache for the currently selected audio-effect values.
    private val currentBandGains = FloatArray(5)
    private var currentBassBoostLevel = 0f
    private var currentVirtualizerLevel = 0f

    fun setSongWithoutPlaying(song: SongEntity) {
        client.setSongWithoutPlaying(song)
    }

    fun play(song: SongEntity) {
        client.play(song)
    }

    fun updateCurrentSongMetadata(song: SongEntity) {
        client.updateCurrentSongMetadata(song)
    }

    fun pause() {
        client.pause()
    }

    fun resume() {
        client.resume()
    }

    fun stop() {
        client.stop()
    }

    fun clearCurrentSource() {
        client.clearCurrentSource()
    }

    fun seekTo(posMs: Long) {
        client.seekTo(posMs)
    }

    fun release() {
        client.release()
        scope.cancel()
    }

    // --- Equalizer & FX API ---
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in currentBandGains.indices) {
            currentBandGains[bandIndex] = gainDb
            client.setBandGain(bandIndex, gainDb)
        }
    }

    fun getBandGains(): FloatArray = currentBandGains.copyOf()

    fun setBassBoost(levelPercent: Float) {
        currentBassBoostLevel = levelPercent.coerceIn(0f, 100f)
        client.setBassBoost(currentBassBoostLevel)
    }

    fun getBassBoost(): Float = currentBassBoostLevel

    fun setVirtualizer(levelPercent: Float) {
        currentVirtualizerLevel = levelPercent.coerceIn(0f, 100f)
        client.setVirtualizer(currentVirtualizerLevel)
    }

    fun getVirtualizer(): Float = currentVirtualizerLevel

    fun applyPreset(preset: com.example.data.entity.EqualizerPresetEntity) {
        currentBandGains[0] = preset.band60Hz
        currentBandGains[1] = preset.band230Hz
        currentBandGains[2] = preset.band910Hz
        currentBandGains[3] = preset.band4kHz
        currentBandGains[4] = preset.band14kHz
        currentBassBoostLevel = preset.bassBoost
        currentVirtualizerLevel = preset.virtualizer

        client.applyPreset(preset)
    }

    fun setPlaylist(list: List<SongEntity>) {
        val current = state.value.currentSong
        val idx = list.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        client.setQueue(list, idx, state.value.isPlaying)
    }

    fun setPlaylist(list: List<SongEntity>, startIndex: Int, playImmediately: Boolean) {
        client.setQueue(list, startIndex, playImmediately)
    }

    fun addToQueue(song: SongEntity) {
        client.addToQueue(song)
    }

    fun playNext(song: SongEntity) {
        client.playNext(song)
    }

    fun toggleShuffle() {
        client.setShuffle(!state.value.shuffleEnabled, state.value.shuffleMode)
    }

    fun setShuffle(value: Boolean) {
        client.setShuffle(value, state.value.shuffleMode)
    }

    fun toggleRepeat() {
        val nextOne = state.value.repeatMode != RepeatMode.ONE
        client.setRepeat(nextOne)
    }

    fun setRepeat(value: Boolean) {
        client.setRepeat(value)
    }

    fun setShuffleMode(mode: ShuffleMode) {
        client.setShuffle(true, mode)
    }

    fun setFloatingLyricsEnabled(enabled: Boolean) {
        _floatingLyricsEnabled.value = enabled
    }

    fun skipNext() {
        client.next()
    }

    fun skipPrevious() {
        client.previous()
    }
}
