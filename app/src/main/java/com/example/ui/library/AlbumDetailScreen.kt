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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.viewmodel.MusicPlayerViewModel
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
    modifier: Modifier = Modifier,
    artworkActionRequest: Int = 0,
    viewModel: MusicPlayerViewModel? = null
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
        val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                viewModel?.batchUpdateTags(
                    songIds = songsInAlbum.map { it.id },
                    albumArtUri = uri.toString(),
                    removeArt = false,
                    artist = "", album = "", albumArtist = "", genre = "", composer = "",
                    disc = "", track = "", year = "", comment = "", bpm = "", rating = null
                )
            }
        }
        LaunchedEffect(artworkActionRequest) {
            if (artworkActionRequest > 0 && viewModel != null) imagePickerLauncher.launch("image/*")
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

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
