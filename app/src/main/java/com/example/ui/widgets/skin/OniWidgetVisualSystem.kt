package com.example.ui.widgets.skin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkinTokens

/**
 * Glance-safe visual values shared by every oniPlayer launcher widget.
 *
 * Everything is derived from the active skin: colours from [OniSkinTokens.colors],
 * surface treatments from [OniSkinTokens.surfaces], artwork radius from
 * [OniSkinTokens.artwork], and widget-only values from [OniSkinTokens.widgets].
 * Alpha colours are used deliberately: RemoteViews renders ARGB backgrounds
 * natively, so translucent layers need no blur or custom drawing.
 */
object OniWidgetVisualSystem {

    // ---- Surfaces -------------------------------------------------------

    fun background(skin: OniSkinTokens): Color = skin.colors.background

    /** Outer widget canvas: calm and slightly separated from the app surface. */
    fun surface(skin: OniSkinTokens): Color = blend(
        skin.colors.surface,
        skin.colors.background,
        0.16f
    )

    /** Opaque version of [surface], used as the wash colour baked into artwork. */
    fun surfaceOpaque(skin: OniSkinTokens): Color = surface(skin).copy(alpha = 1f)

    /** Raised layer for grouped content. */
    fun elevated(skin: OniSkinTokens): Color = blend(
        skin.surfaces.elevated.containerColor,
        skin.colors.surface,
        0.24f
    )

    /** Subtle translucent lift, taken from the skin's frosted surface role. */
    fun frosted(skin: OniSkinTokens): Color {
        val base = skin.surfaces.frosted.containerColor
        return base.copy(alpha = (base.alpha * 1.25f).coerceIn(0.04f, 0.18f))
    }

    /** Control capsule: a touch stronger than [frosted] so transport reads as one unit. */
    fun control(skin: OniSkinTokens): Color =
        skin.colors.onSurface.copy(alpha = if (skin.colors.isDark) 0.11f else 0.07f)

    /** Translucent control layer used on top of full-bleed artwork. */
    fun glass(skin: OniSkinTokens): Color =
        skin.colors.onSurface.copy(alpha = if (skin.colors.isDark) 0.16f else 0.12f)

    /** Accent-tinted container used for quiet primary actions and artwork fallback. */
    fun tonal(skin: OniSkinTokens): Color =
        skin.colors.primary.copy(alpha = if (skin.colors.isDark) 0.22f else 0.14f)

    fun scrim(skin: OniSkinTokens): Color = blend(surface(skin), background(skin), 0.35f)

    fun scrim(skin: OniSkinTokens, alpha: Float): Color =
        background(skin).copy(alpha = alpha.coerceIn(0f, 1f))

    // ---- Content --------------------------------------------------------

    fun primary(skin: OniSkinTokens): Color = skin.colors.primary
    fun secondary(skin: OniSkinTokens): Color = skin.colors.accentSecondary
    fun text(skin: OniSkinTokens): Color = skin.colors.textPrimary
    fun muted(skin: OniSkinTokens): Color = skin.colors.textSecondary
    fun faint(skin: OniSkinTokens): Color = skin.colors.textTertiary
    fun controlOnPrimary(skin: OniSkinTokens): Color = skin.colors.onPrimary
    fun outline(skin: OniSkinTokens): Color = skin.colors.outlineVariant

    /** Inactive rail: derived from text colour so it stays visible on washes and artwork. */
    fun track(skin: OniSkinTokens): Color =
        skin.colors.textPrimary.copy(alpha = if (skin.colors.isDark) 0.16f else 0.12f)

    /** Inactive rail on top of hero artwork. */
    fun trackOnArtwork(skin: OniSkinTokens): Color =
        skin.colors.textPrimary.copy(alpha = if (skin.colors.isDark) 0.24f else 0.20f)

    // ---- Controls -------------------------------------------------------

    fun secondaryControlTint(skin: OniSkinTokens): Color = skin.playbackControls.secondaryTint
    fun tertiaryActiveTint(skin: OniSkinTokens): Color = skin.playbackControls.tertiaryActiveTint
    fun tertiaryInactiveTint(skin: OniSkinTokens): Color = skin.playbackControls.tertiaryInactiveTint

    // ---- Shape ----------------------------------------------------------

    fun cornerRadius(skin: OniSkinTokens): Dp = skin.widgets.cornerRadius
    fun innerRadius(skin: OniSkinTokens): Dp = skin.widgets.innerCornerRadius

    /** Artwork radius from the skin, reduced proportionally for small thumbnails. */
    fun artworkRadius(skin: OniSkinTokens, sizeDp: Float): Dp {
        val token = skin.artwork.cornerRadius.value
        return minOf(token, sizeDp * 0.22f).coerceAtLeast(6f).dp
    }

    fun minTouchTarget(skin: OniSkinTokens): Dp = skin.widgets.minTouchTarget

    /**
     * Deterministic RGB blending for Glance ColorProvider values.
     * The amount represents the contribution of b, from 0f to 1f.
     */
    fun blend(a: Color, b: Color, amount: Float): Color {
        val t = amount.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = a.alpha + (b.alpha - a.alpha) * t
        )
    }
}
