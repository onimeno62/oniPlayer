package com.example.ui.library.hero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin
import java.util.Locale

@Composable
fun HeroProgressBar(position: Long, duration: Long, tintColor: Color = OniSkin.colors.primary, onOpenNowPlaying: () -> Unit, modifier: Modifier = Modifier) {
    val targetProgress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val smoothProgress = remember { Animatable(targetProgress) }
    val motion = OniSkin.motion
    LaunchedEffect(targetProgress) {
        smoothProgress.animateTo(targetProgress, animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing))
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val trackColor = OniSkin.colors.outline.copy(alpha = 0.25f)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
        Text(formatTime(position), style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onOpenNowPlaying)
                .semantics { contentDescription = "Playback progress: ${formatTime(position)} of ${formatTime(duration)}" }
        ) {
            val centerY = size.height / 2f
            val width = size.width
            val progressX = if (isRtl) width * (1f - smoothProgress.value) else width * smoothProgress.value
            val startX = if (isRtl) width else 0f
            drawLine(trackColor, Offset(0f, centerY), Offset(width, centerY), 4.dp.toPx(), StrokeCap.Round)
            drawLine(tintColor, Offset(startX, centerY), Offset(progressX, centerY), 4.dp.toPx(), StrokeCap.Round)
            drawCircle(tintColor, 4.dp.toPx(), Offset(progressX, centerY))
        }
        Text(formatTime(duration), style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}
