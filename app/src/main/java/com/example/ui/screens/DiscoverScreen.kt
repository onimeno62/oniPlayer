package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.online.RemoteAlbum
import com.example.data.online.RemoteArtist
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import com.example.data.online.SearchFilter
import com.example.ui.components.surface.OniSurface
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.DiscoverDetailState
import com.example.ui.viewmodel.DiscoverLoadState
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = viewModel(),
    musicPlayerViewModel: MusicPlayerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val followedArtistIds by viewModel.followedArtistIds.collectAsStateWithLifecycle()
    val followedArtistReleases by viewModel.followedArtistReleases.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeMenuTrack by remember { mutableStateOf<RemoteMusicItem?>(null) }
    var searchVisible by remember { mutableStateOf(false) }

    val onTrackPlay: (RemoteMusicItem) -> Unit = { item ->
        viewModel.playTrack(item, musicPlayerViewModel) { success, error ->
            if (!success && error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Detail screens (Album / Artist) with BackHandler
    val activeDetail = state.activeDetail
    if (activeDetail != null) {
        BackHandler { viewModel.closeDetail() }
        when (activeDetail) {
            is DiscoverDetailState.RemoteAlbumDetail -> {
                RemoteAlbumDetailView(
                    detail = activeDetail,
                    followedArtistIds = followedArtistIds,
                    onBack = viewModel::closeDetail,
                    onPlayAll = {
                        viewModel.playAllAlbum(activeDetail.album, musicPlayerViewModel) { success, error ->
                            if (!success && error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onTrackPlay = onTrackPlay,
                    onOpenArtist = { artistName ->
                        viewModel.openArtistDetail(
                            RemoteMusicItem(
                                identity = activeDetail.album.identity.copy(type = RemoteMusicType.Artist),
                                title = artistName,
                                artistName = artistName,
                                artworkUrl = activeDetail.album.artworkUrl,
                                musicIdentity = activeDetail.album.musicIdentity
                            )
                        )
                    },
                    onToggleFollow = { artistId, artistName, artUrl ->
                        viewModel.toggleFollowArtist(artistId, artistName, artUrl)
                    },
                    onShowMenu = { activeMenuTrack = it }
                )
            }
            is DiscoverDetailState.RemoteArtistDetail -> {
                RemoteArtistDetailView(
                    detail = activeDetail,
                    isFollowed = followedArtistIds.contains(activeDetail.artist.musicIdentity.canonicalArtistId ?: activeDetail.artist.identity.itemId),
                    onBack = viewModel::closeDetail,
                    onToggleFollow = {
                        viewModel.toggleFollowArtist(
                            artistId = activeDetail.artist.musicIdentity.canonicalArtistId ?: activeDetail.artist.identity.itemId,
                            artistName = activeDetail.artist.name,
                            artworkUrl = activeDetail.artist.artworkUrl
                        )
                    },
                    onOpenAlbum = { album ->
                        viewModel.openAlbumDetail(
                            RemoteMusicItem(
                                identity = album.identity,
                                title = album.title,
                                artistName = album.artistName ?: activeDetail.artist.name,
                                artworkUrl = album.artworkUrl ?: activeDetail.artist.artworkUrl,
                                musicIdentity = album.musicIdentity,
                                metadata = mapOf("release_date" to (album.releaseDate ?: ""))
                            )
                        )
                    },
                    onTrackPlay = onTrackPlay,
                    onShowMenu = { activeMenuTrack = it }
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OniSkin.spacing.screenHorizontal,
                end = OniSkin.spacing.screenHorizontal,
                top = OniSkin.spacing.md,
                bottom = OniSkin.spacing.xl + 80.dp // Leave room for player bar
            ),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
        ) {
            // ---- Discover Header -----------------------------------------------------------------
            item {
                DiscoverHeader(
                    isRefreshing = state.isRefreshing,
                    isSearchOpen = searchVisible || state.query.isNotBlank(),
                    onToggleSearch = { searchVisible = !searchVisible },
                    onRefresh = viewModel::refreshAll
                )
            }

            // ---- Search Bar (expandable) ---------------------------------------------------------
            if (searchVisible || state.query.isNotBlank()) {
                item {
                    DiscoverSearchSection(
                        query = state.query,
                        selectedFilter = state.searchFilter,
                        onQueryChange = viewModel::setQuery,
                        onFilterChange = viewModel::setSearchFilter,
                        onSearch = viewModel::search,
                        onClear = {
                            viewModel.clearSearch()
                            searchVisible = false
                        }
                    )
                }

                when (val searchState = state.search) {
                    DiscoverLoadState.Idle -> Unit
                    DiscoverLoadState.Loading -> item { LoadingSection("Searching online music...") }
                    is DiscoverLoadState.Success -> item {
                        DiscoverTrackSection(
                            title = "Search Results",
                            subtitle = "Online results from connected providers",
                            items = searchState.items,
                            hasPartialFailures = searchState.hasPartialFailures,
                            canStream = viewModel::canStream,
                            onTrackClick = onTrackPlay,
                            onOpenArtist = { item -> viewModel.openArtistDetail(item) },
                            onShowMenu = { activeMenuTrack = it }
                        )
                    }
                    is DiscoverLoadState.Error -> item { ErrorCard(searchState.message ?: "Search failed") { viewModel.search() } }
                }
            }

            // ---- Section A: New From Your Artists ------------------------------------------------
            item {
                SectionHeader(
                    title = "New From Your Artists",
                    subtitle = "Releases from artists you follow",
                    badge = if (followedArtistReleases.isNotEmpty()) "${followedArtistReleases.size}" else null
                )
            }
            if (followedArtistIds.isEmpty()) {
                item {
                    EmptyFollowedArtistsCard(
                        onDiscoverClick = {
                            coroutineScope.launch {
                                // Scroll to Discover Artists section
                                listState.animateScrollToItem(index = 8)
                            }
                        }
                    )
                }
            } else if (followedArtistReleases.isEmpty()) {
                item {
                    Text(
                        "No new releases yet from your followed artists. They will appear here when discovered.",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(followedArtistReleases, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                            ReleaseCard(
                                item = item,
                                onClick = { viewModel.openAlbumDetail(item) },
                                onArtistClick = { viewModel.openArtistDetail(item) },
                                onPlayClick = if (viewModel.canStream(item)) { { onTrackPlay(item) } } else null,
                                onMenuClick = { activeMenuTrack = item }
                            )
                        }
                    }
                }
            }

            // ---- Section B: Because You Listen To... --------------------------------------------
            val becauseArtist = state.becauseYouListenToArtist
            val becauseTitle = if (!becauseArtist.isNullOrBlank()) "Because You Listen To $becauseArtist" else "Inspired by Your Taste"
            item {
                SectionHeader(
                    title = becauseTitle,
                    subtitle = "Fresh online tracks related to your listening habits"
                )
            }
            when (val becauseState = state.becauseYouListenTo) {
                DiscoverLoadState.Idle -> Unit
                DiscoverLoadState.Loading -> item { LoadingSection("Finding personalized tracks...") }
                is DiscoverLoadState.Success -> {
                    if (becauseState.items.isEmpty()) {
                        item {
                            Text(
                                "Listen to more local music to unlock personalized online suggestions.",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textSecondary
                            )
                        }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                                items(becauseState.items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                                    OnlineTrackCard(
                                        item = item,
                                        canStream = viewModel.canStream(item),
                                        onPlay = { onTrackPlay(item) },
                                        onArtistClick = { viewModel.openArtistDetail(item) },
                                        onMenuClick = { activeMenuTrack = item }
                                    )
                                }
                            }
                        }
                    }
                }
                is DiscoverLoadState.Error -> item { ErrorCard(becauseState.message) { viewModel.refreshBecauseYouListenTo() } }
            }

            // ---- Section C: Similar Music --------------------------------------------------------
            item {
                SectionHeader(
                    title = "Similar Music",
                    subtitle = "Ranked online candidates by genre and artist affinity"
                )
            }
            when (val similarState = state.similarMusic) {
                DiscoverLoadState.Idle -> Unit
                DiscoverLoadState.Loading -> item { LoadingSection("Analyzing music signals...") }
                is DiscoverLoadState.Success -> {
                    if (similarState.items.isEmpty()) {
                        item {
                            Text(
                                "No similar online tracks found yet.",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textSecondary
                            )
                        }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                                items(similarState.items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                                    OnlineTrackCard(
                                        item = item,
                                        canStream = viewModel.canStream(item),
                                        badgeText = item.metadata["recommendation_reason"],
                                        onPlay = { onTrackPlay(item) },
                                        onArtistClick = { viewModel.openArtistDetail(item) },
                                        onMenuClick = { activeMenuTrack = item }
                                    )
                                }
                            }
                        }
                    }
                }
                is DiscoverLoadState.Error -> item { ErrorCard(similarState.message) { viewModel.refreshSimilarMusic() } }
            }

            // ---- Section D: Trending ------------------------------------------------------------
            item {
                SectionHeader(
                    title = "Trending Now",
                    subtitle = "Popular tracks across streaming networks",
                    onRefresh = viewModel::refreshTrending
                )
            }
            when (val trendingState = state.trending) {
                DiscoverLoadState.Idle -> Unit
                DiscoverLoadState.Loading -> item { LoadingSection("Fetching trending online tracks...") }
                is DiscoverLoadState.Success -> {
                    if (trendingState.items.isEmpty()) {
                        item { Text("No trending tracks available.", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary) }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                                items(trendingState.items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                                    OnlineTrackCard(
                                        item = item,
                                        canStream = viewModel.canStream(item),
                                        onPlay = { onTrackPlay(item) },
                                        onArtistClick = { viewModel.openArtistDetail(item) },
                                        onMenuClick = { activeMenuTrack = item }
                                    )
                                }
                            }
                        }
                    }
                }
                is DiscoverLoadState.Error -> item { ErrorCard(trendingState.message) { viewModel.refreshTrending() } }
            }

            // ---- Section E: New Releases ---------------------------------------------------------
            item {
                SectionHeader(
                    title = "New Releases",
                    subtitle = "Latest albums, EPs, and canonical releases",
                    onRefresh = viewModel::refreshNewReleases
                )
            }
            when (val releaseState = state.newReleases) {
                DiscoverLoadState.Idle -> Unit
                DiscoverLoadState.Loading -> item { LoadingSection("Loading latest releases...") }
                is DiscoverLoadState.Success -> {
                    if (releaseState.items.isEmpty()) {
                        item { Text("No new releases available.", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary) }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                                items(releaseState.items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                                    ReleaseCard(
                                        item = item,
                                        onClick = { viewModel.openAlbumDetail(item) },
                                        onArtistClick = { viewModel.openArtistDetail(item) },
                                        onMenuClick = { activeMenuTrack = item }
                                    )
                                }
                            }
                        }
                    }
                }
                is DiscoverLoadState.Error -> item { ErrorCard(releaseState.message) { viewModel.refreshNewReleases() } }
            }

            // ---- Section F: Discover Artists -----------------------------------------------------
            item {
                SectionHeader(
                    title = "Artists You May Like",
                    subtitle = "Explore artist catalogs and follow for new releases",
                    onRefresh = viewModel::refreshDiscoverArtists
                )
            }
            when (val artistsState = state.discoverArtists) {
                DiscoverLoadState.Idle -> Unit
                DiscoverLoadState.Loading -> item { LoadingSection("Finding artists...") }
                is DiscoverLoadState.Success -> {
                    if (artistsState.items.isEmpty()) {
                        item { Text("No artists to explore right now.", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary) }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)) {
                                items(artistsState.items, key = { it.artistName ?: it.title }) { item ->
                                    val artistId = item.musicIdentity.canonicalArtistId ?: item.identity.itemId
                                    val isFollowed = followedArtistIds.contains(artistId)
                                    ArtistDiscoveryCard(
                                        item = item,
                                        isFollowed = isFollowed,
                                        onClick = { viewModel.openArtistDetail(item) },
                                        onToggleFollow = { viewModel.toggleFollowArtist(item) }
                                    )
                                }
                            }
                        }
                    }
                }
                is DiscoverLoadState.Error -> item { ErrorCard(artistsState.message) { viewModel.refreshDiscoverArtists() } }
            }

            // ---- Section G: Genre / Music Exploration --------------------------------------------
            item {
                SectionHeader(
                    title = "Explore by Genre",
                    subtitle = "Tap a genre to discover online music"
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    items(viewModel.availableGenres, key = { it }) { genre ->
                        val isSelected = state.selectedGenre.equals(genre, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectGenre(genre) },
                            label = { Text(genre, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OniSkin.colors.primary.copy(alpha = 0.2f),
                                selectedLabelColor = OniSkin.colors.primary
                            )
                        )
                    }
                }
            }
            when (val genreState = state.genreExplore) {
                DiscoverLoadState.Idle -> Unit
                DiscoverLoadState.Loading -> item { LoadingSection("Loading ${state.selectedGenre} tracks...") }
                is DiscoverLoadState.Success -> {
                    if (genreState.items.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                                items(genreState.items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                                    OnlineTrackCard(
                                        item = item,
                                        canStream = viewModel.canStream(item),
                                        onPlay = { onTrackPlay(item) },
                                        onArtistClick = { viewModel.openArtistDetail(item) },
                                        onMenuClick = { activeMenuTrack = item }
                                    )
                                }
                            }
                        }
                    }
                }
                is DiscoverLoadState.Error -> item { ErrorCard(genreState.message) { state.selectedGenre?.let { viewModel.selectGenre(it) } } }
            }
        }
    }

    // Modal track menu
    activeMenuTrack?.let { track ->
        val songId = "remote_${track.identity.providerId}_${track.identity.itemId}"
        val isFavorite = favoriteSongIds.contains(songId)
        val isArtistFollowed = followedArtistIds.contains(track.musicIdentity.canonicalArtistId ?: track.identity.itemId)
        val canStream = viewModel.canStream(track)
        val canDownload = viewModel.canDownload(track)

        TrackActionsModal(
            track = track,
            isFavorite = isFavorite,
            isArtistFollowed = isArtistFollowed,
            canStream = canStream,
            canDownload = canDownload,
            onDismiss = { activeMenuTrack = null },
            onPlay = {
                activeMenuTrack = null
                onTrackPlay(track)
            },
            onPlayNext = {
                activeMenuTrack = null
                viewModel.playNextTrack(track, musicPlayerViewModel) { success, error ->
                    if (success) Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
                    else if (error != null) Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            },
            onAddToQueue = {
                activeMenuTrack = null
                viewModel.addToQueueTrack(track, musicPlayerViewModel) { success, error ->
                    if (success) Toast.makeText(context, "Added to queue: ${track.title}", Toast.LENGTH_SHORT).show()
                    else if (error != null) Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            },
            onToggleFavorite = {
                viewModel.toggleFavoriteTrack(track)
                activeMenuTrack = null
                Toast.makeText(context, if (isFavorite) "Removed from favorites" else "Added to favorites", Toast.LENGTH_SHORT).show()
            },
            onToggleFollowArtist = {
                viewModel.toggleFollowArtist(track)
                activeMenuTrack = null
                Toast.makeText(context, if (isArtistFollowed) "Unfollowed artist" else "Followed artist", Toast.LENGTH_SHORT).show()
            },
            onOpenArtist = {
                activeMenuTrack = null
                viewModel.openArtistDetail(track)
            }
        )
    }
}

// =================================================================================================
// Header & Search Components
// =================================================================================================

@Composable
private fun DiscoverHeader(
    isRefreshing: Boolean,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Discover",
                    style = OniSkin.typography.displayMedium,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                // Subtle online status badge
                Box(
                    modifier = Modifier
                        .clip(OniSkin.shapes.full)
                        .background(OniSkin.colors.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "ONLINE",
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                "Find something new to listen to.",
                style = OniSkin.typography.bodyMedium,
                color = OniSkin.colors.textSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchOpen) "Close search" else "Search online",
                    tint = OniSkin.colors.textPrimary
                )
            }
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh all sections",
                        tint = OniSkin.colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverSearchSection(
    query: String,
    selectedFilter: SearchFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search songs, artists, albums online...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            shape = OniSkin.shapes.full,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OniSkin.colors.textPrimary,
                unfocusedTextColor = OniSkin.colors.textPrimary,
                focusedBorderColor = OniSkin.colors.primary,
                unfocusedBorderColor = OniSkin.colors.outline.copy(alpha = 0.4f),
                focusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.2f)
            )
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            items(SearchFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.name) }
                )
            }
        }
    }
}

