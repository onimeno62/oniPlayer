package com.example.ui.library.hero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OniAura(
    color: Color,
    isPlaying: Boolean,
    reduceMotion: Boolean,
    cornerRadius: Dp,
    bleed: Dp,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Optimization: Build path, pathMeasure and segmentPath once, avoid per-frame allocations
    val baseRoundRectPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    val segmentPath = remember { Path() }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val bleedPx = remember(bleed, density) { with(density) { bleed.toPx() } }
    val cornerRadiusPx = remember(cornerRadius, density) { with(density) { cornerRadius.toPx() } }

    // Rebuild the rounded rect path and measure it only when dimensions, corners or bleed changes
    LaunchedEffect(canvasSize, cornerRadiusPx, bleedPx) {
        val width = canvasSize.width
        val height = canvasSize.height
        val cardWidth = width - bleedPx * 2
        val cardHeight = height - bleedPx * 2

        if (cardWidth > 0 && cardHeight > 0) {
            baseRoundRectPath.reset()
            baseRoundRectPath.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(bleedPx, bleedPx),
                        size = Size(cardWidth, cardHeight)
                    ),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
            pathMeasure.setPath(baseRoundRectPath, forceClosed = true)
        }
    }

    // Loop progress continuously. Speed shifts smoothly on playback state changes.
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPlaying, reduceMotion) {
        if (reduceMotion) {
            return@LaunchedEffect
        }

        val duration = if (isPlaying) 8000 else 50000 // 8s when playing, 50s when paused
        var currentProgress = progress.value

        while (true) {
            val remaining = 1f - currentProgress
            val remainingDuration = (remaining * duration).toInt()
            if (remainingDuration > 0) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingDuration,
                        easing = LinearEasing
                    )
                )
            }
            progress.snapTo(0f)
            currentProgress = 0f
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { intSize ->
                canvasSize = Size(intSize.width.toFloat(), intSize.height.toFloat())
            }
            .blur(8.dp) // Soft blur applied once to the entire Canvas layer
    ) {
        val pathLength = pathMeasure.length
        if (pathLength > 0f) {
            segmentPath.reset()

            val segmentPercent = 0.18f // 18% of perimeter
            val segmentLength = pathLength * segmentPercent
            val startDistance = progress.value * pathLength
            val stopDistance = startDistance + segmentLength

            if (stopDistance <= pathLength) {
                pathMeasure.getSegment(startDistance, stopDistance, segmentPath, startWithMoveTo = true)
            } else {
                // Wrap around at path start/end boundaries cleanly
                pathMeasure.getSegment(startDistance, pathLength, segmentPath, startWithMoveTo = true)
                pathMeasure.getSegment(0f, stopDistance - pathLength, segmentPath, startWithMoveTo = true)
            }

            // Dual-pass rendering for a gorgeous, high-fidelity premium glow effect
            // Thicker background glow (softer, lower alpha)
            drawPath(
                path = segmentPath,
                color = color.copy(alpha = 0.40f),
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Thinner foreground core (sharper, higher alpha)
            drawPath(
                path = segmentPath,
                color = color.copy(alpha = 0.85f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
