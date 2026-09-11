package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.library.components.ArtistRow
import com.example.ui.library.components.LibraryCategoryHero
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.theme.OniSkin

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
    } else {
        val heroArtwork = artists.firstOrNull()?.artworkUri
        val heroSubtitle = if (artists.size == 1) "1 artist" else "${artists.size} artists"

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
                contentPadding = PaddingValues(
                    start = OniSkin.spacing.sm,
                    end = OniSkin.spacing.sm,
                    top = OniSkin.spacing.sm,
                    bottom = OniSkin.spacing.lg
                ),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryCategoryHero(
                        title = "Artists",
                        subtitle = heroSubtitle,
                        artworkUri = heroArtwork,
                        icon = androidx.compose.material.icons.Icons.Default.Person,
                        modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
                    )
                }
                items(artists, key = { it.artistKey }) { artist ->
                    val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                    val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"

                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.9f)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${artist.name}, $albumText, $songText"
                            }
                            .clickable { onArtistClick(artist) },
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            OniArtwork(
                                artworkUri = artist.artworkUri,
                                shape = OniSkin.artwork.shape,
                                contentDescription = null,
                                placeholder = {
                                    val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                                    Text(
                                        text = initial,
                                        style = OniSkin.typography.displayMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OniSkin.colors.primary.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )

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

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(OniSkin.spacing.sm)
                            ) {
                                Text(
                                    text = artist.name,
                                    style = OniSkin.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                                Text(
                                    text = "$albumText • $songText",
                                    style = OniSkin.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.82f),
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
                contentPadding = PaddingValues(
                    horizontal = OniSkin.spacing.screenHorizontal,
                    vertical = OniSkin.spacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = modifier.fillMaxSize()
            ) {
                item(key = "artists_hero") {
                    LibraryCategoryHero(
                        title = "Artists",
                        subtitle = heroSubtitle,
                        artworkUri = heroArtwork,
                        icon = androidx.compose.material.icons.Icons.Default.Person,
                        modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
                    )
                }
                items(artists, key = { it.artistKey }) { artist ->
                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 72.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${artist.name}, ${artist.albumCount} albums, ${artist.songCount} songs"
                            }
                            .clickable { onArtistClick(artist) },
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
                                artworkUri = artist.artworkUri,
                                size = 64.dp,
                                shape = OniSkin.artwork.shape,
                                contentDescription = null,
                                placeholder = {
                                    val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                                    Text(
                                        text = initial,
                                        style = OniSkin.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OniSkin.colors.primary
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artist.name,
                                    style = OniSkin.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                                Text(
                                    text = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"} • ${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                                    style = OniSkin.typography.bodySmall,
                                    color = OniSkin.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OniSkin.colors.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
