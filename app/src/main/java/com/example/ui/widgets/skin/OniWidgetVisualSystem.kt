package com.example.ui.widgets.skin

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.OniSkinTokens

/**
 * Glance-safe visual primitives shared by the Default Skin widgets.
 *
 * The visual language intentionally follows the PixelPlayer-inspired direction:
 * quiet outer surfaces, a slightly lifted control layer, restrained contrast,
 * and artwork-first hierarchy. The active oniPlayer skin remains the only
 * palette source.
 */
object OniWidgetVisualSystem {
    fun background(skin: OniSkinTokens): Color = skin.colors.background

    /** Outer widget canvas: calm and slightly separated from the app surface. */
    fun surface(skin: OniSkinTokens): Color = blend(
        skin.colors.surface,
        skin.colors.background,
        0.16f
    )

    /** Raised layer for prominent controls and secondary grouping. */
    fun elevated(skin: OniSkinTokens): Color = blend(
        skin.colors.surfaceElevated,
        skin.colors.surface,
        0.24f
    )

    fun primary(skin: OniSkinTokens): Color = skin.colors.primary
    fun secondary(skin: OniSkinTokens): Color = skin.colors.accentSecondary
    fun text(skin: OniSkinTokens): Color = skin.colors.textPrimary
    fun muted(skin: OniSkinTokens): Color = skin.colors.textSecondary

    /** Low-emphasis progress rail that remains visible on light and dark skins. */
    fun track(skin: OniSkinTokens): Color = blend(
        skin.colors.surfaceVariant,
        skin.colors.outlineVariant,
        0.22f
    )

    fun outline(skin: OniSkinTokens): Color = skin.colors.outlineVariant

    fun scrim(skin: OniSkinTokens): Color = blend(surface(skin), background(skin), 0.35f)

    /** Glance-safe control background; no alpha-dependent rendering required. */
    fun control(skin: OniSkinTokens): Color = blend(
        elevated(skin),
        surface(skin),
        0.30f
    )

    fun controlOnPrimary(skin: OniSkinTokens): Color = skin.colors.onPrimary

    /**
     * Deterministic RGB blending for Glance ColorProvider values.
     * The amount represents the contribution of b, from 0f to 1f.
     */
    private fun blend(a: Color, b: Color, amount: Float): Color {
        val t = amount.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = a.alpha + (b.alpha - a.alpha) * t
        )
    }
}
