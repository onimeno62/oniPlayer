package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation and depth tokens for oniPlayer skins.
 * Specifies soft visual depth rather than harsh Material shadows,
 * as required by Section 13:
 * - Flat: 0dp
 * - Raised: 2dp (soft shadow / subtle border)
 * - Floating: 6dp (soft ambient elevation)
 */
data class OniElevationTokens(
    val flat: Dp = 0.dp,
    val raised: Dp = 2.dp,
    val floating: Dp = 6.dp,
    val shadowAlpha: Float = 0.08f,
    val ambientAlpha: Float = 0.04f
) {
    companion object {
        fun defaultElevation(): OniElevationTokens = OniElevationTokens()
    }
}
