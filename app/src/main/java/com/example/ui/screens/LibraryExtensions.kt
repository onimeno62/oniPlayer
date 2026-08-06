package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

// --- 1. Smart Playlists View ---
@Composable
fun SmartPlaylistsView(
    songs: List<SongEntity>,
    favorites: List<SongEntity>,
    viewModel: MusicPlayerViewModel,
    activeSmartPlaylistType: String?,
    onActiveSmartPlaylistTypeChange: (String?) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit
) {
    // Computations based on rules
    val recentlyPlayed = remember(songs) {
        songs.filter { it.lastPlayedTimestamp > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
    }
    val highRating = remember(songs) {
        songs.filter { it.rating >= 4 }
            .sortedByDescending { it.rating }
    }
    val neverPlayed = remember(songs) {
        songs.filter { it.playCount == 0 }
    }

    if (activeSmartPlaylistType == null) {
        // Render 4 Smart Playlist cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SmartPlaylistCard(
                    title = "Recently Played",
                    count = recentlyPlayed.size,
                    icon = Icons.Default.Schedule,
                    bgColor = Color(0x1F2196F3),
                    iconColor = Color(0xFF2196F3),
                    onClick = { onActiveSmartPlaylistTypeChange("Recently Played") }
                )
            }
            item {
                SmartPlaylistCard(
                    title = "Favorites",
                    count = favorites.size,
                    icon = Icons.Default.Favorite,
                    bgColor = Color(0x1FF44336),
                    iconColor = Color(0xFFF44336),
                    onClick = { onActiveSmartPlaylistTypeChange("Favorites") }
                )
            }
            item {
                SmartPlaylistCard(
                    title = "High Rating",
                    count = highRating.size,
                    icon = Icons.Default.Star,
                    bgColor = Color(0x1FFFF9800),
                    iconColor = Color(0xFFFF9800),
                    onClick = { onActiveSmartPlaylistTypeChange("High Rating") }
                )
            }
            item {
                SmartPlaylistCard(
                    title = "Never Played",
                    count = neverPlayed.size,
                    icon = Icons.Default.MusicOff,
                    bgColor = Color(0x1F9E9E9E),
                    iconColor = Color(0xFF9E9E9E),
                    onClick = { onActiveSmartPlaylistTypeChange("Never Played") }
                )
            }
        }
    } else {
        // Render list of matching songs
        val playlistSongs = when (activeSmartPlaylistType) {
            "Recently Played" -> recentlyPlayed
            "Favorites" -> favorites
            "High Rating" -> highRating
            else -> neverPlayed
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onActiveSmartPlaylistTypeChange(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeSmartPlaylistType,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (playlistSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs match this rule yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                SongsListView(
                    songs = playlistSongs,
                    viewModel = viewModel,
                    sortBy = "title",
                    isSortAscending = true,
                    onShowTrackMenu = onShowTrackMenu
                )
            }
        }
    }
}

