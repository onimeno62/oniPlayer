package com.example.ui.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.data.preferences.LibraryPreferencesStore
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.components.*
import com.example.ui.library.hero.ContinueListeningHeroV2
import com.example.ui.library.model.DashboardSection
import com.example.ui.library.model.encodeDashboardSections
import com.example.ui.library.model.parseDashboardSections
import com.example.ui.screens.*
import com.example.ui.theme.OniSkin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private const val KEY_RESUME = "continue_listening_hero"
private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

// Category indices (see LibraryHostScreen)
private const val CAT_ALL = 0
private const val CAT_FOLDERS = 1
private const val CAT_ALBUMS = 2
private const val CAT_ARTISTS = 3
private const val CAT_GENRES = 4
private const val CAT_FAVORITES = 5
private const val CAT_MOST_PLAYED = 6
private const val CAT_RECENT = 7
private const val CAT_PLAYLISTS = 8
private const val CAT_SMART = 9
private const val CAT_HIGH_RATED = 12
private const val CAT_NEVER = 13

/** Library counts shown in the Browse grid. */
data class DashboardCounts(
    val albums: Int,
    val artists: Int,
    val folders: Int,
    val genres: Int,
    val playlists: Int
)

private data class BrowseEntry(val title: String, val count: Int, val icon: ImageVector, val category: Int)

/** MediaStore DATE_ADDED is seconds; our own default is millis. Normalize to millis. */
private fun normalizeEpochMs(value: Long): Long = if (value in 1 until 100_000_000_000L) value * 1000 else value

