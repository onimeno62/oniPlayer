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
        com.oniplayer.ui.library.components.LibraryEmptyState(
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
                com.oniplayer.ui.library.components.SongRow(
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
            com.oniplayer.ui.library.components.LibraryEmptyState(
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
