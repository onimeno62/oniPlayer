package com.example.ui.library.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.screens.dashboardRadiusMedium

@Composable
fun ArtworkPanel(
    artworkUri: String?,
    isPlaying: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    // Elegant infinite cycle between 1.0 and 1.02, reversing smoothly every 7s total (3.5s in, 3.5s out)
    val scale = if (isPlaying && !reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "ArtworkBreathing")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Scale"
        ).value
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .size(112.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(dashboardRadiusMedium()))
            .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = artworkUri,
            contentDescription = "Album Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
        )
    }
}
