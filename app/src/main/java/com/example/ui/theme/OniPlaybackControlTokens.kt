package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Playback control visual tokens for oniPlayer skins.
 * Defines sizes, shapes, elevations, and states for:
 * - Primary control (Play/Pause): circular, strong emphasis, soft elevation
 * - Secondary control (Previous/Next): medium size, balanced weight
 * - Tertiary control (Shuffle/Repeat): compact, subtle emphasis
 * Specified in Section 17.
 */
data class OniPlaybackControlTokens(
    val primaryControlSize: Dp = 64.dp,
    val secondaryControlSize: Dp = 48.dp,
    val tertiaryControlSize: Dp = 40.dp,
    val primaryShape: Shape = CircleShape,
    val secondaryShape: Shape = CircleShape,
    val tertiaryShape: Shape = CircleShape,
    val primaryElevation: Dp = 4.dp,
    val secondaryElevation: Dp = 0.dp,
    val primaryActiveTint: Color,
    val primaryInactiveTint: Color,
    val secondaryTint: Color,
    val tertiaryActiveTint: Color,
    val tertiaryInactiveTint: Color
) {
    companion object {
        fun defaultControls(colors: OniColorTokens): OniPlaybackControlTokens = OniPlaybackControlTokens(
            primaryControlSize = 64.dp,
            secondaryControlSize = 48.dp,
            tertiaryControlSize = 40.dp,
            primaryShape = CircleShape,
            secondaryShape = CircleShape,
            tertiaryShape = CircleShape,
            primaryElevation = 4.dp,
            secondaryElevation = 0.dp,
            primaryActiveTint = colors.primary,
            primaryInactiveTint = colors.surfaceElevated,
            secondaryTint = colors.onSurface,
            tertiaryActiveTint = colors.primary,
            tertiaryInactiveTint = colors.onSurfaceVariant
        )
    }
}
