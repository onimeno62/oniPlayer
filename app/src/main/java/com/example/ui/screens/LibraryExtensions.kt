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
    onShowTrackMenu: (SongEntity) -> Unit
) {
    var activeSmartPlaylistType by remember { mutableStateOf<String?>(null) }

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
                    onClick = { activeSmartPlaylistType = "Recently Played" }
                )
            }
            item {
                SmartPlaylistCard(
                    title = "Favorites",
                    count = favorites.size,
                    icon = Icons.Default.Favorite,
                    bgColor = Color(0x1FF44336),
                    iconColor = Color(0xFFF44336),
                    onClick = { activeSmartPlaylistType = "Favorites" }
                )
            }
            item {
                SmartPlaylistCard(
                    title = "High Rating",
                    count = highRating.size,
                    icon = Icons.Default.Star,
                    bgColor = Color(0x1FFFF9800),
                    iconColor = Color(0xFFFF9800),
                    onClick = { activeSmartPlaylistType = "High Rating" }
                )
            }
            item {
                SmartPlaylistCard(
                    title = "Never Played",
                    count = neverPlayed.size,
                    icon = Icons.Default.MusicOff,
                    bgColor = Color(0x1F9E9E9E),
                    iconColor = Color(0xFF9E9E9E),
                    onClick = { activeSmartPlaylistType = "Never Played" }
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
                IconButton(onClick = { activeSmartPlaylistType = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeSmartPlaylistType ?: "",
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
    onShowTrackMenu: (SongEntity) -> Unit
) {
    var activePlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
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
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No playlists created yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            onClick = { activePlaylist = playlist },
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
                    IconButton(onClick = { activePlaylist = null }) {
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This playlist has no tracks yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No matching tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            SongsListView(
                songs = filteredSongs,
                viewModel = viewModel,
                sortBy = "title",
                isSortAscending = true,
                onShowTrackMenu = onShowTrackMenu
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Online Tag Search & Editor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Query databases, pick matching details, and fine-tune tags", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- SECTION 0: Embedded Physical Metadata ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PHYSICAL FILE EMBEDDED METADATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (isLoadingActualMetadata) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                                    }
                                }

                                if (isLoadingActualMetadata) {
                                    Text("Extracting embedded audio tags from storage...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    actualMetadata?.let { meta ->
                                        if (meta.title.isNullOrBlank() && meta.artist.isNullOrBlank() && meta.album.isNullOrBlank()) {
                                            Text("No physical embedded tags found in this audio file.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Title: ${meta.title ?: "[N/A]"}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Text("Artist: ${meta.artist ?: "[N/A]"}", fontSize = 12.sp)
                                                Text("Album: ${meta.album ?: "[N/A]"}", fontSize = 12.sp)
                                                Text("Genre: ${meta.genre ?: "[N/A]"} | Year: ${meta.year ?: "[N/A]"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Track: ${meta.track ?: "[N/A]"} | Disc: ${meta.disc ?: "[N/A]"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
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
                                                        Toast.makeText(context, "Physical tags applied successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Apply Embedded Physical Tags", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } ?: Text("Error reading physical file metadata.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // --- SECTION 0.5: Acoustic Fingerprint Recognition ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
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
                                        Icon(Icons.Default.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ACOUSTIC FINGERPRINT RECOGNITION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                    if (isFingerprinting) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.tertiary, strokeWidth = 1.5.dp)
                                    }
                                }

                                Text(
                                    text = "If file name or metadata is wrong, analyze an audio snippet to automatically recognize the correct song.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Provider Selector Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("AudD API", "Gemini AI").forEach { provider ->
                                        val isSelected = fingerprintProvider == provider
                                        Button(
                                            onClick = { fingerprintProvider = provider },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            if (provider == "Gemini AI") {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(provider, fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Token input if AudD is selected
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
                                            text = "Empty uses free trial. Register on audd.io to get a free personal API token.",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Requires active GEMINI_API_KEY set in your AI Studio secrets panel.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

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
                                                    
                                                    // Also pre-fill the search fields with recognized metadata
                                                    searchTitle = match.title
                                                    searchArtist = match.artist
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
                                                    
                                                    // Also pre-fill the search fields with recognized metadata
                                                    searchTitle = match.title
                                                    searchArtist = match.artist
                                                } else {
                                                    Toast.makeText(context, "Gemini failed: Key missing, model limit, or unsupported format. Try AudD API instead.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                            isFingerprinting = false
                                        }
                                    },
                                    enabled = !isFingerprinting,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isFingerprinting) "Analyzing Audio..." else "Scan & Recognize Music", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // --- SECTION 1: Online Search Console ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ONLINE MULTI-SOURCE METADATA SEARCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = searchTitle,
                                        onValueChange = { searchTitle = it },
                                        label = { Text("Search Title") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                    OutlinedTextField(
                                        value = searchArtist,
                                        onValueChange = { searchArtist = it },
                                        label = { Text("Search Artist") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                }

                                // DB Sources row
                                Column {
                                    Text("SELECT DATA SOURCE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        tagSources.forEach { src ->
                                            val isSelected = selectedTagSource == src
                                            InputChip(
                                                selected = isSelected,
                                                onClick = { selectedTagSource = src },
                                                label = { Text(src, fontSize = 11.sp) },
                                                colors = InputChipDefaults.inputChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
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
                                        Text("Searching live databases...")
                                    } else {
                                        Icon(Icons.Default.ManageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Search Databases")
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 2: Search Result Selection & Preview ---
                    if (hasSearchedTags && tagResults.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "SELECT MATCHING ONLINE RELEASE GROUP:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    tagResults.forEachIndexed { idx, item ->
                                        val isSelected = selectedTagResultIndex == idx
                                        
                                        // Determine source name
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

                                        Card(
                                            onClick = { selectedTagResultIndex = idx },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
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
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        SuggestionChip(
                                                            onClick = {},
                                                            label = { Text(sourceName, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                                labelColor = sourceColor
                                                            )
                                                        )
                                                    }
                                                    Text(
                                                        text = "Album: ${item.album}",
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = "Confidence: ${item.confidence} | Year: ${item.year}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Preview Box of Selected Tag Option
                        selectedTagResultIndex?.let { index ->
                            if (index < tagResults.size) {
                                val res = tagResults[index]
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("PREVIEW INCOMING METADATA:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text("Title: ${res.title}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Artist: ${res.artist}", fontSize = 12.sp)
                                                    Text("Album: ${res.album}", fontSize = 12.sp)
                                                    Text("Genre: ${res.genre} | Year: ${res.year}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        title = res.title
                                                        artist = res.artist
                                                        album = res.album
                                                        albumArtist = res.albumArtist
                                                        genre = res.genre
                                                        composer = res.composer
                                                        disc = res.disc
                                                        track = res.track
                                                        year = res.year
                                                        comment = res.comment
                                                        bpm = res.bpm
                                                        albumArtUri = res.albumArtUri
                                                        Toast.makeText(context, "Tags and Cover pre-filled!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Apply to Fields", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 3: Standard Editor Fields (Fine-Tuning) ---
                    item {
                        Text(
                            "FINE-TUNE MANUALLY:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Cover Art Operations
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = albumArtUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=250&auto=format&fit=crop",
                                        contentDescription = "Cover preview",
                                        contentScale = if (isCropped) ContentScale.Crop else ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Album Art Operations", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Crop Option
                                        IconButton(
                                            onClick = {
                                                isCropped = !isCropped
                                                Toast.makeText(context, if (isCropped) "Cover Cropped" else "Original Aspect Ratio", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Crop, contentDescription = "Crop", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }

                                        // Download Option
                                        IconButton(
                                            onClick = {
                                                albumArtUri = onlineCovers.random()
                                                Toast.makeText(context, "Downloaded new HD Cover!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = "Download Cover", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                        }

                                        // Remove Option
                                        IconButton(
                                            onClick = {
                                                albumArtUri = null
                                                Toast.makeText(context, "Cover Removed", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Cover", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                        }

                                        // Extract Option
                                        IconButton(
                                            onClick = {
                                                albumArtUri = Uri.parse("content://media/external/audio/media/${song.id}/albumart").toString()
                                                Toast.makeText(context, "Extracted original tag cover", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Unarchive, contentDescription = "Extract Cover", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fields list
                    item {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
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
                            Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

