package com.example.ui.library.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

@Composable
fun HeroBackground(
    artworkUri: String?,
    colors: HeroColors,
    reduceMotion: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dominantColor = colors.dominant

    // Almost imperceptible slow 24-second drift cycle for the ambient light wash
    val driftRatio = if (reduceMotion) {
        0.5f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "AmbientLightDrift")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DriftRatio"
        ).value
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Full-bleed blurred album art background
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .background(dominantColor.copy(alpha = 0.30f)),
            contentScale = ContentScale.Crop
        )

        // Subtle drifting diagonal wash using the extracted color (drawBehind optimizes drawing performance)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Subtle horizontal and vertical drift around the diagonal vector
                    val startX = -width * 0.15f + (width * 0.3f * driftRatio)
                    val startY = -height * 0.05f + (height * 0.1f * (1f - driftRatio))
                    val endX = width * 1.15f - (width * 0.3f * driftRatio)
                    val endY = height * 1.05f - (height * 0.1f * (1f - driftRatio))

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                dominantColor.copy(alpha = 0.55f),
                                dominantColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        )
                    )
                }
        )

        // Oni Flow organic animation layer (left-biased and masked before the play button)
        OniFlow(
            colors = colors,
            reduceMotion = reduceMotion || isLoading,
            modifier = Modifier.fillMaxSize()
        )

        // Glass refraction sweep layer (subtle, 0.05-0.10 peak alpha diagonal band)
        GlassRefractionSweep(
            reduceMotion = reduceMotion,
            isLoading = isLoading,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient scrim so text stays readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
    }
}
