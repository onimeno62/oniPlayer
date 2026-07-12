package com.example.ui.theme
 
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.entity.SongEntity
import kotlin.math.absoluteValue

// Composition locals for the gorgeous dynamic Aurora Glass palette
val LocalAccentColor = compositionLocalOf { Color(0xFF7C4DFF) }
val LocalSecondaryColor = compositionLocalOf { Color(0xFF00E5FF) }
val LocalAccentGlowColor = compositionLocalOf { Color(0xFFFF007F) }

fun getSongPalette(song: SongEntity?): Triple<Color, Color, Color> {
    if (song == null) {
        // Fallback gorgeous premium colors
        return Triple(
            Color(0xFF7C4DFF), // Primary
            Color(0xFF00E5FF), // Secondary
            Color(0xFFFF007F)  // Accent/Glow
        )
    }
    val title = song.customTitle ?: song.title
    val artist = song.customArtist ?: song.artist ?: ""
    val seed = (title + artist).hashCode().absoluteValue
    val hue = (seed % 360).toFloat()
    
    // Luxury vibrancy parameters for Aurora Glass
    val primary = Color.hsl(hue = hue, saturation = 0.88f, lightness = 0.60f)
    val secondary = Color.hsl(hue = (hue + 40) % 360f, saturation = 0.80f, lightness = 0.50f)
    val accent = Color.hsl(hue = (hue + 120) % 360f, saturation = 0.90f, lightness = 0.65f)
    
    return Triple(primary, secondary, accent)
}

@Composable
fun OniPlayerTheme(
    theme: OniTheme = OniTheme.COSMIC_OBSIDIAN,
    currentSong: SongEntity? = null,
    content: @Composable () -> Unit
) {
    val themeColors = ThemeProvider.getThemeColors(theme)
    
    // Extract dynamic colors if in a dark theme or requested
    val (dynamicPrimary, dynamicSecondary, dynamicAccent) = getSongPalette(currentSong)
    
    val accent = dynamicPrimary
    val secondary = dynamicSecondary
    val glow = dynamicAccent

    val colorScheme = remember(themeColors, accent, secondary, glow) {
        darkColorScheme(
            primary = accent,
            secondary = secondary,
            tertiary = glow,
            background = Color(0xFF12141B), // Aurora Glass Default Background
            surface = Color(0xFF1A1D26),    // Aurora Glass Default Surface
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1A1D26).copy(alpha = 0.6f),
            onSurfaceVariant = Color(0xFFA7ABB6) // Secondary Text color
        )
    }

    CompositionLocalProvider(
        LocalAccentColor provides accent,
        LocalSecondaryColor provides secondary,
        LocalAccentGlowColor provides glow
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF12141B)) // Immersive dark background
            ) {
                // Smooth atmospheric breathing animation for the glows
                val infiniteTransition = rememberInfiniteTransition(label = "aurora")
                
                val scaleFactor1 by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(12000, easing = SineHeightEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale1"
                )
                val scaleFactor2 by infiniteTransition.animateFloat(
                    initialValue = 1.1f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(14000, easing = SineHeightEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale2"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Sphere 1: Top Right primary glow (Atmospheric Gaussian style)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(w * 0.85f, h * 0.25f),
                            radius = w * 0.9f * scaleFactor1
                        )
                    )
                    
                    // Sphere 2: Bottom Left secondary glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(secondary.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(w * 0.15f, h * 0.75f),
                            radius = w * 0.9f * scaleFactor2
                        )
                    )
                    
                    // Sphere 3: Center accent glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = w * 0.6f
                        )
                    )
                }
                
                content()
            }
        }
    }
}

private val SineHeightEasing = Easing { fraction ->
    val t = fraction * Math.PI
    kotlin.math.sin(t).toFloat()
}