// =================================================================================================
// Section Headers & Base Components
// =================================================================================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onRefresh: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = OniSkin.typography.titleLarge,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(OniSkin.shapes.full)
                            .background(OniSkin.colors.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                    }
                }
            }
            if (subtitle != null) {
                Text(subtitle, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
            }
        }
        if (onRefresh != null) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh $title", tint = OniSkin.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LoadingSection(label: String = "Loading...") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OniSkin.spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        Spacer(Modifier.width(OniSkin.spacing.sm))
        Text(label, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
    }
}

@Composable
private fun ErrorCard(message: String?, onRetry: () -> Unit) {
    OniSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OniSkin.shapes.card),
        containerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(OniSkin.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                message ?: "Unavailable right now.",
                style = OniSkin.typography.bodyMedium,
                color = OniSkin.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyFollowedArtistsCard(onDiscoverClick: () -> Unit) {
    OniSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OniSkin.shapes.card),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Column(
            modifier = Modifier.padding(OniSkin.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = OniSkin.colors.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                "Follow artists to see their new music here",
                style = OniSkin.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Whenever an artist you follow drops a new release, it will be automatically synchronized.",
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(
                onClick = onDiscoverClick,
                shape = OniSkin.shapes.full
            ) {
                Text("Discover artists")
            }
        }
    }
}

