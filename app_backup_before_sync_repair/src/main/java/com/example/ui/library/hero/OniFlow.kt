package com.example.ui.library.hero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun OniFlow(
    colors: HeroColors,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    // 3 separate soft blobs animating independently
    val blob1X = remember { Animatable(0.2f) }
    val blob1Y = remember { Animatable(0.4f) }
    val blob1Radius = remember { Animatable(80f) }

    val blob2X = remember { Animatable(0.35f) }
    val blob2Y = remember { Animatable(0.6f) }
    val blob2Radius = remember { Animatable(100f) }

    val blob3X = remember { Animatable(0.15f) }
    val blob3Y = remember { Animatable(0.5f) }
    val blob3Radius = remember { Animatable(90f) }

    if (!reduceMotion) {
        // Independent retargeting loops to create natural organic motion
        LaunchedEffect(Unit) {
            while (true) {
                blob1X.animateTo(
                    targetValue = Random.nextFloat() * 0.35f + 0.1f, // left-side biased
                    animationSpec = tween(durationMillis = Random.nextInt(4000, 6000))
                )
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                blob1Y.animateTo(
                    targetValue = Random.nextFloat() * 0.5f + 0.2f,
                    animationSpec = tween(durationMillis = Random.nextInt(4000, 6000))
                )
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                blob1Radius.animateTo(
                    targetValue = Random.nextFloat() * 40f + 60f,
                    animationSpec = tween(durationMillis = Random.nextInt(3000, 5000))
                )
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                blob2X.animateTo(
                    targetValue = Random.nextFloat() * 0.3f + 0.15f,
                    animationSpec = tween(durationMillis = Random.nextInt(5000, 7000))
                )
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                blob2Y.animateTo(
                    targetValue = Random.nextFloat() * 0.5f + 0.3f,
                    animationSpec = tween(durationMillis = Random.nextInt(5000, 7000))
                )
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                blob2Radius.animateTo(
                    targetValue = Random.nextFloat() * 50f + 80f,
                    animationSpec = tween(durationMillis = Random.nextInt(4000, 6000))
                )
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                blob3X.animateTo(
                    targetValue = Random.nextFloat() * 0.25f + 0.05f,
                    animationSpec = tween(durationMillis = Random.nextInt(4500, 6500))
                )
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                blob3Y.animateTo(
                    targetValue = Random.nextFloat() * 0.4f + 0.3f,
                    animationSpec = tween(durationMillis = Random.nextInt(4500, 6500))
                )
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                blob3Radius.animateTo(
                    targetValue = Random.nextFloat() * 30f + 70f,
                    animationSpec = tween(durationMillis = Random.nextInt(3500, 5500))
                )
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .blur(24.dp) // One single blur applied to the entire Canvas layer
    ) {
        val width = size.width
        val height = size.height

        // Radial gradient blobs utilizing dominant and secondary album colors with low alphas
        // Even on API < 31 where blur is a no-op, these radial gradients provide natural soft feathering.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.dominant.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(blob1X.value * width, blob1Y.value * height),
                radius = blob1Radius.value * density
            ),
            center = Offset(blob1X.value * width, blob1Y.value * height),
            radius = blob1Radius.value * density
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.secondary.copy(alpha = 0.36f), Color.Transparent),
                center = Offset(blob2X.value * width, blob2Y.value * height),
                radius = blob2Radius.value * density
            ),
            center = Offset(blob2X.value * width, blob2Y.value * height),
            radius = blob2Radius.value * density
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.dominant.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(blob3X.value * width, blob3Y.value * height),
                radius = blob3Radius.value * density
            ),
            center = Offset(blob3X.value * width, blob3Y.value * height),
            radius = blob3Radius.value * density
        )

        // Mask out the portion of the canvas nearest the play button (on the right)
        // using horizontal gradient with BlendMode.DstIn.
        // Left side stays fully visible, fading to 0% visibility at ~70% width.
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.White, Color.Transparent),
                startX = 0f,
                endX = width * 0.7f
            ),
            size = size,
            blendMode = BlendMode.DstIn
        )
    }
}
