package com.example.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.entity.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val TAG = "OniAudioEngine"

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var visualizer: Visualizer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _beatEnergy = MutableStateFlow(0f)
    val beatEnergy: StateFlow<Float> = _beatEnergy.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var isPrepared = false

    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Direct values for UI sliders / states
    private var currentBandGains = floatArrayOf(0f, 0f, 0f, 0f, 0f) // 5 bands
    private var currentBassBoostLevel = 0f // 0 - 100%
    private var currentVirtualizerLevel = 0f // 0 - 100%

    // Equalizer preset callback
    var onPlaybackCompleted: (() -> Unit)? = null

    init {
        // Prepare default MediaPlayer
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer?.release()
            releaseAudioEffects()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    _duration.value = mp.duration.toLong()
                    isPrepared = true
                    _isPreparing.value = false
                    mp.start()
                    _isPlaying.value = true
                    setupAudioEffects(mp.audioSessionId)
                    startPositionUpdate()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    stopPositionUpdate()
                    _position.value = 0L
                    onPlaybackCompleted?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    _isPreparing.value = false
                    stopPositionUpdate()
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPlayer: ${e.message}", e)
        }
    }

    private fun setupAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            releaseAudioEffects()

            // 1. Equalizer setup
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                // Let's set initial gains
                applyEqualizerGains()
            }

            // 2. Bass Boost setup
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
                applyBassBoost()
            }

            // 3. Virtualizer setup
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
                applyVirtualizer()
            }

            // 4. Visualizer setup
            try {
                val captureSizeRange = Visualizer.getCaptureSizeRange()
                if (captureSizeRange != null && captureSizeRange.size >= 2) {
                    val minSize = captureSizeRange[0]
                    val maxSize = captureSizeRange[1]
                    val finalCaptureSize = 1024.coerceIn(minSize, maxSize)

                    visualizer = Visualizer(audioSessionId).apply {
                        captureSize = finalCaptureSize
                        setDataCaptureListener(
                            object : Visualizer.OnDataCaptureListener {
                                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                                    if (fft == null || fft.isEmpty()) return
                                    val numBins = fft.size / 2
                                    val targetBins = (numBins * 0.15f).toInt().coerceAtLeast(1)
                                    var sumMagnitude = 0f
                                    var count = 0
                                    for (i in 1..targetBins) {
                                        val realIndex = i * 2
                                        val imagIndex = i * 2 + 1
                                        if (imagIndex < fft.size) {
                                            val r = fft[realIndex].toFloat()
                                            val im = fft[imagIndex].toFloat()
                                            val magnitude = kotlin.math.sqrt(r * r + im * im)
                                            sumMagnitude += magnitude
                                            count++
                                        }
                                    }
                                    val averageRaw = if (count > 0) sumMagnitude / count else 0f
                                    val normalized = (averageRaw / 40f).coerceIn(0f, 1f)

                                    val current = _beatEnergy.value
                                    _beatEnergy.value = if (normalized > current) {
                                        normalized
                                    } else {
                                        current * 0.85f + normalized * 0.15f
                                    }
                                }
                            },
                            20000, // ~20Hz in millihertz
                            false,
                            true
                        )
                        enabled = true
                    }
                    Log.d(TAG, "Visualizer configured successfully with capture size $finalCaptureSize")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to create/configure Visualizer: ${ex.message}", ex)
            }

            Log.d(TAG, "Audio effects configured successfully for session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/configure audio effects: ${e.message}", e)
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null

            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (ex: Exception) {
                Log.e(TAG, "Error disabling/releasing visualizer: ${ex.message}")
            }
            visualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects: ${e.message}")
        }
    }

    fun setSongWithoutPlaying(song: SongEntity) {
        _currentSong.value = song
        _duration.value = song.duration
        _position.value = 0L
        isPrepared = false
    }

    fun play(song: SongEntity) {
        _currentSong.value = song
        stopPositionUpdate()
        _position.value = 0L
        isPrepared = false
        _isPreparing.value = true

        try {
            initMediaPlayer() // Re-initialize to cleanly apply fresh audioSessionId

            mediaPlayer?.reset()
            val file = java.io.File(song.filePath)
            if (file.exists() && file.isFile) {
                java.io.FileInputStream(file).use { fis ->
                    mediaPlayer?.setDataSource(fis.fd)
                }
            } else {
                mediaPlayer?.setDataSource(context, Uri.parse(song.filePath))
            }
            mediaPlayer?.prepareAsync()
            startPlaybackService()
        } catch (e: Exception) {
            Log.e(TAG, "Error trying to play song: ${song.title}, path: ${song.filePath}. Msg: ${e.message}", e)
            _isPlaying.value = false
            _isPreparing.value = false
        }
    }

    fun updateCurrentSongMetadata(song: SongEntity) {
        val current = _currentSong.value ?: return
        if (current.id == song.id) {
            _currentSong.value = song
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
                stopPositionUpdate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback: ${e.message}")
        }
    }

    fun resume() {
        try {
            val song = _currentSong.value
            if (song != null && !isPrepared) {
                play(song)
            } else if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
                _isPlaying.value = true
                startPositionUpdate()
                startPlaybackService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback: ${e.message}")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            _isPlaying.value = false
            _isPreparing.value = false
            stopPositionUpdate()
            _position.value = 0L
            stopPlaybackService()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
    }

    fun clearCurrentSource() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            releaseAudioEffects()
            isPrepared = false
            _isPreparing.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting/releasing media player to release file: ${e.message}")
        }
    }

    fun seekTo(posMs: Long) {
        try {
            mediaPlayer?.seekTo(posMs.toInt())
            _position.value = posMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position $posMs: ${e.message}")
        }
    }

    fun release() {
        scope.cancel()
        _isPreparing.value = false
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioEffects()
        stopPlaybackService()
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback service: ${e.message}")
        }
    }

    private fun stopPlaybackService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop playback service: ${e.message}")
        }
    }

    private fun startPositionUpdate() {
        updateJob?.cancel()
        updateJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _position.value = it.currentPosition.toLong()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdate() {
        updateJob?.cancel()
        updateJob = null
    }

    // --- Equalizer & FX API ---

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in 0..4) {
            currentBandGains[bandIndex] = gainDb
            applyEqualizerGains()
        }
    }

    fun getBandGains(): FloatArray = currentBandGains

    private fun applyEqualizerGains() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            // Standard Android Eq usually has 5 bands.
            // Map our 5 band index to standard bands
            val minEQLevel = eq.bandLevelRange[0] // e.g. -1500 millibels (-15dB)
            val maxEQLevel = eq.bandLevelRange[1] // e.g. +1500 millibels (+15dB)

            for (i in 0 until minOf(5, numBands.toInt())) {
                val gain = currentBandGains[i] // e.g. -12 to 12 dB
                val milliBels = (gain * 100).toInt().coerceIn(minEQLevel.toInt(), maxEQLevel.toInt())
                eq.setBandLevel(i.toShort(), milliBels.toShort())
            }
            Log.d(TAG, "Equalizer gains applied successfully: ${currentBandGains.joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying equalizer gains: ${e.message}")
        }
    }

    fun setBassBoost(levelPercent: Float) {
        currentBassBoostLevel = levelPercent.coerceIn(0f, 100f)
        applyBassBoost()
    }

    fun getBassBoost(): Float = currentBassBoostLevel

    private fun applyBassBoost() {
        val bb = bassBoost ?: return
        try {
            if (bb.strengthSupported) {
                // Map 0-100% to 0-1000 strength
                val strength = (currentBassBoostLevel * 10).toInt().coerceIn(0, 1000)
                bb.setStrength(strength.toShort())
                Log.d(TAG, "Bass Boost applied: strength = $strength")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Bass Boost strength: ${e.message}")
        }
    }

    fun setVirtualizer(levelPercent: Float) {
        currentVirtualizerLevel = levelPercent.coerceIn(0f, 100f)
        applyVirtualizer()
    }

    fun getVirtualizer(): Float = currentVirtualizerLevel

    private fun applyVirtualizer() {
        val v = virtualizer ?: return
        try {
            if (v.strengthSupported) {
                // Map 0-100% to 0-1000 strength
                val strength = (currentVirtualizerLevel * 10).toInt().coerceIn(0, 1000)
                v.setStrength(strength.toShort())
                Log.d(TAG, "Virtualizer applied: strength = $strength")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Virtualizer strength: ${e.message}")
        }
    }

    fun applyPreset(preset: com.example.data.entity.EqualizerPresetEntity) {
        currentBandGains[0] = preset.band60Hz
        currentBandGains[1] = preset.band230Hz
        currentBandGains[2] = preset.band910Hz
        currentBandGains[3] = preset.band4kHz
        currentBandGains[4] = preset.band14kHz
        currentBassBoostLevel = preset.bassBoost
        currentVirtualizerLevel = preset.virtualizer

        applyEqualizerGains()
        applyBassBoost()
        applyVirtualizer()
    }
}
