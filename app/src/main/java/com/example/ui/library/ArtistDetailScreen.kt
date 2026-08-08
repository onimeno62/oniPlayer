package com.example.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.entity.SongEntity
import com.example.ui.library.components.AlbumCard
import com.example.ui.library.components.OniSectionHeader
import com.example.ui.library.components.SongRow
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.theme.LocalAccentColor
import kotlinx.coroutines.launch

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
    val customArtworkUri by viewModel.artistArtworkUri.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var editBiographyText by remember { mutableStateOf("") }
    var isBiographyExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val artworkToDisplay = customArtworkUri ?: artist.artworkUri
    val accentColor = LocalAccentColor.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Full Width Background Image / Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            if (artworkToDisplay != null) {
                AsyncImage(
                    model = artworkToDisplay,
                    contentDescription = "Background for ${artist.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.4f),
                                    accentColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                )
            }

            // Dark/dimming gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // Main content column over the background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(110.dp))

            // Artist Information Overlay Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable {
                            viewModel.fetchOnlineArtistImages(artist.name)
                            showImagePickerDialog = true
                        }
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change picture",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$albumText • $songText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
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
        val accentColor = LocalAccentColor.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.06f), Color.Transparent),
                                center = Offset(size.width, 0f),
                                radius = size.width * 0.9f
                            )
                        )
                    }
                    .padding(16.dp)
            ) {
                // Background quote decoration
                Text(
                    text = "“",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 100.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = accentColor.copy(alpha = 0.04f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 30.dp)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ARTIST STORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Biography",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Refresh button
                            IconButton(
                                onClick = { viewModel.searchArtistSummaryOnline(artist.name) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
                                enabled = !isSearchingSummary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate biography",
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
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSearchingSummary) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Searching artist biography...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val displaySummary = summary
                        if (displaySummary.isNullOrBlank()) {
                            Text(
                                text = "No biography available. Tap the refresh icon above to automatically search and retrieve the biography from TheAudioDB.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp
                            )
                        } else {
                            val maxLines = if (isBiographyExpanded) Int.MAX_VALUE else 3
                            Text(
                                text = displaySummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isBiographyExpanded = !isBiographyExpanded }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isBiographyExpanded) "Read Less" else "Read Full Story",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                Icon(
                                    imageVector = if (isBiographyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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

        // Select Artist Image Dialog
        if (showImagePickerDialog) {
            val onlineImages by viewModel.onlineArtistImages.collectAsState()
            val isFetchingImages by viewModel.isFetchingArtistImages.collectAsState()

            Dialog(onDismissRequest = { showImagePickerDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Select Artist Picture",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Drag left or right to browse online pictures from TheAudioDB, then tap Save.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (isFetchingImages) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = LocalAccentColor.current,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Searching online images...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (onlineImages.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImageNotSupported,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No online pictures found for \"${artist.name}\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            val pagerState = rememberPagerState(pageCount = { onlineImages.size })

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = onlineImages[page],
                                            contentDescription = "Artist picture option",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                // Left Chevron Arrow
                                if (pagerState.currentPage > 0) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 12.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Previous photo",
                                            tint = Color.White
                                        )
                                    }
                                }

                                // Right Chevron Arrow
                                if (pagerState.currentPage < onlineImages.size - 1) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 12.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Next photo",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Dot Page Indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(onlineImages.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(if (isSelected) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) LocalAccentColor.current else MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.2f
                                                )
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { showImagePickerDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        val selectedUrl = onlineImages[pagerState.currentPage]
                                        viewModel.saveArtistArtworkUri(artist.name, selectedUrl)
                                        showImagePickerDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LocalAccentColor.current
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save Picture")
                                }
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
                modifier = Modifier.padding(top = 8.dp)
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
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        // Songs List
        if (sortedSongs.isEmpty()) {
            com.example.ui.library.components.LibraryEmptyState(
                title = "No songs found",
                message = "Scan your local storage to import music or add songs.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                icon = Icons.Default.LibraryMusic
            )
        } else if (layoutMode == "grid") {
            val songChunks = remember(sortedSongs) { sortedSongs.chunked(2) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp) // Provide ample bottom space for playback bar
            ) {
                songChunks.forEach { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { song ->
                            Box(modifier = Modifier.weight(1f)) {
                                ArtistSongGridCard(
                                    song = song,
                                    viewModel = viewModel,
                                    onShowTrackMenu = onShowTrackMenu,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying
                                )
                            }
                        }
                        if (chunk.size < 2) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp) // Provide ample bottom space for playback bar
            ) {
                sortedSongs.forEach { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSong?.id,
                        isPlaying = isPlaying,
                        onClick = { viewModel.playSong(song, sortedSongs) },
                        onShowMenu = { onShowTrackMenu(song) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
fun ArtistSongGridCard(
    song: SongEntity,
    viewModel: com.example.ui.viewmodel.MusicPlayerViewModel,
    onShowTrackMenu: (SongEntity) -> Unit,
    currentSong: SongEntity?,
    isPlaying: Boolean
) {
    val isCurrent = song.id == currentSong?.id
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { viewModel.playSong(song, listOf(song)) },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                LocalAccentColor.current.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        border = if (isCurrent) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = LocalAccentColor.current.copy(alpha = 0.3f)
            )
        } else null
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Cover art for ${song.displayTitle}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                )

                IconButton(
                    onClick = { onShowTrackMenu(song) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        com.example.ui.screens.PlayingEqualizerWave(
                            color = Color.White,
                            modifier = Modifier.size(16.dp, 12.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = song.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) LocalAccentColor.current else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.displayAlbum.ifBlank { "Unknown Album" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
