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
    songs: List<SongEntity>, sortedSongs: List<SongEntity>, currentSong: SongEntity?, isPlaying: Boolean,
    lastPlayedSong: SongEntity?, recentlyPlayedSongs: List<SongEntity>, mostPlayedSongs: List<SongEntity>,
    recentlyAddedSongs: List<SongEntity>, uniqueArtistsCount: Int, uniqueAlbumsCount: Int, favoritesCount: Int,
    searchQuery: String, onSearchQueryChange: (String) -> Unit, isScanning: Boolean,
    showOptionsMenu: () -> Unit, onRescan: () -> Unit, layoutMode: String, onToggleLayoutMode: () -> Unit,
    categoryList: List<CategoryInfo>, onSelectCategory: (Int) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit, onShowTrackMenu: (SongEntity) -> Unit,
    albumUiModels: List<AlbumUiModel>, artistUiModels: List<ArtistUiModel>, position: Long = 0L,
    duration: Long = 0L, isPreparing: Boolean = false, onTogglePlayPause: () -> Unit, onOpenPlayer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = OniSkin.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
    ) {
        item(key = "dashboard_header") { LibraryDashboardHeader(isScanning, showOptionsMenu, onRescan) }
        item(key = "dashboard_search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search songs, artists, albums...", style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textTertiary) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.screenHorizontal).testTag("search_input"),
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = OniSkin.colors.textSecondary) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton({ onSearchQueryChange("") }) { Icon(Icons.Default.Clear, "Clear search", tint = OniSkin.colors.textSecondary) } },
                singleLine = true,
                shape = OniSkin.shapes.full,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.45f), unfocusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.32f),
                    focusedBorderColor = OniSkin.colors.primary.copy(alpha = 0.5f), unfocusedBorderColor = OniSkin.colors.outline.copy(alpha = 0.2f),
                    focusedTextColor = OniSkin.colors.textPrimary, unfocusedTextColor = OniSkin.colors.textPrimary, cursorColor = OniSkin.colors.primary
                )
            )
        }

        if (searchQuery.isNotBlank()) {
            item(key = "search_header") { Text("Found ${sortedSongs.size} tracks", Modifier.padding(horizontal = OniSkin.spacing.screenHorizontal), OniSkin.typography.titleMedium, FontWeight.Bold, OniSkin.colors.textPrimary) }
            if (sortedSongs.isEmpty()) {
                item(key = "search_no_results") { LibraryEmptyState("No matches found", "No tracks found matching \"$searchQuery\"", Icons.Default.Search) }
            } else {
                items(sortedSongs, key = { "search_${it.id}" }) { song -> SongRow(song, song.id == currentSong?.id, isPlaying, { onPlaySong(song, sortedSongs) }, { onShowTrackMenu(song) }) }
            }
        } else if (songs.isEmpty()) {
            item(key = "empty_library_state") { LibraryEmptyState("Your Library is Empty", "No audio files were found. Scan your local storage to get started.", Icons.Default.LibraryMusic, "Scan Local Storage", onRescan) }
        } else {
            val activeHeroSong = currentSong ?: lastPlayedSong
            if (activeHeroSong != null) {
                item(key = "continue_listening_hero") {
                    val active = currentSong?.id == activeHeroSong.id
                    ContinueListeningHeroV2(activeHeroSong, isPlaying && active, if (active) position else 0L, if (active) duration else activeHeroSong.duration, { if (active) onTogglePlayPause() else onPlaySong(activeHeroSong, songs) }, onOpenPlayer, Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.screenHorizontal), isPreparing && active)
                }
            }

            item(key = "library_shortcuts") {
                LibrarySection("Browse your library") {
                    LazyRow(contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        item("songs") { LibraryShortcutCard("Songs", "${songs.size}", Icons.Default.MusicNote) { onSelectCategory(0) } }
                        item("folders") { LibraryShortcutCard("Folders", "", Icons.Default.Folder) { onSelectCategory(1) } }
                        item("albums") { LibraryShortcutCard("Albums", "$uniqueAlbumsCount", Icons.Default.Album) { onSelectCategory(2) } }
                        item("artists") { LibraryShortcutCard("Artists", "$uniqueArtistsCount", Icons.Default.Person) { onSelectCategory(3) } }
                        item("genres") { LibraryShortcutCard("Genres", "", Icons.Default.Category) { onSelectCategory(4) } }
                        item("playlists") { LibraryShortcutCard("Playlists", "Browse lists", Icons.AutoMirrored.Filled.QueueMusic) { onSelectCategory(8) } }
                    }
                }
            }

            item(key = "made_for_you") {
                LibrarySection("Made For You", "Your listening, organized", { SectionAction("View all") { onSelectCategory(9) } }) {
                    LazyRow(contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        item("most_played") { MadeForYouCompactCard("Most Played", "${mostPlayedSongs.size} tracks", Icons.Default.Whatshot) { onSelectCategory(6) } }
                        item("recently_played") { MadeForYouCompactCard("Recently Played", "${recentlyPlayedSongs.size} tracks", Icons.Default.Schedule) { onSelectCategory(9) } }
                        item("recently_added") { MadeForYouCompactCard("Recently Added", "Smart list", Icons.Default.Schedule) { onSelectCategory(9) } }
                        item("high_rating") { MadeForYouCompactCard("High Rated", "4+ star songs", Icons.Default.Star) { onSelectCategory(9) } }
                        item("never_played") { MadeForYouCompactCard("Never Played", "Fresh discoveries", Icons.Default.Explore) { onSelectCategory(9) } }
                    }
                }
            }

            if (recentlyPlayedSongs.isNotEmpty()) item(key = "recently_played") {
                LibrarySection("Recently Played", trailing = { SectionAction("View all") { onSelectCategory(9) } }) {
                    LazyRow(contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)) {
                        items(recentlyPlayedSongs.take(12), key = { "recent_${it.id}" }) { song -> HorizontalSongCard(song) { onPlaySong(song, recentlyPlayedSongs) } }
                    }
                }
            }
            if (recentlyAddedSongs.isNotEmpty()) item(key = "recently_added") {
                LibrarySection("Recently Added", trailing = { SectionAction("View all") { onSelectCategory(9) } }) {
                    LazyRow(contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)) {
                        items(recentlyAddedSongs.take(12), key = { "added_${it.id}" }) { song -> HorizontalSongCard(song) { onPlaySong(song, recentlyAddedSongs) } }
                    }
                }
            }
        }
    }
}

