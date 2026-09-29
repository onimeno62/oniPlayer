package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.components.SongRow
import com.example.ui.library.model.toAlbumUiModels
import com.example.ui.library.model.toArtistUiModels
import com.example.ui.library.model.toFolderUiModels
import com.example.ui.library.model.toGenreUiModels
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import java.io.File
import org.json.JSONArray

private const val PREFS_RECENT_SEARCHES_KEY = "poweramp_recent_searches"
private const val MAX_RECENT_SEARCHES = 10

enum class SearchCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.AllInclusive),
    TRACKS("Tracks", Icons.Default.MusicNote),
    ALBUMS("Albums", Icons.Default.Album),
    ARTISTS("Artists", Icons.Default.Person),
    FOLDERS("Folders", Icons.Default.Folder),
    GENRES("Genres", Icons.Default.Category),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic)
}

@Composable
fun SearchScreen(viewModel: MusicPlayerViewModel) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val artistSummaries by viewModel.allArtistSummaries.collectAsStateWithLifecycle()
    val currentSong by viewModel.audioEngine.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }
    var songForMenu by remember { mutableStateOf<SongEntity?>(null) }
    var songToEdit by remember { mutableStateOf<SongEntity?>(null) }

    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("oniplayer_search_prefs", android.content.Context.MODE_PRIVATE)
    }

    var recentSearches by remember {
        val saved = sharedPrefs.getString(PREFS_RECENT_SEARCHES_KEY, null)
        val ordered = runCatching {
            if (saved.isNullOrBlank()) {
                emptyList()
            } else {
                JSONArray(saved).let { array ->
                    buildList(array.length()) {
                        for (index in 0 until array.length()) add(array.getString(index))
                    }
                }
            }
        }.getOrDefault(emptyList())
        mutableStateOf(ordered.take(MAX_RECENT_SEARCHES))
    }

    fun persistRecentSearches(items: List<String>) {
        sharedPrefs.edit()
            .putString(PREFS_RECENT_SEARCHES_KEY, JSONArray(items).toString())
            .apply()
    }

    fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val updated = (listOf(trimmed) + recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        recentSearches = updated
        persistRecentSearches(updated)
    }

    fun removeRecentSearch(query: String) {
        val updated = recentSearches.filterNot { it.equals(query, ignoreCase = true) }
        recentSearches = updated
        persistRecentSearches(updated)
    }

    fun clearAllRecentSearches() {
        recentSearches = emptyList()
        sharedPrefs.edit().remove(PREFS_RECENT_SEARCHES_KEY).apply()
    }

    val trimmedQuery = searchQuery.trim()

    val albumUiModels = remember(songs) { songs.toAlbumUiModels() }
    val artistUiModels = remember(songs, artistSummaries) { songs.toArtistUiModels(artistSummaries) }
    val folderUiModels = remember(songs) { songs.toFolderUiModels() }
    val genreUiModels = remember(songs) { songs.toGenreUiModels() }

    // Scoped search results
    val matchingTracks = remember(songs, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList() else {
            songs.filter {
                it.displayTitle.contains(trimmedQuery, ignoreCase = true) ||
                it.displayArtist.contains(trimmedQuery, ignoreCase = true) ||
                it.displayAlbum.contains(trimmedQuery, ignoreCase = true) ||
                it.displayGenre.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingAlbums = remember(albumUiModels, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList() else {
            albumUiModels.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                it.artist.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingArtists = remember(artistUiModels, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList() else {
            artistUiModels.filter {
                it.name.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingFolders = remember(folderUiModels, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList() else {
            folderUiModels.filter {
                it.displayName.contains(trimmedQuery, ignoreCase = true) ||
                it.folderPath.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingGenres = remember(genreUiModels, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList() else {
            genreUiModels.filter {
                it.genre.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingPlaylists = remember(playlists, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList() else {
            playlists.filter {
                it.name.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val totalMatches = matchingTracks.size + matchingAlbums.size + matchingArtists.size +
        matchingFolders.size + matchingGenres.size + matchingPlaylists.size

    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Search Input Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("poweramp_search_input"),
                placeholder = {
                    Text(
                        text = "Search tracks, albums, artists, folders...",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = OniSkin.colors.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = OniSkin.colors.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = OniSkin.shapes.full,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OniSkin.colors.textPrimary,
                    unfocusedTextColor = OniSkin.colors.textPrimary,
                    focusedBorderColor = OniSkin.colors.primary,
                    unfocusedBorderColor = OniSkin.colors.outline.copy(alpha = 0.5f),
                    cursorColor = OniSkin.colors.primary,
                    focusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.2f)
                )
            )
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OniSkin.spacing.xs),
            contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            items(SearchCategory.values()) { category ->
                val isSelected = selectedCategory == category
                val count = when (category) {
                    SearchCategory.ALL -> totalMatches
                    SearchCategory.TRACKS -> matchingTracks.size
                    SearchCategory.ALBUMS -> matchingAlbums.size
                    SearchCategory.ARTISTS -> matchingArtists.size
                    SearchCategory.FOLDERS -> matchingFolders.size
                    SearchCategory.GENRES -> matchingGenres.size
                    SearchCategory.PLAYLISTS -> matchingPlaylists.size
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = if (trimmedQuery.isNotEmpty() && count > 0) "${category.label} ($count)" else category.label,
                            style = OniSkin.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = OniSkin.shapes.full,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OniSkin.colors.primary,
                        selectedLabelColor = OniSkin.colors.onPrimary,
                        selectedLeadingIconColor = OniSkin.colors.onPrimary,
                        containerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = OniSkin.colors.textSecondary,
                        iconColor = OniSkin.colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) OniSkin.colors.primary else Color.Transparent,
                        selectedBorderColor = OniSkin.colors.primary,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        // Search Body Content
        if (trimmedQuery.isEmpty()) {
            // Idle state: Recent searches & quick suggestions
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = OniSkin.spacing.screenHorizontal),
                contentPadding = PaddingValues(top = OniSkin.spacing.sm, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
            ) {
                if (recentSearches.isNotEmpty()) {
                    item(key = "recent_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT SEARCHES",
                                style = OniSkin.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.primary
                            )
                            TextButton(onClick = { clearAllRecentSearches() }) {
                                Text(
                                    text = "Clear all",
                                    style = OniSkin.typography.labelSmall,
                                    color = OniSkin.colors.textTertiary
                                )
                            }
                        }
                    }

                    items(recentSearches, key = { "recent_$it" }) { queryItem ->
                        OniSurface(
                            variant = OniSurfaceVariant.Soft,
                            shape = OniSkin.shapes.card,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { searchQuery = queryItem }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = OniSkin.colors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                                Text(
                                    text = queryItem,
                                    style = OniSkin.typography.bodyMedium,
                                    color = OniSkin.colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { removeRecentSearch(queryItem) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove search",
                                        tint = OniSkin.colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item(key = "empty_recent_prompt") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = OniSkin.spacing.xxl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = OniSkin.colors.textTertiary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(OniSkin.spacing.md))
                            Text(
                                text = "Universal Library Search",
                                style = OniSkin.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                            Text(
                                text = "Quickly search across ${songs.size} tracks, ${albumUiModels.size} albums, ${artistUiModels.size} artists, folders and playlists",
                                style = OniSkin.typography.bodySmall,
                                color = OniSkin.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = OniSkin.spacing.lg),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else if (totalMatches == 0) {
            // No results state
            LibraryEmptyState(
                title = "No results found",
                message = "Nothing matches \"$trimmedQuery\" in your music library.",
                icon = Icons.Default.SearchOff,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Active results view
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = OniSkin.spacing.screenHorizontal),
                contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
            ) {
                // 1. ARTISTS SECTION
                if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.ARTISTS) && matchingArtists.isNotEmpty()) {
                    item(key = "header_artists") {
                        SearchSectionHeader(title = "Artists (${matchingArtists.size})")
                    }
                    items(if (selectedCategory == SearchCategory.ALL) matchingArtists.take(5) else matchingArtists, key = { "artist_${it.artistKey}" }) { artist ->
                        SearchResultRow(
                            title = artist.name,
                            subtitle = "${artist.songCount} songs • ${artist.albumCount} albums",
                            icon = Icons.Default.Person,
                            onClick = {
                                saveRecentSearch(trimmedQuery)
                                viewModel.selectTab(0)
                                viewModel.setActiveCategoryIndex(3)
                                viewModel.setSelectedGroup(artist.artistKey)
                            }
                        )
                    }
                }

                // 2. ALBUMS SECTION
                if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.ALBUMS) && matchingAlbums.isNotEmpty()) {
                    item(key = "header_albums") {
                        SearchSectionHeader(title = "Albums (${matchingAlbums.size})")
                    }
                    items(if (selectedCategory == SearchCategory.ALL) matchingAlbums.take(5) else matchingAlbums, key = { "album_${it.albumKey}" }) { album ->
                        SearchResultRow(
                            title = album.title,
                            subtitle = "${album.artist} • ${album.songCount} songs",
                            icon = Icons.Default.Album,
                            onClick = {
                                saveRecentSearch(trimmedQuery)
                                viewModel.selectTab(0)
                                viewModel.setActiveCategoryIndex(2)
                                viewModel.setSelectedGroup(album.albumKey)
                            }
                        )
                    }
                }

                // 3. FOLDERS SECTION
                if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.FOLDERS) && matchingFolders.isNotEmpty()) {
                    item(key = "header_folders") {
                        SearchSectionHeader(title = "Folders (${matchingFolders.size})")
                    }
                    items(if (selectedCategory == SearchCategory.ALL) matchingFolders.take(5) else matchingFolders, key = { "folder_${it.folderPath}" }) { folder ->
                        SearchResultRow(
                            title = folder.displayName,
                            subtitle = "${folder.songCount} songs • ${folder.folderPath}",
                            icon = Icons.Default.Folder,
                            onClick = {
                                saveRecentSearch(trimmedQuery)
                                viewModel.selectTab(0)
                                viewModel.setActiveCategoryIndex(1)
                                viewModel.setSelectedGroup(folder.displayName)
                            }
                        )
                    }
                }

                // 4. GENRES SECTION
                if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.GENRES) && matchingGenres.isNotEmpty()) {
                    item(key = "header_genres") {
                        SearchSectionHeader(title = "Genres (${matchingGenres.size})")
                    }
                    items(if (selectedCategory == SearchCategory.ALL) matchingGenres.take(5) else matchingGenres, key = { "genre_${it.genre}" }) { genre ->
                        SearchResultRow(
                            title = genre.genre,
                            subtitle = "${genre.songCount} songs",
                            icon = Icons.Default.Category,
                            onClick = {
                                saveRecentSearch(trimmedQuery)
                                viewModel.selectTab(0)
                                viewModel.setActiveCategoryIndex(4)
                                viewModel.setSelectedGroup(genre.genre)
                            }
                        )
                    }
                }

                // 5. PLAYLISTS SECTION
                if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.PLAYLISTS) && matchingPlaylists.isNotEmpty()) {
                    item(key = "header_playlists") {
                        SearchSectionHeader(title = "Playlists (${matchingPlaylists.size})")
                    }
                    items(if (selectedCategory == SearchCategory.ALL) matchingPlaylists.take(5) else matchingPlaylists, key = { "playlist_${it.id}" }) { playlist ->
                        SearchResultRow(
                            title = playlist.name,
                            subtitle = "Playlist",
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            onClick = {
                                saveRecentSearch(trimmedQuery)
                                viewModel.selectTab(0)
                                viewModel.setActiveCategoryIndex(8)
                                viewModel.setActivePlaylist(playlist)
                            }
                        )
                    }
                }

                // 6. TRACKS SECTION
                if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.TRACKS) && matchingTracks.isNotEmpty()) {
                    item(key = "header_tracks") {
                        SearchSectionHeader(title = "Tracks (${matchingTracks.size})")
                    }
                    items(matchingTracks, key = { "track_${it.id}" }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSong?.id,
                            isPlaying = isPlaying,
                            onClick = {
                                saveRecentSearch(trimmedQuery)
                                viewModel.playSong(song, matchingTracks)
                            },
                            onShowMenu = { songForMenu = song }
                        )
                    }
                }
            }
        }
    }

    songToEdit?.let { song ->
        AdvancedTagEditorDialog(song = song, viewModel = viewModel, onDismiss = { songToEdit = null })
    }

    songForMenu?.let { song ->
        TrackMenuBottomSheetDialog(
            song = song,
            songs = songs,
            viewModel = viewModel,
            onManualEdit = { songToEdit = song },
            onDismiss = { songForMenu = null }
        )
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = OniSkin.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = OniSkin.colors.primary,
        modifier = Modifier.padding(top = OniSkin.spacing.sm, bottom = OniSkin.spacing.xxs)
    )
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OniSurface(
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(OniSkin.shapes.small)
                    .background(OniSkin.colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OniSkin.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(OniSkin.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OniSkin.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OniSkin.colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
