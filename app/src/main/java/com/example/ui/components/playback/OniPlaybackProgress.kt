package com.example.ui.components.playback

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin
import java.util.Locale

/**
 * Reusable playback progress component.
 * Features a thin, elegant track with a generous touch target for accurate scrubbing.
 * Consumes [OniSkin.colors], [OniSkin.typography], and [OniSkin.spacing] tokens.
 * Pure presentation: receives position and duration, emits [onSeek].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniPlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showTimeLabels: Boolean = true,
    activeTrackColor: Color = OniSkin.colors.primary,
    inactiveTrackColor: Color = OniSkin.colors.outline.copy(alpha = 0.3f),
    thumbColor: Color = OniSkin.colors.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    // Local scrub position while dragging to prevent stream rubber-banding
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = if (durationMs > 0) durationMs else 1L
    val streamFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragProgress else streamFraction

    val displayedPositionMs = if (isDragging) {
        (dragProgress * safeDuration).toLong()
    } else {
        positionMs
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Playback progress: ${formatTime(displayedPositionMs)} of ${formatTime(durationMs)}"
            }
    ) {
        Slider(
            value = displayFraction,
            onValueChange = { fraction ->
                dragProgress = fraction
            },
            onValueChangeFinished = {
                val targetMs = (dragProgress * safeDuration).toLong().coerceIn(0L, durationMs)
                onSeek(targetMs)
            },
            enabled = enabled && durationMs > 0,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = activeTrackColor,
                inactiveTrackColor = inactiveTrackColor,
                disabledThumbColor = OniSkin.colors.disabled,
                disabledActiveTrackColor = OniSkin.colors.disabled.copy(alpha = 0.5f),
                disabledInactiveTrackColor = OniSkin.colors.disabled.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )

        if (showTimeLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(displayedPositionMs),
                    style = OniSkin.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = OniSkin.colors.textSecondary
                )
                Text(
                    text = formatTime(durationMs),
                    style = OniSkin.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = OniSkin.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Format milliseconds into standard mm:ss or hh:mm:ss display.
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
