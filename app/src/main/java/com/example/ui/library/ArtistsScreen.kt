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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        LibraryEmptyState(
            title = "No Artists",
            message = "No artists found in your library",
            modifier = modifier.fillMaxSize()
        )
    } else if (layoutMode == "grid") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
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
                    colors = glassCardColors(),
                    border = glassCardBorder(),
                    elevation = glassCardElevation()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
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
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAccentColor.current
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                        val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                        Text(
                            text = "$albumText • $songText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
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
