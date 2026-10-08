package com.example.ui.player

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

enum class AudioVisualizerMode(val label: String) {
    OFF("Artwork"),
    BARS("Spectrum Bars"),
    MIRRORED("Mirrored Spectrum"),
    SPECTROGRAM("Spectrogram"),
    RADIAL("Radial Spectrum")
}

private const val BAND_COUNT = 48
private const val HISTORY_ROWS = 90

@Composable
fun AudioSpectrumVisualizer(
    mode: AudioVisualizerMode,
    audioSessionId: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val levels = remember { mutableStateOf(FloatArray(BAND_COUNT)) }
    val history = remember { mutableStateListOf<FloatArray>() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(mode, audioSessionId, isPlaying) {
        if (mode == AudioVisualizerMode.OFF ||
            audioSessionId <= 0 ||
            !isPlaying
        ) {
            levels.value = FloatArray(BAND_COUNT)
            history.clear()
            onDispose { }
        } else {
            var visualizer: Visualizer? = null
            val fftListener = object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) = Unit

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray,
                    samplingRate: Int
                ) {
                    val bands = extractBands(fft, samplingRate)
                    mainHandler.post {
                        levels.value = bands
                        if (mode == AudioVisualizerMode.SPECTROGRAM) {
                            history.add(bands)
                            while (history.size > HISTORY_ROWS) history.removeAt(0)
                        }
                    }
                }
            }

            try {
                val range = runCatching { Visualizer.getCaptureSizeRange() }.getOrNull()
                if (range != null && range.size >= 2) {
                    visualizer = Visualizer(audioSessionId).apply {
                        val captureSize = minOf(1024, range[1]).coerceAtLeast(range[0])
                        setCaptureSize(captureSize)
                        setScalingMode(Visualizer.SCALING_MODE_NORMALIZED)
                        setDataCaptureListener(
                            fftListener,
                            Visualizer.getMaxCaptureRate(),
                            false,
                            true
                        )
                        enabled = true
                    }
                }
            } catch (_: Throwable) {
                runCatching { visualizer?.release() }
                visualizer = null
            }

            onDispose {
                runCatching {
                    visualizer?.setDataCaptureListener(null, 0, false, false)
                    visualizer?.enabled = false
                    visualizer?.release()
                }
                mainHandler.removeCallbacksAndMessages(null)
            }
        }
    }

    val primary = OniSkin.colors.primary
    val secondary = OniSkin.colors.accentSecondary

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val values = levels.value

        when (mode) {
            AudioVisualizerMode.OFF -> Unit
            AudioVisualizerMode.BARS -> drawBars(values, primary, secondary)
            AudioVisualizerMode.MIRRORED -> drawMirrored(values, primary, secondary)
            AudioVisualizerMode.SPECTROGRAM -> drawSpectrogram(history, primary, secondary)
            AudioVisualizerMode.RADIAL -> drawRadial(values, primary, secondary)
        }
    }
}

