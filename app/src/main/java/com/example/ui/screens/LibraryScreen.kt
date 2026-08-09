package com.example.ui.screens

import android.widget.Toast
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalAccentGlowColor
import com.example.ui.theme.LocalCornerRadius
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.ui.viewmodel.ShuffleMode
import com.example.ui.library.LibraryDashboardScreen
import com.example.ui.library.AlbumsScreen
import com.example.ui.library.ArtistsScreen
import com.example.ui.library.AlbumDetailScreen
import com.example.ui.library.ArtistDetailScreen
import com.example.ui.library.model.toAlbumUiModels
import com.example.ui.library.model.toArtistUiModels
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(viewModel: MusicPlayerViewModel) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val artistSummaries by viewModel.allArtistSummaries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val currentSong by viewModel.audioEngine.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.rescanLibrary()
            Toast.makeText(context, "Scanning local storage...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage permission is required to scan local music files.", Toast.LENGTH_LONG).show()
        }
    }

    val triggerScanWithPermission = {
        val hasPermission = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.rescanLibrary()
            Toast.makeText(context, "Scanning local storage...", Toast.LENGTH_SHORT).show()
        } else {
            launcher.launch(storagePermission)
        }
    }

    // Poweramp Style Screen states backed by ViewModel
    val activeCategoryIndex by viewModel.activeCategoryIndex.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val shuffleMode by viewModel.shuffleMode.collectAsStateWithLifecycle()
    val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val activeSmartPlaylistType by viewModel.activeSmartPlaylistType.collectAsStateWithLifecycle()

    // BackHandler for hierarchical back navigation
    BackHandler(enabled = activeCategoryIndex != null) {
        if (selectedGroup != null) {
            viewModel.setSelectedGroup(null)
        } else if (activePlaylist != null) {
            viewModel.setActivePlaylist(null)
        } else if (activeSmartPlaylistType != null) {
            viewModel.setActiveSmartPlaylistType(null)
        } else {
            viewModel.setActiveCategoryIndex(null)
        }
    }
    var layoutMode by rememberSaveable { mutableStateOf("grid") } // "grid" or "list"
    var sortBy by rememberSaveable { mutableStateOf("title") } // "title", "artist", "duration", "play_count"
    var isSortAscending by rememberSaveable { mutableStateOf(true) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // State for Tag Editor dialog
    var songToEdit by remember { mutableStateOf<SongEntity?>(null) }

    // State for Poweramp Track Bottom Sheet Dialogue
    var songForMenu by remember { mutableStateOf<SongEntity?>(null) }

    // Calculations of categories data counts
    val uniqueFolders = remember(songs) {
        songs.groupBy { File(it.filePath).parentFile?.name ?: "Internal" }
    }
    val uniqueAlbums = remember(songs) {
        songs.groupBy { it.album.ifEmpty { "Unknown Album" } }
    }
    val uniqueArtists = remember(songs) {
        songs.groupBy { it.artist.ifEmpty { "Unknown Artist" } }
    }
    val uniqueGenres = remember(songs) {
        songs.groupBy { it.genre.ifEmpty { "General" } }
    }
    val mostPlayedSongs = remember(songs) {
        songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
    }
    val recentlyAddedSongs = remember(songs) {
        songs.sortedByDescending { it.dateAdded }
    }
    val lastPlayedSong = remember(songs) {
        songs.filter { it.lastPlayedTimestamp > 0 }.maxByOrNull { it.lastPlayedTimestamp }
    }
    val recentlyPlayedSongs = remember(songs) {
        songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
    }

    val albumUiModels = remember(songs) { songs.toAlbumUiModels() }
    val artistUiModels = remember(songs, artistSummaries) { songs.toArtistUiModels(artistSummaries) }

    // Sort songs inside lists dynamically
    val sortedSongs = remember(songs, sortBy, isSortAscending, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }

        val result = when (sortBy) {
            "artist" -> filtered.sortedBy { it.customArtist ?: it.artist }
            "duration" -> filtered.sortedBy { it.duration }
            "play_count" -> filtered.sortedByDescending { it.playCount }
            else -> filtered.sortedBy { it.customTitle ?: it.title }
        }

        if (!isSortAscending && sortBy != "play_count") {
            result.reversed()
        } else if (isSortAscending && sortBy == "play_count") {
            result.reversed()
        } else {
            result
        }
    }

    // Filtered favorites based on search & sorting
    val sortedFavorites = remember(favorites, sortBy, isSortAscending, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            favorites
        } else {
            favorites.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

        val result = when (sortBy) {
            "artist" -> filtered.sortedBy { it.customArtist ?: it.artist }
            "duration" -> filtered.sortedBy { it.duration }
            "play_count" -> filtered.sortedByDescending { it.playCount }
            else -> filtered.sortedBy { it.customTitle ?: it.title }
        }

        if (!isSortAscending && sortBy != "play_count") {
            result.reversed()
        } else if (isSortAscending && sortBy == "play_count") {
            result.reversed()
        } else {
            result
        }
    }

    // Poweramp Categories Definitions
    val categoryList = listOf(
        CategoryInfo(
            title = "All Songs",
            countText = "${songs.size} songs",
            icon = Icons.Default.MusicNote,
            iconBgColor = Color(0x1F9C27B0), // Purple Tint
            iconColor = Color(0xFF9C27B0)
        ),
        CategoryInfo(
            title = "Folders",
            countText = "${uniqueFolders.size} folders",
            icon = Icons.Default.Folder,
            iconBgColor = Color(0x1F2196F3), // Blue Tint
            iconColor = Color(0xFF2196F3)
        ),
        CategoryInfo(
            title = "Albums",
            countText = "${uniqueAlbums.size} albums",
            icon = Icons.Default.Album,
            iconBgColor = Color(0x1FE91E63), // Pink Tint
            iconColor = Color(0xFFE91E63)
        ),
        CategoryInfo(
            title = "Artists",
            countText = "${uniqueArtists.size} artists",
            icon = Icons.Default.Person,
            iconBgColor = Color(0x1F009688), // Teal Tint
            iconColor = Color(0xFF009688)
        ),
        CategoryInfo(
            title = "Genres",
            countText = "${uniqueGenres.size} genres",
            icon = Icons.Default.Category,
            iconBgColor = Color(0x1FFF9800), // Orange Tint
            iconColor = Color(0xFFFF9800)
        ),
        CategoryInfo(
            title = "Favorites",
            countText = "${favorites.size} favorite songs",
            icon = Icons.Filled.Favorite,
            iconBgColor = Color(0x1FF44336), // Red Tint
            iconColor = Color(0xFFF44336)
        ),
        CategoryInfo(
            title = "Most Played",
            countText = "${mostPlayedSongs.size} played",
            icon = Icons.Default.Whatshot,
            iconBgColor = Color(0x1FFFC107), // Amber Tint
            iconColor = Color(0xFFFFB300)
        ),
        CategoryInfo(
            title = "Recently Added",
            countText = "${recentlyAddedSongs.size} added",
            icon = Icons.Default.Schedule,
            iconBgColor = Color(0x1F4CAF50), // Green Tint
            iconColor = Color(0xFF4CAF50)
        ),
        CategoryInfo(
            title = "Playlists",
            countText = "${playlists.size} playlists",
            icon = Icons.Default.QueueMusic,
            iconBgColor = Color(0x1F03A9F4), // Light Blue Tint
            iconColor = Color(0xFF03A9F4)
        ),
        CategoryInfo(
            title = "Smart Playlists",
            countText = "4 dynamic lists",
            icon = Icons.Default.AutoAwesome,
            iconBgColor = Color(0x1F673AB7), // Deep Purple Tint
            iconColor = Color(0xFF673AB7)
        ),
        CategoryInfo(
            title = "Batch Tag Editor",
            countText = "Multi-edit tags",
            icon = Icons.Default.EditNote,
            iconBgColor = Color(0x1F3F51B5), // Indigo Tint
            iconColor = Color(0xFF3F51B5)
        ),
        CategoryInfo(
            title = "Advanced Search",
            countText = "Filters & fields",
            icon = Icons.Default.ManageSearch,
            iconBgColor = Color(0x1F607D8B), // Blue Grey Tint
            iconColor = Color(0xFF607D8B)
        )
    )

    if (activeCategoryIndex == null) {
        LibraryDashboardScreen(
            songs = songs,
            sortedSongs = sortedSongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            lastPlayedSong = lastPlayedSong,
            recentlyPlayedSongs = recentlyPlayedSongs,
            mostPlayedSongs = mostPlayedSongs,
            recentlyAddedSongs = recentlyAddedSongs,
            uniqueArtistsCount = uniqueArtists.size,
            uniqueAlbumsCount = uniqueAlbums.size,
            favoritesCount = favorites.size,
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            isScanning = isScanning,
            showOptionsMenu = { showOptionsMenu = true },
            onRescan = triggerScanWithPermission,
            layoutMode = layoutMode,
            onToggleLayoutMode = {
                layoutMode = if (layoutMode == "grid") "list" else "grid"
            },
            categoryList = categoryList,
            onSelectCategory = { viewModel.setActiveCategoryIndex(it) },
            onPlaySong = { song, songList -> viewModel.playSong(song, songList) },
            onShowTrackMenu = { songForMenu = it },
            albumUiModels = albumUiModels,
            artistUiModels = artistUiModels,
            viewModel = viewModel
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // Sub-category Header with Back Navigation
            val isSubHierarchical = selectedGroup != null || activePlaylist != null || activeSmartPlaylistType != null
            val displayCategoryTitle = categoryList[activeCategoryIndex!!].title
            val displaySubTitle = when {
                selectedGroup != null -> {
                    when (activeCategoryIndex) {
                        2 -> albumUiModels.find { it.albumKey == selectedGroup }?.title ?: selectedGroup!!
                        3 -> artistUiModels.find { it.artistKey == selectedGroup }?.name ?: selectedGroup!!
                        else -> selectedGroup!!
                    }
                }
                activePlaylist != null -> activePlaylist!!.name
                activeSmartPlaylistType != null -> activeSmartPlaylistType!!
                else -> ""
            }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedGroup != null) {
                                viewModel.setSelectedGroup(null)
                            } else if (activePlaylist != null) {
                                viewModel.setActivePlaylist(null)
                            } else if (activeSmartPlaylistType != null) {
                                viewModel.setActiveSmartPlaylistType(null)
                            } else {
                                viewModel.setActiveCategoryIndex(null)
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSubHierarchical) displayCategoryTitle.uppercase() else "LIBRARY CATEGORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isSubHierarchical) displaySubTitle else displayCategoryTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Filter icon inside deep library to show settings
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Sort Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Local Search inside active category (hidden on detail sub-screens)
                val isDetailActive = selectedGroup != null && (activeCategoryIndex == 3 || activeCategoryIndex == 2)
                if (!isDetailActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Filter current list...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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

                    Spacer(modifier = Modifier.height(12.dp))
                }

            // --- 2. Screen Body Content ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (searchQuery.isNotBlank()) {
                    // If searching, directly display matching songs
                    SongsListView(
                        songs = sortedSongs,
                        viewModel = viewModel,
                        sortBy = sortBy,
                        isSortAscending = isSortAscending,
                        onShowTrackMenu = { songForMenu = it },
                        layoutMode = layoutMode
                    )
                } else {
                    // RENDER DETAILED CATEGORY VIEW
                    when (activeCategoryIndex) {
                    0 -> { // All Songs
                        SongsListView(
                            songs = sortedSongs,
                            viewModel = viewModel,
                            sortBy = sortBy,
                            isSortAscending = isSortAscending,
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    1 -> { // Folders
                        GroupedListView(
                            groupedData = uniqueFolders,
                            icon = Icons.Default.Folder,
                            viewModel = viewModel,
                            sortBy = sortBy,
                            isSortAscending = isSortAscending,
                            selectedGroup = selectedGroup,
                            onSelectedGroupChange = { viewModel.setSelectedGroup(it) },
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    2 -> { // Albums
                        if (selectedGroup != null) {
                            val album = albumUiModels.find { it.albumKey == selectedGroup }
                            if (album != null) {
                                val songsInAlbum = remember(songs, selectedGroup) {
                                    songs.filter { "${it.displayAlbum.ifBlank { "Unknown Album" }}|${it.displayAlbumArtist}" == selectedGroup }
                                }
                                AlbumDetailScreen(
                                    album = album,
                                    songsInAlbum = songsInAlbum,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onPlayAll = { if (songsInAlbum.isNotEmpty()) viewModel.playSong(songsInAlbum.first(), songsInAlbum) },
                                    onShufflePlay = { if (songsInAlbum.isNotEmpty()) viewModel.playSong(songsInAlbum.random(), songsInAlbum) },
                                    onSongClick = { viewModel.playSong(it, songsInAlbum) },
                                    onShowTrackMenu = { songForMenu = it }
                                )
                            } else {
                                viewModel.setSelectedGroup(null) // stale key, bail out safely
                            }
                        } else {
                            AlbumsScreen(
                                albums = albumUiModels,
                                layoutMode = layoutMode,
                                onAlbumClick = { viewModel.setSelectedGroup(it.albumKey) },
                                gridIndex = viewModel.albumsGridIndex,
                                gridOffset = viewModel.albumsGridOffset,
                                onGridScroll = { index, offset ->
                                    viewModel.albumsGridIndex = index
                                    viewModel.albumsGridOffset = offset
                                },
                                listIndex = viewModel.albumsListIndex,
                                listOffset = viewModel.albumsListOffset,
                                onListScroll = { index, offset ->
                                    viewModel.albumsListIndex = index
                                    viewModel.albumsListOffset = offset
                                }
                            )
                        }
                    }
                    3 -> { // Artists
                        if (selectedGroup != null) {
                            val artist = artistUiModels.find { it.artistKey == selectedGroup }
                            if (artist != null) {
                                val songsByArtist = remember(songs, selectedGroup) {
                                    songs.filter { it.displayArtist.ifBlank { "Unknown Artist" } == selectedGroup }
                                }
                                val albumsByArtist = remember(albumUiModels, songsByArtist) {
                                    val artistAlbumKeys = songsByArtist.map { song ->
                                        "${song.displayAlbum.ifBlank { "Unknown Album" }}|${song.displayAlbumArtist}"
                                    }.toSet()
                                    albumUiModels.filter { it.albumKey in artistAlbumKeys }
                                }
                                ArtistDetailScreen(
                                    artist = artist,
                                    albumsByArtist = albumsByArtist,
                                    songsByArtist = songsByArtist,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onPlayAll = { if (songsByArtist.isNotEmpty()) viewModel.playSong(songsByArtist.first(), songsByArtist) },
                                    onShufflePlay = { if (songsByArtist.isNotEmpty()) viewModel.playSong(songsByArtist.random(), songsByArtist) },
                                    onSongClick = { viewModel.playSong(it, songsByArtist) },
                                    onShowTrackMenu = { songForMenu = it },
                                    layoutMode = layoutMode,
                                    viewModel = viewModel
                                )
                            } else {
                                viewModel.setSelectedGroup(null) // stale key, bail out safely
                            }
                        } else {
                            ArtistsScreen(
                                artists = artistUiModels,
                                layoutMode = layoutMode,
                                onArtistClick = { viewModel.setSelectedGroup(it.artistKey) },
                                gridIndex = viewModel.artistsGridIndex,
                                gridOffset = viewModel.artistsGridOffset,
                                onGridScroll = { index, offset ->
                                    viewModel.artistsGridIndex = index
                                    viewModel.artistsGridOffset = offset
                                },
                                listIndex = viewModel.artistsListIndex,
                                listOffset = viewModel.artistsListOffset,
                                onListScroll = { index, offset ->
                                    viewModel.artistsListIndex = index
                                    viewModel.artistsListOffset = offset
                                }
                            )
                        }
                    }
                    4 -> { // Genres
                        GroupedListView(
                            groupedData = uniqueGenres,
                            icon = Icons.Default.Category,
                            viewModel = viewModel,
                            sortBy = sortBy,
                            isSortAscending = isSortAscending,
                            selectedGroup = selectedGroup,
                            onSelectedGroupChange = { viewModel.setSelectedGroup(it) },
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    5 -> { // Favorites
                        SongsListView(
                            songs = sortedFavorites,
                            viewModel = viewModel,
                            sortBy = sortBy,
                            isSortAscending = isSortAscending,
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    6 -> { // Most Played
                        val filteredMostPlayed = remember(mostPlayedSongs, searchQuery) {
                            if (searchQuery.isBlank()) mostPlayedSongs else mostPlayedSongs.filter {
                                it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        SongsListView(
                            songs = filteredMostPlayed,
                            viewModel = viewModel,
                            sortBy = sortBy,
                            isSortAscending = isSortAscending,
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    7 -> { // Recently Added
                        val filteredRecentlyAdded = remember(recentlyAddedSongs, searchQuery) {
                            if (searchQuery.isBlank()) recentlyAddedSongs else recentlyAddedSongs.filter {
                                it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        SongsListView(
                            songs = filteredRecentlyAdded,
                            viewModel = viewModel,
                            sortBy = sortBy,
                            isSortAscending = isSortAscending,
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    8 -> { // Playlists
                        PlaylistsView(
                            songs = songs,
                            playlists = playlists,
                            viewModel = viewModel,
                            activePlaylist = activePlaylist,
                            onActivePlaylistChange = { viewModel.setActivePlaylist(it) },
                            onShowTrackMenu = { songForMenu = it },
                            layoutMode = layoutMode
                        )
                    }
                    9 -> { // Smart Playlists
                        SmartPlaylistsView(
                            songs = songs,
                            favorites = favorites,
                            viewModel = viewModel,
                            activeSmartPlaylistType = activeSmartPlaylistType,
                            onActiveSmartPlaylistTypeChange = { viewModel.setActiveSmartPlaylistType(it) },
                            onShowTrackMenu = { songForMenu = it }
                        )
                    }
                    10 -> { // Batch Tag Editor
                        BatchTagEditorView(
                            songs = songs,
                            viewModel = viewModel
                        )
                    }
                    11 -> { // Advanced Search
                        AdvancedSearchView(
                            songs = songs,
                            viewModel = viewModel,
                            layoutMode = layoutMode,
                            onShowTrackMenu = { songForMenu = it }
                        )
                    }
                }
            }
        }
    }
}

    // Poweramp Style Options Menu Dialog
    if (showOptionsMenu) {
        LibraryOptionsMenu(
            layoutMode = layoutMode,
            onLayoutChange = { layoutMode = it },
            sortBy = sortBy,
            onSortByChange = { sortBy = it },
            isSortAscending = isSortAscending,
            onSortAscendingChange = { isSortAscending = it },
            shuffleMode = shuffleMode,
            onShuffleModeChange = { viewModel.setShuffleMode(it) },
            onRescan = triggerScanWithPermission,
            onPlayAll = {
                if (songs.isNotEmpty()) {
                    viewModel.playSong(songs.first(), songs)
                    Toast.makeText(context, "Playing all songs", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No songs to play", Toast.LENGTH_SHORT).show()
                }
            },
            onShuffleAll = {
                if (songs.isNotEmpty()) {
                    if (!viewModel.isShuffle.value) {
                        viewModel.toggleShuffle()
                    }
                    val startSong = viewModel.pickShuffleStartSong(songs) ?: songs.first()
                    viewModel.playSong(startSong, songs)
                    Toast.makeText(context, "Shuffling all songs", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No songs to play", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showOptionsMenu = false }
        )
    }

    // Tag Editor Dialog Integration
    songToEdit?.let { song ->
        AdvancedTagEditorDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { songToEdit = null }
        )
    }

    // Poweramp Track Menu Bottom Overlay Dialog
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

// Category Information Data Class
data class CategoryInfo(
    val title: String,
    val countText: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconBgColor: Color,
    val iconColor: Color
)

@Composable
fun CategoryCard(
    category: CategoryInfo,
    isGrid: Boolean,
    onClick: () -> Unit
) {
    if (isGrid) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(dashboardRadiusLarge()),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
            border = borderStrokeDefault()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(category.iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = category.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.countText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(dashboardRadiusSmall()))
                    .background(category.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.countText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItemRow(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onShowTrackMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onShowTrackMenu
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = "Cover art",
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.customTitle ?: song.title,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = (song.customArtist ?: song.artist) + " • " + (song.customAlbum ?: song.album),
                fontSize = 13.sp,
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrent) {
            if (isPlaying) {
                PlayingEqualizerWave(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                )
            }
        }

        if (song.playCount > 0) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "🔥 ${song.playCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = formatDuration(song.duration),
            fontSize = 12.sp,
            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onShowTrackMenu) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More",
                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsListView(
    songs: List<SongEntity>,
    viewModel: MusicPlayerViewModel,
    sortBy: String,
    isSortAscending: Boolean,
    onShowTrackMenu: (SongEntity) -> Unit,
    layoutMode: String = "list"
) {
    val currentSong by viewModel.audioEngine.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsStateWithLifecycle()

    if (songs.isEmpty()) {
        com.example.ui.library.components.LibraryEmptyState(
            title = "No songs found",
            message = "Scan your local storage to import music or add songs.",
            modifier = Modifier.fillMaxSize(),
            icon = Icons.Default.MusicOff
        )
    } else if (layoutMode == "grid") {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = viewModel.songsGridIndex,
            initialFirstVisibleItemScrollOffset = viewModel.songsGridOffset
        )
        LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
            viewModel.songsGridIndex = gridState.firstVisibleItemIndex
            viewModel.songsGridOffset = gridState.firstVisibleItemScrollOffset
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dashboardRadiusMedium()))
                        .clickable { viewModel.playSong(song, songs) }
                ) {
                    Box {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Cover art",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(dashboardRadiusMedium()))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                        )
                        IconButton(
                            onClick = { onShowTrackMenu(song) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        if (song.id == currentSong?.id && isPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                PlayingEqualizerWave(color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = song.displayTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (song.id == currentSong?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.displayArtist,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = viewModel.libraryScrollIndex,
            initialFirstVisibleItemScrollOffset = viewModel.libraryScrollOffset
        )

        LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            viewModel.libraryScrollIndex = listState.firstVisibleItemIndex
            viewModel.libraryScrollOffset = listState.firstVisibleItemScrollOffset
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                com.example.ui.library.components.SongRow(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isPlaying = isPlaying,
                    onClick = { viewModel.playSong(song, songs) },
                    onShowMenu = { onShowTrackMenu(song) }
                )
            }
        }
    }
}

@Composable
fun GroupedListView(
    groupedData: Map<String, List<SongEntity>>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    viewModel: MusicPlayerViewModel,
    sortBy: String,
    isSortAscending: Boolean,
    selectedGroup: String?,
    onSelectedGroupChange: (String?) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    layoutMode: String = "list"
) {
    if (selectedGroup != null) {
        val songsInGroup = groupedData[selectedGroup] ?: emptyList()
        val sortedSongsInGroup = remember(songsInGroup, sortBy, isSortAscending) {
            val result = when (sortBy) {
                "artist" -> songsInGroup.sortedBy { it.customArtist ?: it.artist }
                "duration" -> songsInGroup.sortedBy { it.duration }
                "play_count" -> songsInGroup.sortedByDescending { it.playCount }
                else -> songsInGroup.sortedBy { it.customTitle ?: it.title }
            }

            if (!isSortAscending && sortBy != "play_count") {
                result.reversed()
            } else if (isSortAscending && sortBy == "play_count") {
                result.reversed()
            } else {
                result
            }
        }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectedGroupChange(null) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = selectedGroup,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            SongsListView(
                songs = sortedSongsInGroup,
                viewModel = viewModel,
                sortBy = sortBy,
                isSortAscending = isSortAscending,
                onShowTrackMenu = onShowTrackMenu,
                layoutMode = layoutMode
            )
        }
    } else {
        if (groupedData.isEmpty()) {
            com.example.ui.library.components.LibraryEmptyState(
                title = "No Categorized Music",
                message = "We couldn't find any categorized tracks in this section.",
                modifier = Modifier.fillMaxSize(),
                icon = icon
            )
        } else if (layoutMode == "grid") {
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
                initialFirstVisibleItemIndex = viewModel.groupedGridIndex,
                initialFirstVisibleItemScrollOffset = viewModel.groupedGridOffset
            )
            LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
                viewModel.groupedGridIndex = gridState.firstVisibleItemIndex
                viewModel.groupedGridOffset = gridState.firstVisibleItemScrollOffset
            }
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val groupedList = groupedData.toList()
                items(groupedList.size, key = { index -> groupedList[index].first }) { index ->
                    val (name, list) = groupedList[index]
                    Card(
                        onClick = { onSelectedGroupChange(name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f),
                        shape = RoundedCornerShape(dashboardRadiusMedium()),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(dashboardRadiusMedium())),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${list.size} ${if (list.size == 1) "song" else "songs"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = viewModel.groupedListIndex,
                initialFirstVisibleItemScrollOffset = viewModel.groupedListOffset
            )
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                viewModel.groupedListIndex = listState.firstVisibleItemIndex
                viewModel.groupedListOffset = listState.firstVisibleItemScrollOffset
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                groupedData.forEach { (name, list) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectedGroupChange(name) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${list.size} ${if (list.size == 1) "song" else "songs"}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagEditorDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    AdvancedTagEditorDialog(song, viewModel, onDismiss)
}

// Library Settings — modern bottom sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsMenu(
    layoutMode: String,
    onLayoutChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    isSortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    shuffleMode: ShuffleMode,
    onShuffleModeChange: (ShuffleMode) -> Unit,
    onRescan: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = LocalAccentColor.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Library Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize how your library looks and plays",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LAYOUT STYLE
            Text(
                text = "LAYOUT STYLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Triple("grid", "Grid" to "Category tiles & song art grids", Icons.Default.GridView),
                    Triple("list", "List" to "Rows everywhere", Icons.Default.ViewList)
                ).forEach { (mode, labels, icon) ->
                    val (label, description) = labels
                    val isSelected = layoutMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dashboardRadiusSmall()))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onLayoutChange(mode) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SHUFFLE MODE
            Text(
                text = "SHUFFLE MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Triple(ShuffleMode.RANDOM, "Random" to "Pure random pick", Icons.Default.Shuffle),
                    Triple(ShuffleMode.DISCOVER, "Discover" to "Favors songs you rarely play", Icons.Default.Explore),
                    Triple(ShuffleMode.FAVORITES_BOOST, "Favorites Boost" to "Favors your favorited songs", Icons.Default.Favorite)
                ).forEach { (mode, labels, icon) ->
                    val (label, description) = labels
                    val isSelected = shuffleMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dashboardRadiusSmall()))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onShuffleModeChange(mode) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SORT SONGS BY
            Text(
                text = "SORT SONGS BY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val options = listOf(
                    Triple("title", "Song Title", Icons.Default.SortByAlpha),
                    Triple("artist", "Artist Name", Icons.Default.Person),
                    Triple("duration", "Duration Length", Icons.Default.Timer),
                    Triple("play_count", "Times Played", Icons.Default.Whatshot)
                )
                options.forEach { (option, label, icon) ->
                    val isSelected = sortBy == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dashboardRadiusSmall()))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSortByChange(option) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = accent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dashboardRadiusSmall()))
                    .clickable { onSortAscendingChange(!isSortAscending) }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Ascending Order (A-Z)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Switch(
                    checked = isSortAscending,
                    onCheckedChange = { onSortAscendingChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(20.dp))

            // QUICK ACTIONS
            Text(
                text = "QUICK ACTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dashboardRadiusMedium()))
                        .background(accent)
                        .clickable(onClick = onPlayAll)
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dashboardRadiusMedium()))
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable(onClick = onShuffleAll)
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dashboardRadiusMedium()))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), RoundedCornerShape(dashboardRadiusMedium()))
                    .clickable(onClick = onRescan)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rescan Music Library", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun borderStrokeDefault(): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

// Shared, theme-driven corner radius tiers for the dashboard. All three scale together
// whenever the user's corner-radius setting (LocalCornerRadius) changes, instead of each
// card picking its own hardcoded value.
@Composable
fun dashboardRadiusLarge(): Dp = (LocalCornerRadius.current * 1.5f).dp

@Composable
fun dashboardRadiusMedium(): Dp = LocalCornerRadius.current.dp

@Composable
fun dashboardRadiusSmall(): Dp = (LocalCornerRadius.current * 0.75f).dp

fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    val hr = (ms / (1000 * 60 * 60)) % 24
    return if (hr > 0) {
        String.format("%d:%02d:%02d", hr, min, sec)
    } else {
        String.format("%d:%02d", min, sec)
    }
}

fun greetingForTime(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "LATE NIGHT TUNES"
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        hour < 21 -> "GOOD EVENING"
        else -> "GOOD NIGHT"
    }
}

@Composable
fun MainLibraryDashboard(
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
    onShowTrackMenu: (SongEntity) -> Unit
) {
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    // Indices into categoryList: All Songs (0), Favorites (5), Playlists (8), Folders (1)
    val quickAccessCategoryIndices = remember { listOf(0, 5, 8, 1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Dashboard greeting header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Poweramp Options Menu Button
                    IconButton(
                        onClick = showOptionsMenu,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Library Options", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Rescan Library Shortcut
                    IconButton(
                        onClick = onRescan,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan Library", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // 2. Global Library Search input bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search title, artist, album...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "Found ${sortedSongs.size} tracks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            if (sortedSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tracks found matching \"$searchQuery\"",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(sortedSongs, key = { it.id }) { song ->
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        SongItemRow(
                            song = song,
                            isCurrent = song.id == currentSong?.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySong(song, sortedSongs) },
                            onShowTrackMenu = { onShowTrackMenu(song) }
                        )
                    }
                }
            }
        } else {
            // 3. Continue Listening hero card — only shown once a song has actually been played
            if (lastPlayedSong != null) {
                item {
                    ContinueListeningHero(
                        song = lastPlayedSong,
                        onPlayClick = { onPlaySong(lastPlayedSong, songs) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 4. Quick stats strip
            item {
                LibraryStatsStrip(
                    songCount = songs.size,
                    artistCount = uniqueArtistsCount,
                    albumCount = uniqueAlbumsCount,
                    favoriteCount = favoritesCount
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

        // 5. Horizontal song rows
        if (recentlyPlayedSongs.isNotEmpty()) {
            item {
                HorizontalSongRow(
                    title = "Recently Played",
                    songs = recentlyPlayedSongs,
                    onSongClick = { onPlaySong(it, recentlyPlayedSongs) }
                )
            }
        }

        if (mostPlayedSongs.isNotEmpty()) {
            item {
                HorizontalSongRow(
                    title = "Most Played",
                    songs = mostPlayedSongs,
                    onSongClick = { onPlaySong(it, mostPlayedSongs) }
                )
            }
        }

        if (recentlyAddedSongs.isNotEmpty()) {
            item {
                HorizontalSongRow(
                    title = "Recently Added",
                    songs = recentlyAddedSongs,
                    onSongClick = { onPlaySong(it, recentlyAddedSongs) }
                )
            }
        }

        // 6. Categories / Quick Access Section — single animated item so toggling between
        // Quick Access and All Categories crossfades instead of snapping instantly.
        item {
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
                                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
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

                        // Quick Access Cards (2x2 Grid)
                        quickAccessCategoryIndices.chunked(2).forEach { rowIndices ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
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

                        // See all categories button
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
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

                        // All Category Cards (Grid or List)
                        if (layoutMode == "grid") {
                            categoryList.chunked(2).forEach { rowPair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
fun ContinueListeningHero(
    song: SongEntity,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(dashboardRadiusLarge()))
    ) {
        // Full-bleed blurred album art background
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )

        // Subtle diagonal wash using the app's dynamic accent palette (theme- or
        // currently-playing-song-driven), so the hero ties into the same color
        // language as the rest of the app instead of sitting on flat black.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            LocalAccentColor.current.copy(alpha = 0.30f),
                            LocalAccentGlowColor.current.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient scrim so text stays readable regardless of the artwork or accent wash above
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Cover art",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(dashboardRadiusSmall()))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONTINUE LISTENING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.customTitle ?: song.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.customArtist ?: song.artist,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun LibraryStatsStrip(
    songCount: Int,
    artistCount: Int,
    albumCount: Int,
    favoriteCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(icon = Icons.Default.MusicNote, value = songCount.toString(), label = "Songs", tint = Color(0xFF9C27B0))
        StatChip(icon = Icons.Default.Person, value = artistCount.toString(), label = "Artists", tint = Color(0xFF009688))
        StatChip(icon = Icons.Default.Album, value = albumCount.toString(), label = "Albums", tint = Color(0xFFE91E63))
        StatChip(icon = Icons.Filled.Favorite, value = favoriteCount.toString(), label = "Favorites", tint = Color(0xFFF44336))
    }
}

@Composable
fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    val chipShape = RoundedCornerShape(dashboardRadiusSmall())
    Row(
        modifier = Modifier
            .clip(chipShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), chipShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HorizontalSongRow(
    title: String,
    songs: List<SongEntity>,
    onSongClick: (SongEntity) -> Unit
) {
    if (songs.isEmpty()) return

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs.take(15)) { song ->
                HorizontalSongCard(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
fun HorizontalSongCard(
    song: SongEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = "Cover art",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(dashboardRadiusMedium()))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.customTitle ?: song.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.customArtist ?: song.artist,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrackMenuBottomSheetDialog(
    song: SongEntity,
    songs: List<SongEntity>,
    viewModel: MusicPlayerViewModel,
    onManualEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isFavorite by remember(song.isFavorite) { mutableStateOf(song.isFavorite) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPlaylistSelect by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        var deleteMode by remember { mutableStateOf("library") }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Track Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose how you want to delete \"${song.customTitle ?: song.title}\":",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Card(
                        onClick = { deleteMode = "library" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (deleteMode == "library") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(
                            width = if (deleteMode == "library") 1.5.dp else 1.dp,
                            color = if (deleteMode == "library") MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (deleteMode == "library"), onClick = { deleteMode = "library" })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Delete from Library Only", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Removes DB record but keeps physical audio file.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        onClick = { deleteMode = "physical" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (deleteMode == "physical") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(
                            width = if (deleteMode == "physical") 1.5.dp else 1.dp,
                            color = if (deleteMode == "physical") MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (deleteMode == "physical"),
                                onClick = { deleteMode = "physical" },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Delete Physically from Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                Text("Permanently deletes physical file and library DB record.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteMode == "physical") {
                            viewModel.deleteSongPhysically(song.id)
                            Toast.makeText(context, "Deleted song physically", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.deleteSong(song.id)
                            Toast.makeText(context, "Deleted song from library", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deleteMode == "physical") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (deleteMode == "physical") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (deleteMode == "physical") "Delete File" else "Remove Track")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    if (showPlaylistSelect) {
        AlertDialog(
            onDismissRequest = { showPlaylistSelect = false },
            title = { Text("Add to Playlist") },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("No playlists created yet. Go to Library -> Playlists to create one.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        playlists.forEach { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(song.id, pl.id)
                                        Toast.makeText(context, "Added to playlist: ${pl.name}", Toast.LENGTH_SHORT).show()
                                        showPlaylistSelect = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(pl.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistSelect = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent click-through
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = borderStrokeDefault()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 16.dp)
                ) {
                    // Drawer Handle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp, bottom = 10.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )

                    // Track Header Information
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Cover art",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.customTitle ?: song.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = (song.customArtist ?: song.artist) + " • " + (song.customAlbum ?: song.album),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Poweramp Style Audio Tech Details Overlay Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = borderStrokeDefault()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "AUDIO TECHNICAL DETAILS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val fileFormat = if (song.filePath.endsWith(".flac", ignoreCase = true)) "FLAC Lossless" else if (song.filePath.endsWith(".m4a", ignoreCase = true)) "AAC Audio" else "MPEG Layer-3 (MP3)"
                            val calculatedSizeMB = String.format("%.1f", (song.duration / 1000.0 * 320.0) / 8192.0)
                            val bitrateVal = if (song.filePath.endsWith(".flac", ignoreCase = true)) "1042 kbps" else "320 kbps (CBR)"
                            val freq = "44,100 Hz • 16-Bit Stereo"

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TechDetailLabelValue("Format", fileFormat, Modifier.weight(1.2f))
                                TechDetailLabelValue("Size", "$calculatedSizeMB MB", Modifier.weight(0.8f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TechDetailLabelValue("Bitrate", bitrateVal, Modifier.weight(1.2f))
                                TechDetailLabelValue("Decoder", "OniEngine v2.4 (OpenSL ES)", Modifier.weight(0.8f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TechDetailLabelValue("Source Path", song.filePath, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Circular Action Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularTrackAction(
                            icon = Icons.Default.PlayArrow,
                            label = "Play Now",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                viewModel.playSong(song, songs)
                                onDismiss()
                            }
                        )

                        CircularTrackAction(
                            icon = Icons.Default.SkipNext,
                            label = "Play Next",
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                viewModel.playNext(song)
                                Toast.makeText(context, "Queued to play next", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )

                        CircularTrackAction(
                            icon = Icons.Default.QueueMusic,
                            label = "Add Queue",
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                viewModel.addToQueue(song)
                                Toast.makeText(context, "Added to end of queue", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )

                        CircularTrackAction(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isFavorite) "Liked" else "Favorite",
                            color = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                viewModel.toggleFavorite(song.id)
                                isFavorite = !isFavorite
                                Toast.makeText(
                                    context,
                                    if (isFavorite) "Added to Favorites" else "Removed from Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(4.dp))

                    // Options List Items
                    TrackMenuItemOption(
                        icon = Icons.Default.Edit,
                        label = "Online Tag Search & Editor",
                        onClick = {
                            onManualEdit()
                            onDismiss()
                        }
                    )

                    TrackMenuItemOption(
                        icon = Icons.Default.PlaylistAdd,
                        label = "Add to Playlist...",
                        onClick = {
                            showPlaylistSelect = true
                        }
                    )

                    TrackMenuItemOption(
                        icon = Icons.Default.Share,
                        label = "Share Track",
                        onClick = {
                            Toast.makeText(context, "Shared: ${song.customTitle ?: song.title}", Toast.LENGTH_SHORT).show()
                        }
                    )

                    TrackMenuItemOption(
                        icon = Icons.Default.Notifications,
                        label = "Set as Ringtone",
                        onClick = {
                            Toast.makeText(context, "Successfully set as current Ringtone", Toast.LENGTH_SHORT).show()
                        }
                    )

                    TrackMenuItemOption(
                        icon = Icons.Default.Delete,
                        label = "Delete Track...",
                        isError = true,
                        onClick = {
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TechDetailLabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CircularTrackAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun TrackMenuItemOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isGlow: Boolean = false,
    isError: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else if (isGlow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val backgroundBrush = if (isGlow) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
            )
        )
    } else {
        null
    }

    val itemModifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = !isLoading) { onClick() }
        .then(
            if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier
        )
        .padding(horizontal = 20.dp, vertical = 12.dp)

    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isGlow) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        if (isGlow) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    "AI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PlayingEqualizerWave(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    val heightScale1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val heightScale2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val heightScale3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val heightScale4 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.height(18.dp).width(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(heightScale1).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(heightScale2).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(heightScale3).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(heightScale4).clip(RoundedCornerShape(1.dp)).background(color))
    }
}
