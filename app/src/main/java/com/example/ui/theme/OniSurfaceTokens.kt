package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Surface styling definition for a semantic surface role.
 */
data class OniSurfaceStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderStroke: BorderStroke? = null,
    val elevation: Dp = 0.dp,
    val alpha: Float = 1f
)

/**
 * Surface tokens for oniPlayer skins.
 * Defines four semantic surface treatments specified in Section 14:
 * - Flat: Clean zero-elevation surface
 * - Soft: Subtle contrast with delicate border
 * - Elevated: Distinct surface with gentle elevation
 * - Frosted: Translucent container with soft border and blur
 */
data class OniSurfaceTokens(
    val flat: OniSurfaceStyle,
    val soft: OniSurfaceStyle,
    val elevated: OniSurfaceStyle,
    val frosted: OniSurfaceStyle
) {
    companion object {
        fun defaultSurfaces(
            colors: OniColorTokens,
            elevation: OniElevationTokens
        ): OniSurfaceTokens {
            return OniSurfaceTokens(
                flat = OniSurfaceStyle(
                    containerColor = colors.background,
                    contentColor = colors.textPrimary,
                    elevation = elevation.flat
                ),
                soft = OniSurfaceStyle(
                    containerColor = colors.surface,
                    contentColor = colors.textPrimary,
                    borderStroke = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                    elevation = elevation.flat
                ),
                elevated = OniSurfaceStyle(
                    containerColor = colors.surfaceElevated,
                    contentColor = colors.textPrimary,
                    elevation = elevation.raised
                ),
                frosted = OniSurfaceStyle(
                    containerColor = colors.onSurface.copy(alpha = if (colors.isDark) 0.08f else 0.05f),
                    contentColor = colors.textPrimary,
                    borderStroke = BorderStroke(1.dp, colors.onSurface.copy(alpha = if (colors.isDark) 0.12f else 0.08f)),
                    elevation = elevation.flat,
                    alpha = if (colors.isDark) 0.08f else 0.05f
                )
            )
        }
    }
}
