package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.library.components.ArtistRow
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.components.glassCardColors
import com.example.ui.library.components.glassCardBorder
import com.example.ui.library.components.glassCardElevation
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.screens.dashboardRadiusMedium
import com.example.ui.theme.LocalAccentColor

@Composable
fun ArtistsScreen(
    artists: List<ArtistUiModel>,
    layoutMode: String,
    onArtistClick: (ArtistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    gridIndex: Int = 0,
    gridOffset: Int = 0,
    onGridScroll: (Int, Int) -> Unit = { _, _ -> },
    listIndex: Int = 0,
    listOffset: Int = 0,
    onListScroll: (Int, Int) -> Unit = { _, _ -> }
) {
    if (artists.isEmpty()) {
        LibraryEmptyState(
            title = "No Artists",
            message = "No artists found in your library",
            modifier = modifier.fillMaxSize()
        )
    } else if (layoutMode == "grid") {
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
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(artists.size, key = { index -> artists[index].artistKey }) { index ->
                val artist = artists[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.9f)
                        .clickable { onArtistClick(artist) },
                    shape = RoundedCornerShape(dashboardRadiusMedium()),
                    border = glassCardBorder(),
                    elevation = glassCardElevation()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val accentColor = LocalAccentColor.current
                        if (artist.artworkUri != null) {
                            AsyncImage(
                                model = artist.artworkUri,
                                contentDescription = "Artwork for ${artist.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Abstract aesthetic gradient background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.5f),
                                                accentColor.copy(alpha = 0.15f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Gradient protection overlay to guarantee text legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        // Artist metadata (name, albums, songs) sitting at the bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                            val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                            Text(
                                text = "$albumText • $songText",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(artists, key = { it.artistKey }) { artist ->
                ArtistRow(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}
