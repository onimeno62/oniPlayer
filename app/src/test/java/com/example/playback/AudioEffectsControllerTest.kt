package com.example.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.EqualizerPresetEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioEffectsControllerTest {

    private lateinit var context: Context
    private lateinit var controller: AudioEffectsController

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        controller = AudioEffectsController(context)
    }

    @Test
    fun setBand_updatesSpecificBandWithinRange() {
        controller.setBand(0, 3.5f)
        controller.setBand(2, -4.0f)
        controller.setBand(4, 6.0f)

        val gains = controller.getBandGains()
        assertEquals(3.5f, gains[0], 0.001f)
        assertEquals(0.0f, gains[1], 0.001f)
        assertEquals(-4.0f, gains[2], 0.001f)
        assertEquals(0.0f, gains[3], 0.001f)
        assertEquals(6.0f, gains[4], 0.001f)
    }

    @Test
    fun setBand_ignoresOutOfBoundsIndices() {
        controller.setBand(-1, 5f)
        controller.setBand(5, 5f)
        controller.setBand(100, 5f)

        val gains = controller.getBandGains()
        assertArrayEquals(FloatArray(5), gains, 0.001f)
    }

    @Test
    fun setBass_clampsBetweenZeroAndHundred() {
        controller.setBass(50f)
        assertEquals(50f, controller.getBass(), 0.001f)

        controller.setBass(-10f)
        assertEquals(0f, controller.getBass(), 0.001f)

        controller.setBass(150f)
        assertEquals(100f, controller.getBass(), 0.001f)
    }

    @Test
    fun setVirtualizer_clampsBetweenZeroAndHundred() {
        controller.setVirtualizer(75f)
        assertEquals(75f, controller.getVirtualizer(), 0.001f)

        controller.setVirtualizer(-5f)
        assertEquals(0f, controller.getVirtualizer(), 0.001f)

        controller.setVirtualizer(120f)
        assertEquals(100f, controller.getVirtualizer(), 0.001f)
    }

    @Test
    fun applyPreset_updatesAllBandsBassAndVirtualizer() {
        val preset = EqualizerPresetEntity(
            name = "Rock",
            isCustom = false,
            band60Hz = 5.0f,
            band230Hz = 3.0f,
            band910Hz = -2.0f,
            band4kHz = 3.0f,
            band14kHz = 6.0f,
            bassBoost = 50.0f,
            virtualizer = 25.0f
        )

        controller.applyPreset(preset)

        val gains = controller.getBandGains()
        assertEquals(5.0f, gains[0], 0.001f)
        assertEquals(3.0f, gains[1], 0.001f)
        assertEquals(-2.0f, gains[2], 0.001f)
        assertEquals(3.0f, gains[3], 0.001f)
        assertEquals(6.0f, gains[4], 0.001f)
        assertEquals(50.0f, controller.getBass(), 0.001f)
        assertEquals(25.0f, controller.getVirtualizer(), 0.001f)
    }

    @Test
    fun calculateNextBeatEnergy_withNullOrEmptyFft_returnsCurrentEnergy() {
        val current = 0.5f
        assertEquals(current, controller.calculateNextBeatEnergy(current, null), 0.001f)
        assertEquals(current, controller.calculateNextBeatEnergy(current, ByteArray(0)), 0.001f)
    }

    @Test
    fun calculateNextBeatEnergy_withHighFft_increasesEnergy() {
        val fft = ByteArray(64) { 80.toByte() }
        val current = 0.1f
        val next = controller.calculateNextBeatEnergy(current, fft)
        assertTrue(next > current)
        assertTrue(next <= 1.0f)
    }

    @Test
    fun calculateNextBeatEnergy_withZeroFft_smoothlyDecays() {
        val fft = ByteArray(64) { 0 }
        val current = 0.8f
        val next = controller.calculateNextBeatEnergy(current, fft)
        assertEquals(current * 0.85f, next, 0.001f)
    }

    @Test
    fun release_resetsBeatEnergyAndIsIdempotent() {
        controller.attachToAudioSession(1)
        controller.release()

        assertEquals(0f, controller.beatEnergy.value, 0.001f)

        // Multiple calls should succeed without error
        controller.release()
        assertEquals(0f, controller.beatEnergy.value, 0.001f)
    }

    @Test
    fun release_succeedsEvenWhenUnattached() {
        // Calling release when never attached or partially attached must be safe and idempotent
        controller.release()
        assertEquals(0f, controller.beatEnergy.value, 0.001f)
    }
}
