package com.example.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.ui.library.components.AlbumCard
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.screens.formatDuration
import com.example.ui.theme.OniSkin

@Composable
fun AlbumsScreen(
    albums: List<AlbumUiModel>,
    layoutMode: String,
    onAlbumClick: (AlbumUiModel) -> Unit,
    modifier: Modifier = Modifier,
    gridIndex: Int = 0,
    gridOffset: Int = 0,
    onGridScroll: (Int, Int) -> Unit = { _, _ -> },
    listIndex: Int = 0,
    listOffset: Int = 0,
    onListScroll: (Int, Int) -> Unit = { _, _ -> }
) {
    if (albums.isEmpty()) {
        LibraryEmptyState(
            title = "No Albums",
            message = "No albums found in your library",
            modifier = modifier.fillMaxSize()
        )
    } else {
        if (layoutMode == "grid") {
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = gridIndex,
                initialFirstVisibleItemScrollOffset = gridOffset
            )
            LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
                onGridScroll(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(OniSkin.spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
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
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = listIndex,
                initialFirstVisibleItemScrollOffset = listOffset
            )
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                onListScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(OniSkin.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = modifier.fillMaxSize()
            ) {
                items(albums, key = { it.albumKey }) { album ->
                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${album.title} by ${album.artist}, ${album.songCount} songs"
                            }
                            .clickable { onAlbumClick(album) },
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OniArtwork(
                                artworkUri = album.artworkUri,
                                size = 56.dp,
                                shape = OniSkin.artwork.shape,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(OniSkin.spacing.md))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = album.title,
                                    style = OniSkin.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = album.artist,
                                    style = OniSkin.typography.bodyMedium,
                                    color = OniSkin.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${album.songCount} songs • ${formatDuration(album.totalDurationMs)}",
                                    style = OniSkin.typography.caption,
                                    color = OniSkin.colors.textTertiary,
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
}
