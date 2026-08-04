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

@Composable
fun HeroBackground(
    artworkUri: String?,
    dominantColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Full-bleed blurred album art background
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .background(dominantColor.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )

        // Subtle diagonal wash using the extracted color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.35f),
                            dominantColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient scrim so text stays readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.80f)
                        )
                    )
                )
        )
    }
}