@OptIn(ExperimentalMaterial3Api::class)
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
    favoriteSongs: List<SongEntity>,
    highRatedSongs: List<SongEntity>,
    neverPlayedSongs: List<SongEntity>,
    counts: DashboardCounts,
    searchQuery: String,
    isScanning: Boolean,
    showOptionsMenu: () -> Unit,
    onRescan: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onShowTrackMenu: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    position: Long,
    duration: Long,
    isPreparing: Boolean,
    onTogglePlayPause: () -> Unit,
    onReplay: () -> Unit,
    onOpenPlayer: () -> Unit,
    jumpBackLabel: String? = null,
    onJumpBack: () -> Unit = {},
    onResumeCardVisibleChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val sectionsRaw by remember { LibraryPreferencesStore.dashboardSections(context) }
        .collectAsStateWithLifecycle(initialValue = "")
    val sections = remember(sectionsRaw) { parseDashboardSections(sectionsRaw) }
    var showCustomize by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val favoriteIds = remember(favoriteSongs) { favoriteSongs.mapTo(HashSet()) { it.id } }
    val heroSong = currentSong ?: lastPlayedSong
    val resumeEnabled = sections.any { it.section == DashboardSection.RESUME && it.visible }
    val showHero = searchQuery.isBlank() && songs.isNotEmpty() && heroSong != null && resumeEnabled

    // Report whether the resume card is on screen so the app-level mini-player can stay hidden
    // while it is (they show the same thing).
    val latestOnResumeVisible by rememberUpdatedState(onResumeCardVisibleChange)
    LaunchedEffect(listState, showHero) {
        snapshotFlow { showHero && listState.layoutInfo.visibleItemsInfo.any { it.key == KEY_RESUME } }
            .distinctUntilChanged()
            .collect { latestOnResumeVisible(it) }
    }
    DisposableEffect(Unit) {
        onDispose { latestOnResumeVisible(true) }
    }

    // Pull-to-refresh: only show the indicator for user-initiated refreshes, not the launch scan.
    var userRefreshing by remember { mutableStateOf(false) }
    val scanningNow by rememberUpdatedState(isScanning)
    LaunchedEffect(isScanning) { if (!isScanning) userRefreshing = false }
    LaunchedEffect(userRefreshing) {
        if (userRefreshing) {
            delay(1500)
            if (!scanningNow) userRefreshing = false
        }
    }

    val stats = remember(songs) { computeListeningStats(songs) }
    val libraryStale = remember(recentlyAddedSongs) {
        val newest = recentlyAddedSongs.maxOfOrNull { normalizeEpochMs(it.dateAdded) } ?: 0L
        newest > 0L && System.currentTimeMillis() - newest > WEEK_MS
    }

    val playAll: () -> Unit = { songs.firstOrNull()?.let { onPlaySong(it, songs) } }
    val shuffleAll: () -> Unit = {
        val shuffled = songs.shuffled()
        shuffled.firstOrNull()?.let { onPlaySong(it, shuffled) }
    }

    PullToRefreshBox(
        isRefreshing = userRefreshing,
        onRefresh = {
            userRefreshing = true
            onRescan()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = OniSkin.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
        ) {
            // 1. Header: greeting + title, scan + menu
            item(key = "dashboard_header") {
                LibraryDashboardHeader(
                    isScanning = isScanning,
                    onRescan = onRescan,
                    onOpenLibraryOptions = showOptionsMenu,
                    onCustomizeHome = { showCustomize = true }
                )
            }

            // 2. Permanent search pill + compact Shuffle / Play All
            item(key = "dashboard_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OniSkin.spacing.screenHorizontal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    DashboardSearchPill(onClick = onOpenSearch, modifier = Modifier.weight(1f))
                    DashboardActionIcon(
                        icon = Icons.Default.Shuffle,
                        label = "Shuffle library",
                        onClick = shuffleAll,
                        enabled = songs.isNotEmpty()
                    )
                    DashboardActionIcon(
                        icon = Icons.Default.PlayArrow,
                        label = "Play all",
                        onClick = playAll,
                        emphasized = true,
                        enabled = songs.isNotEmpty()
                    )
                }
            }

            when {
                searchQuery.isNotBlank() -> {
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
                }

                songs.isEmpty() && isScanning -> {
                    item(key = "dashboard_skeleton") { DashboardSkeleton() }
                }

                songs.isEmpty() -> {
                    item(key = "empty_library_state") {
                        LibraryEmptyState(
                            title = "Your Library is Empty",
                            message = "No audio files were found. Scan your local storage to get started.",
                            icon = Icons.Default.LibraryMusic,
                            actionLabel = "Scan Local Storage",
                            onActionClick = onRescan
                        )
                    }
                }

                else -> {
                    sections.filter { it.visible }.forEach { pref ->
                        when (pref.section) {
                            DashboardSection.RESUME -> if (heroSong != null) {
                                item(key = KEY_RESUME) {
                                    val active = currentSong?.id == heroSong.id
                                    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                                        ContinueListeningHeroV2(
                                            song = heroSong,
                                            isPlaying = isPlaying && active,
                                            position = if (active) position else 0L,
                                            duration = if (active) duration else heroSong.duration,
                                            onPlayPauseClick = {
                                                if (active) onTogglePlayPause() else onPlaySong(heroSong, songs)
                                            },
                                            onOpenNowPlaying = onOpenPlayer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = OniSkin.spacing.screenHorizontal),
                                            isPreparing = active && isPreparing,
                                            onReplayClick = if (active) onReplay else null
                                        )
                                        if (jumpBackLabel != null) {
                                            JumpBackChip(
                                                label = jumpBackLabel,
                                                onClick = onJumpBack,
                                                modifier = Modifier.padding(horizontal = OniSkin.spacing.screenHorizontal)
                                            )
                                        }
                                    }
                                }
                            }

                            DashboardSection.BROWSE -> item(key = "browse") {
                                LibrarySection(title = "Browse") {
                                    BrowseGrid(
                                        entries = listOf(
                                            BrowseEntry("Songs", songs.size, Icons.Default.MusicNote, CAT_ALL),
                                            BrowseEntry("Albums", counts.albums, Icons.Default.Album, CAT_ALBUMS),
                                            BrowseEntry("Artists", counts.artists, Icons.Default.Person, CAT_ARTISTS),
                                            BrowseEntry("Folders", counts.folders, Icons.Default.Folder, CAT_FOLDERS),
                                            BrowseEntry("Genres", counts.genres, Icons.Default.Category, CAT_GENRES),
                                            BrowseEntry("Playlists", counts.playlists, Icons.AutoMirrored.Filled.QueueMusic, CAT_PLAYLISTS)
                                        ),
                                        onSelectCategory = onSelectCategory
                                    )
                                }
                            }

                            DashboardSection.RECENTLY_PLAYED -> {
                                val displayRecent = recentlyPlayedSongs.ifEmpty { songs }
                                if (displayRecent.isNotEmpty()) {
                                    item(key = "recently_played") {
                                        LibrarySection(
                                            title = "Recently Played",
                                            trailing = { SectionAction(label = "View all") { onSelectCategory(CAT_SMART) } }
                                        ) {
                                            SongCarousel(
                                                songs = displayRecent.take(12),
                                                queue = displayRecent,
                                                keyPrefix = "recent",
                                                currentSong = currentSong,
                                                isPlaying = isPlaying,
                                                favoriteIds = favoriteIds,
                                                onPlaySong = onPlaySong,
                                                onPlayNext = onPlayNext,
                                                onAddToQueue = onAddToQueue,
                                                onToggleFavorite = onToggleFavorite
                                            )
                                        }
                                    }
                                }
                            }

                            DashboardSection.MADE_FOR_YOU -> item(key = "made_for_you") {
                                LibrarySection(
                                    title = "Made For You",
                                    trailing = { SectionAction(label = "View all") { onSelectCategory(CAT_SMART) } }
                                ) {
                                    MadeForYouGrid(
                                        mostPlayed = mostPlayedSongs,
                                        favorites = favoriteSongs,
                                        neverPlayed = neverPlayedSongs,
                                        highRated = highRatedSongs,
                                        onSelectCategory = onSelectCategory,
                                        onPlaySong = onPlaySong
                                    )
                                }
                            }

                            DashboardSection.LISTENING_STATS -> if (stats != null) {
                                item(key = "listening_stats") {
                                    LibrarySection(title = "Your listening") {
                                        ListeningStatsRow(
                                            stats = stats,
                                            modifier = Modifier.padding(horizontal = OniSkin.spacing.screenHorizontal)
                                        )
                                    }
                                }
                            }

                            DashboardSection.RECENTLY_ADDED -> if (recentlyAddedSongs.isNotEmpty()) {
                                item(key = "recently_added") {
                                    LibrarySection(
                                        title = "Recently Added",
                                        trailing = { SectionAction(label = "View all") { onSelectCategory(CAT_RECENT) } }
                                    ) {
                                        SongCarousel(
                                            songs = recentlyAddedSongs.take(12),
                                            queue = recentlyAddedSongs,
                                            keyPrefix = "added",
                                            currentSong = currentSong,
                                            isPlaying = isPlaying,
                                            favoriteIds = favoriteIds,
                                            onPlaySong = onPlaySong,
                                            onPlayNext = onPlayNext,
                                            onAddToQueue = onAddToQueue,
                                            onToggleFavorite = onToggleFavorite
                                        )
                                        if (libraryStale) {
                                            TextButton(
                                                onClick = onRescan,
                                                enabled = !isScanning,
                                                modifier = Modifier.padding(horizontal = OniSkin.spacing.xs)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                                                Text("Scan for new music", style = OniSkin.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "customize_footer") {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { showCustomize = true }) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = OniSkin.colors.textSecondary)
                                Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                                Text("Customize home", style = OniSkin.typography.labelLarge, color = OniSkin.colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomize) {
        DashboardCustomizeDialog(
            current = sections,
            onSave = {
                LibraryPreferencesStore.setDashboardSections(context, it.encodeDashboardSections())
                showCustomize = false
            },
            onDismiss = { showCustomize = false }
        )
    }
}

@Composable
private fun LibraryDashboardHeader(
    isScanning: Boolean,
    onRescan: () -> Unit,
    onOpenLibraryOptions: () -> Unit,
    onCustomizeHome: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = OniSkin.spacing.screenHorizontal,
                end = OniSkin.spacing.screenHorizontal,
                top = OniSkin.spacing.lg
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
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
        }

        OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.full) {
            IconButton(onClick = onRescan, modifier = Modifier.size(48.dp), enabled = !isScanning) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OniSkin.colors.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan library",
                        tint = OniSkin.colors.textPrimary
                    )
                }
            }
        }

        Box {
            OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.full) {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Library menu",
                        tint = OniSkin.colors.textPrimary
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Layout & sorting") },
                    leadingIcon = { Icon(Icons.Default.SettingsSuggest, contentDescription = null) },
                    onClick = { menuOpen = false; onOpenLibraryOptions() }
                )
                DropdownMenuItem(
                    text = { Text("Customize home") },
                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    onClick = { menuOpen = false; onCustomizeHome() }
                )
            }
        }
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
                .heightIn(min = 40.dp)
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalAlignment = Alignment.CenterVertically
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

