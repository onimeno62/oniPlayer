package com.example.ui.components.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable Default Skin music track row.
 * Designed for high-performance lazy lists, strictly presentation-driven.
 * Consumes [OniSkin.colors], [OniSkin.shapes], [OniSkin.spacing], and [OniSkin.typography] tokens.
 */
@Composable
fun OniTrackRow(
    title: String,
    artist: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    album: String? = null,
    artworkUri: String? = null,
    durationText: String? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    isFavorite: Boolean? = null,
    onFavoriteToggle: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    playingIndicator: (@Composable () -> Unit)? = null
) {
    val subtitle = if (!album.isNullOrBlank()) "$artist • $album" else artist
    val shape = OniSkin.shapes.listItem

    val backgroundModifier = if (isCurrent) {
        Modifier.background(OniSkin.colors.primary.copy(alpha = 0.12f))
    } else {
        Modifier.background(Color.Transparent)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .then(backgroundModifier)
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                selected = isCurrent
            }
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork
        OniArtwork(
            artworkUri = artworkUri,
            contentDescription = "Cover art for $title",
            size = 48.dp,
            shape = OniSkin.shapes.small
        )

        Spacer(modifier = Modifier.width(OniSkin.spacing.md))

        // Metadata
        OniTrackMetadata(
            title = title,
            subtitle = subtitle,
            tertiaryText = durationText,
            isCurrent = isCurrent,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(OniSkin.spacing.sm))

        // Playing status indicator
        if (isCurrent) {
            if (playingIndicator != null) {
                playingIndicator()
            } else {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = if (isPlaying) "Playing" else "Paused",
                    tint = OniSkin.colors.primary,
                    modifier = Modifier
                        .padding(end = OniSkin.spacing.xs)
                        .size(20.dp)
                )
            }
        }

        // Optional favorite action
        if (isFavorite != null && onFavoriteToggle != null) {
            OniFavoriteAction(
                isFavorite = isFavorite,
                onToggle = onFavoriteToggle,
                touchTargetSize = 40.dp,
                iconSize = 20.dp
            )
        }

        // Optional overflow action
        if (onMoreClick != null) {
            OniOverflowAction(
                onClick = onMoreClick,
                contentDescription = "Options for $title",
                touchTargetSize = 40.dp,
                iconSize = 20.dp
            )
        }
    }
}
