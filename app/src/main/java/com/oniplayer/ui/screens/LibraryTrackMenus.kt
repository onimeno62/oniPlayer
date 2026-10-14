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

// Library Menu — configuration and settings bottom sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsMenu(
    layoutMode: String,
    onLayoutChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    isSortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
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
                                text = "Library Menu",
                                style = OniSkin.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.textPrimary
                            )
                            Text(
                                text = "Preferences, layout, and sorting",
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
                            contentDescription = "Close menu",
                            tint = OniSkin.colors.textSecondary
                        )
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

    val audioDetails = remember(song, file) {
        val detectedBitrate = if (song.bitrate > 0) {
            "${song.bitrate} kbps"
        } else if (file.exists() && file.length() > 0 && song.duration > 0) {
            val kbps = ((file.length() * 8) / song.duration).toInt()
            if (kbps > 0) "$kbps kbps" else null
        } else null
        val path = song.filePath.lowercase()
        val defaultSampleRate = if (path.endsWith(".opus") || path.endsWith(".ogg") || path.endsWith(".aac")) "48.0 kHz" else "44.1 kHz"
        Pair(defaultSampleRate, detectedBitrate)
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
                        MetadataKeyValue(label = "Track / Disc", value = trackDiscStr)
                    }
                    if (song.displayYear.isNotBlank()) {
                        MetadataKeyValue(label = "Year", value = song.displayYear)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetadataKeyValue(label = "Play Count", value = "${song.playCount} times")
                    MetadataKeyValue(label = "Last Played", value = lastPlayedStr)
                    if (ratingStr != null) {
                        MetadataKeyValue(label = "Rating", value = ratingStr)
                    }
                }
            }

            // 3. Expandable Section for Technical & File Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = OniSkin.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(
                        color = OniSkin.colors.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = OniSkin.spacing.xxs)
                    )
                    MetadataKeyValue(label = "File Name", value = fileName)
                    MetadataKeyValue(label = "File Path", value = song.filePath)
                    if (dateAddedStr != null) {
                        MetadataKeyValue(label = "Date Added", value = dateAddedStr)
                    }
                    if (lastModifiedStr != null) {
                        MetadataKeyValue(label = "Last Modified", value = lastModifiedStr)
                    }
                    if (song.displayComposer.isNotBlank()) {
                        MetadataKeyValue(label = "Composer", value = song.displayComposer)
                    }
                    if (song.displayBpm.isNotBlank()) {
                        MetadataKeyValue(label = "BPM", value = song.displayBpm)
                    }
                    if (song.displayComment.isNotBlank()) {
                        MetadataKeyValue(label = "Comment", value = song.displayComment)
                    }
                }
            }

            // 4. Show More / Show Less Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = OniSkin.spacing.xxs),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Show Less" else "Technical Details",
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = OniSkin.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StaticDataChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isHighlight: Boolean = false
) {
    OniSurface(
        variant = if (isHighlight) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.chip,
        containerColor = if (isHighlight) OniSkin.colors.primaryContainer else OniSkin.colors.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = OniSkin.spacing.xs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
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
                color = if (isHighlight) OniSkin.colors.primary else OniSkin.colors.textSecondary
            )
        }
    }
}

@Composable
fun MetadataKeyValue(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = OniSkin.typography.caption,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = OniSkin.colors.textTertiary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = OniSkin.typography.bodySmall,
            color = OniSkin.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrackMenuActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    OniSurface(
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = OniSkin.spacing.sm, horizontal = OniSkin.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: OniSkin.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = OniSkin.typography.caption,
                fontWeight = FontWeight.Medium,
                color = textColor ?: OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
