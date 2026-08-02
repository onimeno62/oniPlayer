package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.entity.SongEntity
import com.example.ui.library.components.AlbumCard
import com.example.ui.library.components.OniSectionHeader
import com.example.ui.library.components.SongRow
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.theme.LocalAccentColor

@Composable
fun ArtistDetailScreen(
    artist: ArtistUiModel,
    albumsByArtist: List<AlbumUiModel>,
    songsByArtist: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedSongs = remember(songsByArtist) {
        songsByArtist.sortedWith(
            compareBy<SongEntity> { it.displayAlbum }
                .thenBy { it.displayTrack.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.displayTitle }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Artist Header Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(LocalAccentColor.current.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (artist.artworkUri != null) {
                        AsyncImage(
                            model = artist.artworkUri,
                            contentDescription = "Avatar for ${artist.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = LocalAccentColor.current
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                Text(
                    text = "$albumText • $songText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalAccentColor.current,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
                    modifier = Modifier.weight(1f)
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
        }

        // Albums Horizontal Row (if any)
        if (albumsByArtist.isNotEmpty()) {
            item {
                OniSectionHeader(
                    title = "Albums",
                    modifier = Modifier.padding(top = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(albumsByArtist, key = { it.albumKey }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = {} // Not clickable this stage per spec
                        )
                    }
                }
            }
        }

        // Songs Header
        item {
            OniSectionHeader(
                title = "Songs",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Songs List
        items(sortedSongs, key = { it.id }) { song ->
            SongRow(
                song = song,
                isCurrent = song.id == currentSong?.id,
                isPlaying = isPlaying,
                onClick = { onSongClick(song) },
                onShowMenu = { onShowTrackMenu(song) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}
