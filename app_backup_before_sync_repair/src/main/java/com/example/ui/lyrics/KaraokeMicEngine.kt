package com.example.ui.lyrics

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
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
import kotlin.math.sqrt

class KaraokeMicEngine {
    private val TAG = "KaraokeMicEngine"

    private val _isMicEnabled = MutableStateFlow(false)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _micGain = MutableStateFlow(1.0f)
    val micGain: StateFlow<Float> = _micGain.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun setMicGain(gain: Float) {
        _micGain.value = gain.coerceIn(0.1f, 3.0f)
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
                        for (i in 0 until readSize) {
                            val sample = buffer[i] * _micGain.value
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readSize)
                        // Normalize RMS to a nice 0f..100f range for UI responsiveness
                        val level = (rms / 32767.0 * 350f).coerceIn(0.0, 100.0).toFloat()
                        _amplitude.value = level
                    }
                    delay(30) // ~30 fps for smooth animation updates
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
        }
    }
}