/** 3x2 grid: every category visible at once, each with its count. */
@Composable
private fun BrowseGrid(entries: List<BrowseEntry>, onSelectCategory: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
    ) {
        entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                row.forEach { entry ->
                    BrowseTile(entry = entry, onClick = { onSelectCategory(entry.category) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BrowseTile(entry: BrowseEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val countText = String.format("%,d", entry.count)
    OniSurface(
        modifier = modifier
            .height(92.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${entry.title}, $countText" },
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OniSurface(
                variant = OniSurfaceVariant.Flat,
                shape = OniSkin.shapes.full,
                containerColor = OniSkin.colors.primaryContainer
            ) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(entry.icon, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
            Text(
                text = entry.title,
                style = OniSkin.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SongCarousel(
    songs: List<SongEntity>,
    queue: List<SongEntity>,
    keyPrefix: String,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = OniSkin.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
    ) {
        items(songs, key = { "${keyPrefix}_${it.id}" }) { song ->
            DashboardCarouselCard(
                song = song,
                isCurrent = song.id == currentSong?.id,
                isPlaying = isPlaying,
                isFavorite = song.id in favoriteIds,
                onClick = { onPlaySong(song, queue) },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onToggleFavorite = { onToggleFavorite(song) }
            )
        }
    }
}

@Composable
private fun MadeForYouGrid(
    mostPlayed: List<SongEntity>,
    favorites: List<SongEntity>,
    neverPlayed: List<SongEntity>,
    highRated: List<SongEntity>,
    onSelectCategory: (Int) -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit
) {
    // Fixed hues (same palette family as the category list) so each mix is recognizable.
    val pink = Color(0xFFE91E63)
    val indigo = Color(0xFF5C6BC0)
    val teal = Color(0xFF009688)
    val amber = Color(0xFFFF9800)

    val play: (List<SongEntity>, Boolean) -> Unit = { list, shuffle ->
        val queue = if (shuffle) list.shuffled() else list
        queue.firstOrNull()?.let { onPlaySong(it, queue) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            MixCard(
                title = "Most Played",
                subtitle = if (mostPlayed.isEmpty()) "Play some music to fill this" else "${mostPlayed.size} tracks",
                icon = Icons.Default.Whatshot,
                tint = pink,
                isEmpty = mostPlayed.isEmpty(),
                onClick = { onSelectCategory(CAT_MOST_PLAYED) },
                onPlay = { play(mostPlayed, false) },
                modifier = Modifier.weight(1f)
            )
            MixCard(
                title = "Favorites",
                subtitle = if (favorites.isEmpty()) "Heart a song to start" else "${favorites.size} songs",
                icon = Icons.Default.Favorite,
                tint = indigo,
                isEmpty = favorites.isEmpty(),
                onClick = { onSelectCategory(CAT_FAVORITES) },
                onPlay = { play(favorites, false) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            MixCard(
                title = "Discover",
                subtitle = if (neverPlayed.isEmpty()) "You've heard everything" else "${neverPlayed.size} never played",
                icon = Icons.Default.Explore,
                tint = teal,
                isEmpty = neverPlayed.isEmpty(),
                onClick = { onSelectCategory(CAT_NEVER) },
                onPlay = { play(neverPlayed, true) },
                modifier = Modifier.weight(1f)
            )
            MixCard(
                title = "High Rated",
                subtitle = if (highRated.isEmpty()) "Rate songs 4+ stars" else "${highRated.size} top rated",
                icon = Icons.Default.Star,
                tint = amber,
                isEmpty = highRated.isEmpty(),
                onClick = { onSelectCategory(CAT_HIGH_RATED) },
                onPlay = { play(highRated, false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** "Jump back in": reopens the last album or playlist, not just the last song. */
@Composable
private fun JumpBackChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OniSurface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Jump back in to $label" },
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.full,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs)
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(16.dp))
            Text(
                text = "Jump back in",
                style = OniSkin.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary
            )
            Text(
                text = label,
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Placeholder layout shown while the first scan runs and the library is still empty. */
@Composable
private fun DashboardSkeleton() {
    val transition = rememberInfiniteTransition(label = "dashboard_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "dashboard_skeleton_alpha"
    )
    val placeholder = OniSkin.colors.outline.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(OniSkin.shapes.card)
                .background(placeholder)
        )
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clip(OniSkin.shapes.card)
                            .background(placeholder)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(OniSkin.artwork.shape)
                        .background(placeholder)
                )
            }
        }
    }
}
