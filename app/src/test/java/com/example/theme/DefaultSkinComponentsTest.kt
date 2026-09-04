package com.example.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.button.OniIconButtonStyle
import com.example.ui.components.playback.OniRepeatMode
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.DefaultSkin
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification test suite for oniPlayer Phase 2 Core UI Components.
 * Validates component token resolution, variant contracts, and structural invariants.
 */
class DefaultSkinComponentsTest {

    @Test
    fun `surface token resolution adheres to skin specifications`() {
        val lightSkin = DefaultSkin.createSkin(isDark = false)
        val darkSkin = DefaultSkin.createSkin(isDark = true)

        // Light mode surfaces
        assertEquals(DefaultSkin.LightSurface, lightSkin.surfaces.soft.containerColor)
        assertEquals(DefaultSkin.LightElevatedSurface, lightSkin.surfaces.elevated.containerColor)
        assertEquals(DefaultSkin.LightPrimaryText.copy(alpha = 0.05f), lightSkin.surfaces.frosted.containerColor)

        // Dark mode surfaces
        assertEquals(DefaultSkin.DarkSurface, darkSkin.surfaces.soft.containerColor)
        assertEquals(DefaultSkin.DarkElevatedSurface, darkSkin.surfaces.elevated.containerColor)
        assertEquals(DefaultSkin.DarkPrimaryText.copy(alpha = 0.08f), darkSkin.surfaces.frosted.containerColor)

        // Surface variant types
        val variants = OniSurfaceVariant.values()
        assertTrue(variants.contains(OniSurfaceVariant.Soft))
        assertTrue(variants.contains(OniSurfaceVariant.Elevated))
        assertTrue(variants.contains(OniSurfaceVariant.Frosted))
        assertTrue(variants.contains(OniSurfaceVariant.Flat))
        assertTrue(variants.contains(OniSurfaceVariant.Outlined))
    }

    @Test
    fun `button and control tokens have required touch targets and shapes`() {
        val skin = DefaultSkin.createSkin(isDark = true)

        // Controls meet accessibility guidelines (>= 48dp)
        assertTrue(skin.playbackControls.primaryControlSize >= 48.dp)
        assertTrue(skin.playbackControls.secondaryControlSize >= 48.dp)
        assertTrue(skin.playbackControls.primaryControlSize == 64.dp)
        assertTrue(skin.playbackControls.secondaryControlSize == 48.dp)

        // Shapes
        assertNotNull(skin.shapes.button)
        assertNotNull(skin.shapes.card)
        assertNotNull(skin.shapes.listItem)

        // Button styles exist
        val styles = OniIconButtonStyle.values()
        assertTrue(styles.contains(OniIconButtonStyle.Ghost))
        assertTrue(styles.contains(OniIconButtonStyle.Neutral))
        assertTrue(styles.contains(OniIconButtonStyle.Accent))
        assertTrue(styles.contains(OniIconButtonStyle.Selected))
        assertTrue(styles.contains(OniIconButtonStyle.Floating))
    }

    @Test
    fun `playback repeat modes are complete`() {
        val modes = OniRepeatMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(OniRepeatMode.OFF))
        assertTrue(modes.contains(OniRepeatMode.ALL))
        assertTrue(modes.contains(OniRepeatMode.ONE))
    }

    @Test
    fun `navigation tokens provide required floating bar dimensions`() {
        val skin = DefaultSkin.createSkin(isDark = true)

        assertEquals(64.dp, skin.navigation.barHeight)
        assertEquals(24.dp, skin.navigation.marginHorizontal)
        assertEquals(12.dp, skin.navigation.marginBottom)
        assertEquals(DefaultSkin.DefaultPrimaryBlue, skin.navigation.selectedItemColor)
    }

    @Test
    fun `artwork tokens maintain defined radius and elevation`() {
        val skin = DefaultSkin.createSkin(isDark = true)

        assertEquals(4.dp, skin.artwork.shadowElevation)
        assertEquals(0.20f, skin.artwork.glowAlpha)
        assertNotNull(skin.artwork.shape)
    }
}
