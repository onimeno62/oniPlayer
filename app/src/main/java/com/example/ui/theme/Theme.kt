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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.dynamicDarkColorScheme
import android.os.Build
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import kotlin.math.absoluteValue

// Composition locals for the dynamic accent and visual tokens
val LocalAccentColor = compositionLocalOf { Color(0xFF3B73E3) }
val LocalSecondaryColor = compositionLocalOf { Color(0xFF4D8DFF) }
val LocalAccentGlowColor = compositionLocalOf { Color(0xFF3B73E3).copy(alpha = 0.5f) }

// Composition locals for Visual Effects Settings
val LocalGlassEffectEnabled = compositionLocalOf { true }
val LocalBlurStrength = compositionLocalOf { 20f }
val LocalCornerRadius = compositionLocalOf { 16f }
val LocalBackgroundTransparency = compositionLocalOf { 50f }

fun getSongPalette(song: SongEntity?): Triple<Color, Color, Color> {
    if (song == null) {
        // Fallback default skin blue palette
        return Triple(
            Color(0xFF3B73E3), // Primary
            Color(0xFF4D8DFF), // Secondary
            Color(0xFF3B73E3).copy(alpha = 0.5f)  // Accent/Glow
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
    theme: OniTheme = OniTheme.CLASSIC_DARK,
    currentSong: SongEntity? = null,
    customAccentColor: String = "#3B73E3",
    materialYouEnabled: Boolean = false,
    glassEffectEnabled: Boolean = true,
    blurStrength: Float = 20f,
    cornerRadius: Float = 16f,
    backgroundTransparency: Float = 50f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeColors = ThemeProvider.getThemeColors(theme)
    
    // Resolve dynamic accent color based on Material You or custom accent color
    val baseAccentColor = remember(customAccentColor, materialYouEnabled) {
        if (materialYouEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                dynamicDarkColorScheme(context).primary
            } catch (e: Exception) {
                try {
                    Color(android.graphics.Color.parseColor(customAccentColor))
                } catch (ex: Exception) {
                    Color(0xFF3B73E3)
                }
            }
        } else {
            try {
                Color(android.graphics.Color.parseColor(customAccentColor))
            } catch (e: Exception) {
                Color(0xFF3B73E3)
            }
        }
    }

    // Extract dynamic colors if in a dark theme or requested
    val (dynamicPrimary, dynamicSecondary, dynamicAccent) = if (currentSong == null) {
        // Derive secondary and glow from baseAccentColor for cohesive appearance
        val sec = baseAccentColor.copy(alpha = 0.8f)
        val glow = baseAccentColor.copy(alpha = 0.6f)
        Triple(baseAccentColor, sec, glow)
    } else {
        getSongPalette(currentSong)
    }
    
    val accent = if (themeColors.isDark) dynamicPrimary else themeColors.accent
    val secondary = if (themeColors.isDark) dynamicSecondary else themeColors.secondary
    val glow = if (themeColors.isDark) dynamicAccent else themeColors.accent.copy(alpha = 0.3f)

    // Build the active OniSkinTokens
    val activeSkin = remember(themeColors.isDark, accent, secondary, glow, cornerRadius) {
        val baseSkin = DefaultSkin.createSkin(
            isDark = themeColors.isDark,
            accentColor = accent,
            accentSecondary = secondary,
            accentGlow = glow
        )
        if (cornerRadius != 16f) {
            val ratio = cornerRadius / 16f
            baseSkin.copy(
                shapes = baseSkin.shapes.copy(
                    xs = androidx.compose.foundation.shape.RoundedCornerShape((6f * ratio).dp),
                    small = androidx.compose.foundation.shape.RoundedCornerShape((10f * ratio).dp),
                    medium = androidx.compose.foundation.shape.RoundedCornerShape((14f * ratio).dp),
                    large = androidx.compose.foundation.shape.RoundedCornerShape((18f * ratio).dp),
                    xl = androidx.compose.foundation.shape.RoundedCornerShape((24f * ratio).dp),
                    xxl = androidx.compose.foundation.shape.RoundedCornerShape((32f * ratio).dp),
                    button = androidx.compose.foundation.shape.RoundedCornerShape((14f * ratio).dp),
                    card = androidx.compose.foundation.shape.RoundedCornerShape((18f * ratio).dp),
                    listItem = androidx.compose.foundation.shape.RoundedCornerShape((14f * ratio).dp),
                    dialog = androidx.compose.foundation.shape.RoundedCornerShape((24f * ratio).dp),
                    bottomSheet = androidx.compose.foundation.shape.RoundedCornerShape((24f * ratio).dp),
                    artwork = androidx.compose.foundation.shape.RoundedCornerShape((18f * ratio).dp),
                    chip = androidx.compose.foundation.shape.RoundedCornerShape((10f * ratio).dp)
                )
            )
        } else {
            baseSkin
        }
    }

    CompositionLocalProvider(
        LocalOniSkin provides activeSkin,
        LocalAccentColor provides accent,
        LocalSecondaryColor provides secondary,
        LocalAccentGlowColor provides glow,
        LocalGlassEffectEnabled provides glassEffectEnabled,
        LocalBlurStrength provides blurStrength,
        LocalCornerRadius provides cornerRadius,
        LocalBackgroundTransparency provides backgroundTransparency
    ) {
        MaterialTheme(
            colorScheme = activeSkin.colors.toMaterialColorScheme(),
            typography = activeSkin.typography.toMaterialTypography(),
            shapes = activeSkin.shapes.toMaterialShapes()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        themeColors.background.copy(
                            alpha = if (!themeColors.isDark) 1f else if (glassEffectEnabled) backgroundTransparency / 100f else 1f
                        )
                    )
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
                    
                    val alphaScale = if (glassEffectEnabled) (backgroundTransparency / 100f) else 0f
                    
                    if (alphaScale > 0f) {
                        // Sphere 1: Top Right primary glow (Atmospheric Gaussian style)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.16f * alphaScale), Color.Transparent),
                                center = Offset(w * 0.85f, h * 0.25f),
                                radius = w * 0.9f * scaleFactor1
                            )
                        )
                        
                        // Sphere 2: Bottom Left secondary glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(secondary.copy(alpha = 0.14f * alphaScale), Color.Transparent),
                                center = Offset(w * 0.15f, h * 0.75f),
                                radius = w * 0.9f * scaleFactor2
                            )
                        )
                        
                        // Sphere 3: Center accent glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glow.copy(alpha = 0.08f * alphaScale), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.6f
                            )
                        )
                    }
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



