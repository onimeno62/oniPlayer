package com.example.playback

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.entity.EqualizerPresetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Controller responsible for managing Android audio effects (Equalizer, BassBoost, Virtualizer, Visualizer),
 * their configuration, lifecycle, and real-time audio FFT beat energy sampling.
 */
class AudioEffectsController(private val context: Context) {

    companion object {
        private const val TAG = "AudioEffectsController"
        const val NUM_BANDS = 5
        const val MAX_FX_PERCENT = 100f
        const val MIN_FX_PERCENT = 0f
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var visualizer: Visualizer? = null

    private val bandGains = FloatArray(NUM_BANDS)
    private var bassLevel = 0f
    private var virtualizerLevel = 0f
    private var currentSessionId: Int = 0

    private val _beatEnergy = MutableStateFlow(0f)
    val beatEnergy: StateFlow<Float> = _beatEnergy.asStateFlow()

    /**
     * Attaches audio effects to the given audio session ID.
     * Re-initializes effects and reapplies stored band gains and effect levels.
     */
    fun attachToAudioSession(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == currentSessionId && equalizer != null) return

        currentSessionId = sessionId
        releaseEffects()

        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer unavailable: ${e.message}")
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost unavailable: ${e.message}")
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer unavailable: ${e.message}")
            virtualizer = null
        }

        val hasRecordPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasRecordPermission) {
            var tempVisualizer: Visualizer? = null
            try {
                val range = Visualizer.getCaptureSizeRange()
                if (range != null && range.size >= 2) {
                    val vis = Visualizer(sessionId)
                    tempVisualizer = vis
                    vis.captureSize = 1024.coerceIn(range[0], range[1])
                    vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            val nextEnergy = calculateNextBeatEnergy(_beatEnergy.value, fft)
                            _beatEnergy.value = nextEnergy
                        }
                    }, 20_000, false, true)
                    vis.enabled = true
                    visualizer = vis
                    tempVisualizer = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Visualizer unavailable: ${e.message}")
                safelyReleaseVisualizer(tempVisualizer)
                visualizer = null
            }
        }

        applyEq()
        applyBass()
        applyVirtualizer()
    }

    /**
     * Updates the gain of a specific frequency band.
     */
    fun setBand(index: Int, gain: Float) {
        if (index in bandGains.indices) {
            bandGains[index] = gain
            applyEq()
        }
    }

    /**
     * Updates the BassBoost strength (0f..100f).
     */
    fun setBass(level: Float) {
        bassLevel = level.coerceIn(MIN_FX_PERCENT, MAX_FX_PERCENT)
        applyBass()
    }

    /**
     * Updates the Spatial Virtualizer strength (0f..100f).
     */
    fun setVirtualizer(level: Float) {
        virtualizerLevel = level.coerceIn(MIN_FX_PERCENT, MAX_FX_PERCENT)
        applyVirtualizer()
    }

    /**
     * Applies a full EqualizerPresetEntity containing 5 band gains, bass boost, and virtualizer.
     */
    fun applyPreset(p: EqualizerPresetEntity) {
        bandGains[0] = p.band60Hz
        bandGains[1] = p.band230Hz
        bandGains[2] = p.band910Hz
        bandGains[3] = p.band4kHz
        bandGains[4] = p.band14kHz
        bassLevel = p.bassBoost.coerceIn(MIN_FX_PERCENT, MAX_FX_PERCENT)
        virtualizerLevel = p.virtualizer.coerceIn(MIN_FX_PERCENT, MAX_FX_PERCENT)

        applyEq()
        applyBass()
        applyVirtualizer()
    }

    /**
     * Returns a copy of the current 5-band EQ gain values.
     */
    fun getBandGains(): FloatArray = bandGains.copyOf()

    /**
     * Returns the current BassBoost level (0f..100f).
     */
    fun getBass(): Float = bassLevel

    /**
     * Returns the current Virtualizer level (0f..100f).
     */
    fun getVirtualizer(): Float = virtualizerLevel

    /**
     * Applies the current band gain configuration to the active Equalizer effect.
     */
    private fun applyEq() {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val numBands = minOf(NUM_BANDS, eq.numberOfBands.toInt())
            for (i in 0 until numBands) {
                val level = (bandGains[i] * 100).roundToInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                eq.setBandLevel(i.toShort(), level)
            }
        } catch (_: Exception) {}
    }

    /**
     * Applies the current bass boost setting to the active BassBoost effect.
     */
    private fun applyBass() {
        try {
            bassBoost?.takeIf { it.strengthSupported }?.setStrength(
                (bassLevel * 10).roundToInt().coerceIn(0, 1000).toShort()
            )
        } catch (_: Exception) {}
    }

    /**
     * Applies the current virtualizer setting to the active Virtualizer effect.
     */
    private fun applyVirtualizer() {
        try {
            virtualizer?.takeIf { it.strengthSupported }?.setStrength(
                (virtualizerLevel * 10).roundToInt().coerceIn(0, 1000).toShort()
            )
        } catch (_: Exception) {}
    }

    /**
     * Safely releases internal audio effect instances.
     */
    private fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null

        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        bassBoost = null

        try {
            virtualizer?.release()
        } catch (_: Exception) {}
        virtualizer = null

        val vis = visualizer
        visualizer = null
        safelyReleaseVisualizer(vis)
    }

    private fun safelyReleaseVisualizer(vis: Visualizer?) {
        if (vis == null) return
        try {
            vis.setDataCaptureListener(null, 0, false, false)
        } catch (_: Exception) {}
        try {
            vis.enabled = false
        } catch (_: Exception) {}
        try {
            vis.release()
        } catch (_: Exception) {}
    }

    /**
     * Fully releases all audio effects and resets session state.
     * This method is idempotent and safe to call multiple times.
     */
    fun release() {
        releaseEffects()
        currentSessionId = 0
        _beatEnergy.value = 0f
    }

    /**
     * Pure function for calculating smoothed beat energy from FFT frequency data.
     * Isolated for deterministic testing and analysis.
     */
    fun calculateNextBeatEnergy(currentEnergy: Float, fft: ByteArray?): Float {
        if (fft == null || fft.isEmpty()) return currentEnergy
        val bins = (fft.size / 2 * 0.15f).toInt().coerceAtLeast(1)
        var sum = 0f
        var count = 0
        for (i in 1..bins) {
            val r = i * 2
            val im = r + 1
            if (im < fft.size) {
                val a = fft[r].toFloat()
                val b = fft[im].toFloat()
                sum += sqrt(a * a + b * b)
                count++
            }
        }
        val n = ((if (count == 0) 0f else sum / count) / 40f).coerceIn(0f, 1f)
        return if (n > currentEnergy) n else currentEnergy * 0.85f + n * 0.15f
    }
}
