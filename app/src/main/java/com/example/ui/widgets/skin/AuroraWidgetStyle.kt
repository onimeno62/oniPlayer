package com.example.ui.widgets.skin

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.OniSkinTokens

/** Shared visual language for the Default Skin's launcher widgets.
 *
 * Glance cannot reproduce a full Compose blur/shader pipeline, so the reference
 * Aurora Glass treatment is approximated with opaque layered surfaces, soft
 * accent blooms, luminous artwork frames, and restrained translucent controls.
 */
object AuroraWidgetStyle {
    val Navy = Color(0xFF101126)
    val NavyElevated = Color(0xFF1B1A36)
    val Violet = Color(0xFFB28CFF)
    val Pink = Color(0xFFFF72B0)
    val White = Color(0xFFF7F4FF)
    val Muted = Color(0xFFB8B4C8)

    fun surface(skin: OniSkinTokens): Color = blend(Navy, skin.colors.surface, 0.12f).copy(alpha = 0.98f)
    fun elevated(skin: OniSkinTokens): Color = blend(NavyElevated, skin.colors.surfaceElevated, 0.16f).copy(alpha = 0.96f)
    fun primary(skin: OniSkinTokens): Color = blend(Pink, skin.colors.primary, 0.22f)
    fun secondary(skin: OniSkinTokens): Color = blend(Violet, skin.colors.accentSecondary, 0.18f)
    fun glow(skin: OniSkinTokens): Color = blend(Pink, skin.colors.accentGlow, 0.25f)
    fun textPrimary(skin: OniSkinTokens): Color = blend(White, skin.colors.textPrimary, 0.18f)
    fun textSecondary(skin: OniSkinTokens): Color = blend(Muted, skin.colors.textSecondary, 0.20f)
    fun track(skin: OniSkinTokens): Color = blend(Color(0xFF35334E), skin.colors.surfaceVariant, 0.22f)

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
