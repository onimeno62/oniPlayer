package com.example.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

private fun Modifier.seekGesture(progress: Float, onProgressChange: (Float) -> Unit): Modifier =
    pointerInput(progress) {
        detectTapGestures { offset ->
            val width = size.width.toFloat().coerceAtLeast(1f)
            onProgressChange((offset.x / width).coerceIn(0f, 1f))
        }
    }

@Composable
fun WavySeekBar(progress: Float, onProgressChange: (Float) -> Unit, modifier: Modifier = Modifier,
    activeColor: Color = Color.White, inactiveColor: Color = Color.White.copy(alpha = 0.3f)) {
    val transition = rememberInfiniteTransition(label = "wavy_seek")
    val waveOffset by transition.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_offset"
    )
    Canvas(modifier.fillMaxWidth().height(48.dp).background(inactiveColor, RoundedCornerShape(12.dp))
        .seekGesture(progress, onProgressChange)) {
        val progressWidth = size.width * progress.coerceIn(0f, 1f)
        val centerY = size.height / 2f
        val amplitude = 4.dp.toPx()
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(0f, centerY)
        for (x in 0..progressWidth.toInt() step 2) {
            path.lineTo(x.toFloat(), centerY + sin(x * 0.02f + waveOffset) * amplitude)
        }
        drawPath(path, color = activeColor, style = Stroke(width = 4.dp.toPx()))
    }
}

@Composable
fun DotLineSeekBar(progress: Float, onProgressChange: (Float) -> Unit, modifier: Modifier = Modifier,
    activeColor: Color = Color.White, inactiveColor: Color = Color.White.copy(alpha = 0.3f)) {
    Canvas(modifier.fillMaxWidth().height(48.dp).background(inactiveColor, RoundedCornerShape(12.dp))
        .seekGesture(progress, onProgressChange)) {
        val width = size.width
        val centerY = size.height / 2f
        val progressWidth = width * progress.coerceIn(0f, 1f)
        drawLine(activeColor, androidx.compose.ui.geometry.Offset(0f, centerY),
            androidx.compose.ui.geometry.Offset(progressWidth, centerY), strokeWidth = 4.dp.toPx())
        var x = 0f
        while (x <= width) {
            if (x > progressWidth) drawCircle(inactiveColor.copy(alpha = 0.7f), 2.dp.toPx(),
                androidx.compose.ui.geometry.Offset(x, centerY))
            x += 12.dp.toPx()
        }
    }
}

@Composable
fun AudioSpectrumSeekBar(progress: Float, onProgressChange: (Float) -> Unit, beatEnergy: Float = 0.5f,
    modifier: Modifier = Modifier, activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)) {
    val transition = rememberInfiniteTransition(label = "spectrum_seek")
    val phase by transition.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "spectrum_phase"
    )
    Canvas(modifier.fillMaxWidth().height(48.dp)
        .background(inactiveColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
        .seekGesture(progress, onProgressChange)) {
        val width = size.width
        val height = size.height
        val bandWidth = width / 16f
        val energy = beatEnergy.coerceIn(0f, 1f)
        repeat(16) { index ->
            val normalized = (sin(phase * 1.3f + index * 0.72f) + 1f) / 2f
            val barHeight = (height * (0.18f + normalized * 0.52f * energy)).coerceAtLeast(3.dp.toPx())
            val x = index * bandWidth
            drawRect(
                color = if (x <= width * progress.coerceIn(0f, 1f)) activeColor else inactiveColor,
                topLeft = androidx.compose.ui.geometry.Offset(x + 2.dp.toPx(), height - barHeight),
                size = androidx.compose.ui.geometry.Size((bandWidth - 4.dp.toPx()).coerceAtLeast(1f), barHeight)
            )
        }
    }
}

@Composable
fun ApplyArtworkEffects(modifier: Modifier = Modifier, enableKenBurns: Boolean = true,
    enableGlow: Boolean = true, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "artwork_effects")
    val scale by transition.animateFloat(
        1f, if (enableKenBurns) 1.035f else 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "artwork_scale"
    )
    Box(modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale).then(
        if (enableGlow) Modifier.background(Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f))
        )) else Modifier
    )) { content() }
}

@Composable
fun AudioAnalyzerEffect(beatEnergy: Float = 0.5f, modifier: Modifier = Modifier,
    enabled: Boolean = true, content: @Composable () -> Unit) {
    val scaleTarget = if (enabled) 1f + beatEnergy.coerceIn(0f, 1f) * 0.035f else 1f
    val scale by animateFloatAsState(scaleTarget, label = "audio_reactive_scale")
    Box(modifier.graphicsLayer(scaleX = scale, scaleY = scale)) { content() }
}

@Composable
fun LyricLineDisplay(lyric: String, lineCount: Int = 2, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Column(modifier = modifier) {
        lyric.lines().take(lineCount.coerceAtLeast(1)).forEach { line ->
            androidx.compose.material3.Text(text = line, color = Color.White.copy(alpha = alpha),
                fontSize = 14.sp)
        }
    }
}
