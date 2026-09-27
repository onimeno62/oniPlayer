package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Launcher-widget presentation tokens for oniPlayer skins.
 *
 * Widgets resolve colour, typography, artwork, and control values from the
 * regular skin tokens. These tokens only cover concerns that are unique to
 * home-screen widgets, so installed skins can restyle the widget system
 * without a parallel theme.
 */
data class OniWidgetTokens(
    /** Outer launcher-widget corner radius. */
    val cornerRadius: Dp = 24.dp,
    /** Radius for nested panels and control capsules. */
    val innerCornerRadius: Dp = 16.dp,
    /** Whether artwork-derived atmosphere may be rendered behind widget content. */
    val artworkAtmosphere: Boolean = true,
    /** 0..1: how much softened artwork shows through the skin surface wash. */
    val atmosphereStrength: Float = 0.42f,
    /** 0..1: scrim opacity at the text edge of full-bleed hero artwork. */
    val heroScrimStrength: Float = 0.9f,
    /** Progress rail thickness. */
    val progressThickness: Dp = 4.dp,
    /** Whether flagship progress rails draw a position knob. */
    val progressKnob: Boolean = true,
    /** Minimum interactive target for widget controls. */
    val minTouchTarget: Dp = 40.dp
) {
    companion object {
        fun defaultWidgets(isDark: Boolean): OniWidgetTokens = OniWidgetTokens(
            atmosphereStrength = if (isDark) 0.46f else 0.30f,
            heroScrimStrength = if (isDark) 0.92f else 0.88f
        )
    }
}
