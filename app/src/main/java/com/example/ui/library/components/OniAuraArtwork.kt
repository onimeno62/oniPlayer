package com.example.ui.library.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.screens.dashboardRadiusLarge
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.getSongPalette

@Composable
fun OniAuraArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    songForPalette: SongEntity? = null,
    enableAnimation: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glowColor = if (songForPalette != null) {
        getSongPalette(songForPalette).third
    } else {
        LocalAccentColor.current
    }

    val (scale, alpha) = if (enableAnimation) {
        val infiniteTransition = rememberInfiniteTransition(label = "oni_aura_transition")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "oni_aura_scale"
        )
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "oni_aura_alpha"
        )
        Pair(animatedScale, animatedAlpha)
    } else {
        Pair(1.0f, 0.20f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Restrained radial aura glow behind artwork
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = alpha),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(dashboardRadiusLarge())
                )
        )
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun OniAuraArtworkPreview() {
    OniAuraArtwork(
        artworkUri = null,
        modifier = Modifier.size(160.dp),
        enableAnimation = true
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sample Art",
                color = Color.White
            )
        }
    }
}