private fun extractBands(fft: ByteArray, sampleRateMilliHz: Int): FloatArray {
    val result = FloatArray(BAND_COUNT)
    val sampleRateHz = (sampleRateMilliHz / 1000f).coerceAtLeast(1f)
    val binCount = fft.size / 2
    val nyquist = sampleRateHz / 2f
    val minFrequency = 40f
    val maxFrequency = minOf(16_000f, nyquist)

    for (band in 0 until BAND_COUNT) {
        val t0 = band.toFloat() / BAND_COUNT
        val t1 = (band + 1).toFloat() / BAND_COUNT
        val low = minFrequency * Math.pow((maxFrequency / minFrequency).toDouble(), t0.toDouble()).toFloat()
        val high = minFrequency * Math.pow((maxFrequency / minFrequency).toDouble(), t1.toDouble()).toFloat()
        val firstBin = ((low / nyquist) * (binCount - 1)).toInt().coerceIn(1, binCount - 1)
        val lastBin = ((high / nyquist) * (binCount - 1)).toInt().coerceIn(firstBin, binCount - 1)

        var sum = 0f
        var count = 0
        for (bin in firstBin..lastBin) {
            val index = bin * 2
            if (index + 1 >= fft.size) break
            val real = fft[index].toInt()
            val imaginary = fft[index + 1].toInt()
            sum += hypot(real.toDouble(), imaginary.toDouble()).toFloat() / 128f
            count++
        }
        val magnitude = if (count > 0) sum / count else 0f
        result[band] = ((ln(1f + magnitude * 7f) / ln(8f)) * 1.35f).coerceIn(0f, 1f)
    }
    return result
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(
    values: FloatArray,
    primary: Color,
    secondary: Color
) {
    val gap = size.width / (BAND_COUNT * 1.45f)
    val barWidth = (size.width - gap * (BAND_COUNT + 1)) / BAND_COUNT
    values.forEachIndexed { index, value ->
        val height = size.height * (0.035f + value * 0.86f)
        val x = gap + index * (barWidth + gap)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.98f), secondary.copy(alpha = 0.38f))),
            topLeft = androidx.compose.ui.geometry.Offset(x, size.height - height),
            size = androidx.compose.ui.geometry.Size(barWidth, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMirrored(
    values: FloatArray,
    primary: Color,
    secondary: Color
) {
    val centerY = size.height / 2f
    val gap = size.width / (BAND_COUNT * 1.45f)
    val barWidth = (size.width - gap * (BAND_COUNT + 1)) / BAND_COUNT
    drawLine(
        color = primary.copy(alpha = 0.16f),
        start = androidx.compose.ui.geometry.Offset(0f, centerY),
        end = androidx.compose.ui.geometry.Offset(size.width, centerY),
        strokeWidth = 1.dp.toPx()
    )
    values.forEachIndexed { index, value ->
        val height = size.height * (0.02f + value * 0.39f)
        val x = gap + index * (barWidth + gap)
        val brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.9f), secondary.copy(alpha = 0.25f)))
        drawRoundRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(x, centerY - height),
            size = androidx.compose.ui.geometry.Size(barWidth, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(secondary.copy(alpha = 0.25f), primary.copy(alpha = 0.8f))),
            topLeft = androidx.compose.ui.geometry.Offset(x, centerY),
            size = androidx.compose.ui.geometry.Size(barWidth, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpectrogram(
    history: List<FloatArray>,
    primary: Color,
    secondary: Color
) {
    if (history.isEmpty()) return
    val rowHeight = size.height / HISTORY_ROWS
    history.forEachIndexed { row, bands ->
        val y = size.height - (row + 1) * rowHeight
        bands.forEachIndexed { index, value ->
            val x = size.width * index / BAND_COUNT
            val nextX = size.width * (index + 1) / BAND_COUNT
            val intensity = value.coerceIn(0f, 1f)
            val color = androidx.compose.ui.graphics.lerp(
                Color.Black,
                primary,
                intensity
            ).copy(alpha = 0.35f + intensity * 0.65f)
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(nextX - x + 1.dp.toPx(), rowHeight + 1.dp.toPx())
            )
        }
    }
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent, Color.Black.copy(alpha = 0.18f))
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadial(
    values: FloatArray,
    primary: Color,
    secondary: Color
) {
    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.18f
    val maxRadius = minOf(size.width, size.height) * 0.46f
    val points = BAND_COUNT
    for (index in 0 until points) {
        val angle = (index.toFloat() / points) * (2f * PI) - PI / 2f
        val value = values[index]
        val outer = radius + (maxRadius - radius) * (0.18f + value * 0.82f)
        val start = androidx.compose.ui.geometry.Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius
        )
        val end = androidx.compose.ui.geometry.Offset(
            center.x + cos(angle).toFloat() * outer,
            center.y + sin(angle).toFloat() * outer
        )
        drawLine(
            brush = Brush.linearGradient(listOf(primary, secondary.copy(alpha = 0.35f))),
            start = start,
            end = end,
            strokeWidth = max(2.dp.toPx(), size.minDimension * 0.009f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primary.copy(alpha = 0.28f), secondary.copy(alpha = 0.05f), Color.Transparent)
        ),
        radius = radius * 1.7f,
        center = center
    )
    drawCircle(
        color = primary.copy(alpha = 0.22f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}
