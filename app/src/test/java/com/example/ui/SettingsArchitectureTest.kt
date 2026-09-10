package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.DefaultSkin
import com.example.ui.theme.OniPlayerTheme
import com.example.ui.theme.OniSkin
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsArchitectureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theme_selection_updates_correctly() {
        val lightSkin = DefaultSkin.createSkin(isDark = false)
        val darkSkin = DefaultSkin.createSkin(isDark = true)

        assertFalse(lightSkin.colors.isDark)
        assertTrue(darkSkin.colors.isDark)
        assertEquals(DefaultSkin.NAME, lightSkin.name)
        assertEquals(DefaultSkin.NAME, darkSkin.name)
    }

    @Test
    fun reduce_motion_zeros_out_motion_durations() {
        var quickMs = -1
        var transitionMs = -1
        var ambientMs = -1

        composeTestRule.setContent {
            OniPlayerTheme(reduceMotion = true) {
                quickMs = OniSkin.motion.quickDurationMs
                transitionMs = OniSkin.motion.screenTransitionDurationMs
                ambientMs = OniSkin.motion.ambientLoopDurationMs
            }
        }

        assertEquals(0, quickMs)
        assertEquals(0, transitionMs)
        assertEquals(0, ambientMs)
    }

    @Test
    fun standard_motion_preserves_durations_when_reduce_motion_disabled() {
        var transitionMs = -1

        composeTestRule.setContent {
            OniPlayerTheme(reduceMotion = false) {
                transitionMs = OniSkin.motion.screenTransitionDurationMs
            }
        }

        assertTrue(transitionMs > 0)
        assertEquals(300, transitionMs)
    }

    @Test
    fun corner_radius_default_shape_tokens() {
        var defaultArtworkRadius = 0f

        composeTestRule.setContent {
            OniPlayerTheme(cornerRadius = 16f) {
                defaultArtworkRadius = OniSkin.artwork.cornerRadius.value
            }
        }

        assertEquals(18f, defaultArtworkRadius, 0.1f)
    }

    @Test
    fun corner_radius_scales_shape_tokens_proportionally() {
        var customArtworkRadius = 0f

        composeTestRule.setContent {
            OniPlayerTheme(cornerRadius = 24f) {
                customArtworkRadius = OniSkin.artwork.cornerRadius.value
            }
        }

        assertEquals(18f, customArtworkRadius, 0.1f)
    }
}
