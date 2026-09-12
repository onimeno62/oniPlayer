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

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

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

        recordJob = scope.launch {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size")
                _isMicEnabled.value = false
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

                        // Real-time pitch estimation when vocal amplitude is singing-level
                        if (level > 8f) {
                            val estimatedHz = estimatePitchHz(buffer, readSize, sampleRate)
                            _vocalPitchNote.value = if (estimatedHz > 50f && estimatedHz < 1200f) {
                                hzToMusicalNote(estimatedHz)
                            } else {
                                ""
                            }
                        } else {
                            _vocalPitchNote.value = ""
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
            } catch (e: Exception) {
                Log.e(TAG, "Error recording audio: ${e.message}", e)
                _isMicEnabled.value = false
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
        }
        stopAudioTrack()
    }

    /**
     * Estimates fundamental frequency (Hz) using autocorrelation over zero-mean window.
     */
    private fun estimatePitchHz(buffer: ShortArray, size: Int, sampleRate: Int): Float {
        val maxLag = sampleRate / 60  // ~60 Hz lowest male vocal pitch
        val minLag = sampleRate / 1000 // ~1000 Hz highest vocal fundamental pitch
        if (size <= maxLag * 2) return 0f

        var bestLag = -1
        var maxCorr = 0.0

        for (lag in minLag..maxLag) {
            var corr = 0.0
            val len = minOf(size - lag, 512)
            for (i in 0 until len) {
                corr += buffer[i].toDouble() * buffer[i + lag].toDouble()
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0) sampleRate.toFloat() / bestLag else 0f
    }

    /**
     * Converts frequency in Hz to standard musical note representation (e.g. C4, D#4).
     */
    private fun hzToMusicalNote(hz: Float): String {
        if (hz <= 20f) return ""
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val midiNumber = (12 * (log2(hz / 440.0) / log2(2.0)) + 69).roundToInt()
        if (midiNumber < 12 || midiNumber > 120) return ""
        val noteName = noteNames[midiNumber % 12]
        val octave = (midiNumber / 12) - 1
        return "$noteName$octave"
    }
}
