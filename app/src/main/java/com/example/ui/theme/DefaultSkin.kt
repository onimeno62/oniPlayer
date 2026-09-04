package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Reference implementation of the Default Skin design system for oniPlayer.
 * Strictly implements .ai/skills/oniplayer-default-skin-design-system.md.
 *
 * Identity: Clean, soft, modern, musical, premium, calm, responsive.
 * Core visual signature: Soft surfaces + strong hierarchy + restrained accent color + beautiful artwork.
 */
object DefaultSkin {
    const val ID = "default"
    const val NAME = "Default Skin"

    // Default accent colors from specification
    val DefaultPrimaryBlue = Color(0xFF3B73E3)
    val DefaultBrightBlue = Color(0xFF4D8DFF)
    val DefaultSoftBlue = Color(0xFFDCEAFF)
    val DefaultBlueSurface = Color(0xFFEEF5FF)

    // Light neutrals from specification
    val LightBackground = Color(0xFFF7F5F1)
    val LightSurface = Color(0xFFFCFBF9)
    val LightElevatedSurface = Color(0xFFFFFFFF)
    val LightPrimaryText = Color(0xFF202124)
    val LightSecondaryText = Color(0xFF737373)
    val LightTertiaryText = Color(0xFFA0A0A0)
    val LightDivider = Color(0xFFE8E6E2)
    val LightDisabled = Color(0xFFC8C8C8)

    // Dark neutrals from specification
    val DarkBackground = Color(0xFF141418)
    val DarkSurface = Color(0xFF1C1D22)
    val DarkElevatedSurface = Color(0xFF24262E)
    val DarkPrimaryText = Color(0xFFEDEDF0)
    val DarkSecondaryText = Color(0xFF9E9EA7)
    val DarkTertiaryText = Color(0xFF6E707A)
    val DarkDivider = Color(0xFF2C2D35)
    val DarkDisabled = Color(0xFF50525C)
    val DarkSoftBlue = Color(0xFF1E2B45)
    val DarkBlueSurface = Color(0xFF162033)

    /**
     * Builds the complete set of tokens for the Default Skin in either light or dark mode.
     * Supports user-selected custom accent color (defaults to #3B73E3).
     */
    fun createSkin(
        isDark: Boolean,
        accentColor: Color = DefaultPrimaryBlue,
        accentSecondary: Color = DefaultBrightBlue,
        accentGlow: Color? = null
    ): OniSkinTokens {
        val glow = accentGlow ?: accentColor.copy(alpha = if (isDark) 0.5f else 0.25f)

        val colors = if (isDark) {
            OniColorTokens(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = DarkSoftBlue,
                onPrimaryContainer = DefaultBrightBlue,
                background = DarkBackground,
                onBackground = DarkPrimaryText,
                surface = DarkSurface,
                onSurface = DarkPrimaryText,
                surfaceVariant = DarkSurface,
                onSurfaceVariant = DarkSecondaryText,
                surfaceElevated = DarkElevatedSurface,
                outline = DarkDivider,
                outlineVariant = Color(0xFF22232B),
                error = Color(0xFFEF5350),
                onError = Color.White,
                textPrimary = DarkPrimaryText,
                textSecondary = DarkSecondaryText,
                textTertiary = DarkTertiaryText,
                divider = DarkDivider,
                disabled = DarkDisabled,
                accent = accentColor,
                accentSecondary = accentSecondary,
                accentGlow = glow,
                isDark = true
            )
        } else {
            OniColorTokens(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = DefaultSoftBlue,
                onPrimaryContainer = Color(0xFF184FA2),
                background = LightBackground,
                onBackground = LightPrimaryText,
                surface = LightSurface,
                onSurface = LightPrimaryText,
                surfaceVariant = DefaultBlueSurface,
                onSurfaceVariant = LightSecondaryText,
                surfaceElevated = LightElevatedSurface,
                outline = LightDivider,
                outlineVariant = Color(0xFFF0EEEA),
                error = Color(0xFFD32F2F),
                onError = Color.White,
                textPrimary = LightPrimaryText,
                textSecondary = LightSecondaryText,
                textTertiary = LightTertiaryText,
                divider = LightDivider,
                disabled = LightDisabled,
                accent = accentColor,
                accentSecondary = accentSecondary,
                accentGlow = glow,
                isDark = false
            )
        }

        val typography = OniTypographyTokens.defaultTypography()
        val shapes = OniShapeTokens.defaultShapes()
        val spacing = OniSpacingTokens.defaultSpacing()
        val elevation = OniElevationTokens.defaultElevation()
        val surfaces = OniSurfaceTokens.defaultSurfaces(colors, elevation)
        val motion = OniMotionTokens.defaultMotion()
        val artwork = OniArtworkTokens.defaultArtwork(shapes)
        val playbackControls = OniPlaybackControlTokens.defaultControls(colors)
        val navigation = OniNavigationTokens.defaultNavigation(colors, shapes)

        return OniSkinTokens(
            id = ID,
            name = NAME,
            colors = colors,
            typography = typography,
            shapes = shapes,
            spacing = spacing,
            elevation = elevation,
            surfaces = surfaces,
            motion = motion,
            artwork = artwork,
            playbackControls = playbackControls,
            navigation = navigation
        )
    }
}
