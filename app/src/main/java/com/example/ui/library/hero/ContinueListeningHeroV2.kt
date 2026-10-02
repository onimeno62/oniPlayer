package com.example.ui.library.hero

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
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
    onReplayClick: (() -> Unit)? = null
) {
    val motion = OniSkin.motion
    val finished = !isPlaying && !isPreparing && duration > 0 && position >= duration - 1_000L
    val label = when {
        isPlaying -> "NOW PLAYING"
        finished -> "PLAY AGAIN"
        else -> "CONTINUE LISTENING"
    }
    OniSurface(
        modifier = modifier.fillMaxWidth().testTag("continue_listening_hero").semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "Continue listening: ${song.displayTitle} by ${song.displayArtist}" },
        variant = OniSurfaceVariant.Elevated,
        shape = OniSkin.shapes.card,
        elevation = OniSkin.elevation.raised,
        onClick = onOpenNowPlaying
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm), verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = OniSkin.typography.caption, fontWeight = FontWeight.Bold, color = OniSkin.colors.primary)
                if (duration > 0 && !finished) Text("${(position.toFloat() / duration.toFloat() * 100).toInt().coerceIn(0, 100)}%", style = OniSkin.typography.caption, color = OniSkin.colors.textTertiary)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = song.albumArtUri,
                    transitionSpec = {
                        fadeIn(tween(motion.artworkTransitionDurationMs, easing = motion.decelerateEasing)) togetherWith
                            fadeOut(tween(motion.artworkTransitionDurationMs, easing = motion.standardEasing))
                    },
                    label = "continue_listening_artwork_transition"
                ) { artworkUri ->
                    OniArtwork(artworkUri, contentDescription = null, size = 80.dp, shape = OniSkin.artwork.shape)
                }
                Spacer(modifier = Modifier.width(OniSkin.spacing.md))
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
