package com.oniplayer.ui.screens

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
import com.oniplayer.ui.theme.LocalAccentColor
import com.oniplayer.ui.theme.LocalAccentGlowColor
import com.oniplayer.ui.theme.LocalCornerRadius
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
import com.oniplayer.data.entity.PlaylistEntity
import com.oniplayer.data.entity.SongEntity
import com.oniplayer.ui.components.button.OniPrimaryButton
import com.oniplayer.ui.components.music.OniArtwork
import com.oniplayer.ui.components.surface.OniSurface
import com.oniplayer.ui.components.surface.OniSurfaceVariant
import com.oniplayer.ui.theme.OniSkin
import com.oniplayer.ui.viewmodel.MusicPlayerViewModel
import com.oniplayer.playback.ShuffleMode
import com.oniplayer.ui.library.LibraryDashboardScreen
import com.oniplayer.ui.library.AlbumsScreen
import com.oniplayer.ui.library.ArtistsScreen
import com.oniplayer.ui.library.FoldersScreen
import com.oniplayer.ui.library.GenresScreen
import com.oniplayer.ui.library.AlbumDetailScreen
import com.oniplayer.ui.library.ArtistDetailScreen
import com.oniplayer.ui.library.components.HorizontalSongCard
import com.oniplayer.ui.library.components.LibraryCategoryHero
import com.oniplayer.ui.library.components.LibraryStatsStrip
import com.oniplayer.ui.library.model.toAlbumUiModels
import com.oniplayer.ui.library.model.toArtistUiModels
import com.oniplayer.ui.library.model.toFolderUiModels
import com.oniplayer.ui.library.model.toGenreUiModels
import com.oniplayer.ui.player.components.PlayerDeleteDialog
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
    val folderUiModels = remember(songs) { songs.toFolderUiModels() }
    val genreUiModels = remember(songs) { songs.toGenreUiModels() }

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
            favoriteSongs = favorites,
            highRatedSongs = highRatedSongs,
            neverPlayedSongs = neverPlayedSongs,
            counts = com.oniplayer.ui.library.DashboardCounts(
                albums = uniqueAlbums.size,
                artists = uniqueArtists.size,
                folders = uniqueFolders.size,
                genres = uniqueGenres.size,
                playlists = playlists.size
            ),
            searchQuery = searchQuery,
            isScanning = isScanning,
            showOptionsMenu = { showOptionsMenu = true },
            onRescan = triggerScanWithPermission,
            onOpenSearch = { viewModel.setActiveCategoryIndex(11) },
            onSelectCategory = { viewModel.setActiveCategoryIndex(it) },
            onPlaySong = { song, songList -> viewModel.playSong(song, songList) },
            onShowTrackMenu = { songForMenu = it },
            onPlayNext = { viewModel.playNext(it) },
            onAddToQueue = { viewModel.addToQueue(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
            position = position,
            duration = duration,
            isPreparing = isPreparing,
            beatEnergy = viewModel.audioEngine.beatEnergy.value,
            nowPlayingEffect = viewModel.nowPlayingEffect.value,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onReplay = { viewModel.seekTo(0L); viewModel.resumePlayback() },
            onOpenPlayer = { viewModel.selectTab(1) }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            val isSubHierarchical = selectedGroup != null || activePlaylist != null || activeSmartPlaylistType != null
            val displayCategoryTitle = activeCategoryIndex?.let { categoryList.getOrNull(it) }?.title ?: "Category"
            val displaySubTitle = when {
                selectedGroup != null -> {
                    when (activeCategoryIndex) {
                        2 -> albumUiModels.find { it.albumKey == selectedGroup }?.title ?: selectedGroup.orEmpty()
                        3 -> artistUiModels.find { it.artistKey == selectedGroup }?.name ?: selectedGroup.orEmpty()
                        else -> selectedGroup.orEmpty()
                    }
                }
                activePlaylist != null -> activePlaylist?.name.orEmpty()
                activeSmartPlaylistType != null -> activeSmartPlaylistType.orEmpty()
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
                    Icon(Icons.Default.SettingsSuggest, contentDescription = "Library Menu", tint = MaterialTheme.colorScheme.primary)
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
                        1 -> {
                            val currentGroup = selectedGroup
                            if (currentGroup != null) {
                                val songsInGroup = uniqueFolders[currentGroup] ?: emptyList()
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

                                val heroArtwork = songsInGroup.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
                                val heroSubtitle = if (songsInGroup.size == 1) "1 song" else "${songsInGroup.size} songs"

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = OniSkin.spacing.screenHorizontal)
                                ) {
                                    LibraryCategoryHero(
                                        title = currentGroup,
                                        subtitle = heroSubtitle,
                                        artworkUri = heroArtwork,
                                        icon = Icons.Default.Folder,
                                        modifier = Modifier.padding(vertical = OniSkin.spacing.md)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = OniSkin.spacing.md),
                                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (sortedSongsInGroup.isNotEmpty()) {
                                                    viewModel.playSong(sortedSongsInGroup.first(), sortedSongsInGroup)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = OniSkin.shapes.button,
                                            contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = OniSkin.colors.primary,
                                                contentColor = OniSkin.colors.onPrimary
                                            )
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                                            Text("Play All", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                if (sortedSongsInGroup.isNotEmpty()) {
                                                    val startSong = sortedSongsInGroup.random()
                                                    viewModel.playSong(startSong, sortedSongsInGroup.shuffled())
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = OniSkin.shapes.button,
                                            contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = OniSkin.colors.surfaceVariant,
                                                contentColor = OniSkin.colors.textPrimary
                                            )
                                        ) {
                                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                                            Text("Shuffle", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(
                                        color = OniSkin.colors.outline.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
                                    )

                                    SongsListView(
                                        songs = sortedSongsInGroup,
                                        viewModel = viewModel,
                                        sortBy = sortBy,
                                        isSortAscending = isSortAscending,
                                        onShowTrackMenu = { songForMenu = it },
                                        layoutMode = layoutMode
                                    )
                                }
                            } else {
                                FoldersScreen(
                                    folders = folderUiModels,
                                    layoutMode = layoutMode,
                                    onFolderClick = { viewModel.setSelectedGroup(it.displayName) },
                                    gridIndex = viewModel.groupedGridIndex,
                                    gridOffset = viewModel.groupedGridOffset,
                                    onGridScroll = { i, o -> viewModel.groupedGridIndex = i; viewModel.groupedGridOffset = o },
                                    listIndex = viewModel.groupedListIndex,
                                    listOffset = viewModel.groupedListOffset,
                                    onListScroll = { i, o -> viewModel.groupedListIndex = i; viewModel.groupedListOffset = o }
                                )
                            }
                        }
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
                        4 -> {
                            val currentGroup = selectedGroup
                            if (currentGroup != null) {
                                val songsInGroup = uniqueGenres[currentGroup] ?: emptyList()
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

                                val heroArtwork = songsInGroup.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
                                val heroSubtitle = if (songsInGroup.size == 1) "1 song" else "${songsInGroup.size} songs"

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = OniSkin.spacing.screenHorizontal)
                                ) {
                                    LibraryCategoryHero(
                                        title = currentGroup,
                                        subtitle = heroSubtitle,
                                        artworkUri = heroArtwork,
                                        icon = Icons.Default.Category,
                                        modifier = Modifier.padding(vertical = OniSkin.spacing.md)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = OniSkin.spacing.md),
                                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (sortedSongsInGroup.isNotEmpty()) {
                                                    viewModel.playSong(sortedSongsInGroup.first(), sortedSongsInGroup)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = OniSkin.shapes.button,
                                            contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = OniSkin.colors.primary,
                                                contentColor = OniSkin.colors.onPrimary
                                            )
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                                            Text("Play All", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                if (sortedSongsInGroup.isNotEmpty()) {
                                                    val startSong = sortedSongsInGroup.random()
                                                    viewModel.playSong(startSong, sortedSongsInGroup.shuffled())
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = OniSkin.shapes.button,
                                            contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = OniSkin.colors.surfaceVariant,
                                                contentColor = OniSkin.colors.textPrimary
                                            )
                                        ) {
                                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                                            Text("Shuffle", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(
                                        color = OniSkin.colors.outline.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
                                    )

                                    SongsListView(
                                        songs = sortedSongsInGroup,
                                        viewModel = viewModel,
                                        sortBy = sortBy,
                                        isSortAscending = isSortAscending,
                                        onShowTrackMenu = { songForMenu = it },
                                        layoutMode = layoutMode
                                    )
                                }
                            } else {
                                GenresScreen(
                                    genres = genreUiModels,
                                    layoutMode = layoutMode,
                                    onGenreClick = { viewModel.setSelectedGroup(it.genre) },
                                    gridIndex = viewModel.groupedGridIndex,
                                    gridOffset = viewModel.groupedGridOffset,
                                    onGridScroll = { i, o -> viewModel.groupedGridIndex = i; viewModel.groupedGridOffset = o },
                                    listIndex = viewModel.groupedListIndex,
                                    listOffset = viewModel.groupedListOffset,
                                    onListScroll = { i, o -> viewModel.groupedListIndex = i; viewModel.groupedListOffset = o }
                                )
                            }
                        }
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
            layoutMode = layoutMode,
            onLayoutChange = { layoutMode = it },
            sortBy = sortBy,
            onSortByChange = { sortBy = it },
            isSortAscending = isSortAscending,
            onSortAscendingChange = { isSortAscending = it },
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
