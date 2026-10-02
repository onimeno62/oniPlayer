package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.data.preferences.LibraryPreferencesStore
import com.example.ui.library.AlbumDetailScreen
import com.example.ui.library.AlbumsScreen
import com.example.ui.library.ArtistDetailScreen
import com.example.ui.library.ArtistsScreen
import com.example.ui.library.DashboardCounts
import com.example.ui.library.FoldersScreen
import com.example.ui.library.GenresScreen
import com.example.ui.library.LibraryDashboardScreen
import com.example.ui.library.components.LibraryStickyHeader
import com.example.ui.library.components.SongsListView
import com.example.ui.library.components.libraryStatsLine
import com.example.ui.library.model.toAlbumUiModels
import com.example.ui.library.model.toArtistUiModels
import com.example.ui.library.model.toFolderUiModels
import com.example.ui.library.model.toGenreUiModels
import com.example.ui.viewmodel.MusicPlayerViewModel
import java.io.File
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Category indices (same as the legacy LibraryScreen categoryList)
private const val CAT_ALL = 0
private const val CAT_FOLDERS = 1
private const val CAT_ALBUMS = 2
private const val CAT_ARTISTS = 3
private const val CAT_GENRES = 4
private const val CAT_FAVORITES = 5
private const val CAT_MOST_PLAYED = 6
private const val CAT_RECENT = 7
private const val CAT_PLAYLISTS = 8
private const val CAT_SMART = 9
private const val CAT_BATCH = 10
private const val CAT_ADV_SEARCH = 11
private const val CAT_HIGH_RATED = 12
private const val CAT_NEVER = 13

private fun hostSortSongs(list: List<SongEntity>, sortBy: String, ascending: Boolean): List<SongEntity> {
    val result = when (sortBy) {
        "artist" -> list.sortedBy { it.customArtist ?: it.artist }
        "duration" -> list.sortedBy { it.duration }
        "play_count" -> list.sortedByDescending { it.playCount }
        else -> list.sortedBy { it.customTitle ?: it.title }
    }
    return if (!ascending && sortBy != "play_count") result.reversed()
    else if (ascending && sortBy == "play_count") result.reversed()
    else result
}

private fun hostFilterSongs(list: List<SongEntity>, query: String): List<SongEntity> =
    if (query.isBlank()) list else list.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
    }

/**
 * New library host. Replaces the legacy LibraryScreen composable as the Library tab body.
 *
 * - Category, list and group screens share ONE pinned [LibraryStickyHeader].
 * - Two shared, persistent layout styles (category lists / song lists) from [LibraryPreferencesStore].
 * - Sort order is persisted too.
 * - Search in the header is contextual: it filters whatever list is currently shown.
 * - Song lists use com.example.ui.library.components.SongsListView (swipe actions + fast scroller),
 *   which is imported explicitly and therefore shadows the legacy same-named function in this package.
 * - [onDashboardResumeVisibleChange] tells the app-level mini-player whether the dashboard's
 *   Continue Listening card is on screen (category screens always report false).
 *
 * Legacy helpers (PlaylistsView, dialogs, LibraryOptionsMenu, CategoryInfo, ...) are reused
 * from the old LibraryScreen.kt / LibraryExtensions.kt until they are migrated.
 */
