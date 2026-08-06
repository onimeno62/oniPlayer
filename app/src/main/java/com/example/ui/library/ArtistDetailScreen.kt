package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.data.entity.SongEntity
import com.example.ui.library.components.AlbumCard
import com.example.ui.library.components.OniSectionHeader
import com.example.ui.library.components.SongRow
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.theme.LocalAccentColor

@Composable
fun ArtistDetailScreen(
    artist: ArtistUiModel,
    albumsByArtist: List<AlbumUiModel>,
    songsByArtist: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    layoutMode: String,
    viewModel: com.example.ui.viewmodel.MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val sortedSongs = remember(songsByArtist) {
        songsByArtist.sortedWith(
            compareBy<SongEntity> { it.displayAlbum }
                .thenBy { it.displayTrack.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.displayTitle }
        )
    }

    LaunchedEffect(artist.name) {
        viewModel.loadArtistSummary(artist.name)
    }

    val summary by viewModel.artistSummary.collectAsState()
    val isSearchingSummary by viewModel.isSearchingArtistSummary.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editBiographyText by remember { mutableStateOf("") }
    var isBiographyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Artist Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(LocalAccentColor.current.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (artist.artworkUri != null) {
                    AsyncImage(
                        model = artist.artworkUri,
                        contentDescription = "Avatar for ${artist.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalAccentColor.current
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
            val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
            Text(
                text = "$albumText • $songText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalAccentColor.current,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play All", fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onShufflePlay,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle", fontWeight = FontWeight.Bold)
            }
        }

        // Biography / Artist Summary Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Header of Biography card with buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI biography",
                            tint = LocalAccentColor.current,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Artist Biography",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Refresh button
                        IconButton(
                            onClick = { viewModel.searchArtistSummaryOnline(artist.name) },
                            modifier = Modifier.size(28.dp),
                            enabled = !isSearchingSummary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate with Gemini AI",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Edit button
                        IconButton(
                            onClick = {
                                editBiographyText = summary ?: ""
                                showEditDialog = true
                            },
                            modifier = Modifier.size(28.dp),
                            enabled = !isSearchingSummary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit biography",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearchingSummary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            color = LocalAccentColor.current,
                            trackColor = LocalAccentColor.current.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "Consulting Gemini AI...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val displaySummary = summary
                    if (displaySummary.isNullOrBlank()) {
                        Text(
                            text = "No biography available. Tap the refresh icon to search and generate one automatically with Gemini AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        // Display with expand / collapse
                        val maxLines = if (isBiographyExpanded) Int.MAX_VALUE else 3
                        Text(
                            text = displaySummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isBiographyExpanded) "Show Less" else "Read More",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = LocalAccentColor.current,
                            modifier = Modifier
                                .clickable { isBiographyExpanded = !isBiographyExpanded }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Edit Biography Dialog
        if (showEditDialog) {
            Dialog(onDismissRequest = { showEditDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Edit Artist Biography",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = editBiographyText,
                            onValueChange = { editBiographyText = it },
                            placeholder = { Text("Write something about ${artist.name}...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveManualArtistSummary(artist.name, editBiographyText)
                                    showEditDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LocalAccentColor.current
                                )
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }

        // Albums Horizontal Row (if any)
        if (albumsByArtist.isNotEmpty()) {
            OniSectionHeader(
                title = "Albums",
                modifier = Modifier.padding(top = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(albumsByArtist, key = { it.albumKey }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = {} // Not clickable this stage per spec
                    )
                }
            }
        }

        // Songs Header
        OniSectionHeader(
            title = "Songs",
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        // Songs List
        Box(modifier = Modifier.weight(1f)) {
            com.example.ui.screens.SongsListView(
                songs = sortedSongs,
                viewModel = viewModel,
                sortBy = "title",
                isSortAscending = true,
                onShowTrackMenu = onShowTrackMenu,
                layoutMode = layoutMode
            )
        }
    }
}
