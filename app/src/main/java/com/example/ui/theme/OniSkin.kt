package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Interface defining the contract for any skin in oniPlayer.
 * The Default Skin implements this interface, as will any installed skins in future phases.
 */
interface OniSkinDefinition {
    val id: String
    val name: String
    val colors: OniColorTokens
    val typography: OniTypographyTokens
    val shapes: OniShapeTokens
    val spacing: OniSpacingTokens
    val elevation: OniElevationTokens
    val surfaces: OniSurfaceTokens
    val motion: OniMotionTokens
    val artwork: OniArtworkTokens
    val playbackControls: OniPlaybackControlTokens
    val navigation: OniNavigationTokens
}

/**
 * Concrete immutable representation of an active skin's token set.
 */
data class OniSkinTokens(
    override val id: String,
    override val name: String,
    override val colors: OniColorTokens,
    override val typography: OniTypographyTokens,
    override val shapes: OniShapeTokens,
    override val spacing: OniSpacingTokens,
    override val elevation: OniElevationTokens,
    override val surfaces: OniSurfaceTokens,
    override val motion: OniMotionTokens,
    override val artwork: OniArtworkTokens,
    override val playbackControls: OniPlaybackControlTokens,
    override val navigation: OniNavigationTokens
) : OniSkinDefinition

/**
 * CompositionLocal providing the current active [OniSkinTokens].
 */
val LocalOniSkin = staticCompositionLocalOf<OniSkinTokens> {
    DefaultSkin.createSkin(isDark = false)
}

/**
 * Centralized accessor for the active oniPlayer skin tokens.
 *
 * Usage in composables:
 * ```kotlin
 * val bg = OniSkin.colors.background
 * val cardShape = OniSkin.shapes.card
 * val screenPad = OniSkin.spacing.screenHorizontal
 * val bodyStyle = OniSkin.typography.bodyMedium
 * val elevation = OniSkin.elevation.raised
 * val motionDuration = OniSkin.motion.componentStateDurationMs
 * ```
 */
object OniSkin {
    val current: OniSkinTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current

    val colors: OniColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.colors

    val typography: OniTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.typography

    val shapes: OniShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.shapes

    val spacing: OniSpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.spacing

    val elevation: OniElevationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.elevation

    val surfaces: OniSurfaceTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.surfaces

    val motion: OniMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.motion

    val artwork: OniArtworkTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.artwork

    val playbackControls: OniPlaybackControlTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.playbackControls

    val navigation: OniNavigationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalOniSkin.current.navigation
}
