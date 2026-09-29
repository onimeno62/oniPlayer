package com.example.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.ui.library.components.LibraryCategoryHero
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.components.SongRow
import com.example.ui.screens.formatDuration
import com.example.ui.theme.OniSkin

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    songsInPlaylist: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    onExportM3U: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDurationMs = remember(songsInPlaylist) { songsInPlaylist.sumOf { it.duration } }
    val artworkUri = remember(songsInPlaylist) {
        songsInPlaylist.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
    }

    val subtitle = buildString {
        val songText = if (songsInPlaylist.size == 1) "1 track" else "${songsInPlaylist.size} tracks"
        append(songText)
        if (totalDurationMs > 0) {
            append(" · ${formatDuration(totalDurationMs)}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        // Shared hero banner
        LibraryCategoryHero(
            title = playlist.name,
            subtitle = subtitle,
            artworkUri = artworkUri,
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            modifier = Modifier.padding(vertical = OniSkin.spacing.md)
        )

        // Action Buttons Row (outside hero for consistent list rhythm)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OniSkin.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            Button(
                onClick = onPlayAll,
                enabled = songsInPlaylist.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = OniSkin.shapes.button,
                contentPadding = PaddingValues(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OniSkin.colors.primary,
                    contentColor = OniSkin.colors.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                Text("Play All", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            FilledTonalButton(
                onClick = onShufflePlay,
                enabled = songsInPlaylist.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = OniSkin.shapes.button,
                contentPadding = PaddingValues(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = OniSkin.colors.surfaceVariant,
                    contentColor = OniSkin.colors.textPrimary
                )
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                Text("Shuffle", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            FilledTonalButton(
                onClick = onExportM3U,
                enabled = songsInPlaylist.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = OniSkin.shapes.button,
                contentPadding = PaddingValues(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = OniSkin.colors.surfaceVariant,
                    contentColor = OniSkin.colors.textPrimary
                )
            ) {
                Icon(Icons.Default.Output, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                Text("Export", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        HorizontalDivider(
            color = OniSkin.colors.outline.copy(alpha = 0.2f),
            modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
        )

        if (songsInPlaylist.isEmpty()) {
            LibraryEmptyState(
                title = "Empty Playlist",
                message = "This playlist has no tracks yet. Add tracks from any song's menu.",
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(currentSong) {
                if (currentSong != null && !listState.isScrollInProgress) {
                    val index = songsInPlaylist.indexOfFirst { it.id == currentSong.id }
                    if (index >= 0) {
                        listState.animateScrollToItem((index - 2).coerceAtLeast(0))
                    }
                }
            }
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(songsInPlaylist, key = { it.id }) { song ->
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
}