@Composable
fun LibraryHostScreen(
    viewModel: MusicPlayerViewModel,
    onDashboardResumeVisibleChange: (Boolean) -> Unit = {}
) {
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
    val playedPlaylist by viewModel.playedActivePlaylist.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // ---- scan + permission -------------------------------------------------------------------
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.rescanLibrary()
            Toast.makeText(context, "Scanning local storage...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage permission is required to scan local music files.", Toast.LENGTH_LONG).show()
        }
    }
    val triggerScanWithPermission = {
        val has = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        if (has) {
            viewModel.rescanLibrary()
            Toast.makeText(context, "Scanning local storage...", Toast.LENGTH_SHORT).show()
        } else {
            launcher.launch(storagePermission)
        }
    }

    // ---- navigation state ---------------------------------------------------------------------
    val activeCategoryIndex by viewModel.activeCategoryIndex.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val activeSmartPlaylistType by viewModel.activeSmartPlaylistType.collectAsStateWithLifecycle()

    val goBack: () -> Unit = {
        when {
            selectedGroup != null -> viewModel.setSelectedGroup(null)
            activePlaylist != null -> viewModel.setActivePlaylist(null)
            activeSmartPlaylistType != null -> viewModel.setActiveSmartPlaylistType(null)
            else -> viewModel.setActiveCategoryIndex(null)
        }
    }
    BackHandler(enabled = activeCategoryIndex != null, onBack = goBack)

    // Contextual search starts fresh on every navigation step
    LaunchedEffect(activeCategoryIndex, selectedGroup, activePlaylist?.name, activeSmartPlaylistType) {
        viewModel.updateSearchQuery("")
    }

    // Inside a category the dashboard resume card is never visible -> mini-player may show.
    val latestResumeVisibleChange by rememberUpdatedState(onDashboardResumeVisibleChange)
    LaunchedEffect(activeCategoryIndex) {
        if (activeCategoryIndex != null) latestResumeVisibleChange(false)
    }

    // ---- persistent shared preferences ---------------------------------------------------------
    val categoryStyle by remember { LibraryPreferencesStore.categoryLayoutStyle(context) }
        .collectAsStateWithLifecycle(initialValue = LibraryPreferencesStore.STYLE_GRID)
    val songStyle by remember { LibraryPreferencesStore.songLayoutStyle(context) }
        .collectAsStateWithLifecycle(initialValue = LibraryPreferencesStore.STYLE_LIST)
    val sortBy by remember { LibraryPreferencesStore.sortBy(context) }
        .collectAsStateWithLifecycle(initialValue = "title")
    val sortAscendingRaw by remember { LibraryPreferencesStore.sortAscending(context) }
        .collectAsStateWithLifecycle(initialValue = "true")
    val isSortAscending = sortAscendingRaw != "false"

    var showOptionsMenu by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<SongEntity?>(null) }
    var songForMenu by remember { mutableStateOf<SongEntity?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var artistArtworkActionRequest by remember { mutableIntStateOf(0) }
    val playlistScope = rememberCoroutineScope()
    val importPlaylistLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            playlistScope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
                }
                if (content != null) {
                    val matchedIds = withContext(Dispatchers.Default) {
                        content.lines().asSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.mapNotNull { line ->
                            songs.firstOrNull { song -> song.filePath.endsWith(line.substringAfterLast('/')) || song.filePath.contains(line) || song.title.equals(line, ignoreCase = true) }?.id
                        }.distinct().toList()
                    }
                    viewModel.createPlaylist("Imported Playlist", matchedIds)
                    Toast.makeText(context, "Imported playlist (" + matchedIds.size + " matched tracks)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ---- derived data ------------------------------------------------------------------------
    val uniqueFolders = remember(songs) { songs.groupBy { File(it.filePath).parentFile?.name ?: "Internal" } }
    val uniqueAlbums = remember(songs) { songs.groupBy { it.album.ifEmpty { "Unknown Album" } } }
    val uniqueArtists = remember(songs) { songs.groupBy { it.artist.ifEmpty { "Unknown Artist" } } }
    val uniqueGenres = remember(songs) { songs.groupBy { it.genre.ifEmpty { "General" } } }
    val mostPlayedSongs = remember(songs) { songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount } }
    val recentlyAddedSongs = remember(songs) { songs.sortedByDescending { it.dateAdded } }
    val lastPlayedSong = remember(songs) { songs.filter { it.lastPlayedTimestamp > 0 }.maxByOrNull { it.lastPlayedTimestamp } }
    val recentlyPlayedSongs = remember(songs, currentSong) {
        val played = songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
        val current = currentSong
        when {
            played.isNotEmpty() -> played
            current != null -> listOf(current) + songs.filter { it.id != current.id }
            else -> songs
        }
    }
    val highRatedSongs = remember(songs) { songs.filter { it.rating >= 4 }.sortedByDescending { it.rating } }
    val neverPlayedSongs = remember(songs) { songs.filter { it.playCount == 0 } }

    val albumUiModels = remember(songs) { songs.toAlbumUiModels() }
    val artistUiModels = remember(songs, artistSummaries) { songs.toArtistUiModels(artistSummaries) }
    val folderUiModels = remember(songs) { songs.toFolderUiModels() }
    val genreUiModels = remember(songs) { songs.toGenreUiModels() }

    val categoryList = listOf(
        CategoryInfo("All Songs", "${songs.size} songs", Icons.Default.MusicNote, Color(0x1F9C27B0), Color(0xFF9C27B0)),
        CategoryInfo("Folders", "${uniqueFolders.size} folders", Icons.Default.Folder, Color(0x1F2196F3), Color(0xFF2196F3)),
        CategoryInfo("Albums", "${uniqueAlbums.size} albums", Icons.Default.Album, Color(0x1FE91E63), Color(0xFFE91E63)),
        CategoryInfo("Artists", "${uniqueArtists.size} artists", Icons.Default.Person, Color(0x1F009688), Color(0xFF009688)),
        CategoryInfo("Genres", "${uniqueGenres.size} genres", Icons.Default.Category, Color(0x1FFF9800), Color(0xFFFF9800)),
        CategoryInfo("Favorites", "${favorites.size} favorite songs", Icons.Filled.Favorite, Color(0x1FF44336), Color(0xFFF44336)),
        CategoryInfo("Most Played", "${mostPlayedSongs.size} played", Icons.Default.Whatshot, Color(0x1FFFC107), Color(0xFFFFB300)),
        CategoryInfo("Recently Added", "${recentlyAddedSongs.size} added", Icons.Default.Schedule, Color(0x1F4CAF50), Color(0xFF4CAF50)),
        CategoryInfo("Playlists", "${playlists.size} playlists", Icons.AutoMirrored.Filled.QueueMusic, Color(0x1F03A9F4), Color(0xFF03A9F4)),
        CategoryInfo("Smart Playlists", "4 dynamic lists", Icons.Default.AutoAwesome, Color(0x1F673AB7), Color(0xFF673AB7)),
        CategoryInfo("Batch Tag Editor", "Multi-edit tags", Icons.Default.EditNote, Color(0x1F3F51B5), Color(0xFF3F51B5)),
        CategoryInfo("Advanced Search", "Filters & fields", Icons.Default.ManageSearch, Color(0x1F607D8B), Color(0xFF607D8B)),
        CategoryInfo("High Rated", "${highRatedSongs.size} tracks", Icons.Default.Star, Color(0x1FFFC107), Color(0xFFFFB300)),
        CategoryInfo("Never Played", "${neverPlayedSongs.size} tracks", Icons.Default.Explore, Color(0x1F009688), Color(0xFF009688))
    )

    // Songs belonging to the current screen (static; used for header stats and Play All / Shuffle)
    val baseSongs: List<SongEntity> = remember(
        songs, favorites, mostPlayedSongs, recentlyAddedSongs, highRatedSongs, neverPlayedSongs,
        uniqueFolders, uniqueGenres, activeCategoryIndex, selectedGroup
    ) {
        val group = selectedGroup
        when (activeCategoryIndex) {
            CAT_ALL -> songs
            CAT_FAVORITES -> favorites
            CAT_MOST_PLAYED -> mostPlayedSongs
            CAT_RECENT -> recentlyAddedSongs
            CAT_HIGH_RATED -> highRatedSongs
            CAT_NEVER -> neverPlayedSongs
            CAT_FOLDERS -> if (group != null) uniqueFolders[group] ?: emptyList() else songs
            CAT_GENRES -> if (group != null) uniqueGenres[group] ?: emptyList() else songs
            CAT_ALBUMS -> if (group != null) songs.filter {
                "${it.displayAlbum.ifBlank { "Unknown Album" }}|${it.displayAlbumArtist}" == group
            } else songs
            CAT_ARTISTS -> if (group != null) songs.filter {
                it.displayArtist.ifBlank { "Unknown Artist" } == group
            } else songs
            else -> emptyList()
        }
    }

    // Songs currently visible in a song list: contextual search + persisted sort.
    // Detail/header playback uses the same authoritative song set as the body.
    val playlistSongs = remember(activePlaylist, songs) {
        val playlist = activePlaylist ?: return@remember emptyList()
        val ids = runCatching {
            val json = JSONArray(playlist.songIdsJson)
            List(json.length()) { index -> json.getString(index) }
        }.getOrDefault(emptyList())
        val byId = songs.associateBy { it.id }
        ids.mapNotNull(byId::get)
    }
    val smartPlaylistSongs = remember(activeSmartPlaylistType, songs, favorites) {
        when (activeSmartPlaylistType) {
            "Recently Played" -> songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
            "Favorites" -> favorites
            "High Rating" -> songs.filter { it.rating >= 4 }.sortedByDescending { it.rating }
            "Never Played" -> songs.filter { it.playCount == 0 }
            else -> emptyList()
        }
    }
    val headerBaseSongs = when {
        activePlaylist != null -> playlistSongs
        activeSmartPlaylistType != null -> smartPlaylistSongs
        activeCategoryIndex == CAT_PLAYLISTS || activeCategoryIndex == CAT_SMART -> emptyList()
        else -> baseSongs
    }
    val visibleSongs: List<SongEntity> = remember(headerBaseSongs, searchQuery, sortBy, isSortAscending, activeCategoryIndex, activePlaylist, activeSmartPlaylistType) {
        val filtered = hostFilterSongs(headerBaseSongs, searchQuery)
        if (activeCategoryIndex == CAT_MOST_PLAYED || activeCategoryIndex == CAT_RECENT || activeSmartPlaylistType == "Recently Played") filtered
        else hostSortSongs(filtered, sortBy, isSortAscending)
    }

    // ---- dashboard ----------------------------------------------------------------------------
    val index = activeCategoryIndex
    if (index == null) {
        val dashboardSorted = remember(songs, searchQuery, sortBy, isSortAscending) {
            hostSortSongs(hostFilterSongs(songs, searchQuery), sortBy, isSortAscending)
        }

        // "Jump back in": prefer the playlist the current song was started from, else its album.
        val jumpBackSong = currentSong ?: lastPlayedSong
        val jumpBackAlbum = remember(jumpBackSong?.id, albumUiModels) {
            jumpBackSong?.let { s ->
                val key = "${s.displayAlbum.ifBlank { "Unknown Album" }}|${s.displayAlbumArtist}"
                albumUiModels.find { it.albumKey == key }?.takeIf { it.title.isNotBlank() && it.title != "Unknown Album" }
            }
        }
        val jumpPlaylist = playedPlaylist?.takeIf { p -> playlists.any { it.id == p.id } }
        val jumpBackLabel = jumpPlaylist?.let { "Playlist: ${it.name}" } ?: jumpBackAlbum?.let { "Album: ${it.title}" }
        val onJumpBack: () -> Unit = {
            val playlist = jumpPlaylist
            val album = jumpBackAlbum
            if (playlist != null) {
                viewModel.setActiveCategoryIndex(CAT_PLAYLISTS)
                viewModel.setActivePlaylist(playlist)
            } else if (album != null) {
                viewModel.setActiveCategoryIndex(CAT_ALBUMS)
                viewModel.setSelectedGroup(album.albumKey)
            }
        }

        val beatEnergy by viewModel.audioEngine.beatEnergy.collectAsStateWithLifecycle()
        val nowPlayingEffect by viewModel.nowPlayingEffect.collectAsStateWithLifecycle()

        LibraryDashboardScreen(
            songs = songs,
            sortedSongs = dashboardSorted,
            currentSong = currentSong,
            isPlaying = isPlaying,
            lastPlayedSong = lastPlayedSong,
            recentlyPlayedSongs = recentlyPlayedSongs,
            mostPlayedSongs = mostPlayedSongs,
            recentlyAddedSongs = recentlyAddedSongs,
            favoriteSongs = favorites,
            highRatedSongs = highRatedSongs,
            neverPlayedSongs = neverPlayedSongs,
            counts = DashboardCounts(
                albums = albumUiModels.size,
                artists = artistUiModels.size,
                folders = folderUiModels.size,
                genres = genreUiModels.size,
                playlists = playlists.size
            ),
            searchQuery = searchQuery,
            isScanning = isScanning,
            showOptionsMenu = { showOptionsMenu = true },
            onRescan = triggerScanWithPermission,
            onOpenSearch = { viewModel.selectTab(2) },
            onSelectCategory = { viewModel.setActiveCategoryIndex(it) },
            onPlaySong = { song, list -> viewModel.playSong(song, list) },
            onShowTrackMenu = { songForMenu = it },
            onPlayNext = { song ->
                viewModel.playNext(song)
                Toast.makeText(context, "Playing next: ${song.displayTitle}", Toast.LENGTH_SHORT).show()
            },
            onAddToQueue = { song ->
                viewModel.addToQueue(song)
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            },
            onToggleFavorite = { song -> viewModel.toggleFavorite(song.id) },
            position = position,
            duration = duration,
            isPreparing = isPreparing,
            beatEnergy = beatEnergy,
            nowPlayingEffect = nowPlayingEffect,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onReplay = {
                viewModel.seekTo(0L)
                viewModel.resumePlayback()
            },
            onOpenPlayer = { viewModel.selectTab(1) },
            jumpBackLabel = jumpBackLabel,
            onJumpBack = onJumpBack,
            onResumeCardVisibleChange = onDashboardResumeVisibleChange
        )
    } else {
        val group = selectedGroup
        val isAlbumDetail = index == CAT_ALBUMS && group != null
        val isArtistDetail = index == CAT_ARTISTS && group != null
        val isDetailBody = isAlbumDetail || isArtistDetail
        val isSongScreen = LibraryPreferencesStore.isSongCategory(index) || (group != null && !isDetailBody)

        // header content
        val categoryTitle = categoryList.getOrNull(index)?.title ?: "Library"
        val activeAlbum = if (index == CAT_ALBUMS && group != null) albumUiModels.find { it.albumKey == group } else null
        val title = when {
            group != null -> when (index) {
                CAT_ALBUMS -> albumUiModels.find { it.albumKey == group }?.title ?: group
                CAT_ARTISTS -> artistUiModels.find { it.artistKey == group }?.name ?: group
                else -> group
            }
            activePlaylist != null -> activePlaylist!!.name
            activeSmartPlaylistType != null -> activeSmartPlaylistType!!
            else -> categoryTitle
        }
        val overline = if (group != null || activePlaylist != null || activeSmartPlaylistType != null) categoryTitle else null
        val extra = if (group != null) null else when (index) {
            CAT_FOLDERS -> "${uniqueFolders.size} folders"
            CAT_ALBUMS -> "${albumUiModels.size} albums"
            CAT_ARTISTS -> "${artistUiModels.size} artists"
            CAT_GENRES -> "${uniqueGenres.size} genres"
            else -> null
        }
        val statsText = when (index) {
            CAT_PLAYLISTS -> if (activePlaylist != null) "Playlist" else "${playlists.size} playlists"
            CAT_SMART -> if (activeSmartPlaylistType != null) "Smart playlist" else "4 dynamic lists"
            CAT_BATCH -> "Multi-edit tags"
            CAT_ADV_SEARCH -> "Filters & fields"
            else -> libraryStatsLine(baseSongs.size, baseSongs.sumOf { it.duration.toLong() }, extra)
        }
        val headerArtwork = remember(baseSongs) {
            baseSongs.firstNotNullOfOrNull { it.albumArtUri?.takeIf { uri -> uri.isNotBlank() } }
        }

        // collapse header on scroll down, expand on scroll up (works with any child scrollable)
        var scrolledDown by remember(index, group) { mutableStateOf(false) }
        val nestedConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -12f) scrolledDown = true
                    else if (available.y > 12f) scrolledDown = false
                    return Offset.Zero
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            key(index, group, activePlaylist?.name, activeSmartPlaylistType) {
                LibraryStickyHeader(
                    title = title,
                    icon = categoryList.getOrNull(index)?.icon ?: Icons.Default.LibraryMusic,
                    statsText = statsText,
                    artworkUri = headerArtwork,
                    overline = overline,
                    collapsed = scrolledDown || isDetailBody,
                    actionsEnabled = visibleSongs.isNotEmpty(),
                    onBack = goBack,
                    onPlayAll = { if (visibleSongs.isNotEmpty()) viewModel.playSong(visibleSongs.first(), visibleSongs) },
                    onShuffle = {
                        if (visibleSongs.isNotEmpty()) {
                            val shuffled = visibleSongs.shuffled()
                            viewModel.playSong(shuffled.first(), shuffled)
                        }
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onOpenMenu = { showOptionsMenu = true },
                    onArtworkAction = if (index == CAT_ARTISTS && group != null) {
                        { artistArtworkActionRequest++ }
                    } else {
                        null
                    },
                    playlistActions = index == CAT_PLAYLISTS && activePlaylist == null,
                    onNewPlaylist = { showNewPlaylistDialog = true },
                    onImportPlaylist = { importPlaylistLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/octet-stream", "text/plain")) }
                )
            }

            Box(Modifier.fillMaxWidth().weight(1f).nestedScroll(nestedConnection)) {
                val onMenu: (SongEntity) -> Unit = { songForMenu = it }
                when (index) {
                    CAT_ALL, CAT_FAVORITES, CAT_MOST_PLAYED, CAT_RECENT, CAT_HIGH_RATED, CAT_NEVER ->
                        SongsListView(visibleSongs, viewModel, sortBy, isSortAscending, onMenu, songStyle)

                    CAT_FOLDERS -> if (group != null) {
                        SongsListView(visibleSongs, viewModel, sortBy, isSortAscending, onMenu, songStyle)
                    } else {
                        FoldersScreen(
                            folders = folderUiModels.filter { searchQuery.isBlank() || it.displayName.contains(searchQuery, true) },
                            layoutMode = categoryStyle,
                            onFolderClick = { viewModel.setSelectedGroup(it.displayName) },
                            gridIndex = viewModel.groupedGridIndex,
                            gridOffset = viewModel.groupedGridOffset,
                            onGridScroll = { i, o -> viewModel.groupedGridIndex = i; viewModel.groupedGridOffset = o },
                            listIndex = viewModel.groupedListIndex,
                            listOffset = viewModel.groupedListOffset,
                            onListScroll = { i, o -> viewModel.groupedListIndex = i; viewModel.groupedListOffset = o }
                        )
                    }

                    CAT_ALBUMS -> if (group != null) {
                        val album = albumUiModels.find { it.albumKey == group }
                        if (album != null) {
                            AlbumDetailScreen(
                                album, baseSongs, currentSong, isPlaying,
                                { if (baseSongs.isNotEmpty()) viewModel.playSong(baseSongs.first(), baseSongs) },
                                { if (baseSongs.isNotEmpty()) viewModel.playSong(baseSongs.random(), baseSongs) },
                                { viewModel.playSong(it, baseSongs) },
                                onMenu
                            )
                        } else {
                            LaunchedEffect(group) { viewModel.setSelectedGroup(null) }
                        }
                    } else {
                        AlbumsScreen(
                            albums = albumUiModels.filter { searchQuery.isBlank() || it.title.contains(searchQuery, true) },
                            layoutMode = categoryStyle,
                            onAlbumClick = { viewModel.setSelectedGroup(it.albumKey) },
                            gridIndex = viewModel.albumsGridIndex,
                            gridOffset = viewModel.albumsGridOffset,
                            onGridScroll = { i, o -> viewModel.albumsGridIndex = i; viewModel.albumsGridOffset = o },
                            listIndex = viewModel.albumsListIndex,
                            listOffset = viewModel.albumsListOffset,
                            onListScroll = { i, o -> viewModel.albumsListIndex = i; viewModel.albumsListOffset = o }
                        )
                    }

                    CAT_ARTISTS -> if (group != null) {
                        val artist = artistUiModels.find { it.artistKey == group }
                        if (artist != null) {
                            val albumsByArtist = remember(albumUiModels, baseSongs) {
                                val keys = baseSongs.map { "${it.displayAlbum.ifBlank { "Unknown Album" }}|${it.displayAlbumArtist}" }.toSet()
                                albumUiModels.filter { it.albumKey in keys }
                            }
                            ArtistDetailScreen(
                                artist, albumsByArtist, baseSongs, currentSong, isPlaying,
                                { if (baseSongs.isNotEmpty()) viewModel.playSong(baseSongs.first(), baseSongs) },
                                { if (baseSongs.isNotEmpty()) viewModel.playSong(baseSongs.random(), baseSongs) },
                                { viewModel.playSong(it, baseSongs) },
                                onMenu, categoryStyle, viewModel, artistArtworkActionRequest, { artistArtworkActionRequest = 0 }
                            )
                        } else {
                            LaunchedEffect(Unit) { viewModel.setSelectedGroup(null) }
                        }
                    } else {
                        ArtistsScreen(
                            artists = artistUiModels.filter { searchQuery.isBlank() || it.name.contains(searchQuery, true) },
                            layoutMode = categoryStyle,
                            onArtistClick = { viewModel.setSelectedGroup(it.artistKey) },
                            gridIndex = viewModel.artistsGridIndex,
                            gridOffset = viewModel.artistsGridOffset,
                            onGridScroll = { i, o -> viewModel.artistsGridIndex = i; viewModel.artistsGridOffset = o },
                            listIndex = viewModel.artistsListIndex,
                            listOffset = viewModel.artistsListOffset,
                            onListScroll = { i, o -> viewModel.artistsListIndex = i; viewModel.artistsListOffset = o }
                        )
                    }

                    CAT_GENRES -> if (group != null) {
                        SongsListView(visibleSongs, viewModel, sortBy, isSortAscending, onMenu, songStyle)
                    } else {
                        GenresScreen(
                            genres = genreUiModels.filter { searchQuery.isBlank() || it.genre.contains(searchQuery, true) },
                            layoutMode = categoryStyle,
                            onGenreClick = { viewModel.setSelectedGroup(it.genre) },
                            gridIndex = viewModel.groupedGridIndex,
                            gridOffset = viewModel.groupedGridOffset,
                            onGridScroll = { i, o -> viewModel.groupedGridIndex = i; viewModel.groupedGridOffset = o },
                            listIndex = viewModel.groupedListIndex,
                            listOffset = viewModel.groupedListOffset,
                            onListScroll = { i, o -> viewModel.groupedListIndex = i; viewModel.groupedListOffset = o }
                        )
                    }

                    CAT_PLAYLISTS -> PlaylistsView(
                        songs,
                        playlists.filter { searchQuery.isBlank() || it.name.contains(searchQuery, true) },
                        viewModel, activePlaylist, { viewModel.setActivePlaylist(it) }, onMenu, categoryStyle
                    )
                    CAT_SMART -> SmartPlaylistsView(songs, favorites, viewModel, activeSmartPlaylistType, { viewModel.setActiveSmartPlaylistType(it) }, onMenu)
                    CAT_BATCH -> BatchTagEditorView(songs, viewModel)
                    CAT_ADV_SEARCH -> AdvancedSearchView(songs, viewModel, songStyle, onMenu)
                }
            }
        }

        // isSongScreen decides which shared style the Library menu edits
        if (showOptionsMenu) {
            LibraryOptionsMenu(
                layoutMode = if (isSongScreen) songStyle else categoryStyle,
                onLayoutChange = {
                    if (isSongScreen) LibraryPreferencesStore.setSongLayoutStyle(context, it)
                    else LibraryPreferencesStore.setCategoryLayoutStyle(context, it)
                },
                sortBy = sortBy,
                onSortByChange = { LibraryPreferencesStore.setSortBy(context, it) },
                isSortAscending = isSortAscending,
                onSortAscendingChange = { LibraryPreferencesStore.setSortAscending(context, it) },
                onDismiss = { showOptionsMenu = false }
            )
        }
    }

    // Dashboard menu (index == null) + shared dialogs
    if (index == null && showOptionsMenu) {
        LibraryOptionsMenu(
            layoutMode = categoryStyle,
            onLayoutChange = { LibraryPreferencesStore.setCategoryLayoutStyle(context, it) },
            sortBy = sortBy,
            onSortByChange = { LibraryPreferencesStore.setSortBy(context, it) },
            isSortAscending = isSortAscending,
            onSortAscendingChange = { LibraryPreferencesStore.setSortAscending(context, it) },
            onDismiss = { showOptionsMenu = false }
        )
    }

    if (showNewPlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("New Playlist") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist name") }, singleLine = true) },
            confirmButton = {
                Button(onClick = { val trimmed = name.trim(); if (trimmed.isNotBlank()) { viewModel.createPlaylist(trimmed); showNewPlaylistDialog = false } }, enabled = name.trim().isNotBlank()) { Text("Create") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancel") } }
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
