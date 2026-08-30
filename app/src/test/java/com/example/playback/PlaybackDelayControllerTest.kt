package com.example.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackDelayControllerTest {

    @Test
    fun initialState_hasNoDelayAndNullCountdown() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        var callbackCount = 0

        val controller = PlaybackDelayController(
            scope = testScope,
            onDelayFinished = { callbackCount++ }
        )

        assertEquals(0, controller.delaySeconds)
        assertFalse(controller.hasDelay)
        assertNull(controller.countdown.value)
        assertEquals(0, callbackCount)
    }

    @Test
    fun setDelay_updatesDelayAndClampsNegativeValues() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)

        val controller = PlaybackDelayController(
            scope = testScope,
            onDelayFinished = {}
        )

        controller.setDelay(5)
        assertEquals(5, controller.delaySeconds)
        assertTrue(controller.hasDelay)

        controller.setDelay(-3)
        assertEquals(0, controller.delaySeconds)
        assertFalse(controller.hasDelay)
    }

    @Test
    fun setDelay_toZero_cancelsActiveCountdown() = runTest {
        var callbackCount = 0
        val controller = PlaybackDelayController(
            scope = this,
            onDelayFinished = { callbackCount++ }
        )

        controller.setDelay(3)
        controller.startDelay()
        runCurrent()

        assertEquals(3, controller.countdown.value)

        controller.setDelay(0)
        assertNull(controller.countdown.value)

        advanceTimeBy(4000)
        runCurrent()

        assertEquals(0, callbackCount)
        assertNull(controller.countdown.value)
    }

    @Test
    fun startDelay_withNoDelayConfigured_doesNotTriggerCountdownOrCallback() = runTest {
        var callbackCount = 0
        val controller = PlaybackDelayController(
            scope = this,
            onDelayFinished = { callbackCount++ }
        )

        controller.setDelay(0)
        controller.startDelay()
        runCurrent()

        assertNull(controller.countdown.value)
        advanceTimeBy(2000)
        runCurrent()

        assertEquals(0, callbackCount)
    }

    @Test
    fun startDelay_countsDownAndTriggersCompletionCallbackOnce() = runTest {
        var callbackCount = 0
        val controller = PlaybackDelayController(
            scope = this,
            onDelayFinished = { callbackCount++ }
        )

        controller.setDelay(3)
        controller.startDelay()
        runCurrent()

        assertEquals(3, controller.countdown.value)

        advanceTimeBy(1000)
        runCurrent()
        assertEquals(2, controller.countdown.value)

        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, controller.countdown.value)

        advanceTimeBy(1000)
        runCurrent()
        assertNull(controller.countdown.value)
        assertEquals(1, callbackCount)
    }

    @Test
    fun cancelDelay_clearsCountdownAndPreventsCallback() = runTest {
        var callbackCount = 0
        val controller = PlaybackDelayController(
            scope = this,
            onDelayFinished = { callbackCount++ }
        )

        controller.setDelay(5)
        controller.startDelay()
        runCurrent()

        assertEquals(5, controller.countdown.value)

        advanceTimeBy(2000)
        runCurrent()
        assertEquals(3, controller.countdown.value)

        controller.cancelDelay()
        assertNull(controller.countdown.value)

        advanceTimeBy(5000)
        runCurrent()
        assertEquals(0, callbackCount)
    }

    @Test
    fun startDelay_replacesActiveCountdown() = runTest {
        var callbackCount = 0
        val controller = PlaybackDelayController(
            scope = this,
            onDelayFinished = { callbackCount++ }
        )

        controller.setDelay(5)
        controller.startDelay()
        runCurrent()
        assertEquals(5, controller.countdown.value)

        advanceTimeBy(2000)
        runCurrent()
        assertEquals(3, controller.countdown.value)

        // Change delay to 2 and restart
        controller.setDelay(2)
        controller.startDelay()
        runCurrent()
        assertEquals(2, controller.countdown.value)

        advanceTimeBy(2000)
        runCurrent()
        assertNull(controller.countdown.value)
        assertEquals(1, callbackCount)
    }

    @Test
    fun release_cancelsCountdownAndPreventsFutureOperations() = runTest {
        var callbackCount = 0
        val controller = PlaybackDelayController(
            scope = this,
            onDelayFinished = { callbackCount++ }
        )

        controller.setDelay(4)
        controller.startDelay()
        runCurrent()
        assertEquals(4, controller.countdown.value)

        controller.release()
        assertNull(controller.countdown.value)

        advanceTimeBy(5000)
        runCurrent()
        assertEquals(0, callbackCount)

        // Starting delay after release should be a no-op
        controller.startDelay()
        runCurrent()
        assertNull(controller.countdown.value)
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(0, callbackCount)
    }

    @Test
    fun release_isIdempotent() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)

        val controller = PlaybackDelayController(
            scope = testScope,
            onDelayFinished = {}
        )

        controller.release()
        controller.release()
    }
}
