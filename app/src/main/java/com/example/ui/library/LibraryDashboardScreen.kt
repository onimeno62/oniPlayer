package com.example.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.components.*
import com.example.ui.library.hero.ContinueListeningHeroV2
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.screens.*
import com.example.ui.theme.OniSkin

@Composable
fun LibraryDashboardScreen(
    songs: List<SongEntity>,
    sortedSongs: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    lastPlayedSong: SongEntity?,
    recentlyPlayedSongs: List<SongEntity>,
    mostPlayedSongs: List<SongEntity>,
    recentlyAddedSongs: List<SongEntity>,
    uniqueArtistsCount: Int,
    uniqueAlbumsCount: Int,
    favoritesCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isScanning: Boolean,
    showOptionsMenu: () -> Unit,
    onRescan: () -> Unit,
    layoutMode: String,
    onToggleLayoutMode: () -> Unit,
    categoryList: List<CategoryInfo>,
    onSelectCategory: (Int) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    albumUiModels: List<AlbumUiModel>,
    artistUiModels: List<ArtistUiModel>,
    position: Long = 0L,
    duration: Long = 0L,
    isPreparing: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    // Existing parameters are intentionally retained so the surrounding Library flow does not change.
    @Suppress("UNUSED_PARAMETER")
    fun preserveExistingContract() {
        Unit
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = OniSkin.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
    ) {
        item(key = "dashboard_header") {
            LibraryDashboardHeader(
                isScanning = isScanning,
                showOptionsMenu = showOptionsMenu,
                onRescan = onRescan
            )
        }

        item(key = "dashboard_search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums...",
                        style = OniSkin.typography.bodyLarge,
                        color = OniSkin.colors.textTertiary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.screenHorizontal)
                    .testTag("search_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = OniSkin.colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = OniSkin.colors.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = OniSkin.shapes.full,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.32f),
                    focusedBorderColor = OniSkin.colors.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = OniSkin.colors.outline.copy(alpha = 0.2f),
                    focusedTextColor = OniSkin.colors.textPrimary,
                    unfocusedTextColor = OniSkin.colors.textPrimary,
                    cursorColor = OniSkin.colors.primary
                )
            )
        }

        if (searchQuery.isNotBlank()) {
            item(key = "search_header") {
                Text(
                    text = "Found ${sortedSongs.size} tracks",
                    modifier = Modifier.padding(horizontal = OniSkin.spacing.screenHorizontal),
                    style = OniSkin.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
            }

            if (sortedSongs.isEmpty()) {
                item(key = "search_no_results") {
                    LibraryEmptyState(
                        title = "No matches found",
                        message = "No tracks found matching \"$searchQuery\"",
                        icon = Icons.Default.Search
                    )
                }
            } else {
                items(sortedSongs, key = { "search_${it.id}" }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSong?.id,
                        isPlaying = isPlaying,
                        onClick = { onPlaySong(song, sortedSongs) },
                        onShowMenu = { onShowTrackMenu(song) }
                    )
                }
            }
        } else if (songs.isEmpty()) {
            item(key = "empty_library_state") {
                LibraryEmptyState(
                    title = "Your Library is Empty",
                    message = "No audio files were found. Scan your local storage to get started.",
                    icon = Icons.Default.LibraryMusic,
                    actionLabel = "Scan Local Storage",
                    onActionClick = onRescan
                )
            }
        } else {
            val activeHeroSong = currentSong ?: lastPlayedSong

            if (activeHeroSong != null) {
                item(key = "continue_listening_hero") {
                    val isSongActive = currentSong?.id == activeHeroSong.id
                    ContinueListeningHeroV2(
                        song = activeHeroSong,
                        isPlaying = isPlaying && isSongActive,
                        position = if (isSongActive) position else 0L,
                        duration = if (isSongActive) duration else activeHeroSong.duration,
                        isPreparing = isPreparing && isSongActive,
                        onPlayPauseClick = {
                            if (isSongActive) onTogglePlayPause()
                            else onPlaySong(activeHeroSong, songs)
                        },
                        onOpenNowPlaying = onOpenPlayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OniSkin.spacing.screenHorizontal)
                    )
                }
            }

            item(key = "library_shortcuts") {
                LibrarySection(
                    title = "Browse your library",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        item("songs") {
                            LibraryShortcutCard("Songs", "${songs.size}", Icons.Default.MusicNote) {
                                onSelectCategory(0)
                            }
                        }
                        item("folders") {
                            LibraryShortcutCard("Folders", "", Icons.Default.Folder) {
                                onSelectCategory(1)
                            }
                        }
                        item("albums") {
                            LibraryShortcutCard("Albums", "$uniqueAlbumsCount", Icons.Default.Album) {
                                onSelectCategory(2)
                            }
                        }
                        item("artists") {
                            LibraryShortcutCard("Artists", "$uniqueArtistsCount", Icons.Default.Person) {
                                onSelectCategory(3)
                            }
                        }
                        item("genres") {
                            LibraryShortcutCard("Genres", "", Icons.Default.Category) {
                                onSelectCategory(4)
                            }
                        }
                        item("favorites") {
                            LibraryShortcutCard("Favorites", "$favoritesCount", Icons.Default.Favorite) {
                                onSelectCategory(5)
                            }
                        }
                    }
                }
            }

            item(key = "made_for_you") {
                LibrarySection(
                    title = "Made For You",
                    subtitle = "Your listening, organized",
                    trailing = {
                        SectionAction(label = "View all") { onSelectCategory(6) }
                    }
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        item("most_played") {
                            MadeForYouCompactCard(
                                title = "Most Played",
                                count = "${mostPlayedSongs.size} tracks",
                                icon = Icons.Default.Whatshot,
                                onClick = { onSelectCategory(6) }
                            )
                        }
                        item("recently_played") {
                            MadeForYouCompactCard(
                                title = "Recently Played",
                                count = "${recentlyPlayedSongs.size} tracks",
                                icon = Icons.Default.Schedule,
                                onClick = { onSelectCategory(9) }
                            )
                        }
                        item("favorites") {
                            MadeForYouCompactCard(
                                title = "Favorites",
                                count = "$favoritesCount songs",
                                icon = Icons.Default.Favorite,
                                onClick = { onSelectCategory(5) }
                            )
                        }
                        item("playlists") {
                            MadeForYouCompactCard(
                                title = "Playlists",
                                count = "Browse lists",
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                onClick = { onSelectCategory(8) }
                            )
                        }
                    }
                }
            }

            if (recentlyPlayedSongs.isNotEmpty()) {
                item(key = "recently_played") {
                    LibrarySection(
                        title = "Recently Played",
                        trailing = {
                            SectionAction(label = "View all") { onSelectCategory(9) }
                        }
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
                        ) {
                            items(recentlyPlayedSongs.take(12), key = { "recent_${it.id}" }) { song ->
                                HorizontalSongCard(
                                    song = song,
                                    onClick = { onPlaySong(song, recentlyPlayedSongs) }
                                )
                            }
                        }
                    }
                }
            }

            if (recentlyAddedSongs.isNotEmpty()) {
                item(key = "recently_added") {
                    LibrarySection(
                        title = "Recently Added",
                        trailing = {
                            SectionAction(label = "View all") { onSelectCategory(7) }
                        }
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
                        ) {
                            items(recentlyAddedSongs.take(12), key = { "added_${it.id}" }) { song ->
                                HorizontalSongCard(
                                    song = song,
                                    onClick = { onPlaySong(song, recentlyAddedSongs) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryDashboardHeader(
    isScanning: Boolean,
    showOptionsMenu: () -> Unit,
    onRescan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = OniSkin.spacing.screenHorizontal,
                end = OniSkin.spacing.screenHorizontal,
                top = OniSkin.spacing.lg,
                bottom = OniSkin.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingForTime(),
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.primary
            )
            Text(
                text = "Your Library",
                style = OniSkin.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary
            )
            Text(
                text = "Music for every moment",
                style = OniSkin.typography.bodyMedium,
                color = OniSkin.colors.textSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.full) {
                IconButton(onClick = showOptionsMenu, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Library options",
                        tint = OniSkin.colors.primary
                    )
                }
            }
            OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.full) {
                IconButton(onClick = onRescan, modifier = Modifier.size(48.dp)) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = OniSkin.colors.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan library",
                            tint = OniSkin.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySection(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.textSecondary
                    )
                }
            }
            trailing?.invoke()
        }
        content()
    }
}

@Composable
private fun SectionAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            style = OniSkin.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = OniSkin.colors.primary
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = OniSkin.colors.primary
        )
    }
}

@Composable
private fun LibraryShortcutCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(112.dp)
            .height(112.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (count.isBlank()) title else "$title, $count"
            }
            .clickable(onClick = onClick),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OniSurface(
                variant = OniSurfaceVariant.Flat,
                shape = OniSkin.shapes.full,
                containerColor = OniSkin.colors.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OniSkin.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
            Text(
                text = title,
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count.isNotBlank()) {
                Text(
                    text = count,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MadeForYouCompactCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(168.dp)
            .height(112.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title, $count"
            }
            .clickable(onClick = onClick),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OniSurface(
                variant = OniSurfaceVariant.Flat,
                shape = OniSkin.shapes.full,
                containerColor = OniSkin.colors.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OniSkin.colors.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = OniSkin.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = count,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
