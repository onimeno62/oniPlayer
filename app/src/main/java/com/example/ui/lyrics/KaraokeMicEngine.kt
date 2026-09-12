package com.example.ui.lyrics

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Microphone pitch detection states for clear, non-jittery UI feedback.
 */
sealed class PitchStatus {
    data object Idle : PitchStatus()
    data object Listening : PitchStatus()
    data object Detecting : PitchStatus()
    data object NoClearPitch : PitchStatus()
    data class Detected(val note: String, val frequencyHz: Float) : PitchStatus()
}

class KaraokeMicEngine {
    private val TAG = "KaraokeMicEngine"

    private val _isMicEnabled = MutableStateFlow(false)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _micGain = MutableStateFlow(1.0f)
    val micGain: StateFlow<Float> = _micGain.asStateFlow()

    private val _isAudioPassThroughEnabled = MutableStateFlow(false)
    val isAudioPassThroughEnabled: StateFlow<Boolean> = _isAudioPassThroughEnabled.asStateFlow()

    private val _vocalPitchNote = MutableStateFlow("")
    val vocalPitchNote: StateFlow<String> = _vocalPitchNote.asStateFlow()

    private val _pitchStatus = MutableStateFlow<PitchStatus>(PitchStatus.Idle)
    val pitchStatus: StateFlow<PitchStatus> = _pitchStatus.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Pitch smoothing state
    private var recentPitchHistory = mutableListOf<Float>()
    private var lastStableNote = ""
    private var consecutiveSilenceFrames = 0

    fun setMicGain(gain: Float) {
        _micGain.value = gain.coerceIn(0.1f, 3.0f)
    }

    fun setAudioPassThrough(enabled: Boolean) {
        _isAudioPassThroughEnabled.value = enabled
        if (!enabled) {
            stopAudioTrack()
        }
    }

    fun toggleAudioPassThrough() {
        setAudioPassThrough(!_isAudioPassThroughEnabled.value)
    }

    @SuppressLint("MissingPermission")
    fun startMic() {
        if (_isMicEnabled.value) return
        _isMicEnabled.value = true
        _pitchStatus.value = PitchStatus.Listening
        recentPitchHistory.clear()
        lastStableNote = ""
        consecutiveSilenceFrames = 0

        recordJob = scope.launch {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size")
                _isMicEnabled.value = false
                _pitchStatus.value = PitchStatus.Idle
                return@launch
            }

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord could not be initialized")
                    _isMicEnabled.value = false
                    _pitchStatus.value = PitchStatus.Idle
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)

                while (_isMicEnabled.value) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        val currentGain = _micGain.value
                        for (i in 0 until readSize) {
                            val sample = buffer[i] * currentGain
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readSize)
                        val level = (rms / 32767.0 * 350f).coerceIn(0.0, 100.0).toFloat()
                        _amplitude.value = level

                        // Noise-gated pitch estimation: require minimum RMS level ~10f to reject room noise
                        if (level > 10f) {
                            consecutiveSilenceFrames = 0
                            val estimatedHz = estimatePitchHz(buffer, readSize, sampleRate)
                            
                            if (estimatedHz in 65f..1100f) {
                                // Add to sliding history for pitch stability
                                recentPitchHistory.add(estimatedHz)
                                if (recentPitchHistory.size > 5) {
                                    recentPitchHistory.removeAt(0)
                                }

                                val medianHz = getMedianFrequency(recentPitchHistory)
                                val candidateNote = hzToMusicalNote(medianHz)

                                if (candidateNote.isNotEmpty()) {
                                    lastStableNote = candidateNote
                                    _vocalPitchNote.value = candidateNote
                                    _pitchStatus.value = PitchStatus.Detected(candidateNote, medianHz)
                                } else {
                                    _pitchStatus.value = PitchStatus.Detecting
                                }
                            } else {
                                if (recentPitchHistory.isNotEmpty()) {
                                    recentPitchHistory.removeAt(0)
                                }
                                if (recentPitchHistory.isEmpty()) {
                                    _vocalPitchNote.value = ""
                                    _pitchStatus.value = PitchStatus.NoClearPitch
                                }
                            }
                        } else {
                            // Silence / ambient noise
                            consecutiveSilenceFrames++
                            if (consecutiveSilenceFrames >= 3) {
                                recentPitchHistory.clear()
                                lastStableNote = ""
                                _vocalPitchNote.value = ""
                                _pitchStatus.value = PitchStatus.Listening
                            }
                        }

