package com.example.ui.library.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeroProgressBar(
    position: Long,
    duration: Long,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(position),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.weight(1f).height(4.dp),
            color = tintColor,
            trackColor = Color.White.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
        
        Text(
            text = formatTime(duration),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
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
