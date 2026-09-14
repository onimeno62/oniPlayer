package com.example.ui.widgets.skin

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.OniSkinTokens

/**
 * Glance-safe visual primitives shared by the rebuilt Default Skin widgets.
 * The active skin remains the source of truth; these helpers only establish
 * hierarchy and contrast, not a second palette.
 */
object OniWidgetVisualSystem {
    fun background(skin: OniSkinTokens): Color = skin.colors.background
    fun surface(skin: OniSkinTokens): Color = skin.colors.surface
    fun elevated(skin: OniSkinTokens): Color = skin.colors.surfaceElevated
    fun primary(skin: OniSkinTokens): Color = skin.colors.primary
    fun secondary(skin: OniSkinTokens): Color = skin.colors.accentSecondary
    fun text(skin: OniSkinTokens): Color = skin.colors.textPrimary
    fun muted(skin: OniSkinTokens): Color = skin.colors.textSecondary
    fun track(skin: OniSkinTokens): Color = skin.colors.surfaceVariant
    fun outline(skin: OniSkinTokens): Color = skin.colors.outlineVariant

    fun scrim(skin: OniSkinTokens): Color = blend(surface(skin), background(skin), 0.35f)
    fun control(skin: OniSkinTokens): Color = blend(elevated(skin), surface(skin), 0.35f)
    fun controlOnPrimary(skin: OniSkinTokens): Color = skin.colors.onPrimary

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
