package com.example.ui.library.hero

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.playback.OniPlayPauseButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OniSkin

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Default Skin Continue Listening Hero.
 *
 * Communicates: "This is what you are currently listening to."
 * Serves as the primary focal element in the Library without overwhelming the screen.
 * Consumes [OniSkin] tokens and [OniArtwork], [OniPlayPauseButton], and [OniSurface].
 */
@Composable
fun ContinueListeningHeroV2(
    song: SongEntity,
    isPlaying: Boolean,
    viewModel: com.example.ui.viewmodel.MusicPlayerViewModel,
    onPlayPauseClick: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    isPreparing: Boolean = false
) {
    val reduceMotion = rememberReduceMotion()
    
    val heroColors = rememberHeroColors(
        artworkUri = song.albumArtUri,
        fallbackAccent = OniSkin.colors.primary
    )
    val accentColor = heroColors.dominant

    // Real audio engine state from ViewModel (single source of truth)
    val position by viewModel.audioEngine.position.collectAsStateWithLifecycle()
    val duration by viewModel.audioEngine.duration.collectAsStateWithLifecycle()

    // 1. Subtle, slow breathing glow loop synchronized with playback (3800ms cycle)
    val glowAlpha by if (isPlaying && !isPreparing && !reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "hero_glow_breathing")
        infiniteTransition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.22f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_glow_alpha"
        )
    } else {
        remember { mutableFloatStateOf(0.06f) }
    }

    // 2. Gentle breathing scale on artwork when active
    val artworkScale by if (isPlaying && !isPreparing && !reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "hero_artwork_breathing")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "artwork_scale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val cardShape = OniSkin.shapes.card

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("continue_listening_hero")
            .semantics {
                contentDescription = "Continue listening: ${song.displayTitle} by ${song.displayArtist}"
            }
    ) {
        // Subtle ambient edge glow behind the card (restrained, no heavy blur)
        if (!reduceMotion && isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .blur(16.dp)
                    .clip(cardShape)
                    .background(accentColor.copy(alpha = glowAlpha))
            )
        }

        // Elevated surface container
        OniSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenNowPlaying),
            variant = OniSurfaceVariant.Elevated,
            shape = cardShape,
            elevation = OniSkin.elevation.raised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPlaying) "NOW PLAYING" else "CONTINUE LISTENING",
                        style = OniSkin.typography.caption,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (duration > 0) {
                        val progressPercent = (position.toFloat() / duration.toFloat() * 100).toInt().coerceIn(0, 100)
                        Text(
                            text = "$progressPercent%",
                            style = OniSkin.typography.caption,
                            color = OniSkin.colors.textTertiary
                        )
                    }
                }

                // Main Info Row: Artwork + Metadata + Primary Play/Pause Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dominant Artwork
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                            }
                    ) {
                        OniArtwork(
                            artworkUri = song.albumArtUri,
                            size = 80.dp,
                            shape = OniSkin.artwork.shape,
                            contentDescription = "Album art for ${song.displayTitle}",
                            showGlow = isPlaying && !reduceMotion
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Track details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = song.displayTitle,
                            style = OniSkin.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = song.displayArtist,
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!song.album.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.album,
                                style = OniSkin.typography.bodySmall,
                                color = OniSkin.colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Primary Play/Pause Action
                    OniPlayPauseButton(
                        isPlaying = isPlaying,
                        onClick = onPlayPauseClick,
                        size = 52.dp,
                        loading = isPreparing
                    )
                }

                // Smooth Progress Bar
                HeroProgressBar(
                    position = position,
                    duration = duration,
                    tintColor = accentColor,
                    onOpenNowPlaying = onOpenNowPlaying,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
