package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.entity.SongEntity
import com.example.data.online.RemoteMusicItem
import com.example.data.online.SearchFilter
import com.example.ui.components.surface.OniSurface
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.DiscoverLoadState
import com.example.ui.viewmodel.DiscoverViewModel

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val madeForYou by viewModel.madeForYou.collectAsStateWithLifecycle()
    val becauseYouPlayed by viewModel.becauseYouPlayed.collectAsStateWithLifecycle()
    val similarMusic by viewModel.similarMusic.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val newReleases = state.newReleases

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OniSkin.spacing.screenHorizontal,
            end = OniSkin.spacing.screenHorizontal,
            top = OniSkin.spacing.md,
            bottom = OniSkin.spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                Text("Discover", style = OniSkin.typography.displayMedium, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "Your music, intelligently organized with new music from online sources.",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary
                )
            }
        }
        if (madeForYou.isNotEmpty()) item { LocalSection("Made for You", madeForYou) }
        if (becauseYouPlayed.isNotEmpty()) item { LocalSection("Because You Played...", becauseYouPlayed) }
        if (similarMusic.isNotEmpty()) item { LocalSection("Similar Music", similarMusic) }
        if (recentlyPlayed.isNotEmpty()) item { LocalSection("Continue Listening", recentlyPlayed) }
        if (mostPlayed.isNotEmpty()) item { LocalSection("Most Played", mostPlayed) }
        if (recentlyAdded.isNotEmpty()) item { LocalSection("Recently Added", recentlyAdded) }
        if (favorites.isNotEmpty()) item { LocalSection("Favorites", favorites) }

        when (newReleases) {
            DiscoverLoadState.Idle -> Unit
            DiscoverLoadState.Loading -> item { LoadingSection() }
            is DiscoverLoadState.Success -> item { DiscoverSection("New Releases", newReleases.items, newReleases.hasPartialFailures) }
            is DiscoverLoadState.Error -> item { ErrorText(newReleases.message) }
        }

        if (genres.isNotEmpty()) {
            item {
                DiscoverTextSection("Explore by Genre") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        items(genres, key = { it }) { genre ->
                            OniSurface(
                                modifier = Modifier.clip(OniSkin.shapes.full),
                                containerColor = OniSkin.surfaces.soft.containerColor
                            ) {
                                Text(
                                    genre,
                                    modifier = Modifier.padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                    style = OniSkin.typography.bodyMedium,
                                    color = OniSkin.colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            DiscoverTextSection("Online Music") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search online music") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Online search") },
                        shape = OniSkin.shapes.full,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OniSkin.colors.textPrimary,
                            unfocusedTextColor = OniSkin.colors.textPrimary,
                            focusedBorderColor = OniSkin.colors.primary,
                            unfocusedBorderColor = OniSkin.colors.outline.copy(alpha = 0.5f),
                            focusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                    IconButton(onClick = viewModel::search) {
                        Icon(Icons.Default.Search, contentDescription = "Search online")
                    }
                }
                Spacer(Modifier.height(OniSkin.spacing.sm))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    items(SearchFilter.entries, key = { it.name }) { filter ->
                        FilterChip(
                            selected = state.searchFilter == filter,
                            onClick = { viewModel.setSearchFilter(filter) },
                            label = { Text(filterLabel(filter)) }
                        )
                    }
                }
            }
        }

        when (val search = state.search) {
            DiscoverLoadState.Idle -> Unit
            DiscoverLoadState.Loading -> item { LoadingSection() }
            is DiscoverLoadState.Success -> item { DiscoverSection("Search Results", search.items, search.hasPartialFailures) }
            is DiscoverLoadState.Error -> item { ErrorText(search.message) }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Trending Now",
                    style = OniSkin.typography.titleLarge,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::refreshTrending) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh trending")
                }
            }
        }
        when (val trending = state.trending) {
            DiscoverLoadState.Idle -> Unit
            DiscoverLoadState.Loading -> item { LoadingSection() }
            is DiscoverLoadState.Success -> item { DiscoverSection(null, trending.items, trending.hasPartialFailures) }
            is DiscoverLoadState.Error -> item { ErrorText(trending.message) }
        }
    }
}

private fun filterLabel(filter: SearchFilter): String = when (filter) {
    SearchFilter.All -> "All"
    SearchFilter.Tracks -> "Tracks"
    SearchFilter.Artists -> "Artists"
    SearchFilter.Albums -> "Albums"
    SearchFilter.Playlists -> "Playlists"
}

@Composable
private fun DiscoverTextSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        Text(title, style = OniSkin.typography.titleLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun LocalSection(title: String, songs: List<SongEntity>) {
    DiscoverTextSection(title) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            items(songs, key = { it.id }) { song ->
                OniSurface(
                    modifier = Modifier.size(width = 270.dp, height = 92.dp).clip(OniSkin.shapes.card),
                    containerColor = OniSkin.surfaces.soft.containerColor
                ) {
                    Row(
                        modifier = Modifier.padding(OniSkin.spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = song.displayTitle + " artwork",
                            modifier = Modifier.size(64.dp).clip(OniSkin.shapes.card),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(song.displayTitle, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.displayArtist, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.displayAlbum, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverSection(title: String?, items: List<RemoteMusicItem>, hasPartialFailures: Boolean) {
    DiscoverTextSection(title ?: "Results") {
        if (items.isEmpty()) {
            Text("No results found.", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
            return@DiscoverTextSection
        }
        if (hasPartialFailures) {
            Text("Some online sources are temporarily unavailable.", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            items(items, key = { it.identity.providerId + ":" + it.identity.type + ":" + it.identity.itemId }) {
                DiscoverItem(it)
            }
        }
    }
}

@Composable
private fun DiscoverItem(item: RemoteMusicItem) {
    OniSurface(
        modifier = Modifier.size(width = 300.dp, height = 104.dp).clip(OniSkin.shapes.card),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Row(
            modifier = Modifier.padding(OniSkin.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.artworkUrl,
                contentDescription = item.title + " artwork",
                modifier = Modifier.size(76.dp).clip(OniSkin.shapes.card),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.artistName ?: item.identity.providerId, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.identity.providerId, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorText(message: String?) {
    Text(message ?: "Unable to load this section.", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
}
