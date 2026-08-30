package com.example.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages configured playback delay and active countdown state for auto-advancing tracks.
 */
class PlaybackDelayController(
    private val scope: CoroutineScope,
    private val onDelayFinished: () -> Unit
) {
    private var _delaySeconds: Int = 0
    val delaySeconds: Int
        get() = _delaySeconds

    val hasDelay: Boolean
        get() = _delaySeconds > 0

    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private var countdownJob: Job? = null
    private var countdownGeneration: Long = 0L
    private var isReleased = false

    /**
     * Updates configured delay duration.
     * Setting a new value clamps negative values to 0 and cancels any currently active countdown.
     * It does not automatically start a new countdown.
     */
    fun setDelay(seconds: Int) {
        if (isReleased) return
        _delaySeconds = seconds.coerceAtLeast(0)
        cancelDelay()
    }

    /**
     * Cancels any active countdown without invoking [onDelayFinished].
     */
    fun cancelDelay() {
        countdownGeneration++
        countdownJob?.cancel()
        countdownJob = null
        _countdown.value = null
    }

    /**
     * Starts the countdown using an immutable snapshot of the current delay duration.
     */
    fun startDelay() {
        if (isReleased) return
        cancelDelay()
        val delayDuration = _delaySeconds
        if (delayDuration <= 0) return

        val generation = countdownGeneration
        countdownJob = scope.launch {
            for (remaining in delayDuration downTo 1) {
                if (!isActive || generation != countdownGeneration || isReleased) return@launch
                _countdown.value = remaining
                delay(1000)
            }
            if (!isActive || generation != countdownGeneration || isReleased) return@launch
            _countdown.value = null
            countdownJob = null
            onDelayFinished()
        }
    }

    /**
     * Releases the controller, cancelling any running countdown and preventing future callbacks or operations.
     */
    fun release() {
        isReleased = true
        cancelDelay()
    }
}
