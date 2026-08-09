package com.example.playback

import android.content.Context
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val isPlaying: StateFlow<Boolean> = client.isPlaying
    val beatEnergy: StateFlow<Float> = client.beatEnergy
    val isPreparing: StateFlow<Boolean> = client.isPreparing
    val currentSong: StateFlow<SongEntity?> = client.currentSong
    val position: StateFlow<Long> = client.position
    val duration: StateFlow<Long> = client.duration

    /** Legacy callback is retained only for source compatibility; playback completion is service-owned. */
    var onPlaybackCompleted: (() -> Unit)? = null

    fun setSongWithoutPlaying(song: SongEntity) = client.setQueue(listOf(song), 0, false)
    fun play(song: SongEntity) = client.play(song)
    fun updateCurrentSongMetadata(song: SongEntity) = client.updateCurrentSongMetadata(song)
    fun pause() = client.pause()
    fun resume() = client.resume()
    fun stop() = client.stop()
    fun clearCurrentSource() = client.clearCurrentSource()
    fun seekTo(positionMs: Long) = client.seekTo(positionMs)
    fun setBandGain(bandIndex: Int, gainDb: Float) = client.setBandGain(bandIndex, gainDb)
    fun getBandGains(): FloatArray = FloatArray(5)
    fun setBassBoost(levelPercent: Float) = client.setBassBoost(levelPercent)
    fun getBassBoost(): Float = 0f
    fun setVirtualizer(levelPercent: Float) = client.setVirtualizer(levelPercent)
    fun getVirtualizer(): Float = 0f
    fun applyPreset(preset: EqualizerPresetEntity) = client.applyPreset(preset)

    fun release() {
        client.release()
        scope.cancel()
    }
}
