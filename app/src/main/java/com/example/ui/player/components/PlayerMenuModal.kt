package com.example.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.screens.SongStaticDataCard
import com.example.ui.screens.TrackMenuActionTile
import com.example.ui.theme.OniSkin

/**
 * Now Playing overflow menu.
 *
 * Mirrors the Library song menu for track-management/navigation actions.
 * Player-only tools remain available here, while Play Now / Play Next / Add to Queue
 * are intentionally omitted because the track is already the active track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenuModal(
    song: SongEntity?,
    isFavorite: Boolean,
    floatingLyricsEnabled: Boolean,
    isSleepTimerRunning: Boolean,
    sleepTimerMinutesLeft: Int,
    audioAnalyzerEnabled: Boolean,
    artworkEffectsEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenPlaylistPicker: () -> Unit,
    onOpenFileLocation: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onToggleFloatingLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenKaraoke: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleAudioAnalyzer: () -> Unit,
    onToggleArtworkEffects: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surface,
        contentColor = OniSkin.colors.textPrimary,
        shape = OniSkin.shapes.bottomSheet,
        modifier = modifier,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        OniSkin.colors.outline.copy(alpha = 0.4f),
                        OniSkin.shapes.full
                    )
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
                .padding(bottom = OniSkin.spacing.screenVertical),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OniArtwork(
                        artworkUri = song?.albumArtUri,
                        contentDescription = "Cover art",
                        shape = OniSkin.shapes.small,
                        elevation = OniSkin.elevation.flat,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.displayTitle ?: "Now Playing",
                            style = OniSkin.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song != null) {
                            Text(
                                text = song.displayArtist.ifBlank { "Unknown Artist" },
                                style = OniSkin.typography.bodySmall,
                                color = OniSkin.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.displayAlbum.ifBlank { "Unknown Album" },
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (song != null) {
                item {
                    SongStaticDataCard(song = song)
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        TrackMenuActionTile(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            title = if (isFavorite) "Favorited" else "Favorite",
                            iconTint = if (isFavorite) OniSkin.colors.primary else null,
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onToggleFavorite() }
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.PlaylistAdd,
                            title = "Playlist",
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenPlaylistPicker() }
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.Edit,
                            title = "Edit Tags",
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenTagEditor() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        TrackMenuActionTile(
                            icon = Icons.Default.Album,
                            title = "View Album",
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenAlbum() }
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.Person,
                            title = "View Artist",
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenArtist() }
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.FolderOpen,
                            title = "File Location",
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenFileLocation() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        TrackMenuActionTile(
                            icon = Icons.Default.Lyrics,
                            title = "Lyrics",
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenKaraoke() }
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.PictureInPicture,
                            title = if (floatingLyricsEnabled) "Floating On" else "Floating Lyrics",
                            iconTint = if (floatingLyricsEnabled) OniSkin.colors.primary else null,
                            modifier = Modifier.weight(1f),
                            onClick = onToggleFloatingLyrics
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.Timer,
                            title = if (isSleepTimerRunning) "Timer $sleepTimerMinutesLeft m" else "Sleep Timer",
                            iconTint = if (isSleepTimerRunning) OniSkin.colors.primary else null,
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onOpenSleepTimer() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        TrackMenuActionTile(
                            icon = Icons.Default.GraphicEq,
                            title = if (audioAnalyzerEnabled) "Analyzer On" else "Analyzer Pulse",
                            iconTint = if (audioAnalyzerEnabled) OniSkin.colors.primary else null,
                            modifier = Modifier.weight(1f),
                            onClick = onToggleAudioAnalyzer
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.AutoAwesome,
                            title = if (artworkEffectsEnabled) "Motion & Glow On" else "Motion & Glow",
                            iconTint = if (artworkEffectsEnabled) OniSkin.colors.primary else null,
                            modifier = Modifier.weight(1f),
                            onClick = onToggleArtworkEffects
                        )
                        TrackMenuActionTile(
                            icon = Icons.Default.DeleteOutline,
                            title = "Delete Track",
                            iconTint = OniSkin.colors.error,
                            textColor = OniSkin.colors.error,
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); onDeleteClick() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPlaylistPickerSheet(
    song: SongEntity,
    playlists: List<PlaylistEntity>,
    onAddToPlaylist: (PlaylistEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surface,
        shape = OniSkin.shapes.bottomSheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        OniSkin.colors.outline.copy(alpha = 0.4f),
                        OniSkin.shapes.full
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = OniSkin.spacing.screenHorizontal,
                    bottom = OniSkin.spacing.xl
                )
        ) {
            Text(
                text = "Add to Playlist",
                style = OniSkin.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary
            )
            Text(
                text = song.displayTitle,
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    top = OniSkin.spacing.xxs,
                    bottom = OniSkin.spacing.md
                )
            )

            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists yet. Create one from Library → Playlists.",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary,
                    modifier = Modifier.padding(vertical = OniSkin.spacing.lg)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onAddToPlaylist(playlist) }
                                )
                                .padding(
                                    horizontal = OniSkin.spacing.sm,
                                    vertical = OniSkin.spacing.md
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = OniSkin.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                            Text(
                                text = playlist.name,
                                style = OniSkin.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = OniSkin.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
