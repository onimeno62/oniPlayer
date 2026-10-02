package com.example.ui.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.theme.OniSkin

/**
 * Dashboard carousel card (Recently Played / Recently Added).
 * - Tap plays.
 * - Press shows a play overlay; the current track shows a live indicator.
 * - Long-press opens quick actions: Play next, Add to queue, Favorite.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardCarouselCard(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val title = song.displayTitle
    val artist = song.displayArtist

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .width(size)
                .clip(OniSkin.shapes.medium)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClickLabel = "Play",
                    onLongClickLabel = "More options",
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                    onClick = onClick
                )
                .semantics(mergeDescendants = true) { contentDescription = "$title by $artist" }
        ) {
            Box(modifier = Modifier.size(size)) {
                OniArtwork(
                    artworkUri = song.albumArtUri,
                    contentDescription = null,
                    size = size,
                    shape = OniSkin.artwork.shape
                )
                if (pressed || isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(OniSkin.artwork.shape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OniSkin.colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = OniSkin.colors.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
            Text(
                text = title,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) OniSkin.colors.primary else OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Play next") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                onClick = { menuOpen = false; onPlayNext() }
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                onClick = { menuOpen = false; onAddToQueue() }
            )
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                },
                onClick = { menuOpen = false; onToggleFavorite() }
            )
        }
    }
}
