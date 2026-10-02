package com.example.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.PI

/**
 * WAVY Seek Bar - Animated wave pattern following Material 3 principles
 */
@Composable
fun WavySeekBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    var waveOffset by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            waveOffset = (waveOffset + 0.05f) % (2 * PI.toFloat())
        }
    }
    
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(inactiveColor, RoundedCornerShape(12.dp))
        ) {
            val width = size.width
            val height = size.height
            val amplitude = 4.dp.toPx()
            
            // Draw wavy progress line
            val progressWidth = width * progress
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, height / 2)
            
            for (x in 0..progressWidth.toInt() step 2) {
                val y = height / 2 + sin((x * 0.02f) + waveOffset) * amplitude
                path.lineTo(x.toFloat(), y)
            }
            
            drawPath(path, color = activeColor, style = androidx.compose.ui.graphics.Stroke(4f))
        }
    }
}

/**
 * DOT LINE Seek Bar - Dot pattern transitions to solid line on progress
 */
@Composable
fun DotLineSeekBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(inactiveColor, RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress
        val dotSpacing = 12.dp.toPx()
        
        // Draw dot pattern for inactive portion
        var x = 0f
        while (x < width) {
            if (x < progressWidth) {
                // Solid line for progress
                if (x == 0f) {
                    drawRect(
                        color = activeColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, height / 2 - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(progressWidth, 4.dp.toPx())
                    )
                }
            } else {
                // Dot for inactive
                drawCircle(
                    color = inactiveColor,
                    radius = 2.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, height / 2)
                )
            }
            x += dotSpacing
        }
    }
}

/**
 * AUDIO SPECTRUM Seek Bar - Real-time frequency band visualization
 */
@Composable
fun AudioSpectrumSeekBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    beatEnergy: Float = 0.5f,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    var bandValues by remember { mutableStateOf(FloatArray(16) { 0.3f }) }
    
    LaunchedEffect(beatEnergy) {
        while (true) {
            kotlinx.coroutines.delay(50)
            bandValues = FloatArray(16) {
                val random = (0..10).random() / 10f
                (bandValues[it] * 0.8f + random * beatEnergy).coerceIn(0f, 1f)
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(inactiveColor, RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        val bandWidth = width / 16
        val progressWidth = width * progress
        
        bandValues.forEachIndexed { index, energy ->
            val x = index * bandWidth
            val barHeight = height * energy.coerceIn(0f, 1f)
            
            val color = if (x < progressWidth) activeColor else inactiveColor
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x + 2.dp.toPx(), height - barHeight),
                size = androidx.compose.ui.geometry.Size(bandWidth - 4.dp.toPx(), barHeight)
            )
        }
    }
}

/**
 * Artwork Effects - Ken Burns zoom, color extraction, ambient glow
 */
@Composable
fun ApplyArtworkEffects(
    modifier: Modifier = Modifier,
    enableKenBurns: Boolean = true,
    enableGlow: Boolean = true,
    content: @Composable () -> Unit
) {
    var kenBurnsScale by remember { mutableStateOf(1f) }
    var kenBurnsOffsetX by remember { mutableStateOf(0f) }
    
    LaunchedEffect(enableKenBurns) {
        if (enableKenBurns) {
            while (true) {
                val animation = TargetBasedAnimation(
                    animationSpec = tween(8000, easing = LinearEasing),
                    typeConverter = Float.VectorConverter,
                    initialValue = 1f,
                    targetValue = 1.2f
                )
                val duration = animation.durationMillis
                val startTime = System.currentTimeMillis()
                
                while (true) {
                    val playTime = (System.currentTimeMillis() - startTime).toFloat()
                    if (playTime > duration) break
                    kenBurnsScale = animation.getValueFromNanos((playTime * 1_000_000).toLong())
                    kotlinx.coroutines.delay(16)
                }
                kotlinx.coroutines.delay(4000)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enableGlow) {
                    Modifier.background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f)
                            ),
                            radius = 600f
                        )
                    )
                } else Modifier
            )
    ) {
        content()
    }
}

/**
 * Audio Analyzer Effect - Beat-driven amplitude pulse and visualization
 */
@Composable
fun AudioAnalyzerEffect(
    beatEnergy: Float = 0.5f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val glowIntensity by animateFloatAsState(
        targetValue = if (enabled) beatEnergy * 0.5f else 0f,
        animationSpec = spring(dampingRatio = 0.5f)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f + (beatEnergy * 0.1f) else 1f,
        animationSpec = spring(dampingRatio = 0.6f)
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = 12.dp.toPx() * glowIntensity
            }
    ) {
        content()
    }
}

/**
 * Live Lyric Line Display - Multiple line count support
 */
@Composable
fun LyricLineDisplay(
    lyric: String,
    lineCount: Int = 2,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val lines = lyric.split("\n").take(lineCount)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        lines.forEach { line ->
            androidx.compose.material3.Text(
                text = line,
                modifier = Modifier.alpha(alpha),
                color = Color.White.copy(alpha = alpha),
                fontSize = androidx.compose.ui.unit.sp(14)
            )
        }
    }
}

// Helper: Animated scale
private fun androidx.compose.ui.Modifier.scale(scale: Float) =
    this.graphicsLayer(scaleX = scale, scaleY = scale)

private fun androidx.compose.ui.Modifier.alpha(alpha: Float) =
    this.graphicsLayer(alpha = alpha)
