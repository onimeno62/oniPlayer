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
        // Playback actions are owned by LibraryStickyHeader. Keep only playlist-specific export here.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.sm),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = onExportM3U,
                enabled = songsInPlaylist.isNotEmpty(),
                shape = OniSkin.shapes.button
            ) {
                Icon(Icons.Default.Output, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                Text("Export", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
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
