package com.example.playback

import android.content.Context
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * Compatibility facade retained for the current ViewModel/UI surface.
 * It contains no player and no MediaPlayer. Actual playback is owned by MusicPlaybackService.
 * The facade can be removed once MusicPlayerViewModel is migrated to PlaybackControllerClient directly.
 */
class OniAudioEngine private constructor(context: Context) {
    companion object {
        @Volatile private var instance: OniAudioEngine? = null
        fun getInstance(context: Context): OniAudioEngine = instance ?: synchronized(this) {
            instance ?: OniAudioEngine(context.applicationContext).also { instance = it }
        }
    }

    private val client = PlaybackControllerClient(context)
    private val bandGains = FloatArray(5)
    private var bassBoost = 0f
    private var virtualizer = 0f

    val isPlaying: StateFlow<Boolean> = client.isPlaying
    val beatEnergy: StateFlow<Float> = client.beatEnergy
    val isPreparing: StateFlow<Boolean> = client.isPreparing
    val currentSong: StateFlow<SongEntity?> = client.currentSong
    val position: StateFlow<Long> = client.position
    val duration: StateFlow<Long> = client.duration

    /** Legacy callback retained only for source compatibility; completion is service-owned. */
    var onPlaybackCompleted: (() -> Unit)? = null

    fun setSongWithoutPlaying(song: SongEntity) = client.setQueue(listOf(song), 0, false)
    fun play(song: SongEntity) = client.play(song)
    fun updateCurrentSongMetadata(song: SongEntity) = client.updateCurrentSongMetadata(song)
    fun pause() = client.pause()
    fun resume() = client.resume()
    fun stop() = client.stop()
    fun clearCurrentSource() = client.clearCurrentSource()
    fun seekTo(positionMs: Long) = client.seekTo(positionMs)
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in bandGains.indices) bandGains[bandIndex] = gainDb
        client.setBandGain(bandIndex, gainDb)
    }
    fun getBandGains(): FloatArray = bandGains.copyOf()
    fun setBassBoost(levelPercent: Float) { bassBoost = levelPercent; client.setBassBoost(levelPercent) }
    fun getBassBoost(): Float = bassBoost
    fun setVirtualizer(levelPercent: Float) { virtualizer = levelPercent; client.setVirtualizer(levelPercent) }
    fun getVirtualizer(): Float = virtualizer
    fun applyPreset(preset: EqualizerPresetEntity) {
        bandGains[0] = preset.band60Hz
        bandGains[1] = preset.band230Hz
        bandGains[2] = preset.band910Hz
        bandGains[3] = preset.band4kHz
        bandGains[4] = preset.band14kHz
        bassBoost = preset.bassBoost
        virtualizer = preset.virtualizer
        client.applyPreset(preset)
    }

    fun release() = client.release()
}