@Composable private fun LibraryDashboardHeader(isScanning: Boolean, showOptionsMenu: () -> Unit, onRescan: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = OniSkin.spacing.screenHorizontal, end = OniSkin.spacing.screenHorizontal, top = OniSkin.spacing.lg, bottom = OniSkin.spacing.xs), Alignment.CenterVertically, Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(greetingForTime(), OniSkin.typography.labelLarge, FontWeight.Bold, OniSkin.colors.primary)
            Text("Your Library", OniSkin.typography.displayMedium, FontWeight.Bold, OniSkin.colors.textPrimary)
            Text("Music for every moment", OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            OniSurface(OniSurfaceVariant.Soft, OniSkin.shapes.full) { IconButton(showOptionsMenu, Modifier.size(48.dp)) { Icon(Icons.Default.Tune, "Library options", tint = OniSkin.colors.primary) } }
            OniSurface(OniSurfaceVariant.Soft, OniSkin.shapes.full) { IconButton(onRescan, Modifier.size(48.dp)) { if (isScanning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = OniSkin.colors.primary) else Icon(Icons.Default.Refresh, "Scan library", tint = OniSkin.colors.primary) } }
        }
    }
}

@Composable private fun LibrarySection(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.screenHorizontal), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) { Text(title, OniSkin.typography.titleLarge, FontWeight.Bold, OniSkin.colors.textPrimary); if (subtitle != null) Text(subtitle, OniSkin.typography.caption, color = OniSkin.colors.textSecondary) }
            trailing?.invoke()
        }
        content()
    }
}

@Composable private fun SectionAction(label: String, onClick: () -> Unit) {
    TextButton(onClick) { Text(label, OniSkin.typography.labelLarge, FontWeight.SemiBold, OniSkin.colors.primary); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = OniSkin.colors.primary) }
}

@Composable private fun LibraryShortcutCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OniSurface(Modifier.width(112.dp).height(112.dp).semantics(mergeDescendants = true) { role = Role.Button; contentDescription = if (count.isBlank()) title else "$title, $count" }.clickable(onClick), OniSurfaceVariant.Soft, OniSkin.shapes.card) {
        Column(Modifier.fillMaxSize().padding(OniSkin.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            OniSurface(OniSurfaceVariant.Flat, OniSkin.shapes.full, containerColor = OniSkin.colors.primaryContainer) { Box(Modifier.size(44.dp), Alignment.Center) { Icon(icon, null, tint = OniSkin.colors.primary, modifier = Modifier.size(22.dp)) } }
            Spacer(Modifier.height(OniSkin.spacing.xs)); Text(title, OniSkin.typography.labelLarge, FontWeight.SemiBold, OniSkin.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count.isNotBlank()) Text(count, OniSkin.typography.caption, color = OniSkin.colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable private fun MadeForYouCompactCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OniSurface(Modifier.width(168.dp).height(112.dp).semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "$title, $count" }.clickable(onClick), OniSurfaceVariant.Soft, OniSkin.shapes.card) {
        Column(Modifier.fillMaxSize().padding(OniSkin.spacing.md), verticalArrangement = Arrangement.SpaceBetween) {
            OniSurface(OniSurfaceVariant.Flat, OniSkin.shapes.full, containerColor = OniSkin.colors.primaryContainer) { Box(Modifier.size(40.dp), Alignment.Center) { Icon(icon, null, tint = OniSkin.colors.primary, modifier = Modifier.size(21.dp)) } }
            Column { Text(title, OniSkin.typography.bodyMedium, FontWeight.Bold, OniSkin.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(count, OniSkin.typography.caption, color = OniSkin.colors.textSecondary, maxLines = 1) }
        }
    }
}
