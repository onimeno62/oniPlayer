package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.library.components.AlbumCard
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.components.glassCard
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.screens.dashboardRadiusMedium
import com.example.ui.screens.formatDuration
import com.example.ui.theme.LocalAccentColor

@Composable
fun AlbumsScreen(
    albums: List<AlbumUiModel>,
    layoutMode: String,
    onAlbumClick: (AlbumUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        LibraryEmptyState(
            title = "No Albums",
            message = "No albums found in your library",
            modifier = modifier.fillMaxSize()
        )
    } else {
        if (layoutMode == "grid") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.fillMaxSize()
            ) {
                items(albums, key = { it.albumKey }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.fillMaxSize()
            ) {
                items(albums, key = { it.albumKey }) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(dashboardRadiusMedium()))
                            .clickable { onAlbumClick(album) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(dashboardRadiusMedium()))
                                .background(LocalAccentColor.current.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (album.artworkUri != null) {
                                AsyncImage(
                                    model = album.artworkUri,
                                    contentDescription = "Cover art for ${album.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = LocalAccentColor.current.copy(alpha = 0.7f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${album.songCount} songs • ${formatDuration(album.totalDurationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