                        // Low-latency Headphone Vocal Monitor Passthrough
                        if (_isAudioPassThroughEnabled.value) {
                            ensureAudioTrack(sampleRate, bufferSize)
                            val outBuffer = ShortArray(readSize)
                            for (i in 0 until readSize) {
                                val amplified = (buffer[i] * currentGain).toInt().coerceIn(-32768, 32767)
                                outBuffer[i] = amplified.toShort()
                            }
                            audioTrack?.write(outBuffer, 0, readSize)
                        } else if (audioTrack != null) {
                            stopAudioTrack()
                        }
                    }
                    delay(25) // ~40 fps
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission RECORD_AUDIO not granted: ${e.message}")
                _isMicEnabled.value = false
                _pitchStatus.value = PitchStatus.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error recording audio: ${e.message}", e)
                _isMicEnabled.value = false
                _pitchStatus.value = PitchStatus.Idle
            } finally {
                stopInternal()
            }
        }
    }

    private fun ensureAudioTrack(sampleRate: Int, bufferSize: Int) {
        if (audioTrack != null) return
        try {
            val minOutBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minOutBufferSize.coerceAtLeast(bufferSize))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start vocal monitor track: ${e.message}")
            audioTrack = null
        }
    }

    private fun stopAudioTrack() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack: ${e.message}")
        } finally {
            audioTrack = null
        }
    }

    fun stopMic() {
        _isMicEnabled.value = false
        recordJob?.cancel()
        recordJob = null
        stopInternal()
    }

    private fun stopInternal() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            _amplitude.value = 0f
            _vocalPitchNote.value = ""
            _pitchStatus.value = PitchStatus.Idle
            recentPitchHistory.clear()
            lastStableNote = ""
            consecutiveSilenceFrames = 0
        }
        stopAudioTrack()
    }

    /**
     * Normalized autocorrelation pitch detection with confidence checking.
     */
    private fun estimatePitchHz(buffer: ShortArray, size: Int, sampleRate: Int): Float {
        val maxLag = sampleRate / 65   // ~65 Hz lowest male vocal pitch (C2)
        val minLag = sampleRate / 1100 // ~1100 Hz highest vocal fundamental pitch (C6)
        if (size <= maxLag * 2) return 0f

        var bestLag = -1
        var maxCorr = 0.0
        val windowLen = minOf(size - maxLag, 512)

        // Calculate zero-lag energy
        var zeroLagEnergy = 0.0
        for (i in 0 until windowLen) {
            zeroLagEnergy += buffer[i].toDouble() * buffer[i].toDouble()
        }
        if (zeroLagEnergy < 1e-4) return 0f

        for (lag in minLag..maxLag) {
            var corr = 0.0
            var lagEnergy = 0.0
            for (i in 0 until windowLen) {
                val s0 = buffer[i].toDouble()
                val sLag = buffer[i + lag].toDouble()
                corr += s0 * sLag
                lagEnergy += sLag * sLag
            }
            // Normalized cross-correlation coefficient
            val normCorr = if (lagEnergy > 0) corr / sqrt(zeroLagEnergy * lagEnergy) else 0.0
            if (normCorr > maxCorr) {
                maxCorr = normCorr
                bestLag = lag
            }
        }

        // Require normalized correlation confidence >= 0.55 to avoid octave errors or noise
        return if (bestLag > 0 && maxCorr >= 0.55) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }

    private fun getMedianFrequency(frequencies: List<Float>): Float {
        if (frequencies.isEmpty()) return 0f
        val sorted = frequencies.sorted()
        return sorted[sorted.size / 2]
    }

    /**
     * Converts frequency in Hz to standard musical note representation (e.g. C4, D#4).
     */
    private fun hzToMusicalNote(hz: Float): String {
        if (hz <= 20f) return ""
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val midiNumber = (12 * (log2(hz / 440.0) / log2(2.0)) + 69).roundToInt()
        if (midiNumber < 24 || midiNumber > 108) return ""
        val noteName = noteNames[midiNumber % 12]
        val octave = (midiNumber / 12) - 1
        return "$noteName$octave"
    }
}
