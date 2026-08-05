package com.example.ui.library.hero

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun GlassRefractionSweep(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (reduceMotion) return

    val infiniteTransition = rememberInfiniteTransition(label = "GlassSweep")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val sweepDurationPercent = 0.15f // first 1.5s of a 10s cycle is 15%
        if (progress < sweepDurationPercent) {
            val subProgress = progress / sweepDurationPercent // map perfectly to 0f..1f

            val width = size.width
            val height = size.height

            // Diagonal highlight band of low opacity (0.05-0.10 peak) sweeping horizontally
            val sweepWidth = 120f
            val startX = -sweepWidth + (width + sweepWidth * 2) * subProgress

            val gradient = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0f),
                    Color.White.copy(alpha = 0.08f), // Subtle realism peak opacity
                    Color.White.copy(alpha = 0f)
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + sweepWidth, height)
            )

            drawRect(brush = gradient)
        }
    }
}
