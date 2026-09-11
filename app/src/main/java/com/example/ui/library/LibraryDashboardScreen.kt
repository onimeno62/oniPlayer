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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = OniSkin.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
    ) {
        // 1. Dashboard Header (with SettingsSuggest trigger)
        item(key = "dashboard_header") {
            LibraryDashboardHeader(showOptionsMenu = showOptionsMenu)
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
            // 2. Library Overview: real library counts right above Continue Listening
            item(key = "library_overview") {
                LibraryOverviewBanner(
                    tracksCount = songs.size,
                    albumsCount = uniqueAlbumsCount,
                    artistsCount = uniqueArtistsCount,
                    favoritesCount = favoritesCount
                )
            }

            // 3. Continue Listening Hero
            val activeHeroSong = currentSong ?: lastPlayedSong
            if (activeHeroSong != null) {
                item(key = "continue_listening_hero") {
                    val active = currentSong?.id == activeHeroSong.id
                    ContinueListeningHeroV2(
                        song = activeHeroSong,
                        isPlaying = isPlaying && active,
                        position = if (active) position else 0L,
                        duration = if (active) duration else activeHeroSong.duration,
                        onPlayPauseClick = {
                            if (active) onTogglePlayPause() else onPlaySong(activeHeroSong, songs)
                        },
                        onOpenNowPlaying = onOpenPlayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OniSkin.spacing.screenHorizontal),
                        isPreparing = active && isPreparing
                    )
                }
            }

            // 4. Browse your library (Cards contain ONLY category icon and name)
            item(key = "library_shortcuts") {
                LibrarySection(title = "Browse your library") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        item("songs") {
                            LibraryBrowseCard(title = "Songs", icon = Icons.Default.MusicNote) {
                                onSelectCategory(0)
                            }
                        }
                        item("folders") {
                            LibraryBrowseCard(title = "Folders", icon = Icons.Default.Folder) {
                                onSelectCategory(1)
                            }
                        }
                        item("albums") {
                            LibraryBrowseCard(title = "Albums", icon = Icons.Default.Album) {
                                onSelectCategory(2)
                            }
                        }
                        item("artists") {
                            LibraryBrowseCard(title = "Artists", icon = Icons.Default.Person) {
                                onSelectCategory(3)
                            }
                        }
                        item("genres") {
                            LibraryBrowseCard(title = "Genres", icon = Icons.Default.Category) {
                                onSelectCategory(4)
                            }
                        }
                        item("playlists") {
                            LibraryBrowseCard(title = "Playlists", icon = Icons.AutoMirrored.Filled.QueueMusic) {
                                onSelectCategory(8)
                            }
                        }
                    }
                }
            }

            // 5. Recently Played (above Made For You)
            val displayRecentSongs = if (recentlyPlayedSongs.isNotEmpty()) recentlyPlayedSongs else songs
            if (displayRecentSongs.isNotEmpty()) {
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
                            items(displayRecentSongs.take(12), key = { "recent_${it.id}" }) { song ->
                                HorizontalSongCard(
                                    song = song,
                                    onClick = { onPlaySong(song, displayRecentSongs) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Made For You: EXACTLY Most Played, Favorites, High Rated, Never Played
            item(key = "made_for_you") {
                LibrarySection(
                    title = "Made For You",
                    subtitle = "Curated from your library listening",
                    trailing = {
                        SectionAction(label = "View all") { onSelectCategory(9) }
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
                        item("favorites") {
                            MadeForYouCompactCard(
                                title = "Favorites",
                                count = "$favoritesCount songs",
                                icon = Icons.Default.Favorite,
                                onClick = { onSelectCategory(5) }
                            )
                        }
                        item("high_rated") {
                            MadeForYouCompactCard(
                                title = "High Rated",
                                count = "4+ star tracks",
                                icon = Icons.Default.Star,
                                onClick = { onSelectCategory(12) }
                            )
                        }
                        item("never_played") {
                            MadeForYouCompactCard(
                                title = "Never Played",
                                count = "Unplayed tracks",
                                icon = Icons.Default.Explore,
                                onClick = { onSelectCategory(13) }
                            )
                        }
                    }
                }
            }

            // 7. Recently Added
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
    showOptionsMenu: () -> Unit
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

        OniSurface(
            variant = OniSurfaceVariant.Soft,
            shape = OniSkin.shapes.full
        ) {
            IconButton(
                onClick = showOptionsMenu,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsSuggest,
                    contentDescription = "Library settings",
                    tint = OniSkin.colors.primary
                )
            }
        }
    }
}

/**
 * Compact static Library Overview immediately above Continue Listening Hero.
 * Displays real library counts: Tracks, Albums, Artists, Favorites in a calm, compact format.
 */
@Composable
private fun LibraryOverviewBanner(
    tracksCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    favoritesCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val overviewText = String.format(
            "%,d Tracks  ·  %,d Albums  ·  %,d Artists  ·  %,d Favorites",
            tracksCount,
            albumsCount,
            artistsCount,
            favoritesCount
        )
        Text(
            text = overviewText,
            style = OniSkin.typography.caption,
            fontWeight = FontWeight.Medium,
            color = OniSkin.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibrarySection(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
    ) {
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

/**
 * Clean Browse your library card containing ONLY icon and category name.
 * 48dp+ touch target, no subtitles, no counts, no additional metadata.
 */
@Composable
private fun LibraryBrowseCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(108.dp)
            .height(96.dp)
            .defaultMinSize(minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = title
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
            .defaultMinSize(minHeight = 48.dp)
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
                    maxLines = 1
                )
            }
        }
    }
}
