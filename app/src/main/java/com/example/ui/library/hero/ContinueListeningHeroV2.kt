package com.example.ui.library.hero

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.playback.OniPlayPauseButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Continue Listening card. Layout is intentionally unchanged (approved design).
 * When the track has finished, the label reads PLAY AGAIN (instead of a dead "100%") and the
 * button calls [onReplayClick] when provided, so it restarts instead of resuming at the end.
 */
@Composable
fun ContinueListeningHeroV2(
    song: SongEntity,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    isPreparing: Boolean = false,
    beatEnergy: Float = 0f,
    visualEffect: Int = 1,
    onReplayClick: (() -> Unit)? = null
) {
    val motion = OniSkin.motion
    val finished = !isPlaying && !isPreparing && duration > 0 && position >= duration - 1_000L
    val label = when {
        isPlaying -> "NOW PLAYING"
        finished -> "PLAY AGAIN"
        else -> "CONTINUE LISTENING"
    }
    val energy = if (isPlaying) beatEnergy.coerceIn(0f, 1f) else 0f
    val reactiveScale = if (visualEffect == 1) 1f + energy * 0.008f else 1f
    val glowElevation = if (visualEffect == 2) OniSkin.elevation.raised + (energy * 10f).dp else OniSkin.elevation.raised

    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = reactiveScale; scaleY = reactiveScale }
            .shadow(glowElevation, OniSkin.shapes.card, ambientColor = OniSkin.colors.primary.copy(alpha = if (visualEffect == 2) 0.22f + energy * 0.32f else 0f), spotColor = OniSkin.colors.primary.copy(alpha = if (visualEffect == 2) 0.20f + energy * 0.28f else 0f))
            .testTag("continue_listening_hero")
            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "Continue listening: ${song.displayTitle} by ${song.displayArtist}" },
        variant = OniSurfaceVariant.Elevated,
        shape = OniSkin.shapes.card,
        elevation = OniSkin.elevation.raised,
        containerColor = Color.Transparent,
        onClick = onOpenNowPlaying
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(OniSkin.shapes.card)
                .background(OniSkin.colors.background)
                .border(
                    width = if (visualEffect == 2) 1.5.dp else 0.dp,
                    color = OniSkin.colors.primary.copy(alpha = if (visualEffect == 2) 0.22f + energy * 0.38f else 0f),
                    shape = OniSkin.shapes.card
                )
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = 0.48f + if (visualEffect == 1) energy * 0.08f else energy * 0.12f },
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                OniSkin.colors.background.copy(alpha = 0.30f),
                                OniSkin.colors.background.copy(alpha = 0.72f),
                                OniSkin.colors.background.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
            ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = OniSkin.typography.caption, fontWeight = FontWeight.Bold, color = OniSkin.colors.primary)
                if (duration > 0 && !finished) Text("${(position.toFloat() / duration.toFloat() * 100).toInt().coerceIn(0, 100)}%", style = OniSkin.typography.caption, color = OniSkin.colors.textTertiary)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.displayTitle, style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.displayArtist, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!song.album.isNullOrBlank()) Text(song.album, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                OniPlayPauseButton(
                    isPlaying,
                    if (finished && onReplayClick != null) onReplayClick else onPlayPauseClick,
                    size = OniSkin.playbackControls.secondaryControlSize.coerceAtLeast(48.dp),
                    loading = isPreparing
                )
            }
            HeroProgressBar(position, duration, OniSkin.colors.primary, onOpenNowPlaying)
        }
    }
}
