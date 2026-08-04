package com.example.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SongEntity
import com.example.ui.library.components.*
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.screens.*
import com.example.ui.theme.LocalAccentColor
import com.example.ui.library.hero.ContinueListeningHeroV2

@Composable
fun LibraryDashboardScreen(
    songs: List<SongEntity>,
    sortedSongs: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    lastPlayedSong: SongEntity?,
    recentlyPlayedSongs: List<SongEntity>,
    mostPlayedSongs: List<SongEntity>,
    recentlyAddedSongs: List<SongEntity>,
    uniqueArtistsCount: Int,
    uniqueAlbumsCount: Int,
    favoritesCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isScanning: Boolean,
    showOptionsMenu: () -> Unit,
    onRescan: () -> Unit,
    layoutMode: String,
    onToggleLayoutMode: () -> Unit,
    categoryList: List<CategoryInfo>,
    onSelectCategory: (Int) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    albumUiModels: List<AlbumUiModel>,
    artistUiModels: List<ArtistUiModel>,
    viewModel: com.example.ui.viewmodel.MusicPlayerViewModel
) {
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    // Indices into categoryList: All Songs (0), Favorites (5), Playlists (8), Folders (1)
    val quickAccessCategoryIndices = remember { listOf(0, 5, 8, 1) }

    // Frequently Played Albums: derived from albumUiModels sorted by aggregate play count of songs within each album
    val frequentlyPlayedAlbums = remember(songs, albumUiModels) {
        val playCountsByKey = songs.groupBy { song ->
            val album = song.displayAlbum.ifBlank { "Unknown Album" }
            "$album|${song.displayAlbumArtist}"
        }.mapValues { (_, songsInGroup) ->
            songsInGroup.sumOf { it.playCount }
        }
        albumUiModels
            .filter { (playCountsByKey[it.albumKey] ?: 0) > 0 }
            .sortedByDescending { playCountsByKey[it.albumKey] ?: 0 }
    }

    // Favorite Artists: derived from artistUiModels filtered/sorted by song count among favorited songs
    val favoriteArtists = remember(songs, artistUiModels) {
        val favoriteCountsByArtist = songs
            .filter { it.isFavorite || it.rating >= 4 }
            .groupBy { it.displayArtist.ifBlank { "Unknown Artist" } }
            .mapValues { (_, songsInGroup) -> songsInGroup.size }
        artistUiModels
            .filter { (favoriteCountsByArtist[it.artistKey] ?: 0) > 0 }
            .sortedByDescending { favoriteCountsByArtist[it.artistKey] ?: 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Top bar / greeting — restyled with LibrarySpacing tokens
        item(key = "dashboard_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = greetingForTime(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(LibrarySpacing.sm)) {
                    IconButton(
                        onClick = showOptionsMenu,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Library Options",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onRescan,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Library",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 2. Global Library Search input bar
        item(key = "dashboard_search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search title, artist, album...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.xs)
                    .testTag("search_input"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
            Spacer(modifier = Modifier.height(LibrarySpacing.sm))
        }

        if (searchQuery.isNotBlank()) {
            // Search matches using SongRow from Stage 2
            item(key = "search_header") {
                Text(
                    text = "Found ${sortedSongs.size} tracks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = LibrarySpacing.lg,
                        end = LibrarySpacing.lg,
                        top = LibrarySpacing.sm,
                        bottom = LibrarySpacing.sm
                    )
                )
            }

            if (sortedSongs.isEmpty()) {
                item(key = "search_no_results") {
                    LibraryEmptyState(
                        title = "No matches found",
                        message = "No tracks found matching \"$searchQuery\"",
                        icon = Icons.Default.Search
                    )
                }
            } else {
                items(sortedSongs, key = { "search_${it.id}" }) { song ->
                    Box(modifier = Modifier.padding(horizontal = LibrarySpacing.md)) {
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSong?.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySong(song, sortedSongs) },
                            onShowMenu = { onShowTrackMenu(song) }
                        )
                    }
                }
            }
        } else if (songs.isEmpty()) {
            // 4. Empty library state
            item(key = "empty_library_state") {
                LibraryEmptyState(
                    title = "Your Library is Empty",
                    message = "No audio files were found. Tap below or use the refresh icon above to scan your local storage.",
                    icon = Icons.Default.LibraryMusic,
                    actionLabel = "Scan Local Storage",
                    onActionClick = onRescan
                )
            }
        } else {
            // 3. When searchQuery is blank and library not empty:
            // Continue Listening hero v2 - Foundation Stage A
            if (lastPlayedSong != null) {
                item(key = "continue_listening_hero") {
                    ContinueListeningHeroV2(
                        song = lastPlayedSong,
                        isPlaying = isPlaying && (currentSong?.id == lastPlayedSong.id),
                        viewModel = viewModel,
                        onPlayPauseClick = {
                            if (currentSong?.id == lastPlayedSong.id) {
                                viewModel.togglePlayPause()
                            } else {
                                onPlaySong(lastPlayedSong, songs)
                            }
                        },
                        onOpenNowPlaying = {
                            viewModel.selectTab(1) // jump to Full Player
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LibrarySpacing.lg)
                    )
                    Spacer(modifier = Modifier.height(LibrarySpacing.lg))
                }
            }

            // Quick stats strip
            item(key = "quick_stats_strip") {
                LibraryStatsStrip(
                    songCount = songs.size,
                    artistCount = uniqueArtistsCount,
                    albumCount = uniqueAlbumsCount,
                    favoriteCount = favoritesCount
                )
                Spacer(modifier = Modifier.height(LibrarySpacing.lg))
            }

            // Made For You section
            item(key = "made_for_you_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OniSectionHeader(title = "Made For You")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = LibrarySpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(LibrarySpacing.md)
                    ) {
                        // Most Played -> index 6 in categoryList
                        item(key = "mfy_most_played") {
                            MadeForYouCard(
                                title = "Most Played",
                                countText = "${mostPlayedSongs.size} tracks",
                                icon = Icons.Default.Whatshot,
                                onClick = { onSelectCategory(6) }
                            )
                        }
                        // Recently Played -> index 9 (Smart Playlists where Recently Played dynamic playlist lives)
                        item(key = "mfy_recently_played") {
                            MadeForYouCard(
                                title = "Recently Played",
                                countText = "${recentlyPlayedSongs.size} tracks",
                                icon = Icons.Default.Schedule,
                                onClick = { onSelectCategory(9) }
                            )
                        }
                        // Recently Added -> index 7 in categoryList
                        item(key = "mfy_recently_added") {
                            MadeForYouCard(
                                title = "Recently Added",
                                countText = "${recentlyAddedSongs.size} tracks",
                                icon = Icons.Default.QueueMusic,
                                onClick = { onSelectCategory(7) }
                            )
                        }
                        // Favorites -> index 5 in categoryList
                        item(key = "mfy_favorites") {
                            MadeForYouCard(
                                title = "Favorites",
                                countText = "$favoritesCount tracks",
                                icon = Icons.Filled.Favorite,
                                onClick = { onSelectCategory(5) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(LibrarySpacing.lg))
            }

            // Recently Played section — only rendered if recentlyPlayedSongs is not empty
            if (recentlyPlayedSongs.isNotEmpty()) {
                item(key = "recently_played_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OniSectionHeader(
                            title = "Recently Played",
                            onViewAllClick = { onSelectCategory(9) }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = LibrarySpacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(LibrarySpacing.md)
                        ) {
                            items(recentlyPlayedSongs.take(15), key = { "rp_${it.id}" }) { song ->
                                HorizontalSongCard(
                                    song = song,
                                    onClick = { onPlaySong(song, recentlyPlayedSongs) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(LibrarySpacing.lg))
                }
            }

            // Frequently Played Albums — only rendered if at least one album has playCount > 0
            if (frequentlyPlayedAlbums.isNotEmpty()) {
                item(key = "frequently_played_albums_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OniSectionHeader(
                            title = "Frequently Played Albums",
                            onViewAllClick = { onSelectCategory(2) }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = LibrarySpacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(LibrarySpacing.md)
                        ) {
                            items(frequentlyPlayedAlbums.take(15), key = { "album_${it.albumKey}" }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onSearchQueryChange(album.title) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(LibrarySpacing.lg))
                }
            }

            // Favorite Artists — only rendered if at least one artist has favorited songs
            if (favoriteArtists.isNotEmpty()) {
                item(key = "favorite_artists_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OniSectionHeader(
                            title = "Favorite Artists",
                            onViewAllClick = { onSelectCategory(3) }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = LibrarySpacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(LibrarySpacing.md)
                        ) {
                            items(favoriteArtists.take(15), key = { "artist_${it.artistKey}" }) { artist ->
                                Box(modifier = Modifier.width(240.dp)) {
                                    ArtistRow(
                                        artist = artist,
                                        onClick = { onSearchQueryChange(artist.name) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(LibrarySpacing.lg))
                }
            }

            // Category preview — Quick Access / All Categories toggle and CategoryCard rendering
            item(key = "category_preview_section") {
                AnimatedContent(
                    targetState = showAllCategories,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(260))
                    },
                    label = "Categories section transition"
                ) { isShowingAll ->
                    if (!isShowingAll) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = LibrarySpacing.lg,
                                        end = LibrarySpacing.lg,
                                        top = LibrarySpacing.md,
                                        bottom = LibrarySpacing.sm
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Access",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            quickAccessCategoryIndices.chunked(2).forEach { rowIndices ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.xs),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val idx1 = rowIndices[0]
                                    val cat1 = categoryList.getOrNull(idx1)
                                    if (cat1 != null) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            CategoryCard(
                                                category = cat1,
                                                isGrid = true,
                                                onClick = { onSelectCategory(idx1) }
                                            )
                                        }
                                    }
                                    if (rowIndices.size > 1) {
                                        val idx2 = rowIndices[1]
                                        val cat2 = categoryList.getOrNull(idx2)
                                        if (cat2 != null) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                CategoryCard(
                                                    category = cat2,
                                                    isGrid = true,
                                                    onClick = { onSelectCategory(idx2) }
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(LibrarySpacing.sm))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.xs)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .clickable { showAllCategories = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "See all categories",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = LibrarySpacing.lg,
                                        end = LibrarySpacing.lg,
                                        top = LibrarySpacing.md,
                                        bottom = LibrarySpacing.sm
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Categories",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { showAllCategories = false },
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Show less")
                                    }
                                    IconButton(onClick = onToggleLayoutMode) {
                                        Icon(
                                            imageVector = if (layoutMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                                            contentDescription = "Toggle Layout",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (layoutMode == "grid") {
                                categoryList.chunked(2).forEach { rowPair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.xs),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val cat1 = rowPair[0]
                                        val index1 = categoryList.indexOf(cat1)
                                        Box(modifier = Modifier.weight(1f)) {
                                            CategoryCard(
                                                category = cat1,
                                                isGrid = true,
                                                onClick = { onSelectCategory(index1) }
                                            )
                                        }
                                        if (rowPair.size > 1) {
                                            val cat2 = rowPair[1]
                                            val index2 = categoryList.indexOf(cat2)
                                            Box(modifier = Modifier.weight(1f)) {
                                                CategoryCard(
                                                    category = cat2,
                                                    isGrid = true,
                                                    onClick = { onSelectCategory(index2) }
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                categoryList.forEachIndexed { index, category ->
                                    Box(modifier = Modifier.padding(horizontal = LibrarySpacing.lg, vertical = 4.dp)) {
                                        CategoryCard(
                                            category = category,
                                            isGrid = false,
                                            onClick = { onSelectCategory(index) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MadeForYouCard(
    title: String,
    countText: String,
    icon: ImageVector,
    iconBgColor: Color = LocalAccentColor.current.copy(alpha = 0.12f),
    iconColor: Color = LocalAccentColor.current,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dashboardRadiusMedium()),
        colors = glassCardColors(),
        border = glassCardBorder(),
        elevation = glassCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibrarySpacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
