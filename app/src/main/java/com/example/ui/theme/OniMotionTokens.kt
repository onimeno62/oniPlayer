package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Motion and animation tokens for oniPlayer skins.
 * Defines standard durations and easings specified in Section 15:
 * - Button press / feedback: 100-150ms (default 120ms)
 * - Component state changes: 150-250ms (default 200ms)
 * - Screen / sheet transitions: 250-350ms (default 300ms)
 * - Artwork transitions: 400-700ms (default 500ms)
 * - Ambient loops: multi-second (default 12000ms)
 */
data class OniMotionTokens(
    val quickDurationMs: Int = 150,
    val buttonPressDurationMs: Int = 120,
    val componentStateDurationMs: Int = 200,
    val screenTransitionDurationMs: Int = 300,
    val artworkTransitionDurationMs: Int = 500,
    val ambientLoopDurationMs: Int = 12000,
    val standardEasing: Easing = FastOutSlowInEasing,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val decelerateEasing: Easing = LinearOutSlowInEasing
) {
    companion object {
        fun defaultMotion(): OniMotionTokens = OniMotionTokens()
    }
}
