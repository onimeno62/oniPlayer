package com.example.playback

import android.content.Context
import com.example.data.entity.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class OniAudioEngine private constructor(private val context: Context) {
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

    val isPlaying: StateFlow<Boolean> = client.isPlaying
    val beatEnergy: StateFlow<Float> = client.beatEnergy
    val isPreparing: StateFlow<Boolean> = client.isPreparing
    val currentSong: StateFlow<SongEntity?> = client.currentSong
    val position: StateFlow<Long> = client.position
    val duration: StateFlow<Long> = client.duration
    val currentPlaylist: StateFlow<List<SongEntity>> = client.queue
    val isShuffle: StateFlow<Boolean> = client.isShuffle
    val shuffleMode: StateFlow<ShuffleMode> = client.shuffleMode

    val isRepeat: StateFlow<Boolean> = client.repeatMode
        .map { it == RepeatMode.ONE }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _floatingLyricsEnabled = MutableStateFlow(false)
    val floatingLyricsEnabled: StateFlow<Boolean> = _floatingLyricsEnabled.asStateFlow()

    // Local caches for audio effects
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

    fun getBandGains(): FloatArray = currentBandGains

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
        val current = client.currentSong.value
        val idx = list.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        client.setQueue(list, idx, client.isPlaying.value)
    }

    fun toggleShuffle() {
        client.setShuffle(!client.isShuffle.value, client.shuffleMode.value)
    }

    fun setShuffle(value: Boolean) {
        client.setShuffle(value, client.shuffleMode.value)
    }

    fun toggleRepeat() {
        val nextOne = client.repeatMode.value != RepeatMode.ONE
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
