package com.example.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.library.components.LibrarySpacing
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification test suite for oniPlayer Phase 1 Default Skin Design System.
 * Tests tokens across all visual domains to ensure complete compliance with
 * .ai/skills/oniplayer-default-skin-design-system.md.
 */
class DefaultSkinDesignSystemTest {

    @Test
    fun `default skin light color palette matches specification`() {
        val lightSkin = DefaultSkin.createSkin(isDark = false)
        val colors = lightSkin.colors

        assertFalse(colors.isDark)
        assertEquals(DefaultSkin.LightBackground, colors.background)
        assertEquals(DefaultSkin.LightSurface, colors.surface)
        assertEquals(DefaultSkin.LightElevatedSurface, colors.surfaceElevated)
        assertEquals(DefaultSkin.LightPrimaryText, colors.textPrimary)
        assertEquals(DefaultSkin.LightSecondaryText, colors.textSecondary)
        assertEquals(DefaultSkin.LightTertiaryText, colors.textTertiary)
        assertEquals(DefaultSkin.LightDivider, colors.divider)
        assertEquals(DefaultSkin.LightDisabled, colors.disabled)

        // Accent tokens
        assertEquals(DefaultSkin.DefaultPrimaryBlue, colors.primary)
        assertEquals(DefaultSkin.DefaultPrimaryBlue, colors.accent)
        assertEquals(DefaultSkin.DefaultBrightBlue, colors.accentSecondary)
        assertEquals(DefaultSkin.DefaultSoftBlue, colors.primaryContainer)
        assertEquals(DefaultSkin.DefaultBlueSurface, colors.surfaceVariant)

        // M3 ColorScheme mapping verification
        val m3Scheme = colors.toMaterialColorScheme()
        assertEquals(colors.primary, m3Scheme.primary)
        assertEquals(colors.background, m3Scheme.background)
        assertEquals(colors.surface, m3Scheme.surface)
        assertEquals(colors.error, m3Scheme.error)
    }

    @Test
    fun `default skin dark color palette matches specification`() {
        val darkSkin = DefaultSkin.createSkin(isDark = true)
        val colors = darkSkin.colors

        assertTrue(colors.isDark)
        assertEquals(DefaultSkin.DarkBackground, colors.background)
        assertEquals(DefaultSkin.DarkSurface, colors.surface)
        assertEquals(DefaultSkin.DarkElevatedSurface, colors.surfaceElevated)
        assertEquals(DefaultSkin.DarkPrimaryText, colors.textPrimary)
        assertEquals(DefaultSkin.DarkSecondaryText, colors.textSecondary)
        assertEquals(DefaultSkin.DarkTertiaryText, colors.textTertiary)
        assertEquals(DefaultSkin.DarkDivider, colors.divider)
        assertEquals(DefaultSkin.DarkDisabled, colors.disabled)

        // Accent tokens
        assertEquals(DefaultSkin.DefaultPrimaryBlue, colors.primary)
        assertEquals(DefaultSkin.DefaultPrimaryBlue, colors.accent)
        assertEquals(DefaultSkin.DefaultBrightBlue, colors.accentSecondary)
        assertEquals(DefaultSkin.DarkSoftBlue, colors.primaryContainer)

        // M3 ColorScheme mapping verification
        val m3Scheme = colors.toMaterialColorScheme()
        assertEquals(colors.primary, m3Scheme.primary)
        assertEquals(colors.background, m3Scheme.background)
        assertEquals(colors.surface, m3Scheme.surface)
    }

    @Test
    fun `custom accent color propagates to color tokens and containers`() {
        val customAccent = Color(0xFFFF9800)
        val skin = DefaultSkin.createSkin(isDark = false, accentColor = customAccent)
        assertEquals(customAccent, skin.colors.primary)
        assertEquals(customAccent, skin.colors.accent)
    }

    @Test
    fun `typography tokens match complete scale in design system specification`() {
        val typo = OniTypographyTokens.defaultTypography()

        assertEquals(30.sp, typo.displayLarge.fontSize)
        assertEquals(26.sp, typo.displayMedium.fontSize)
        assertEquals(22.sp, typo.titleLarge.fontSize)
        assertEquals(18.sp, typo.titleMedium.fontSize)
        assertEquals(16.sp, typo.titleSmall.fontSize)
        assertEquals(16.sp, typo.bodyLarge.fontSize)
        assertEquals(14.sp, typo.bodyMedium.fontSize)
        assertEquals(13.sp, typo.bodySmall.fontSize)
        assertEquals(14.sp, typo.labelLarge.fontSize)
        assertEquals(12.sp, typo.labelMedium.fontSize)
        assertEquals(12.sp, typo.caption.fontSize)

        // M3 Typography mapping
        val m3Typo = typo.toMaterialTypography()
        assertEquals(typo.bodyLarge, m3Typo.bodyLarge)
        assertEquals(typo.titleMedium, m3Typo.titleMedium)
        assertEquals(typo.displayLarge, m3Typo.displayLarge)
    }

    @Test
    fun `shape tokens match scale and semantic roles in specification`() {
        val shapes = OniShapeTokens.defaultShapes()

        // Semantic roles
        assertEquals(shapes.medium, shapes.button)
        assertEquals(shapes.large, shapes.card)
        assertEquals(shapes.medium, shapes.listItem)
        assertEquals(shapes.xl, shapes.dialog)
        assertEquals(shapes.xl, shapes.bottomSheet)
        assertEquals(shapes.large, shapes.artwork)
        assertEquals(shapes.small, shapes.chip)
        assertEquals(shapes.full, shapes.pill)

        // M3 Shapes mapping
        val m3Shapes = shapes.toMaterialShapes()
        assertEquals(shapes.xs, m3Shapes.extraSmall)
        assertEquals(shapes.small, m3Shapes.small)
        assertEquals(shapes.medium, m3Shapes.medium)
        assertEquals(shapes.large, m3Shapes.large)
        assertEquals(shapes.xl, m3Shapes.extraLarge)
    }