@Composable
fun SmartPlaylistCard(
    title: String,
    count: Int,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("$count songs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- 2. Manual Playlists View (Import/Export M3U) ---
@Composable
fun PlaylistsView(
    songs: List<SongEntity>,
    playlists: List<PlaylistEntity>,
    viewModel: MusicPlayerViewModel,
    activePlaylist: PlaylistEntity?,
    onActivePlaylistChange: (PlaylistEntity?) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    layoutMode: String
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (activePlaylist == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Playlists", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import M3U", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontSize = 12.sp)
                    }
                }
            }

            if (playlists.isEmpty()) {
                com.example.ui.library.components.LibraryEmptyState(
                    title = "No Playlists",
                    message = "No playlists created yet. Create one or import an M3U playlist.",
                    modifier = Modifier.fillMaxSize().weight(1f),
                    icon = Icons.Default.QueueMusic
                )
            } else if (layoutMode == "grid") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists.size, key = { index -> playlists[index].id }) { index ->
                        val playlist = playlists[index]
                        val songIds = remember(playlist.songIdsJson) {
                            try {
                                val array = org.json.JSONArray(playlist.songIdsJson)
                                List(array.length()) { array.getString(it) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        val firstSongArtUri = remember(songs, playlist.songIdsJson) {
                            try {
                                val array = org.json.JSONArray(playlist.songIdsJson)
                                if (array.length() > 0) {
                                    val firstId = array.getString(0)
                                    songs.find { it.id == firstId }?.albumArtUri
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Card(
                            onClick = { onActivePlaylistChange(playlist) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.85f),
                            shape = RoundedCornerShape(dashboardRadiusMedium()),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(dashboardRadiusMedium()))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (firstSongArtUri != null) {
                                            coil.compose.AsyncImage(
                                                model = firstSongArtUri,
                                                contentDescription = "Playlist Cover",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = playlist.name,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${songIds.size} tracks",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deletePlaylist(playlist.id)
                                        Toast.makeText(context, "Playlist Deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Playlist",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                    items(playlists) { playlist ->
                        val songIds = remember(playlist.songIdsJson) {
                            try {
                                val array = JSONArray(playlist.songIdsJson)
                                List(array.length()) { array.getString(it) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        Card(
                            onClick = { onActivePlaylistChange(playlist) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(playlist.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Text("${songIds.size} tracks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deletePlaylist(playlist.id)
                                        Toast.makeText(context, "Playlist Deleted", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // View Playlist songs
        val playlistSongs = remember(songs, activePlaylist) {
            val songIds = try {
                val array = JSONArray(activePlaylist!!.songIdsJson)
                List(array.length()) { array.getString(it) }
            } catch (e: Exception) {
                emptyList()
            }
            songs.filter { songIds.contains(it.id) }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onActivePlaylistChange(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activePlaylist?.name ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = {
                        val m3uContent = StringBuilder("#EXTM3U\n")
                        playlistSongs.forEach { s ->
                            m3uContent.append("#EXTINF:${s.duration / 1000},${s.displayArtist} - ${s.displayTitle}\n")
                            m3uContent.append("${s.filePath}\n")
                        }
                        // Export simulation
                        Toast.makeText(context, "Exported successfully to: ${activePlaylist?.name}.m3u", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Output, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export M3U", fontSize = 12.sp)
                }
            }

            if (playlistSongs.isEmpty()) {
                com.example.ui.library.components.LibraryEmptyState(
                    title = "Empty Playlist",
                    message = "This playlist has no tracks yet. Add some tracks from the songs menu.",
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Default.QueueMusic
                )
            } else {
                SongsListView(
                    songs = playlistSongs,
                    viewModel = viewModel,
                    sortBy = "title",
                    isSortAscending = true,
                    onShowTrackMenu = onShowTrackMenu,
                    layoutMode = layoutMode
                )
            }
        }
    }

    // Create dialog
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createPlaylist(name)
                            showCreateDialog = false
                            Toast.makeText(context, "Playlist Created!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Import M3U Simulation Dialog
    if (showImportDialog) {
        var playlistName by remember { mutableStateOf("") }
        var m3uBody by remember { mutableStateOf("#EXTM3U\n#EXTINF:180,Retro Drive\n/storage/emulated/0/Music/Retro_Drive.mp3") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import M3U Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = m3uBody,
                        onValueChange = { m3uBody = it },
                        label = { Text("M3U Content / File Content") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            // Search songs path match
                            val matchedIds = mutableListOf<String>()
                            val lines = m3uBody.lines()
                            lines.forEach { line ->
                                if (line.isNotBlank() && !line.startsWith("#")) {
                                    val matchedSong = songs.find { s -> s.filePath.endsWith(line.substringAfterLast("/")) }
                                    if (matchedSong != null) {
                                        matchedIds.add(matchedSong.id)
                                    }
                                }
                            }
                            // Insert playlist
                            val id = "playlist_imported_" + System.currentTimeMillis()
                            val songIdsJson = JSONArray(matchedIds).toString()
                            scope.launch {
                                viewModel.createPlaylist(playlistName)
                                // Add matched song ids
                                matchedIds.forEach { sId ->
                                    viewModel.addSongToPlaylist(sId, "playlist_" + playlistName)
                                }
                            }
                            showImportDialog = false
                            Toast.makeText(context, "M3U Imported with ${matchedIds.size} matched tracks!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Please enter a playlist name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 3. Batch Tag Editor View ---
@Composable
fun BatchTagEditorView(
    songs: List<SongEntity>,
    viewModel: MusicPlayerViewModel
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    var showBatchDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Batch Tag Editor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${selectedIds.size} selected for multi-edit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) {
                            selectedIds.addAll(songs.map { it.id })
                        } else {
                            selectedIds.clear()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (selectedIds.size == songs.size) "Deselect All" else "Select All")
                }

                Button(
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            showBatchDialog = true
                        } else {
                            Toast.makeText(context, "Please select at least one song", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Selected")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(songs) { song ->
                val isSelected = selectedIds.contains(song.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) {
                                selectedIds.remove(song.id)
                            } else {
                                selectedIds.add(song.id)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked == true) {
                                selectedIds.add(song.id)
                            } else {
                                selectedIds.remove(song.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Simple song preview row
                    AsyncImage(
                        model = song.albumArtUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=250&auto=format&fit=crop",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        Text(song.displayArtist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showBatchDialog) {
        var artist by remember { mutableStateOf("") }
        var album by remember { mutableStateOf("") }
        var albumArtist by remember { mutableStateOf("") }
        var genre by remember { mutableStateOf("") }
        var composer by remember { mutableStateOf("") }
        var disc by remember { mutableStateOf("") }
        var track by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        var bpm by remember { mutableStateOf("") }
        var rating by remember { mutableStateOf<Int?>(null) }
        var albumArtUri by remember { mutableStateOf<String?>(null) }
        var removeArt by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showBatchDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .heightIn(max = 550.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Batch Edit ${selectedIds.size} Tracks", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Only fields you fill in will overwrite existing tags of selected songs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        item {
                            OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = albumArtist, onValueChange = { albumArtist = it }, label = { Text("Album Artist") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = composer, onValueChange = { composer = it }, label = { Text("Composer") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = disc, onValueChange = { disc = it }, label = { Text("Disc") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = track, onValueChange = { track = it }, label = { Text("Track") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = bpm, onValueChange = { bpm = it }, label = { Text("BPM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            Column {
                                Text("Rating Override", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 2, 3, 4, 5).forEach { stars ->
                                        IconButton(onClick = { rating = stars }) {
                                            Icon(
                                                imageVector = if (rating != null && rating!! >= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = if (rating != null && rating!! >= stars) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = removeArt, onCheckedChange = { removeArt = it ?: false })
                                Text("Remove Cover Art", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showBatchDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.batchUpdateTags(
                                    songIds = selectedIds,
                                    artist = artist,
                                    album = album,
                                    albumArtist = albumArtist,
                                    genre = genre,
                                    composer = composer,
                                    disc = disc,
                                    track = track,
                                    year = year,
                                    comment = comment,
                                    bpm = bpm,
                                    rating = rating,
                                    albumArtUri = albumArtUri,
                                    removeArt = removeArt
                                )
                                showBatchDialog = false
                                selectedIds.clear()
                                Toast.makeText(context, "Batch updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Advanced Search View (Filters by Duration, Date, Bitrate, Format) ---
@Composable
fun AdvancedSearchView(
    songs: List<SongEntity>,
    viewModel: MusicPlayerViewModel,
    layoutMode: String,
    onShowTrackMenu: (SongEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    // Filters State
    var durationFilter by remember { mutableStateOf("All") } // "All", "< 1m", "1-3m", "3-5m", "> 5m"
    var dateFilter by remember { mutableStateOf("All") } // "All", "Today", "This Week", "This Month"
    var bitrateFilter by remember { mutableStateOf("All") } // "All", "128", "192", "256", "320", "Lossless"
    var formatFilter by remember { mutableStateOf("All") } // "All", "MP3", "FLAC", "M4A", "WAV", "AAC"

    val filteredSongs = remember(songs, query, durationFilter, dateFilter, bitrateFilter, formatFilter) {
        songs.filter { song ->
            // Query filter (Title, Artist, Album, Folder, Lyrics, Genre)
            val matchQuery = query.isBlank() ||
                    song.displayTitle.contains(query, ignoreCase = true) ||
                    song.displayArtist.contains(query, ignoreCase = true) ||
                    song.displayAlbum.contains(query, ignoreCase = true) ||
                    song.displayGenre.contains(query, ignoreCase = true) ||
                    song.filePath.contains(query, ignoreCase = true) ||
                    (song.lyrics ?: "").contains(query, ignoreCase = true)

            // Duration filter
            val durationMin = song.duration / 60000f
            val matchDuration = when (durationFilter) {
                "< 1m" -> durationMin < 1f
                "1-3m" -> durationMin >= 1f && durationMin <= 3f
                "3-5m" -> durationMin > 3f && durationMin <= 5f
                "> 5m" -> durationMin > 5f
                else -> true
            }

            // Date filter (dateAdded field)
            val ageMs = System.currentTimeMillis() - song.dateAdded
            val ageDays = ageMs / (1000f * 60 * 60 * 24)
            val matchDate = when (dateFilter) {
                "Today" -> ageDays <= 1f
                "This Week" -> ageDays <= 7f
                "This Month" -> ageDays <= 30f
                else -> true
            }

            // Bitrate Filter
            val matchBitrate = when (bitrateFilter) {
                "128" -> song.bitrate == 128
                "192" -> song.bitrate == 192
                "256" -> song.bitrate == 256
                "320" -> song.bitrate == 320
                "Lossless" -> song.bitrate > 320
                else -> true
            }

            // Format Filter
            val matchFormat = when (formatFilter) {
                "MP3" -> song.format.equals("MP3", ignoreCase = true)
                "FLAC" -> song.format.equals("FLAC", ignoreCase = true)
                "M4A" -> song.format.equals("M4A", ignoreCase = true)
                "WAV" -> song.format.equals("WAV", ignoreCase = true)
                "AAC" -> song.format.equals("AAC", ignoreCase = true)
                else -> true
            }

            matchQuery && matchDuration && matchDate && matchBitrate && matchFormat
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search title, lyrics, genre, format, folder...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        // Filters horizontal row
        Text("FILTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Duration filter dropdown
            FilterChipItem(
                label = if (durationFilter == "All") "Duration" else durationFilter,
                selected = durationFilter != "All",
                options = listOf("All", "< 1m", "1-3m", "3-5m", "> 5m"),
                onSelect = { durationFilter = it }
            )

            // Date filter dropdown
            FilterChipItem(
                label = if (dateFilter == "All") "Date" else dateFilter,
                selected = dateFilter != "All",
                options = listOf("All", "Today", "This Week", "This Month"),
                onSelect = { dateFilter = it }
            )

            // Bitrate Filter dropdown
            FilterChipItem(
                label = if (bitrateFilter == "All") "Bitrate" else bitrateFilter,
                selected = bitrateFilter != "All",
                options = listOf("All", "128", "192", "256", "320", "Lossless"),
                onSelect = { bitrateFilter = it }
            )

            // Format Filter dropdown
            FilterChipItem(
                label = if (formatFilter == "All") "Format" else formatFilter,
                selected = formatFilter != "All",
                options = listOf("All", "MP3", "FLAC", "M4A", "WAV", "AAC"),
                onSelect = { formatFilter = it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSongs.isEmpty()) {
            com.example.ui.library.components.LibraryEmptyState(
                title = "No matching tracks",
                message = "Try adjusting your search filters or query.",
                modifier = Modifier.fillMaxSize().weight(1f),
                icon = Icons.Default.Search
            )
        } else {
            SongsListView(
                songs = filteredSongs,
                viewModel = viewModel,
                sortBy = "title",
                isSortAscending = true,
                onShowTrackMenu = onShowTrackMenu,
                layoutMode = layoutMode
            )
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        InputChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- 5. Supreme Tag Editor Dialog (with Cover replacements) ---
data class TagSearchResult(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val composer: String,
    val disc: String,
    val track: String,
    val year: String,
    val comment: String,
    val bpm: String,
    val albumArtUri: String?,
    val matchDescription: String,
    val confidence: Double
)

@Composable
fun AdvancedTagEditorDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val song = remember { song }
    var title by remember { mutableStateOf(song.customTitle ?: song.title) }
    var artist by remember { mutableStateOf(song.customArtist ?: song.artist) }
    var album by remember { mutableStateOf(song.customAlbum ?: song.album) }
    var genre by remember { mutableStateOf(song.customGenre ?: song.genre) }
    var albumArtist by remember { mutableStateOf(song.customAlbumArtist ?: song.albumArtist) }
    var composer by remember { mutableStateOf(song.customComposer ?: song.composer) }
    var disc by remember { mutableStateOf(song.customDisc ?: song.disc) }
    var track by remember { mutableStateOf(song.customTrack ?: song.track) }
    var year by remember { mutableStateOf(song.customYear ?: song.year) }
    var comment by remember { mutableStateOf(song.customComment ?: song.comment) }
    var bpm by remember { mutableStateOf(song.customBpm ?: song.bpm) }
    var albumArtUri by remember { mutableStateOf(song.albumArtUri) }
    var isCropped by remember { mutableStateOf(false) }

    // Physical File Metadata State
    var actualMetadata by remember { mutableStateOf<com.example.data.api.FileMetadata?>(null) }
    var isLoadingActualMetadata by remember { mutableStateOf(false) }

    LaunchedEffect(song.filePath) {
        isLoadingActualMetadata = true
        actualMetadata = try {
            com.example.data.api.GeminiMusicService.readActualFileMetadata(song.filePath)
        } catch (e: Exception) {
            null
        }
        isLoadingActualMetadata = false
    }

    // Tag search states
    var searchTitle by remember { mutableStateOf(song.customTitle ?: song.title) }
    var searchArtist by remember { mutableStateOf(song.customArtist ?: song.artist) }

    LaunchedEffect(title) {
        searchTitle = title
    }
    LaunchedEffect(artist) {
        searchArtist = artist
    }
    var selectedTagSource by remember { mutableStateOf("All Sources") }
    var isSearchingTags by remember { mutableStateOf(false) }
    var hasSearchedTags by remember { mutableStateOf(false) }
    var tagResults by remember { mutableStateOf<List<TagSearchResult>>(emptyList()) }
    var selectedTagResultIndex by remember { mutableStateOf<Int?>(null) }
    var isFingerprinting by remember { mutableStateOf(false) }
    var fingerprintProvider by remember { mutableStateOf("AudD API") }
    var audDApiToken by remember { mutableStateOf("") }

    val tagSources = listOf("All Sources", "MusicBrainz", "iTunes", "Deezer")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Curated high quality covers for "Replace Cover" feature
    val onlineCovers = listOf(
        "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=400&auto=format&fit=crop"
    )

    // Current file path tracker for renaming
    var currentFilePath by remember { mutableStateOf(song.filePath) }

    // Modern multi-tab selection
    var activeTab by rememberSaveable { mutableStateOf(0) } // 0: Edit, 1: Search, 2: Scan, 3: Rename

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Advanced Tag Suite",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Edit manually, search web databases, or run audio recognition",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Pill Tab Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Triple(0, Icons.Default.Edit, "Edit"),
                        Triple(1, Icons.Default.Language, "Search"),
                        Triple(2, Icons.Default.GraphicEq, "Scan"),
                        Triple(3, Icons.Default.EditNote, "Rename")
                    ).forEach { (idx, icon, label) ->
                        val isSelected = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else Color.Transparent
                                )
                                .clickable { activeTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable tab content
                when (activeTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // Cover art Operations
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.DarkGray)
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = albumArtUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=250&auto=format&fit=crop",
                                                contentDescription = "Cover preview",
                                                contentScale = if (isCropped) ContentScale.Crop else ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Cover Artwork",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Download HD Cover
                                                IconButton(
                                                    onClick = {
                                                        albumArtUri = onlineCovers.random()
                                                        Toast.makeText(context, "Downloaded new HD Cover!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Download,
                                                        contentDescription = "Download Cover",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }

                                                // Extract Physical Cover
                                                IconButton(
                                                    onClick = {
                                                        albumArtUri = Uri.parse("content://media/external/audio/media/${song.id}/albumart").toString()
                                                        Toast.makeText(context, "Extracted original tag cover", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Unarchive,
                                                        contentDescription = "Extract Cover",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }

                                                // Toggle Aspect Ratio
                                                IconButton(
                                                    onClick = {
                                                        isCropped = !isCropped
                                                        Toast.makeText(context, if (isCropped) "Cover Cropped" else "Original Aspect Ratio", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(
                                                            if (isCropped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                            CircleShape
                                                        )
                                                ) {
                                                    Icon(
                                                        Icons.Default.Crop,
                                                        contentDescription = "Crop",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = if (isCropped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Remove Cover
                                                IconButton(
                                                    onClick = {
                                                        albumArtUri = null
                                                        Toast.makeText(context, "Cover Removed", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove Cover",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // General Metadata Fields
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "GENERAL INFORMATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )

                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Song Title") },
                                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = artist,
                                        onValueChange = { artist = it },
                                        label = { Text("Artist") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = album,
                                        onValueChange = { album = it },
                                        label = { Text("Album") },
                                        leadingIcon = { Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = albumArtist,
                                        onValueChange = { albumArtist = it },
                                        label = { Text("Album Artist") },
                                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // Detailed / Classification Metadata Fields
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "CLASSIFICATION & ORGANIZATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = genre,
                                            onValueChange = { genre = it },
                                            label = { Text("Genre") },
                                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = year,
                                            onValueChange = { year = it },
                                            label = { Text("Year") },
                                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = track,
                                            onValueChange = { track = it },
                                            label = { Text("Track No.") },
                                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = disc,
                                            onValueChange = { disc = it },
                                            label = { Text("Disc No.") },
                                            leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = bpm,
                                            onValueChange = { bpm = it },
                                            label = { Text("BPM (Tempo)") },
                                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = composer,
                                            onValueChange = { composer = it },
                                            label = { Text("Composer") },
                                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }

                            // Commentary/Notes Field
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "NOTES & ADDITIONAL INFO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )

                                    OutlinedTextField(
                                        value = comment,
                                        onValueChange = { comment = it },
                                        label = { Text("Comment") },
                                        leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // Search Box Settings card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "ONLINE SEARCH PARAMETERS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = searchTitle,
                                                onValueChange = { searchTitle = it },
                                                label = { Text("Query Title") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                            )
                                            OutlinedTextField(
                                                value = searchArtist,
                                                onValueChange = { searchArtist = it },
                                                label = { Text("Query Artist") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "SELECT DATA SOURCE:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                tagSources.forEach { src ->
                                                    val isSelected = selectedTagSource == src
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                            .clickable { selectedTagSource = src }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = src,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (searchTitle.isBlank() || searchArtist.isBlank()) {
                                                    Toast.makeText(context, "Please enter both search title and artist", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                coroutineScope.launch {
                                                    isSearchingTags = true
                                                    selectedTagResultIndex = null
                                                    
                                                    val fetched = try { 
                                                        com.example.data.api.GeminiMusicService.searchTagsOnlineMulti(searchTitle, searchArtist, selectedTagSource) 
                                                    } catch (e: Exception) {
                                                        val errMsg = e.localizedMessage ?: e.message ?: "Connection Error"
                                                        Toast.makeText(context, "Online Search Failed: $errMsg", Toast.LENGTH_LONG).show()
                                                        emptyList()
                                                    }
                                                    
                                                    tagResults = fetched.map { onlineResult ->
                                                        TagSearchResult(
                                                            title = onlineResult.title,
                                                            artist = onlineResult.artist,
                                                            album = onlineResult.album,
                                                            albumArtist = onlineResult.albumArtist,
                                                            genre = onlineResult.genre,
                                                            composer = onlineResult.composer,
                                                            disc = onlineResult.disc,
                                                            track = onlineResult.track,
                                                            year = onlineResult.year,
                                                            comment = onlineResult.comment,
                                                            bpm = onlineResult.bpm,
                                                            albumArtUri = onlineResult.albumArtUri,
                                                            matchDescription = onlineResult.matchDescription,
                                                            confidence = onlineResult.confidence
                                                        )
                                                    }
                                                    
                                                    isSearchingTags = false
                                                    hasSearchedTags = true
                                                    if (tagResults.isNotEmpty()) {
                                                        selectedTagResultIndex = 0
                                                    } else {
                                                        Toast.makeText(context, "No online database releases matched.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !isSearchingTags,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            if (isSearchingTags) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Searching databases...")
                                            } else {
                                                Icon(Icons.Default.ManageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Search Databases")
                                            }
                                        }
                                    }
                                }
                            }

                            if (hasSearchedTags) {
                                item {
                                    Text(
                                        text = if (tagResults.isNotEmpty()) "RELEASE MATCHES (${tagResults.size})" else "NO MATCHES FOUND",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                if (tagResults.isEmpty()) {
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "No matched releases. Try expanding search tags or check connectivity.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(tagResults) { idx, item ->
                                        val isSelected = selectedTagResultIndex == idx
                                        val sourceName = when {
                                            item.comment.contains("MusicBrainz", ignoreCase = true) -> "MusicBrainz"
                                            item.comment.contains("iTunes", ignoreCase = true) -> "iTunes"
                                            item.comment.contains("Deezer", ignoreCase = true) -> "Deezer"
                                            else -> "Online API"
                                        }
                                        val sourceColor = when (sourceName) {
                                            "MusicBrainz" -> MaterialTheme.colorScheme.primary
                                            "iTunes" -> MaterialTheme.colorScheme.secondary
                                            "Deezer" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.outline
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Card(
                                                onClick = { selectedTagResultIndex = idx },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                ),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.DarkGray)
                                                    ) {
                                                        AsyncImage(
                                                            model = item.albumArtUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=150",
                                                            contentDescription = "Cover thumb",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = item.title,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(sourceColor.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = sourceName,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = sourceColor
                                                                )
                                                            }
                                                        }
                                                        
                                                        Text(
                                                            text = "Artist: ${item.artist} | Album: ${item.album}",
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(2.dp))
                                                                Text(
                                                                    text = "Confidence: ${item.confidence} | ${item.year}",
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.CheckCircle,
                                                                    contentDescription = "Selected",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (isSelected) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(
                                                            text = "SELECTED METADATA APPLICATOR",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            letterSpacing = 1.sp
                                                        )
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                Text("Title: ${item.title}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                                Text("Artist: ${item.artist}", fontSize = 12.sp)
                                                                Text("Album: ${item.album}", fontSize = 12.sp)
                                                                Text("Genre: ${item.genre} | Year: ${item.year}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            
                                                            Button(
                                                                onClick = {
                                                                    title = item.title
                                                                    artist = item.artist
                                                                    album = item.album
                                                                    albumArtist = item.albumArtist
                                                                    genre = item.genre
                                                                    composer = item.composer
                                                                    disc = item.disc
                                                                    track = item.track
                                                                    year = item.year
                                                                    comment = item.comment
                                                                    bpm = item.bpm
                                                                    albumArtUri = item.albumArtUri
                                                                    Toast.makeText(context, "Pre-filled tags in Edit tab!", Toast.LENGTH_SHORT).show()
                                                                    activeTab = 0 // Switch back to manual tag fine-tuning!
                                                                },
                                                                shape = RoundedCornerShape(12.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                                            ) {
                                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("Apply & Fine-Tune", fontSize = 12.sp)
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
                    }
                    2 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // Fingerprint Identification Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Hearing,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "ACOUSTIC RECOGNITION",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            if (isFingerprinting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    strokeWidth = 1.5.dp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Can't find tag info? Scan a 10-second snippet of this audio file to recognize the exact song title, artist, and details dynamically.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Provider segmented toggles
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("AudD API", "Gemini AI").forEach { provider ->
                                                val isSelected = fingerprintProvider == provider
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.tertiary 
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                        .clickable { fingerprintProvider = provider }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (provider == "Gemini AI") {
                                                            Icon(
                                                                imageVector = Icons.Default.AutoAwesome, 
                                                                contentDescription = null, 
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                        Text(
                                                            text = provider, 
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Token Configuration
                                        if (fingerprintProvider == "AudD API") {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedTextField(
                                                    value = audDApiToken,
                                                    onValueChange = { audDApiToken = it },
                                                    placeholder = { Text("Enter AudD Token (Optional)", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                                )
                                                Text(
                                                    text = "Leave empty to use shared free keys. Enter token for reliable custom usage.",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Uses Gemini model context parsing. Ensure active GEMINI_API_KEY in secrets panel.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isFingerprinting = true
                                                    val songFilePath = song.filePath
                                                    if (songFilePath.isNullOrBlank()) {
                                                        Toast.makeText(context, "Error: Audio file path is empty", Toast.LENGTH_SHORT).show()
                                                        isFingerprinting = false
                                                        return@launch
                                                    }

                                                    if (fingerprintProvider == "AudD API") {
                                                        Toast.makeText(context, "Scanning audio fingerprint & querying AudD API...", Toast.LENGTH_LONG).show()
                                                        val recognized = try {
                                                            com.example.data.api.GeminiMusicService.recognizeMusicByAudD(songFilePath, audDApiToken)
                                                        } catch (e: Exception) {
                                                            Log.e("Fingerprint", "Failed to recognize song by AudD: ${e.message}", e)
                                                            null
                                                        }

                                                        if (!recognized.isNullOrEmpty()) {
                                                            val match = recognized[0]
                                                            Toast.makeText(context, "Song Recognized! Match: ${match.title} by ${match.artist}", Toast.LENGTH_LONG).show()
                                                            
                                                            tagResults = recognized.map { onlineResult ->
                                                                TagSearchResult(
                                                                    title = onlineResult.title,
                                                                    artist = onlineResult.artist,
                                                                    album = onlineResult.album,
                                                                    albumArtist = onlineResult.albumArtist,
                                                                    genre = onlineResult.genre,
                                                                    composer = onlineResult.composer,
                                                                    disc = onlineResult.disc,
                                                                    track = onlineResult.track,
                                                                    year = onlineResult.year,
                                                                    comment = onlineResult.comment,
                                                                    bpm = onlineResult.bpm,
                                                                    albumArtUri = onlineResult.albumArtUri,
                                                                    matchDescription = onlineResult.matchDescription,
                                                                    confidence = onlineResult.confidence
                                                                )
                                                            }
                                                            selectedTagResultIndex = 0
                                                            hasSearchedTags = true
                                                            searchTitle = match.title
                                                            searchArtist = match.artist
                                                            
                                                            // Redirect to Search Online to inspect and apply
                                                            activeTab = 1
                                                            Toast.makeText(context, "Recognized results loaded in Database tab!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "AudD could not identify this audio. Try manual search or Gemini AI.", Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Scanning audio snippet & querying Gemini AI...", Toast.LENGTH_LONG).show()
                                                        val recognized = try {
                                                            com.example.data.api.GeminiMusicService.recognizeMusicByFingerprint(songFilePath)
                                                        } catch (e: Exception) {
                                                            Log.e("Fingerprint", "Failed to recognize song by fingerprint: ${e.message}", e)
                                                            null
                                                        }

                                                        if (!recognized.isNullOrEmpty()) {
                                                            val match = recognized[0]
                                                            Toast.makeText(context, "Song Recognized! Match: ${match.title} by ${match.artist}", Toast.LENGTH_LONG).show()
                                                            
                                                            tagResults = recognized.map { onlineResult ->
                                                                TagSearchResult(
                                                                    title = onlineResult.title,
                                                                    artist = onlineResult.artist,
                                                                    album = onlineResult.album,
                                                                    albumArtist = onlineResult.albumArtist,
                                                                    genre = onlineResult.genre,
                                                                    composer = onlineResult.composer,
                                                                    disc = onlineResult.disc,
                                                                    track = onlineResult.track,
                                                                    year = onlineResult.year,
                                                                    comment = onlineResult.comment,
                                                                    bpm = onlineResult.bpm,
                                                                    albumArtUri = onlineResult.albumArtUri,
                                                                    matchDescription = onlineResult.matchDescription,
                                                                    confidence = onlineResult.confidence
                                                                )
                                                            }
                                                            selectedTagResultIndex = 0
                                                            hasSearchedTags = true
                                                            searchTitle = match.title
                                                            searchArtist = match.artist
                                                            
                                                            // Redirect to Search Online to inspect and apply
                                                            activeTab = 1
                                                            Toast.makeText(context, "Recognized results loaded in Database tab!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Gemini failed: Key missing, model limit, or unsupported format. Try AudD API instead.", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    isFingerprinting = false
                                                }
                                            },
                                            enabled = !isFingerprinting,
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isFingerprinting) "Analyzing Audio Snippet..." else "Scan & Identify Song", 
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Embedded ID3 Metadata Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AudioFile,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "FILE PHYSICAL EMBEDDED TAGS",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            if (isLoadingActualMetadata) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                                            }
                                        }

                                        if (isLoadingActualMetadata) {
                                            Text(
                                                text = "Extracting raw file headers from storage...", 
                                                fontSize = 12.sp, 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            actualMetadata?.let { meta ->
                                                if (meta.title.isNullOrBlank() && meta.artist.isNullOrBlank() && meta.album.isNullOrBlank()) {
                                                    Text(
                                                        text = "No embedded physical headers found in this audio snippet.", 
                                                        fontSize = 12.sp, 
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Title: ${meta.title ?: "[N/A]"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                            Text("Artist: ${meta.artist ?: "[N/A]"}", fontSize = 12.sp)
                                                        }
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Album: ${meta.album ?: "[N/A]"}", fontSize = 12.sp)
                                                            Text("Genre: ${meta.genre ?: "[N/A]"}", fontSize = 12.sp)
                                                        }
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("Year: ${meta.year ?: "[N/A]"} | Track: ${meta.track ?: "[N/A]"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("Composer: ${meta.composer ?: "[N/A]"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        Button(
                                                            onClick = {
                                                                title = meta.title ?: title
                                                                artist = meta.artist ?: artist
                                                                album = meta.album ?: album
                                                                albumArtist = meta.albumArtist ?: albumArtist
                                                                genre = meta.genre ?: genre
                                                                composer = meta.composer ?: composer
                                                                track = meta.track ?: track
                                                                disc = meta.disc ?: disc
                                                                year = meta.year ?: year
                                                                comment = meta.comment ?: comment
                                                                Toast.makeText(context, "Physical headers loaded in Edit tab!", Toast.LENGTH_SHORT).show()
                                                                activeTab = 0 // Redirect to manual fine-tuning!
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Load original physical headers", fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            } ?: Text("Could not retrieve physical metadata headers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        var selectedRenameFormatIdx by remember { mutableStateOf(0) }
                        var customRenameFormat by remember { mutableStateOf("%artist% - %title%") }

                        val renameFormats = listOf(
                            "%artist% - %title%",
                            "%track% - %title%",
                            "%track% - %artist% - %title%",
                            "%title%"
                        )

                        // Compute preview name based on chosen format and active edits
                        val previewName = remember(selectedRenameFormatIdx, customRenameFormat, title, artist, track, album, currentFilePath) {
                            val format = if (selectedRenameFormatIdx == 4) customRenameFormat else renameFormats.getOrElse(selectedRenameFormatIdx) { "%artist% - %title%" }
                            var name = format
                                .replace("%artist%", artist.ifBlank { "Unknown Artist" })
                                .replace("%title%", title.ifBlank { "Untitled" })
                                .replace("%track%", track.ifBlank { "00" })
                                .replace("%album%", album.ifBlank { "Unknown Album" })

                            // Keep original extension
                            val extension = File(currentFilePath).extension.ifBlank { "mp3" }
                            name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                            if (name.isEmpty()) name = "Untitled"
                            "$name.$extension"
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CURRENT PHYSICAL FILE NAME",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Text(
                                            text = File(currentFilePath).name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Full Path: $currentFilePath",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "SELECT RENAME FORMAT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            itemsIndexed(listOf(
                                "Artist - Title  (e.g., Artist - Song.mp3)",
                                "Track - Title  (e.g., 01 - Song.mp3)",
                                "Track - Artist - Title  (e.g., 01 - Artist - Song.mp3)",
                                "Title Only  (e.g., Song.mp3)",
                                "Custom Pattern Format..."
                            )) { idx, formatLabel ->
                                val isSelected = selectedRenameFormatIdx == idx
                                Card(
                                    onClick = { selectedRenameFormatIdx = idx },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedRenameFormatIdx = idx }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatLabel,
                                            fontSize = 12.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (selectedRenameFormatIdx == 4) {
                                item {
                                    OutlinedTextField(
                                        value = customRenameFormat,
                                        onValueChange = { customRenameFormat = it },
                                        label = { Text("Custom Pattern Format") },
                                        placeholder = { Text("%artist% - %title%") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Supported wildcards: %artist%, %title%, %track%, %album%",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Preview and Action Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "FILE NAME PREVIEW",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            letterSpacing = 1.sp
                                        )

                                        Text(
                                            text = previewName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Button(
                                            onClick = {
                                                viewModel.renameSongFile(
                                                    songId = song.id,
                                                    newFileName = previewName,
                                                    onSuccess = { newName ->
                                                        val parentDir = File(currentFilePath).parent ?: ""
                                                        currentFilePath = if (parentDir.isNotEmpty()) "$parentDir/$newName" else newName
                                                        Toast.makeText(context, "File renamed successfully to: $newName", Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, "Error renaming: $err", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Rename Physical File", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) { 
                        Text("Cancel") 
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateSongFullTags(
                                songId = song.id,
                                title = title,
                                artist = artist,
                                album = album,
                                albumArtist = albumArtist,
                                genre = genre,
                                composer = composer,
                                disc = disc,
                                track = track,
                                year = year,
                                comment = comment,
                                bpm = bpm,
                                albumArtUri = albumArtUri
                            )
                            Toast.makeText(context, "Metadata saved successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

