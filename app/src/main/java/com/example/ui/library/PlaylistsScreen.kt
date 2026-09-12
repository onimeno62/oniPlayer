package com.example.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.library.components.LibraryCategoryHero
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.components.OniSectionHeader
import com.example.ui.library.components.PlaylistCard
import com.example.ui.library.components.PlaylistRow
import com.example.ui.library.model.PlaylistUiModel
import com.example.ui.theme.OniSkin

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistUiModel>,
    layoutMode: String,
    onPlaylistClick: (PlaylistUiModel) -> Unit,
    onPlayPlaylist: (PlaylistUiModel) -> Unit,
    onShufflePlaylist: (PlaylistUiModel) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onImportM3U: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    gridIndex: Int = 0,
    gridOffset: Int = 0,
    onGridScroll: (Int, Int) -> Unit = { _, _ -> },
    listIndex: Int = 0,
    listOffset: Int = 0,
    onListScroll: (Int, Int) -> Unit = { _, _ -> }
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PlaylistUiModel?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistUiModel?>(null) }

    val heroArtwork = playlists.firstNotNullOfOrNull { it.artworkUri }
    val totalTracks = playlists.sumOf { it.songCount }
    val heroSubtitle = if (playlists.size == 1) "1 playlist • $totalTracks tracks" else "${playlists.size} playlists • $totalTracks tracks"

    Box(modifier = modifier.fillMaxSize()) {
        if (layoutMode == "grid") {
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = gridIndex,
                initialFirstVisibleItemScrollOffset = gridOffset
            )
            LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
                onGridScroll(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = OniSkin.spacing.screenHorizontal,
                    end = OniSkin.spacing.screenHorizontal,
                    top = OniSkin.spacing.xs,
                    bottom = 96.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LibraryCategoryHero(
                            title = "Playlists",
                            subtitle = heroSubtitle,
                            artworkUri = heroArtwork,
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            modifier = Modifier.padding(bottom = OniSkin.spacing.sm)
                        )

                        PlaylistsActionRow(
                            onCreateClick = { showCreateDialog = true },
                            onImportClick = { showImportDialog = true }
                        )

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

                        OniSectionHeader(
                            title = "Your Playlists",
                            modifier = Modifier.padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LibraryEmptyState(
                            title = "No Playlists",
                            message = "No playlists created yet. Tap New Playlist or Import M3U above to get started.",
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                } else {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            onPlay = { onPlayPlaylist(playlist) },
                            onShuffle = { onShufflePlaylist(playlist) },
                            onRename = { playlistToRename = playlist },
                            onDelete = { playlistToDelete = playlist },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = listIndex,
                initialFirstVisibleItemScrollOffset = listOffset
            )
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                onListScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = OniSkin.spacing.screenHorizontal,
                    vertical = OniSkin.spacing.xs
                ),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "playlists_hero") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LibraryCategoryHero(
                            title = "Playlists",
                            subtitle = heroSubtitle,
                            artworkUri = heroArtwork,
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            modifier = Modifier.padding(bottom = OniSkin.spacing.sm)
                        )

                        PlaylistsActionRow(
                            onCreateClick = { showCreateDialog = true },
                            onImportClick = { showImportDialog = true }
                        )

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

                        OniSectionHeader(
                            title = "Your Playlists",
                            modifier = Modifier.padding(top = OniSkin.spacing.xs, bottom = OniSkin.spacing.xxs)
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    item(key = "empty_state") {
                        LibraryEmptyState(
                            title = "No Playlists",
                            message = "No playlists created yet. Tap New Playlist or Import M3U above to get started.",
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                } else {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            onPlay = { onPlayPlaylist(playlist) },
                            onShuffle = { onShufflePlaylist(playlist) },
                            onRename = { playlistToRename = playlist },
                            onDelete = { playlistToDelete = playlist },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "New Playlist",
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OniSkin.colors.textPrimary,
                        unfocusedTextColor = OniSkin.colors.textPrimary,
                        focusedBorderColor = OniSkin.colors.primary,
                        cursorColor = OniSkin.colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) {
                            onCreatePlaylist(trimmed)
                            showCreateDialog = false
                        }
                    },
                    enabled = name.trim().isNotBlank(),
                    shape = OniSkin.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OniSkin.colors.primary,
                        contentColor = OniSkin.colors.onPrimary
                    )
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.textSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.card
        )
    }

    // Rename Playlist Dialog
    playlistToRename?.let { playlist ->
        var renameText by remember(playlist.id) { mutableStateOf(playlist.name) }
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = {
                Text(
                    text = "Rename Playlist",
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OniSkin.colors.textPrimary,
                        unfocusedTextColor = OniSkin.colors.textPrimary,
                        focusedBorderColor = OniSkin.colors.primary,
                        cursorColor = OniSkin.colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotBlank()) {
                            onRenamePlaylist(playlist.id, trimmed)
                            playlistToRename = null
                        }
                    },
                    enabled = renameText.trim().isNotBlank(),
                    shape = OniSkin.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OniSkin.colors.primary,
                        contentColor = OniSkin.colors.onPrimary
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToRename = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.textSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.card
        )
    }

    // Delete Playlist Confirmation Dialog
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = {
                Text(
                    text = "Delete Playlist?",
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${playlist.name}\"? Tracks in this playlist will remain in your library.",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(playlist.id)
                        playlistToDelete = null
                    },
                    shape = OniSkin.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OniSkin.colors.error,
                        contentColor = OniSkin.colors.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.textSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.card
        )
    }

    // Import M3U Dialog
    if (showImportDialog) {
        var playlistName by remember { mutableStateOf("") }
        var m3uBody by remember { mutableStateOf("#EXTM3U\n#EXTINF:180,Track Title\n/storage/emulated/0/Music/track.mp3") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "Import M3U Playlist",
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OniSkin.colors.textPrimary,
                            unfocusedTextColor = OniSkin.colors.textPrimary,
                            focusedBorderColor = OniSkin.colors.primary,
                            cursorColor = OniSkin.colors.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = m3uBody,
                        onValueChange = { m3uBody = it },
                        label = { Text("M3U Content") },
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OniSkin.colors.textPrimary,
                            unfocusedTextColor = OniSkin.colors.textPrimary,
                            focusedBorderColor = OniSkin.colors.primary,
                            cursorColor = OniSkin.colors.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = playlistName.trim().ifEmpty { "Imported Playlist" }
                        onImportM3U(name, m3uBody)
                        showImportDialog = false
                    },
                    shape = OniSkin.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OniSkin.colors.primary,
                        contentColor = OniSkin.colors.onPrimary
                    )
                ) {
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.textSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.card
        )
    }
}

@Composable
private fun PlaylistsActionRow(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = OniSkin.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = OniSkin.shapes.button,
            contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
            colors = ButtonDefaults.buttonColors(
                containerColor = OniSkin.colors.primary,
                contentColor = OniSkin.colors.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
            Text(
                text = "New Playlist",
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        FilledTonalButton(
            onClick = onImportClick,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = OniSkin.shapes.button,
            contentPadding = PaddingValues(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = OniSkin.colors.surfaceVariant,
                contentColor = OniSkin.colors.textPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Input,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
            Text(
                text = "Import M3U",
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
