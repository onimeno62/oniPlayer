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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.playback.ShuffleMode
import com.example.ui.library.LibraryDashboardScreen
import com.example.ui.library.AlbumsScreen
import com.example.ui.library.ArtistsScreen
import com.example.ui.library.AlbumDetailScreen
import com.example.ui.library.ArtistDetailScreen
import com.example.ui.library.components.HorizontalSongCard
import com.example.ui.library.components.LibraryStatsStrip
import com.example.ui.library.model.toAlbumUiModels
import com.example.ui.library.model.toArtistUiModels
import com.example.ui.player.components.PlayerDeleteDialog
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
    val position by viewModel.audioEngine.position.collectAsStateWithLifecycle()
    val duration by viewModel.audioEngine.duration.collectAsStateWithLifecycle()
    val isPreparing by viewModel.audioEngine.isPreparing.collectAsStateWithLifecycle()

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

    // State for Track Menu Bottom Sheet Dialogue
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
    val recentlyPlayedSongs = remember(songs, currentSong) {
        val played = songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
        val current = currentSong
        if (played.isNotEmpty()) {
            played
        } else if (current != null) {
            listOf(current) + songs.filter { it.id != current.id }
        } else {
            songs
        }
    }
    val highRatedSongs = remember(songs) {
        songs.filter { it.rating >= 4 }.sortedByDescending { it.rating }
    }
    val neverPlayedSongs = remember(songs) {
        songs.filter { it.playCount == 0 }
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

    val sortedHighRated = remember(highRatedSongs, sortBy, isSortAscending, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            highRatedSongs
        } else {
            highRatedSongs.filter {
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

    val sortedNeverPlayed = remember(neverPlayedSongs, sortBy, isSortAscending, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            neverPlayedSongs
        } else {
            neverPlayedSongs.filter {
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

    // Poweramp Categories Definitions (IDs 0–13)
    val categoryList = listOf(
        CategoryInfo(title = "All Songs", countText = "${songs.size} songs", icon = Icons.Default.MusicNote, iconBgColor = Color(0x1F9C27B0), iconColor = Color(0xFF9C27B0)),
        CategoryInfo(title = "Folders", countText = "${uniqueFolders.size} folders", icon = Icons.Default.Folder, iconBgColor = Color(0x1F2196F3), iconColor = Color(0xFF2196F3)),
        CategoryInfo(title = "Albums", countText = "${uniqueAlbums.size} albums", icon = Icons.Default.Album, iconBgColor = Color(0x1FE91E63), iconColor = Color(0xFFE91E63)),
        CategoryInfo(title = "Artists", countText = "${uniqueArtists.size} artists", icon = Icons.Default.Person, iconBgColor = Color(0x1F009688), iconColor = Color(0xFF009688)),
        CategoryInfo(title = "Genres", countText = "${uniqueGenres.size} genres", icon = Icons.Default.Category, iconBgColor = Color(0x1FFF9800), iconColor = Color(0xFFFF9800)),
        CategoryInfo(title = "Favorites", countText = "${favorites.size} favorite songs", icon = Icons.Filled.Favorite, iconBgColor = Color(0x1FF44336), iconColor = Color(0xFFF44336)),
        CategoryInfo(title = "Most Played", countText = "${mostPlayedSongs.size} played", icon = Icons.Default.Whatshot, iconBgColor = Color(0x1FFFC107), iconColor = Color(0xFFFFB300)),
        CategoryInfo(title = "Recently Added", countText = "${recentlyAddedSongs.size} added", icon = Icons.Default.Schedule, iconBgColor = Color(0x1F4CAF50), iconColor = Color(0xFF4CAF50)),
        CategoryInfo(title = "Playlists", countText = "${playlists.size} playlists", icon = Icons.AutoMirrored.Filled.QueueMusic, iconBgColor = Color(0x1F03A9F4), iconColor = Color(0xFF03A9F4)),
        CategoryInfo(title = "Smart Playlists", countText = "4 dynamic lists", icon = Icons.Default.AutoAwesome, iconBgColor = Color(0x1F673AB7), iconColor = Color(0xFF673AB7)),
        CategoryInfo(title = "Batch Tag Editor", countText = "Multi-edit tags", icon = Icons.Default.EditNote, iconBgColor = Color(0x1F3F51B5), iconColor = Color(0xFF3F51B5)),
        CategoryInfo(title = "Advanced Search", countText = "Filters & fields", icon = Icons.Default.ManageSearch, iconBgColor = Color(0x1F607D8B), iconColor = Color(0xFF607D8B)),
        CategoryInfo(title = "High Rated", countText = "${highRatedSongs.size} tracks", icon = Icons.Default.Star, iconBgColor = Color(0x1FFFC107), iconColor = Color(0xFFFFB300)),
        CategoryInfo(title = "Never Played", countText = "${neverPlayedSongs.size} tracks", icon = Icons.Default.Explore, iconBgColor = Color(0x1F009688), iconColor = Color(0xFF009688))
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
            position = position,
            duration = duration,
            isPreparing = isPreparing,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onOpenPlayer = { viewModel.selectTab(1) }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            val isSubHierarchical = selectedGroup != null || activePlaylist != null || activeSmartPlaylistType != null
            val displayCategoryTitle = categoryList.getOrNull(activeCategoryIndex!!)?.title ?: "Category"
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
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.SettingsSuggest, contentDescription = "Library Settings", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Screen Body Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (searchQuery.isNotBlank()) {
                    SongsListView(
                        songs = sortedSongs,
                        viewModel = viewModel,
                        sortBy = sortBy,
                        isSortAscending = isSortAscending,
                        onShowTrackMenu = { songForMenu = it },
                        layoutMode = layoutMode
                    )
                } else {
                    when (activeCategoryIndex) {
                        0 -> SongsListView(sortedSongs, viewModel, sortBy, isSortAscending, { songForMenu = it }, layoutMode)
                        1 -> GroupedListView(uniqueFolders, Icons.Default.Folder, viewModel, sortBy, isSortAscending, selectedGroup, { viewModel.setSelectedGroup(it) }, { songForMenu = it }, layoutMode)
                        2 -> {
                            if (selectedGroup != null) {
                                val album = albumUiModels.find { it.albumKey == selectedGroup }
                                if (album != null) {
                                    val songsInAlbum = remember(songs, selectedGroup) {
                                        songs.filter { "${it.displayAlbum.ifBlank { "Unknown Album" }}|${it.displayAlbumArtist}" == selectedGroup }
                                    }
                                    AlbumDetailScreen(album, songsInAlbum, currentSong, isPlaying, { if (songsInAlbum.isNotEmpty()) viewModel.playSong(songsInAlbum.first(), songsInAlbum) }, { if (songsInAlbum.isNotEmpty()) viewModel.playSong(songsInAlbum.random(), songsInAlbum) }, { viewModel.playSong(it, songsInAlbum) }, { songForMenu = it })
                                } else viewModel.setSelectedGroup(null)
                            } else {
                                AlbumsScreen(
                                    albums = albumUiModels,
                                    layoutMode = layoutMode,
                                    onAlbumClick = { viewModel.setSelectedGroup(it.albumKey) },
                                    gridIndex = viewModel.albumsGridIndex,
                                    gridOffset = viewModel.albumsGridOffset,
                                    onGridScroll = { i, o -> viewModel.albumsGridIndex = i; viewModel.albumsGridOffset = o },
                                    listIndex = viewModel.albumsListIndex,
                                    listOffset = viewModel.albumsListOffset,
                                    onListScroll = { i, o -> viewModel.albumsListIndex = i; viewModel.albumsListOffset = o }
                                )
                            }
                        }
                        3 -> {
                            if (selectedGroup != null) {
                                val artist = artistUiModels.find { it.artistKey == selectedGroup }
                                if (artist != null) {
                                    val songsByArtist = remember(songs, selectedGroup) {
                                        songs.filter { it.displayArtist.ifBlank { "Unknown Artist" } == selectedGroup }
                                    }
                                    val albumsByArtist = remember(albumUiModels, songsByArtist) {
                                        val artistAlbumKeys = songsByArtist.map { "${it.displayAlbum.ifBlank { "Unknown Album" }}|${it.displayAlbumArtist}" }.toSet()
                                        albumUiModels.filter { it.albumKey in artistAlbumKeys }
                                    }
                                    ArtistDetailScreen(artist, albumsByArtist, songsByArtist, currentSong, isPlaying, { if (songsByArtist.isNotEmpty()) viewModel.playSong(songsByArtist.first(), songsByArtist) }, { if (songsByArtist.isNotEmpty()) viewModel.playSong(songsByArtist.random(), songsByArtist) }, { viewModel.playSong(it, songsByArtist) }, { songForMenu = it }, layoutMode, viewModel)
                                } else viewModel.setSelectedGroup(null)
                            } else {
                                ArtistsScreen(
                                    artists = artistUiModels,
                                    layoutMode = layoutMode,
                                    onArtistClick = { viewModel.setSelectedGroup(it.artistKey) },
                                    gridIndex = viewModel.artistsGridIndex,
                                    gridOffset = viewModel.artistsGridOffset,
                                    onGridScroll = { i, o -> viewModel.artistsGridIndex = i; viewModel.artistsGridOffset = o },
                                    listIndex = viewModel.artistsListIndex,
                                    listOffset = viewModel.artistsListOffset,
                                    onListScroll = { i, o -> viewModel.artistsListIndex = i; viewModel.artistsListOffset = o }
                                )
                            }
                        }
                        4 -> GroupedListView(uniqueGenres, Icons.Default.Category, viewModel, sortBy, isSortAscending, selectedGroup, { viewModel.setSelectedGroup(it) }, { songForMenu = it }, layoutMode)
                        5 -> SongsListView(sortedFavorites, viewModel, sortBy, isSortAscending, { songForMenu = it }, layoutMode)
                        6 -> {
                            val filtered = remember(mostPlayedSongs, searchQuery) {
                                if (searchQuery.isBlank()) mostPlayedSongs else mostPlayedSongs.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
                            }
                            SongsListView(filtered, viewModel, sortBy, isSortAscending, { songForMenu = it }, layoutMode)
                        }
                        7 -> {
                            val filtered = remember(recentlyAddedSongs, searchQuery) {
                                if (searchQuery.isBlank()) recentlyAddedSongs else recentlyAddedSongs.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
                            }
                            SongsListView(filtered, viewModel, sortBy, isSortAscending, { songForMenu = it }, layoutMode)
                        }
                        8 -> PlaylistsView(songs, playlists, viewModel, activePlaylist, { viewModel.setActivePlaylist(it) }, { songForMenu = it }, layoutMode)
                        9 -> SmartPlaylistsView(songs, favorites, viewModel, activeSmartPlaylistType, { viewModel.setActiveSmartPlaylistType(it) }, { songForMenu = it })
                        10 -> BatchTagEditorView(songs, viewModel)
                        11 -> AdvancedSearchView(songs, viewModel, layoutMode, { songForMenu = it })
                        12 -> SongsListView(sortedHighRated, viewModel, sortBy, isSortAscending, { songForMenu = it }, layoutMode)
                        13 -> SongsListView(sortedNeverPlayed, viewModel, sortBy, isSortAscending, { songForMenu = it }, layoutMode)
                    }
                }
            }
        }
    }

    if (showOptionsMenu) {
        LibraryOptionsMenu(
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            layoutMode = layoutMode,
            onLayoutChange = { layoutMode = it },
            sortBy = sortBy,
            onSortByChange = { sortBy = it },
            isSortAscending = isSortAscending,
            onSortAscendingChange = { isSortAscending = it },
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
        OniSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .defaultMinSize(minHeight = 48.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "${category.title}, ${category.countText}"
                }
                .clickable { onClick() },
            variant = OniSurfaceVariant.Soft,
            shape = OniSkin.shapes.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OniSkin.spacing.md),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OniSkin.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = OniSkin.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = category.title,
                        style = OniSkin.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.countText,
                        style = OniSkin.typography.bodySmall,
                        color = OniSkin.colors.primary
                    )
                }
            }
        }
    } else {
        OniSurface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "${category.title}, ${category.countText}"
                }
                .clickable { onClick() },
            variant = OniSurfaceVariant.Soft,
            shape = OniSkin.shapes.card
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OniSkin.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = OniSkin.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(OniSkin.spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = OniSkin.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.countText,
                        style = OniSkin.typography.bodySmall,
                        color = OniSkin.colors.textSecondary
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = OniSkin.colors.textTertiary
                )
            }
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
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
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
        LaunchedEffect(currentSong) {
            if (currentSong != null && !gridState.isScrollInProgress) {
                val index = songs.indexOfFirst { it.id == currentSong?.id }
                if (index >= 0) {
                    val firstVisible = gridState.firstVisibleItemIndex
                    val lastVisible = firstVisible + 12
                    if (index < firstVisible || index > lastVisible) {
                        val targetIndex = (index - index % 2 - 2).coerceAtLeast(0)
                        gridState.scrollToItem(targetIndex)
                    }
                }
            }
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

        LaunchedEffect(currentSong) {
            if (currentSong != null && !listState.isScrollInProgress) {
                val index = songs.indexOfFirst { it.id == currentSong?.id }
                if (index >= 0) {
                    val firstVisible = listState.firstVisibleItemIndex
                    val lastVisible = firstVisible + 8
                    if (index < firstVisible || index > lastVisible) {
                        listState.scrollToItem((index - 2).coerceAtLeast(0))
                    }
                }
            }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

// Library Settings — modern bottom sheet (No Shuffle Mode configuration)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsMenu(
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    layoutMode: String,
    onLayoutChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    isSortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OniSkin.colors.surface,
        shape = OniSkin.shapes.bottomSheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(OniSkin.shapes.full)
                    .background(OniSkin.colors.outline.copy(alpha = 0.4f))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
                .padding(bottom = OniSkin.spacing.screenVertical),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OniSurface(
                            variant = OniSurfaceVariant.Flat,
                            shape = OniSkin.shapes.full,
                            containerColor = OniSkin.colors.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SettingsSuggest,
                                    contentDescription = null,
                                    tint = OniSkin.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
                        Column {
                            Text(
                                text = "Library Settings",
                                style = OniSkin.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.textPrimary
                            )
                            Text(
                                text = "Scan, layout, and playback preferences",
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings",
                            tint = OniSkin.colors.textSecondary
                        )
                    }
                }
            }

            // Quick Actions (Play All, Shuffle All, Scan Library)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    Text(
                        text = "QUICK ACTIONS",
                        style = OniSkin.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        Button(
                            onClick = {
                                onPlayAll()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = OniSkin.shapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                            Text("Play All", style = OniSkin.typography.labelMedium, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                onShuffleAll()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = OniSkin.shapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.surfaceElevated, contentColor = OniSkin.colors.textPrimary)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                            Text("Shuffle", style = OniSkin.typography.labelMedium, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                onRescan()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = OniSkin.shapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.surfaceElevated, contentColor = OniSkin.colors.textPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                            Text("Scan", style = OniSkin.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }

            // Layout Style
            item {
                Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    Text(
                        text = "LAYOUT STYLE",
                        style = OniSkin.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        listOf("grid" to ("Grid" to Icons.Default.GridView), "list" to ("List" to Icons.AutoMirrored.Filled.ViewList)).forEach { (mode, pair) ->
                            val isSelected = layoutMode == mode
                            OniSurface(
                                variant = if (isSelected) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft,
                                shape = OniSkin.shapes.button,
                                containerColor = if (isSelected) OniSkin.colors.primaryContainer else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { onLayoutChange(mode) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(pair.second, contentDescription = null, tint = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                                    Text(
                                        text = pair.first,
                                        style = OniSkin.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sort Options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    Text(
                        text = "SORT SONGS BY",
                        style = OniSkin.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary
                    )
                    val sortOptions = listOf("title" to "Title", "artist" to "Artist", "duration" to "Duration", "play_count" to "Most Played")
                    sortOptions.forEach { (key, label) ->
                        val isSelected = sortBy == key
                        OniSurface(
                            variant = if (isSelected) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft,
                            shape = OniSkin.shapes.small,
                            containerColor = if (isSelected) OniSkin.colors.primaryContainer.copy(alpha = 0.5f) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onSortByChange(key) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = OniSkin.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textPrimary
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = OniSkin.colors.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onSortAscendingChange(!isSortAscending) }
                            .padding(horizontal = OniSkin.spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sort in ascending order (A-Z)",
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textPrimary
                        )
                        Switch(
                            checked = isSortAscending,
                            onCheckedChange = onSortAscendingChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OniSkin.colors.onPrimary,
                                checkedTrackColor = OniSkin.colors.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

// Redesigned Song Menu & Playlist Picker
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuBottomSheetDialog(
    song: SongEntity,
    songs: List<SongEntity>,
    viewModel: MusicPlayerViewModel,
    onManualEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        PlayerDeleteDialog(
            song = song,
            onConfirmDelete = { physical ->
                if (physical) {
                    viewModel.deleteSongPhysically(song.id)
                } else {
                    viewModel.deleteSong(song.id)
                }
                showDeleteDialog = false
                onDismiss()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showPlaylistPicker) {
        PlaylistPickerBottomSheet(
            song = song,
            playlists = playlists,
            viewModel = viewModel,
            onDismiss = {
                showPlaylistPicker = false
                onDismiss()
            }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.bottomSheet,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(OniSkin.shapes.full)
                        .background(OniSkin.colors.outline.copy(alpha = 0.4f))
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.screenHorizontal)
                    .padding(bottom = OniSkin.spacing.screenVertical),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
            ) {
                // Header: Artwork, Title, Artist, Album
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OniArtwork(
                            artworkUri = song.albumArtUri,
                            contentDescription = "Cover art",
                            shape = OniSkin.shapes.small,
                            elevation = OniSkin.elevation.flat,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.displayTitle,
                                style = OniSkin.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.displayArtist.ifBlank { "Unknown Artist" },
                                style = OniSkin.typography.bodySmall,
                                color = OniSkin.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.displayAlbum.ifBlank { "Unknown Album" },
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Songs Static Data
                item {
                    SongStaticDataCard(song = song)
                }

                // Action Icon Tiles
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        // Row 1: Play Now, Play Next, Add to Queue
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                        ) {
                            TrackMenuActionTile(
                                icon = Icons.Default.PlayArrow,
                                title = "Play Now",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.playSong(song, songs)
                                    onDismiss()
                                }
                            )
                            TrackMenuActionTile(
                                icon = Icons.Default.SkipNext,
                                title = "Play Next",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.playNext(song)
                                    Toast.makeText(context, "Playing next: ${song.displayTitle}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                            TrackMenuActionTile(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                title = "Add to Queue",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.addToQueue(song)
                                    Toast.makeText(context, "Added to queue: ${song.displayTitle}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                        }

                        // Row 2: Favorite, Add to Playlist, Edit Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                        ) {
                            TrackMenuActionTile(
                                icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                title = if (song.isFavorite) "Favorited" else "Favorite",
                                iconTint = if (song.isFavorite) OniSkin.colors.primary else null,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.toggleFavorite(song.id)
                                    val msg = if (song.isFavorite) "Removed from favorites" else "Added to favorites"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                            TrackMenuActionTile(
                                icon = Icons.Default.PlaylistAdd,
                                title = "Playlist",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    showPlaylistPicker = true
                                }
                            )
                            TrackMenuActionTile(
                                icon = Icons.Default.Edit,
                                title = "Edit Tags",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onManualEdit()
                                    onDismiss()
                                }
                            )
                        }

                        // Row 3: View Album, View Artist, Open File Location (Created after Edit Tags)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                        ) {
                            TrackMenuActionTile(
                                icon = Icons.Default.Album,
                                title = "View Album",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val albumKey = "${song.displayAlbum.ifBlank { "Unknown Album" }}|${song.displayAlbumArtist}"
                                    viewModel.setActiveCategoryIndex(2)
                                    viewModel.setSelectedGroup(albumKey)
                                    onDismiss()
                                }
                            )
                            TrackMenuActionTile(
                                icon = Icons.Default.Person,
                                title = "View Artist",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val artistKey = song.displayArtist.ifBlank { "Unknown Artist" }
                                    viewModel.setActiveCategoryIndex(3)
                                    viewModel.setSelectedGroup(artistKey)
                                    onDismiss()
                                }
                            )
                            TrackMenuActionTile(
                                icon = Icons.Default.FolderOpen,
                                title = "File Location",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val file = File(song.filePath)
                                    val folderName = file.parentFile?.name ?: "Internal"
                                    viewModel.setActiveCategoryIndex(1)
                                    viewModel.setSelectedGroup(folderName)
                                    Toast.makeText(context, "Location: ${file.parent ?: song.filePath}", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            )
                        }

                        // Row 4: Delete Track (opens modal like player screen)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                        ) {
                            TrackMenuActionTile(
                                icon = Icons.Default.DeleteOutline,
                                title = "Delete Track",
                                iconTint = OniSkin.colors.error,
                                textColor = OniSkin.colors.error,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongStaticDataCard(song: SongEntity) {
    val file = remember(song.filePath) { File(song.filePath) }
    val fileSizeStr = remember(file) {
        if (file.exists() && file.length() > 0) {
            val bytes = file.length()
            val mb = bytes / (1024.0 * 1024.0)
            if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
            else String.format(java.util.Locale.US, "%d KB", bytes / 1024)
        } else null
    }

    val audioDetails = remember(song.filePath) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(song.filePath)
            val sr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            val bitr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
            retriever.release()

            val sampleRateStr = sr?.toIntOrNull()?.let { hz ->
                String.format(java.util.Locale.US, "%.1f kHz", hz / 1000.0)
            }
            val detectedBitrate = bitr?.toIntOrNull()?.let { b ->
                "${b / 1000} kbps"
            }
            Pair(sampleRateStr, detectedBitrate)
        } catch (_: Throwable) {
            Pair(null, null)
        }
    }

    val formatBadge = remember(song) {
        val path = song.filePath.lowercase()
        val ext = path.substringAfterLast('.', "")
        when {
            ext == "flac" || song.format.equals("FLAC", ignoreCase = true) -> "FLAC"
            ext == "wav" -> "WAV"
            ext == "m4a" || ext == "aac" -> "AAC"
            ext == "ogg" || ext == "opus" -> "OPUS"
            ext.isNotBlank() -> ext.uppercase()
            song.format.isNotBlank() -> song.format.uppercase()
            else -> "MP3"
        }
    }

    val bitrateStr = when {
        song.bitrate > 0 -> "${song.bitrate} kbps"
        audioDetails.second != null -> audioDetails.second
        else -> null
    }
    val durationStr = formatDuration(song.duration)
    val sampleRateStr = audioDetails.first

    val dateFormat = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val dateTimeFormat = remember { java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault()) }

    val dateAddedStr = remember(song.dateAdded) {
        if (song.dateAdded > 0) dateFormat.format(java.util.Date(song.dateAdded)) else null
    }

    val lastPlayedStr = remember(song.lastPlayedTimestamp) {
        if (song.lastPlayedTimestamp > 0) {
            val now = System.currentTimeMillis()
            val diff = now - song.lastPlayedTimestamp
            when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 7 * 86400_000 -> "${diff / 86400_000}d ago"
                else -> dateFormat.format(java.util.Date(song.lastPlayedTimestamp))
            }
        } else "Never"
    }

    val fileName = remember(file) { file.name.ifBlank { song.displayTitle } }
    val lastModifiedStr = remember(file) {
        if (file.exists() && file.lastModified() > 0) {
            dateTimeFormat.format(java.util.Date(file.lastModified()))
        } else null
    }

    val trackDiscStr = buildString {
        if (song.displayTrack.isNotBlank()) append("Track ${song.displayTrack}")
        if (song.displayDisc.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append("Disc ${song.displayDisc}")
        }
    }

    val ratingStr = if (song.rating > 0) {
        val filled = "★".repeat(song.rating.coerceIn(1, 5))
        val empty = "☆".repeat((5 - song.rating).coerceAtLeast(0))
        "$filled$empty (${song.rating}/5)"
    } else null

    var isExpanded by remember { mutableStateOf(false) }

    OniSurface(
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            // 1. Audio Specs Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StaticDataChip(text = formatBadge, isHighlight = true)
                StaticDataChip(text = durationStr, icon = Icons.Default.Schedule)
                if (bitrateStr != null) {
                    StaticDataChip(text = bitrateStr, icon = Icons.Default.Speed)
                }
                if (sampleRateStr != null) {
                    StaticDataChip(text = sampleRateStr)
                }
                if (fileSizeStr != null) {
                    StaticDataChip(text = fileSizeStr, icon = Icons.Default.Save)
                }
            }

            // 2. Primary Metadata Grid (2 Columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (song.displayGenre.isNotBlank() && song.displayGenre != "Unknown") {
                        MetadataKeyValue(label = "Genre", value = song.displayGenre)
                    }
                    if (trackDiscStr.isNotBlank()) {
                        MetadataKeyValue(label = "Track", value = trackDiscStr)
                    }
                    if (song.displayAlbumArtist.isNotBlank() && song.displayAlbumArtist != song.displayArtist) {
                        MetadataKeyValue(label = "Album Artist", value = song.displayAlbumArtist)
                    }
                    MetadataKeyValue(
                        label = "Plays",
                        value = if (song.playCount > 0) "${song.playCount} times" else "Unplayed"
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (song.displayYear.isNotBlank()) {
                        MetadataKeyValue(label = "Year", value = song.displayYear)
                    }
                    if (song.displayBpm.isNotBlank()) {
                        MetadataKeyValue(label = "BPM", value = "${song.displayBpm} BPM")
                    }
                    if (song.displayComposer.isNotBlank()) {
                        MetadataKeyValue(label = "Composer", value = song.displayComposer)
                    }
                    if (lastPlayedStr != "Never") {
                        MetadataKeyValue(label = "Last Played", value = lastPlayedStr)
                    } else if (dateAddedStr != null) {
                        MetadataKeyValue(label = "Added", value = dateAddedStr)
                    }
                }
            }

            // 3. Extended Details (Rating, Comments, Date Added, Last Modified)
            if (isExpanded) {
                SettingDivider()

                if (ratingStr != null) {
                    MetadataKeyValue(label = "Rating", value = ratingStr)
                }

                if (song.displayComment.isNotBlank()) {
                    MetadataKeyValue(label = "Comment", value = song.displayComment)
                }

                if (dateAddedStr != null && lastPlayedStr != "Never") {
                    MetadataKeyValue(label = "Date Added", value = dateAddedStr)
                }

                if (lastModifiedStr != null) {
                    MetadataKeyValue(label = "File Modified", value = lastModifiedStr)
                }

                MetadataKeyValue(label = "File Name", value = fileName)
            }

            // 4. File Path row with toggle button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = OniSkin.colors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                    Text(
                        text = song.filePath,
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                Text(
                    text = if (isExpanded) "Less" else "More",
                    style = OniSkin.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.primary,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MetadataKeyValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = OniSkin.typography.caption,
            fontWeight = FontWeight.Bold,
            color = OniSkin.colors.textSecondary
        )
        Text(
            text = value,
            style = OniSkin.typography.caption,
            color = OniSkin.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StaticDataChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isHighlight: Boolean = false
) {
    OniSurface(
        variant = OniSurfaceVariant.Flat,
        shape = OniSkin.shapes.full,
        containerColor = if (isHighlight) OniSkin.colors.primary.copy(alpha = 0.15f) else OniSkin.colors.surfaceElevated,
        modifier = Modifier.height(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) OniSkin.colors.primary else OniSkin.colors.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = OniSkin.typography.caption,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlight) OniSkin.colors.primary else OniSkin.colors.textPrimary
            )
        }
    }
}

@Composable
fun TrackMenuActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    containerColor: Color? = null,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    OniSurface(
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card,
        containerColor = containerColor,
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.sm, horizontal = OniSkin.spacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: OniSkin.colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
            Text(
                text = title,
                style = OniSkin.typography.caption,
                fontWeight = FontWeight.Medium,
                color = textColor ?: OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun TrackMenuActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    OniSurface(
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: OniSkin.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OniSkin.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OniSkin.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Dedicated Playlist Picker Bottom Sheet allowing song addition or creating a new playlist on the fly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerBottomSheet(
    song: SongEntity,
    playlists: List<PlaylistEntity>,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surface,
        shape = OniSkin.shapes.bottomSheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(OniSkin.shapes.full)
                    .background(OniSkin.colors.outline.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
                .padding(bottom = OniSkin.spacing.screenVertical),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add to Playlist",
                        style = OniSkin.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textPrimary
                    )
                    Text(
                        text = "Choose target playlist for \"${song.displayTitle}\"",
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OniSkin.colors.textSecondary)
                }
            }

            // Create New Playlist option
            OniSurface(
                variant = OniSurfaceVariant.Elevated,
                shape = OniSkin.shapes.small,
                containerColor = OniSkin.colors.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { showCreateDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OniSkin.colors.primary)
                    Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                    Text(
                        text = "Create New Playlist",
                        style = OniSkin.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary
                    )
                }
            }

            SettingDivider()

            if (playlists.isEmpty()) {
                Text(
                    text = "No custom playlists found. Tap above to create your first playlist.",
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary,
                    modifier = Modifier.padding(vertical = OniSkin.spacing.md)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val containsSong = remember(playlist.songIdsJson, song.id) {
                            try {
                                val arr = org.json.JSONArray(playlist.songIdsJson)
                                (0 until arr.length()).any { arr.getString(it) == song.id }
                            } catch (e: Exception) {
                                false
                            }
                        }

                        OniSurface(
                            variant = OniSurfaceVariant.Soft,
                            shape = OniSkin.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    if (containsSong) {
                                        Toast.makeText(context, "Song is already in \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addSongToPlaylist(song.id, playlist.id)
                                        Toast.makeText(context, "Added to \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = OniSkin.colors.primary)
                                    Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                                    Text(
                                        text = playlist.name,
                                        style = OniSkin.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = OniSkin.colors.textPrimary
                                    )
                                }
                                if (containsSong) {
                                    Text(
                                        text = "Added",
                                        style = OniSkin.typography.caption,
                                        color = OniSkin.colors.textTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "New Playlist",
                    style = OniSkin.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    Text(
                        text = "Enter a name for the new playlist. \"${song.displayTitle}\" will be added automatically.",
                        style = OniSkin.typography.bodySmall,
                        color = OniSkin.colors.textSecondary
                    )
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = {
                            playlistName = it
                            errorMessage = null
                        },
                        placeholder = { Text("Playlist name", color = OniSkin.colors.textSecondary) },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OniSkin.shapes.button,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OniSkin.colors.primary,
                            unfocusedBorderColor = OniSkin.colors.outline,
                            focusedTextColor = OniSkin.colors.textPrimary,
                            unfocusedTextColor = OniSkin.colors.textPrimary
                        )
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = OniSkin.typography.caption,
                            color = OniSkin.colors.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = playlistName.trim()
                        if (trimmed.isEmpty()) {
                            errorMessage = "Playlist name cannot be empty."
                        } else {
                            val newId = "playlist_" + System.currentTimeMillis()
                            viewModel.createPlaylist(trimmed)
                            // Add song directly to the newly created playlist
                            viewModel.addSongToPlaylist(song.id, newId)
                            Toast.makeText(context, "Created \"$trimmed\" and added song!", Toast.LENGTH_SHORT).show()
                            showCreateDialog = false
                            onDismiss()
                        }
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Create", color = OniSkin.colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", color = OniSkin.colors.textSecondary)
                }
            },
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.dialog
        )
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

