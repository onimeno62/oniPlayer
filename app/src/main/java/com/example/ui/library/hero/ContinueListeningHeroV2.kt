package com.example.ui.library.hero

import androidx.compose.foundation.layout.*
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

@Composable
fun ContinueListeningHeroV2(
    song: SongEntity,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    isPreparing: Boolean = false
) {
    val cardShape = OniSkin.shapes.card
    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("continue_listening_hero")
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Continue listening: ${song.displayTitle} by ${song.displayArtist}"
            },
        variant = OniSurfaceVariant.Elevated,
        shape = cardShape,
        elevation = OniSkin.elevation.raised,
        onClick = onOpenNowPlaying
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                androidx.compose.material3.Text(if (isPlaying) "NOW PLAYING" else "CONTINUE LISTENING", style = OniSkin.typography.caption, fontWeight = FontWeight.Bold, color = OniSkin.colors.primary)
                if (duration > 0) {
                    val percent = (position.toFloat() / duration.toFloat() * 100).toInt().coerceIn(0, 100)
                    androidx.compose.material3.Text("$percent%", style = OniSkin.typography.caption, color = OniSkin.colors.textTertiary)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OniArtwork(artworkUri = song.albumArtUri, size = 80.dp, shape = OniSkin.artwork.shape, contentDescription = null)
                Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(song.displayTitle, style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    androidx.compose.material3.Text(song.displayArtist, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!song.album.isNullOrBlank()) androidx.compose.material3.Text(song.album, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                OniPlayPauseButton(isPlaying = isPlaying, onClick = onPlayPauseClick, size = OniSkin.playbackControls.secondaryControlSize.coerceAtLeast(48.dp), loading = isPreparing)
            }
            HeroProgressBar(position = position, duration = duration, tintColor = OniSkin.colors.primary, onOpenNowPlaying = onOpenNowPlaying)
        }
    }
}
