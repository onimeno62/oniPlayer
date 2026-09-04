package com.example.ui.components.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.music.OniTrackMetadata
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Reusable Default Skin mini-player bar.
 * Lightweight floating capsule showing currently playing track, quick play/pause,
 * and bottom progress indicator.
 *
 * Consumes [OniSkin.surfaces], [OniSkin.shapes], [OniSkin.colors], and [OniSkin.spacing] tokens.
 * Pure UI component: does NOT own or modify playback state.
 */
@Composable
fun OniMiniPlayer(
    title: String,
    artist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    progressFraction: Float,
    onPlayPauseClick: () -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNextClick: (() -> Unit)? = null,
    shape: Shape = OniSkin.shapes.card,
    surfaceVariant: OniSurfaceVariant = OniSurfaceVariant.Soft
) {
    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = 6.dp)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onPlayerClick
            )
            .semantics { role = Role.Button },
        variant = surfaceVariant,
        shape = shape,
        elevation = OniSkin.elevation.raised
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.md, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                OniArtwork(
                    artworkUri = artworkUri,
                    contentDescription = "Artwork for $title",
                    size = 44.dp,
                    shape = OniSkin.shapes.small
                )

                Spacer(modifier = Modifier.width(OniSkin.spacing.md))

                // Metadata
                OniTrackMetadata(
                    title = title,
                    subtitle = artist,
                    isCurrent = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(OniSkin.spacing.sm))

                // Optional skip next action
                if (onNextClick != null) {
                    OniNextButton(
                        onClick = onNextClick,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                }

                // Primary Play/Pause action
                OniPlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPauseClick,
                    size = 40.dp,
                    iconSize = 22.dp,
                    elevation = 0.dp,
                    modifier = Modifier.testTag("mini_player_play_pause")
                )
            }

            // 2dp Mini Progress Bar at bottom of card
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = OniSkin.colors.primary,
                trackColor = OniSkin.colors.outline.copy(alpha = 0.15f)
            )
        }
    }
}
