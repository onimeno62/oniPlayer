package com.example.ui.library.hero

import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeroProgressBar(
    position: Long,
    duration: Long,
    tintColor: Color,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetProgress = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Smoothly animate towards target progress to avoid stepped/jumping playhead movement.
    // Since OniAudioEngine updates position periodically (every ~250-500ms), a 350ms linear tween
    // guarantees absolute continuity without visual lag or overshooting.
    val smoothProgress = remember { Animatable(targetProgress) }

    LaunchedEffect(targetProgress) {
        smoothProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 350, easing = LinearEasing)
        )
    }

    // Detect if the playhead is currently in active motion
    val isMoving = smoothProgress.isRunning && targetProgress > 0f && targetProgress < 1f
    
    // Stretch playhead horizontally (1.20x) when moving, and spring-ease back to 1.0x when settled
    val stretchFactor by animateFloatAsState(
        targetValue = if (isMoving) 1.20f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PlayheadStretch"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(position),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        // Custom Canvas Progress Bar (Clicking anywhere on it opens Now Playing without seeking)
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // Suppress default click ripple
                    onClick = onOpenNowPlaying
                )
        ) {
            val width = size.width
            val height = size.height
            val trackHeight = 4.dp.toPx()
            val centerHeight = height / 2f

            // 1. Draw rounded background track (low alpha)
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, centerHeight),
                end = Offset(width, centerHeight),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            // 2. Draw rounded filled portion using extracted color
            val playheadX = smoothProgress.value * width
            drawLine(
                color = tintColor,
                start = Offset(0f, centerHeight),
                end = Offset(playheadX, centerHeight),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            // 3. Draw playhead particle with soft bloom (radial gradient glow)
            val coreRadius = 4.dp.toPx()
            val bloomRadius = 14.dp.toPx()

            withTransform({
                translate(left = playheadX, top = centerHeight)
                scale(scaleX = stretchFactor, scaleY = 1.0f, pivot = Offset.Zero)
            }) {
                // Glow bloom (uses radial gradient, compatible and performant across all SDK versions)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tintColor.copy(alpha = 0.50f), Color.Transparent),
                        center = Offset.Zero,
                        radius = bloomRadius
                    ),
                    center = Offset.Zero,
                    radius = bloomRadius
                )

                // Colored core
                drawCircle(
                    color = tintColor,
                    center = Offset.Zero,
                    radius = coreRadius
                )

                // High-fidelity tiny white core spark
                drawCircle(
                    color = Color.White,
                    center = Offset.Zero,
                    radius = coreRadius * 0.5f
                )
            }
        }

        Text(
            text = formatTime(duration),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