    @Test
    fun `spacing tokens match scale, semantic roles, and library spacing bridge`() {
        val spacing = OniSpacingTokens.defaultSpacing()

        // Scale
        assertEquals(4.dp, spacing.xxs)
        assertEquals(8.dp, spacing.xs)
        assertEquals(12.dp, spacing.sm)
        assertEquals(16.dp, spacing.md)
        assertEquals(20.dp, spacing.lg)
        assertEquals(24.dp, spacing.xl)
        assertEquals(32.dp, spacing.xxl)
        assertEquals(40.dp, spacing.xxxl)
        assertEquals(48.dp, spacing.huge)

        // Semantic roles
        assertEquals(16.dp, spacing.screenHorizontal)
        assertEquals(16.dp, spacing.screenVertical)
        assertEquals(24.dp, spacing.section)
        assertEquals(12.dp, spacing.listItem)
        assertEquals(16.dp, spacing.card)
        assertEquals(16.dp, spacing.content)
        assertEquals(8.dp, spacing.control)

        // LibrarySpacing backward-compatibility bridge
        assertEquals(spacing.xxs, LibrarySpacing.xs)
        assertEquals(spacing.xs, LibrarySpacing.sm)
        assertEquals(spacing.sm, LibrarySpacing.md)
        assertEquals(spacing.md, LibrarySpacing.lg)
        assertEquals(spacing.lg, LibrarySpacing.xl)
        assertEquals(spacing.xl, LibrarySpacing.xxl)
        assertEquals(spacing.xxl, LibrarySpacing.xxxl)
    }

    @Test
    fun `elevation tokens match specification`() {
        val elevation = OniElevationTokens.defaultElevation()

        assertEquals(0.dp, elevation.flat)
        assertEquals(2.dp, elevation.raised)
        assertEquals(6.dp, elevation.floating)
        assertTrue(elevation.shadowAlpha > 0f)
        assertTrue(elevation.ambientAlpha > 0f)
    }

    @Test
    fun `surface tokens provide flat, soft, elevated, and frosted styles`() {
        val skin = DefaultSkin.createSkin(isDark = false)
        val surfaces = skin.surfaces

        // Flat
        assertEquals(skin.colors.background, surfaces.flat.containerColor)
        assertEquals(0.dp, surfaces.flat.elevation)

        // Soft
        assertEquals(skin.colors.surface, surfaces.soft.containerColor)
        assertNotNull(surfaces.soft.borderStroke)

        // Elevated
        assertEquals(skin.colors.surfaceElevated, surfaces.elevated.containerColor)
        assertEquals(2.dp, surfaces.elevated.elevation)

        // Frosted
        assertNotNull(surfaces.frosted.borderStroke)
        assertTrue(surfaces.frosted.alpha < 0.2f)
    }

    @Test
    fun `motion tokens provide standard durations and easings`() {
        val motion = OniMotionTokens.defaultMotion()

        assertEquals(120, motion.buttonPressDurationMs)
        assertEquals(200, motion.componentStateDurationMs)
        assertEquals(300, motion.screenTransitionDurationMs)
        assertEquals(500, motion.artworkTransitionDurationMs)
        assertEquals(12000, motion.ambientLoopDurationMs)
        assertNotNull(motion.standardEasing)
        assertNotNull(motion.emphasizedEasing)
        assertNotNull(motion.decelerateEasing)
    }

    @Test
    fun `artwork tokens match specification`() {
        val artwork = DefaultSkin.createSkin(isDark = false).artwork

        assertEquals(18.dp, artwork.cornerRadius)
        assertEquals(4.dp, artwork.shadowElevation)
        assertEquals(20f, artwork.blurStrength)
        assertFalse(artwork.reflectionEnabled)
    }

    @Test
    fun `playback control tokens match specification`() {
        val controls = DefaultSkin.createSkin(isDark = false).playbackControls

        assertEquals(64.dp, controls.primaryControlSize)
        assertEquals(48.dp, controls.secondaryControlSize)
        assertEquals(40.dp, controls.tertiaryControlSize)
        assertEquals(4.dp, controls.primaryElevation)
        assertEquals(0.dp, controls.secondaryElevation)
    }

    @Test
    fun `navigation tokens match specification`() {
        val nav = DefaultSkin.createSkin(isDark = false).navigation

        assertEquals(64.dp, nav.barHeight)
        assertEquals(24.dp, nav.marginHorizontal)
        assertEquals(12.dp, nav.marginBottom)
        assertEquals(0.dp, nav.elevation)
    }

    @Test
    fun `skin abstraction metadata and contract`() {
        val skin: OniSkinDefinition = DefaultSkin.createSkin(isDark = false)

        assertEquals("default", skin.id)
        assertEquals("Default Skin", skin.name)
        assertNotNull(skin.colors)
        assertNotNull(skin.typography)
        assertNotNull(skin.shapes)
        assertNotNull(skin.spacing)
        assertNotNull(skin.elevation)
        assertNotNull(skin.surfaces)
        assertNotNull(skin.motion)
        assertNotNull(skin.artwork)
        assertNotNull(skin.playbackControls)
        assertNotNull(skin.navigation)
    }
}
