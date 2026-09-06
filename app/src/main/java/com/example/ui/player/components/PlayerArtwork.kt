package com.example.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Centered album artwork presentation for the Player screen in Default Skin.
 *
 * Uses the shared [OniArtwork] component for consistent artwork rendering,
 * caching, and placeholder display across the application.
 *
 * Consumes [OniSkin.artwork], [OniSkin.shapes], and [OniSkin.colors] tokens.
 * Replaces the legacy Aurora Glass breathing/drifting vinyl presentation.
 */
@Composable
fun PlayerArtwork(
    song: SongEntity?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val formatBadge = song?.let { s ->
        val path = s.filePath.lowercase()
        val ext = path.substringAfterLast('.', "")
        when {
            ext == "flac" || s.format.equals("FLAC", ignoreCase = true) -> "FLAC"
            ext == "wav" -> "WAV 24b"
            ext == "m4a" || ext == "aac" -> "AAC"
            ext == "ogg" || ext == "opus" -> "OPUS"
            s.bitrate >= 320 -> "320 KBPS"
            s.bitrate > 0 -> "${s.bitrate} KBPS"
            else -> null
        }
    }

    val artworkShape = OniSkin.artwork.shape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        contentAlignment = Alignment.Center
    ) {
        val clickModifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick
            )
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artworkShape)
                .then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            OniArtwork(
                artworkUri = song?.albumArtUri,
                contentDescription = song?.title?.let { "Album art for $it" } ?: "Album artwork",
                shape = artworkShape,
                elevation = OniSkin.artwork.shadowElevation,
                modifier = Modifier.fillMaxSize()
            )

            // Audio Quality / Format badge in top-right corner
            if (formatBadge != null) {
                OniSurface(
                    variant = OniSurfaceVariant.Frosted,
                    shape = OniSkin.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = formatBadge,
                        style = OniSkin.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