// =================================================================================================
// Cards & Item Views
// =================================================================================================

@Composable
private fun OnlineTrackCard(
    item: RemoteMusicItem,
    canStream: Boolean,
    badgeText: String? = null,
    onPlay: () -> Unit,
    onArtistClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(260.dp)
            .clip(OniSkin.shapes.card)
            .clickable(enabled = canStream, onClick = onPlay),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Row(
            modifier = Modifier.padding(OniSkin.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(68.dp)) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = "${item.title} artwork",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(OniSkin.shapes.card),
                    contentScale = ContentScale.Crop
                )
                if (canStream) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(OniSkin.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = OniSkin.colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (badgeText != null) {
                    Text(
                        badgeText.uppercase(),
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    item.title,
                    style = OniSkin.typography.bodyLarge,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.artistName ?: "Unknown",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onArtistClick)
                )
                Text(
                    item.identity.providerId.uppercase(),
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = OniSkin.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    item: RemoteMusicItem,
    onClick: () -> Unit,
    onArtistClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(160.dp)
            .clip(OniSkin.shapes.card)
            .clickable(onClick = onClick),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Column(modifier = Modifier.padding(OniSkin.spacing.sm)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = "${item.title} artwork",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(OniSkin.shapes.card),
                    contentScale = ContentScale.Crop
                )
                if (onPlayClick != null) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(OniSkin.colors.primary)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play release",
                            tint = OniSkin.colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(OniSkin.spacing.xs))
            Text(
                item.title,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.artistName ?: "Unknown",
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onArtistClick)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val releaseDate = item.metadata["release_date"]?.take(4) ?: ""
                Text(
                    releaseDate.ifBlank { item.identity.providerId },
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary.copy(alpha = 0.7f)
                )
                IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = OniSkin.colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtistDiscoveryCard(
    item: RemoteMusicItem,
    isFollowed: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(130.dp)
            .clip(OniSkin.shapes.card)
            .clickable(onClick = onClick),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Column(
            modifier = Modifier.padding(OniSkin.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            AsyncImage(
                model = item.artworkUrl,
                contentDescription = "${item.title} photo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                item.artistName ?: item.title,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FilledTonalButton(
                onClick = onToggleFollow,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = OniSkin.shapes.full,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isFollowed) OniSkin.colors.surfaceVariant else OniSkin.colors.primary.copy(alpha = 0.15f),
                    contentColor = if (isFollowed) OniSkin.colors.textSecondary else OniSkin.colors.primary
                )
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isFollowed) "Following" else "Follow",
                    style = OniSkin.typography.caption,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DiscoverTrackSection(
    title: String,
    subtitle: String,
    items: List<RemoteMusicItem>,
    hasPartialFailures: Boolean,
    canStream: (RemoteMusicItem) -> Boolean,
    onTrackClick: (RemoteMusicItem) -> Unit,
    onOpenArtist: (RemoteMusicItem) -> Unit,
    onShowMenu: (RemoteMusicItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
        SectionHeader(title = title, subtitle = subtitle)
        if (hasPartialFailures) {
            Text(
                "Some online providers are temporarily unreachable.",
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            items(items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                OnlineTrackCard(
                    item = item,
                    canStream = canStream(item),
                    onPlay = { onTrackClick(item) },
                    onArtistClick = { onOpenArtist(item) },
                    onMenuClick = { onShowMenu(item) }
                )
            }
        }
    }
}

// =================================================================================================
// Remote Detail Views (Album & Artist)
// =================================================================================================

@Composable
private fun RemoteAlbumDetailView(
    detail: DiscoverDetailState.RemoteAlbumDetail,
    followedArtistIds: Set<String>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onTrackPlay: (RemoteMusicItem) -> Unit,
    onOpenArtist: (String) -> Unit,
    onToggleFollow: (String, String, String?) -> Unit,
    onShowMenu: (RemoteMusicItem) -> Unit
) {
    val album = detail.album
    val artistName = album.artistName ?: "Unknown Artist"
    val artistId = album.musicIdentity.canonicalArtistId ?: album.identity.itemId
    val isArtistFollowed = followedArtistIds.contains(artistId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Release Details", style = OniSkin.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = album.artworkUrl,
                        contentDescription = "${album.title} artwork",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(OniSkin.shapes.card),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            album.title,
                            style = OniSkin.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary
                        )
                        Text(
                            artistName,
                            style = OniSkin.typography.bodyLarge,
                            color = OniSkin.colors.primary,
                            modifier = Modifier.clickable { onOpenArtist(artistName) }
                        )
                        album.releaseDate?.let { date ->
                            Text("Released $date", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        }
                        Text("Source: ${album.identity.providerId.uppercase()}", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                ) {
                    Button(
                        onClick = onPlayAll,
                        modifier = Modifier.weight(1f),
                        shape = OniSkin.shapes.full
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play All")
                    }
                    FilledTonalButton(
                        onClick = { onToggleFollow(artistId, artistName, album.artworkUrl) },
                        shape = OniSkin.shapes.full
                    ) {
                        Icon(
                            if (isArtistFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isArtistFollowed) "Following" else "Follow Artist")
                    }
                }
            }

            if (detail.isLoading) {
                item { LoadingSection("Loading album tracklist...") }
            } else if (album.tracks.isEmpty()) {
                item {
                    Text(
                        "No individual streaming tracks cataloged for this release.",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary
                    )
                }
            } else {
                item {
                    Text("Tracks (${album.tracks.size})", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                itemsIndexed(album.tracks, key = { _, track -> track.identity.itemId }) { index, track ->
                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OniSkin.shapes.card)
                            .clickable { onTrackPlay(track) },
                        containerColor = OniSkin.surfaces.soft.containerColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
                        ) {
                            Text(
                                "${index + 1}",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textSecondary,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = OniSkin.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OniSkin.colors.textPrimary)
                                Text(track.artistName ?: artistName, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                            }
                            IconButton(onClick = { onShowMenu(track) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteArtistDetailView(
    detail: DiscoverDetailState.RemoteArtistDetail,
    isFollowed: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenAlbum: (RemoteAlbum) -> Unit,
    onTrackPlay: (RemoteMusicItem) -> Unit,
    onShowMenu: (RemoteMusicItem) -> Unit
) {
    val artist = detail.artist

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Artist Details", style = OniSkin.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = artist.artworkUrl,
                        contentDescription = "${artist.name} photo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            artist.name,
                            style = OniSkin.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary
                        )
                        Text("Source: ${artist.identity.providerId.uppercase()}", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        FilledTonalButton(
                            onClick = onToggleFollow,
                            shape = OniSkin.shapes.full
                        ) {
                            Icon(
                                if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFollowed) "Following" else "Follow Artist")
                        }
                    }
                }
            }

            if (detail.isLoading) {
                item { LoadingSection("Loading artist catalog...") }
            }

            if (detail.releases.isNotEmpty()) {
                item {
                    Text("Releases (${detail.releases.size})", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        items(detail.releases, key = { it.identity.itemId }) { album ->
                            ReleaseCard(
                                item = RemoteMusicItem(
                                    identity = album.identity,
                                    title = album.title,
                                    artistName = album.artistName ?: artist.name,
                                    artworkUrl = album.artworkUrl ?: artist.artworkUrl,
                                    musicIdentity = album.musicIdentity,
                                    metadata = mapOf("release_date" to (album.releaseDate ?: ""))
                                ),
                                onClick = { onOpenAlbum(album) },
                                onArtistClick = {},
                                onMenuClick = {
                                    onShowMenu(
                                        RemoteMusicItem(
                                            identity = album.identity,
                                            title = album.title,
                                            artistName = album.artistName ?: artist.name,
                                            artworkUrl = album.artworkUrl ?: artist.artworkUrl
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (detail.tracks.isNotEmpty()) {
                item {
                    Text("Popular Tracks", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(detail.tracks, key = { it.identity.itemId }) { track ->
                    OnlineTrackCard(
                        item = track,
                        canStream = true,
                        onPlay = { onTrackPlay(track) },
                        onArtistClick = {},
                        onMenuClick = { onShowMenu(track) }
                    )
                }
            }
        }
    }
}

// =================================================================================================
// Track Actions Modal
// =================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackActionsModal(
    track: RemoteMusicItem,
    isFavorite: Boolean,
    isArtistFollowed: Boolean,
    canStream: Boolean,
    canDownload: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFollowArtist: () -> Unit,
    onOpenArtist: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(OniSkin.shapes.card),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = OniSkin.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artistName ?: "Unknown", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = OniSkin.spacing.xs))

            if (canStream) {
                ActionMenuItem(icon = Icons.Default.PlayArrow, title = "Play now", onClick = onPlay)
                ActionMenuItem(icon = Icons.Default.SkipNext, title = "Play next", onClick = onPlayNext)
                ActionMenuItem(icon = Icons.AutoMirrored.Filled.QueueMusic, title = "Add to queue", onClick = onAddToQueue)
            }

            ActionMenuItem(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = onToggleFavorite
            )

            ActionMenuItem(
                icon = if (isArtistFollowed) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                title = if (isArtistFollowed) "Unfollow ${track.artistName ?: "artist"}" else "Follow ${track.artistName ?: "artist"}",
                onClick = onToggleFollowArtist
            )

            ActionMenuItem(
                icon = Icons.Default.Person,
                title = "View artist details",
                onClick = onOpenArtist
            )

            if (canDownload) {
                ActionMenuItem(
                    icon = Icons.Default.Download,
                    title = "Download track",
                    onClick = { /* handled if download URL present */ }
                )
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = OniSkin.spacing.lg, vertical = OniSkin.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = OniSkin.colors.textPrimary)
        Text(title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary)
    }
}
