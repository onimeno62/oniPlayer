package com.example.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.library.components.SongRow
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.screens.formatDuration
import com.example.ui.theme.OniSkin

@Composable
fun AlbumDetailScreen(
    album: AlbumUiModel,
    songsInAlbum: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedSongs = remember(songsInAlbum) {
        songsInAlbum.sortedWith(
            compareBy<SongEntity> { it.displayTrack.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.displayTitle }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        // Album Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork using Default Skin OniArtwork
            OniArtwork(
                artworkUri = album.artworkUri,
                size = 110.dp,
                shape = OniSkin.artwork.shape,
                contentDescription = "Cover art for ${album.title}"
            )

            Spacer(modifier = Modifier.width(OniSkin.spacing.md))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.title,
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = album.artist,
                    style = OniSkin.typography.bodyLarge,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${album.songCount} songs • ${formatDuration(album.totalDurationMs)}",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Playback Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OniSkin.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OniSkin.colors.primary,
                    contentColor = OniSkin.colors.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play All", fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onShufflePlay,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = OniSkin.colors.surfaceVariant,
                    contentColor = OniSkin.colors.textPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle", fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(
            color = OniSkin.colors.outline.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
        )

        // Song List
        val albumListState = rememberLazyListState()
        LaunchedEffect(currentSong) {
            if (currentSong != null && !albumListState.isScrollInProgress) {
                val index = sortedSongs.indexOfFirst { it.id == currentSong.id }
                if (index >= 0) {
                    albumListState.animateScrollToItem((index - 2).coerceAtLeast(0))
                }
            }
        }

        LazyColumn(
            state = albumListState,
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sortedSongs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isPlaying = isPlaying,
                    onClick = { onSongClick(song) },
                    onShowMenu = { onShowTrackMenu(song) }
                )
            }
        }
    }
}
