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
import com.example.ui.theme.OniSkin

@Composable
fun HeroProgressBar(
    position: Long,
    duration: Long,
    tintColor: Color = OniSkin.colors.primary,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetProgress = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val smoothProgress = remember { Animatable(targetProgress) }

    LaunchedEffect(targetProgress) {
        smoothProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 350, easing = LinearEasing)
        )
    }

    val isMoving = smoothProgress.isRunning && targetProgress > 0f && targetProgress < 1f
    
    val stretchFactor by animateFloatAsState(
        targetValue = if (isMoving) 1.20f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PlayheadStretch"
    )

    val labelColor = OniSkin.colors.textSecondary
    val trackBgColor = OniSkin.colors.outline.copy(alpha = 0.25f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(position),
            style = OniSkin.typography.caption,
            color = labelColor
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenNowPlaying
                )
        ) {
            val width = size.width
            val height = size.height
            val trackHeight = 4.dp.toPx()
            val centerHeight = height / 2f

            drawLine(
                color = trackBgColor,
                start = Offset(0f, centerHeight),
                end = Offset(width, centerHeight),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            val playheadX = smoothProgress.value * width
            drawLine(
                color = tintColor,
                start = Offset(0f, centerHeight),
                end = Offset(playheadX, centerHeight),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            val coreRadius = 4.dp.toPx()
            val bloomRadius = 12.dp.toPx()

            withTransform({
                translate(left = playheadX, top = centerHeight)
                scale(scaleX = stretchFactor, scaleY = 1.0f, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tintColor.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset.Zero,
                        radius = bloomRadius
                    ),
                    center = Offset.Zero,
                    radius = bloomRadius
                )

                drawCircle(
                    color = tintColor,
                    center = Offset.Zero,
                    radius = coreRadius
                )

                drawCircle(
                    color = Color.White,
                    center = Offset.Zero,
                    radius = coreRadius * 0.5f
                )
            }
        }

        Text(
            text = formatTime(duration),
            style = OniSkin.typography.caption,
            color = labelColor
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
