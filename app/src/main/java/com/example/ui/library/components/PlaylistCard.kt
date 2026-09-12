package com.example.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.model.PlaylistUiModel
import com.example.ui.screens.formatDuration
import com.example.ui.theme.OniSkin

@Composable
fun PlaylistCard(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val songText = if (playlist.songCount == 1) "1 track" else "${playlist.songCount} tracks"

    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "${playlist.name}, $songText"
            }
            .clickable(onClick = onClick),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OniSkin.spacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                OniArtwork(
                    artworkUri = playlist.artworkUri,
                    shape = OniSkin.artwork.shape,
                    contentDescription = null,
                    placeholder = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = OniSkin.colors.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(OniSkin.spacing.xxs)
                ) {
                    OniSurface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        variant = OniSurfaceVariant.Flat,
                        containerColor = OniSkin.colors.surface.copy(alpha = 0.85f)
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Playlist options",
                                tint = OniSkin.colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (playlist.songCount > 0) {
                            DropdownMenuItem(
                                text = { Text("Play", style = OniSkin.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OniSkin.colors.primary)
                                },
                                onClick = {
                                    showMenu = false
                                    onPlay()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Shuffle", style = OniSkin.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = OniSkin.colors.primary)
                                },
                                onClick = {
                                    showMenu = false
                                    onShuffle()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Rename", style = OniSkin.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = OniSkin.colors.textPrimary)
                            },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = OniSkin.colors.error)
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

            Text(
                text = playlist.name,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val durationText = if (playlist.totalDurationMs > 0) " • ${formatDuration(playlist.totalDurationMs)}" else ""
            Text(
                text = "$songText$durationText",
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistRow(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val songText = if (playlist.songCount == 1) "1 track" else "${playlist.songCount} tracks"
    val durationText = if (playlist.totalDurationMs > 0) " • ${formatDuration(playlist.totalDurationMs)}" else ""

    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "${playlist.name}, $songText"
            }
            .clickable(onClick = onClick),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniArtwork(
                artworkUri = playlist.artworkUri,
                size = 52.dp,
                shape = OniSkin.artwork.shape,
                contentDescription = null,
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = OniSkin.colors.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(OniSkin.spacing.md))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = OniSkin.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$songText$durationText",
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = OniSkin.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (playlist.songCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Play", style = OniSkin.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OniSkin.colors.primary)
                            },
                            onClick = {
                                showMenu = false
                                onPlay()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shuffle", style = OniSkin.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = OniSkin.colors.primary)
                            },
                            onClick = {
                                showMenu = false
                                onShuffle()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rename", style = OniSkin.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = OniSkin.colors.textPrimary)
                        },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = OniSkin.colors.error)
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OniSkin.colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
