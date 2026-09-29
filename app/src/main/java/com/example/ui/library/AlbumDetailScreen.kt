package com.example.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
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
import com.example.data.entity.SongEntity
import com.example.ui.library.components.LibraryCategoryHero
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

    val subtitle = buildString {
        append(album.artist)
        if (album.songCount > 0) {
            append(" · ${album.songCount} songs")
        }
        if (album.totalDurationMs > 0) {
            append(" · ${formatDuration(album.totalDurationMs)}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        // Shared hero banner
        LibraryCategoryHero(
            title = album.title,
            subtitle = subtitle,
            artworkUri = album.artworkUri,
            icon = Icons.Default.Album,
            modifier = Modifier.padding(vertical = OniSkin.spacing.md)
        )

        // Action Buttons Row (outside hero for consistent list rhythm)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OniSkin.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
                shape = OniSkin.shapes.button,
                contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OniSkin.colors.primary,
                    contentColor = OniSkin.colors.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                Text("Play All", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                onClick = onShufflePlay,
                modifier = Modifier.weight(1f),
                shape = OniSkin.shapes.button,
                contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = OniSkin.colors.surfaceVariant,
                    contentColor = OniSkin.colors.textPrimary
                )
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                Text("Shuffle", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(
            color = OniSkin.colors.outline.copy(alpha = 0.2f),
            modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
        )

        val albumListState = rememberLazyListState()
        LaunchedEffect(currentSong) {
            if (currentSong != null && !albumListState.isScrollInProgress) {
                val index = sortedSongs.indexOfFirst { it.id == currentSong.id }
                if (index >= 0) albumListState.animateScrollToItem((index - 2).coerceAtLeast(0))
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
