package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.data.online.RemoteMusicItem
import com.example.data.online.SearchFilter
import com.example.ui.components.surface.OniSurface
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.DiscoverLocalCard
import com.example.ui.viewmodel.DiscoverLoadState
import com.example.ui.viewmodel.DiscoverRemoteCard
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory),
    playerViewModel: MusicPlayerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val local by viewModel.localSections.collectAsStateWithLifecycle()
    val followedState by viewModel.followedReleases.collectAsStateWithLifecycle()
    val followedArtistIds by viewModel.followedArtistIds.collectAsStateWithLifecycle()

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

        if (local.libraryIsEmpty) item { EmptyText("Your local library is empty. Scan music from Library to build Discover.") }
        item { LocalStateSection("Made for You", local.madeForYou, viewModel::retryLocal) { LocalCards(it, playerViewModel) } }
        item { LocalStateSection("Continue Listening", local.continueListening, viewModel::retryLocal) { LocalCards(it, playerViewModel) } }
        item { LocalStateSection("Most Played", local.mostPlayed, viewModel::retryLocal) { LocalCards(it, playerViewModel) } }
        item { LocalStateSection("Recently Added", local.recentlyAdded, viewModel::retryLocal) { LocalCards(it, playerViewModel) } }
        item { LocalStateSection("Favorites", local.favorites, viewModel::retryLocal) { LocalCards(it, playerViewModel) } }
        item { StateSection("From Your Artists", followedState, viewModel::retryFollowedReleases) { RemoteCards(it, followedArtistIds, viewModel::toggleFollowArtist) } }
        item { StateSection("New Releases", state.newReleases, viewModel::refreshNewReleases) { RemoteCards(it, followedArtistIds, viewModel::toggleFollowArtist) } }

        item {
            when (val genres = local.genres) {
                DiscoverLoadState.Loading -> LoadingSection("Explore by Genre")
                DiscoverLoadState.Empty, DiscoverLoadState.Idle -> Unit
                is DiscoverLoadState.Error -> ErrorSection("Explore by Genre", genres.message, viewModel::retryLocal)
                is DiscoverLoadState.Success -> DiscoverTextSection("Explore by Genre") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        items(genres.items, key = { it }) { genre ->
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
                    IconButton(onClick = viewModel::search) { Icon(Icons.Default.Search, contentDescription = "Search online") }
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

        item { StateSection("Search Results", state.search, viewModel::search) { RemoteCards(it, followedArtistIds, viewModel::toggleFollowArtist) } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Trending Now", style = OniSkin.typography.titleLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::refreshTrending) { Icon(Icons.Default.Refresh, contentDescription = "Refresh trending") }
            }
        }
        item { StateSection(null, state.trending, viewModel::refreshTrending) { RemoteCards(it, followedArtistIds, viewModel::toggleFollowArtist) } }
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
private fun LocalStateSection(
    title: String,
    state: DiscoverLoadState<DiscoverLocalCard>,
    onRetry: () -> Unit,
    content: @Composable (List<DiscoverLocalCard>) -> Unit
) {
    when (state) {
        DiscoverLoadState.Idle, DiscoverLoadState.Empty -> Unit
        DiscoverLoadState.Loading -> LoadingSection(title)
        is DiscoverLoadState.Error -> ErrorSection(title, state.message, onRetry)
        is DiscoverLoadState.Success -> if (state.items.isNotEmpty()) DiscoverTextSection(title) { content(state.items) }
    }
}

@Composable
private fun StateSection(
    title: String?,
    state: DiscoverLoadState<DiscoverRemoteCard>,
    onRetry: () -> Unit,
    content: @Composable (List<DiscoverRemoteCard>) -> Unit
) {
    when (state) {
        DiscoverLoadState.Idle -> Unit
        DiscoverLoadState.Loading -> LoadingSection(title)
        DiscoverLoadState.Empty -> EmptyText("${title ?: "Results"}: nothing to show yet.")
        is DiscoverLoadState.Error -> ErrorSection(title, state.message, onRetry)
        is DiscoverLoadState.Success -> DiscoverTextSection(title ?: "Results") {
            if (state.hasPartialFailures) Text("Some online sources are temporarily unavailable.", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
            if (state.items.isEmpty()) EmptyText("Nothing to show yet.") else content(state.items)
        }
    }
}

@Composable
private fun LocalCards(cards: List<DiscoverLocalCard>, playerViewModel: MusicPlayerViewModel) {
    val songs = cards.map { it.song }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        items(cards, key = { it.song.id }) { card ->
            LocalItem(
                card = card,
                onPlay = { playerViewModel.playSong(card.song, songs) },
                onPlayNext = { playerViewModel.playNext(card.song) },
                onQueue = { playerViewModel.addToQueue(card.song) },
                onToggleFavorite = { playerViewModel.toggleFavorite(card.song.id) }
            )
        }
    }
}

@Composable
private fun LocalItem(
    card: DiscoverLocalCard,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onQueue: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val song = card.song
    OniSurface(
        modifier = Modifier.size(width = 300.dp, height = 112.dp).clip(OniSkin.shapes.card).clickable(onClick = onPlay),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Row(modifier = Modifier.padding(OniSkin.spacing.sm), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = song.albumArtUri, contentDescription = "${song.displayTitle} artwork", modifier = Modifier.size(64.dp).clip(OniSkin.shapes.card), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(song.displayTitle, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.displayArtist, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                card.reason?.let { Text(it, style = OniSkin.typography.caption, color = OniSkin.colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Row {
                    IconButton(onClick = onPlayNext, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.PlayArrow, "Play next") }
                    IconButton(onClick = onQueue, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.AddToQueue, "Add to queue") }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) { Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Toggle favorite") }
                }
            }
        }
    }
}

@Composable
private fun RemoteCards(cards: List<DiscoverRemoteCard>, followedArtistIds: Set<String>, onToggleFollow: (RemoteMusicItem) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        items(cards, key = { it.key }) { card ->
            RemoteItem(card, followedArtistIds.contains(card.item.musicIdentity.canonicalArtistId), onToggleFollow)
        }
    }
}

@Composable
private fun RemoteItem(card: DiscoverRemoteCard, isArtistFollowed: Boolean, onToggleFollow: (RemoteMusicItem) -> Unit) {
    val item = card.item
    OniSurface(modifier = Modifier.size(width = 300.dp, height = 112.dp).clip(OniSkin.shapes.card), containerColor = OniSkin.surfaces.soft.containerColor) {
        Row(modifier = Modifier.padding(OniSkin.spacing.sm), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = item.artworkUrl, contentDescription = "${item.title} artwork", modifier = Modifier.size(76.dp).clip(OniSkin.shapes.card), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.artistName ?: card.providerName, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (card.canStream) "${card.providerName} · Stream available" else "${card.providerName} · ${card.sourceLabel}", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (card.canFollowArtist) {
                IconButton(onClick = { onToggleFollow(item) }, modifier = Modifier.size(40.dp)) {
                    Icon(if (isArtistFollowed) Icons.Default.PersonRemove else Icons.Default.PersonAdd, if (isArtistFollowed) "Unfollow artist" else "Follow artist")
                }
            }
        }
    }
}

@Composable
private fun DiscoverTextSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        Text(title, style = OniSkin.typography.titleLarge, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun LoadingSection(title: String?) {
    DiscoverTextSection(title ?: "Loading") { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
}

@Composable
private fun ErrorSection(title: String?, message: String?, onRetry: () -> Unit) {
    DiscoverTextSection(title ?: "Unavailable") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            Text(message ?: "Unable to load this section.", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyText(message: String) { Text(message, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary) }
