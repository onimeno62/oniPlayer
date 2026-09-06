package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.player.components.*
import com.example.ui.player.model.PlayerUiState
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlin.math.absoluteValue

/**
 * Primary Player Screen for oniPlayer in Default Skin.
 *
 * Coordinates player presentation components, local dialog states, and user actions.
 * Consumes [PlayerUiState] from [MusicPlayerViewModel] via [collectAsStateWithLifecycle].
 */
@Composable
fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.playerUiState.collectAsStateWithLifecycle()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showKaraoke by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button to return to library screen context
    BackHandler(enabled = true) {
        viewModel.goBackToLibraryContext()
    }

    val song = uiState.currentSong

    // Calculate active line preview if synced lyrics are present
    val currentLyricLine = remember(song?.lyrics, uiState.position) {
        val lyrics = song?.lyrics
        if (!lyrics.isNullOrBlank() && LyricsHelper.isSynced(lyrics)) {
            val parsed = LyricsHelper.parseLrc(lyrics)
            val idx = LyricsHelper.getActiveLineIndex(parsed, uiState.position)
            if (idx in parsed.indices) parsed[idx].text else null
        } else null
    }

    PlayerContent(
        uiState = uiState,
        currentLyricLine = currentLyricLine,
        onNavigateBack = { viewModel.goBackToLibraryContext() },
        onOpenQueue = { showQueueSheet = true },
        onDeleteClick = { showDeleteDialog = true },
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSkipNext = { viewModel.skipNext() },
        onSkipPrevious = { viewModel.skipPrevious() },
        onSeek = { viewModel.seekTo(it) },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onToggleRepeat = { viewModel.toggleRepeat() },
        onToggleFavorite = { song?.id?.let { viewModel.toggleFavorite(it) } },
        onToggleFloatingLyrics = { viewModel.setFloatingLyricsEnabled(!uiState.floatingLyricsEnabled) },
        onOpenSleepTimer = { showSleepTimer = true },
        onOpenTagEditor = { showTagEditor = true },
        onOpenKaraoke = { showKaraoke = true },
        modifier = modifier
    )

    // Dialogs & Sheets
    if (showQueueSheet) {
        PlayerQueueSheet(
            queue = uiState.queue,
            currentSong = song,
            onPlaySong = { selectedSong ->
                viewModel.playSong(selectedSong, uiState.queue)
            },
            onDismiss = { showQueueSheet = false }
        )
    }

    if (showSleepTimer) {
        PlayerSleepTimerDialog(
            isTimerRunning = uiState.isSleepTimerRunning,
            minutesLeft = uiState.sleepTimerMinutesLeft,
            onSelectMinutes = { viewModel.setSleepTimer(it) },
            onDismiss = { showSleepTimer = false }
        )
    }

    if (showDeleteDialog && song != null) {
        PlayerDeleteDialog(
            song = song,
            onConfirmDelete = { physical ->
                if (physical) {
                    viewModel.deleteSongPhysically(song.id)
                } else {
                    viewModel.deleteSong(song.id)
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showKaraoke && song != null) {
        PlayerKaraokeDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showKaraoke = false }
        )
    }

    if (showTagEditor && song != null) {
        TagEditorDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showTagEditor = false }
        )
    }
}

/**
 * Pure presentation layout for the Player Screen.
 *
 * Implemented with Default Skin tokens and components.
 */
@Composable
fun PlayerContent(
    uiState: PlayerUiState,
    currentLyricLine: String?,
    onNavigateBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onDeleteClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFloatingLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onOpenKaraoke: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = uiState.currentSong
    val scrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Surface(
        color = OniSkin.colors.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("player_screen")
            .pointerInput(isRtl) {
                // Horizontal swipe gesture to skip tracks with RTL support
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDragEnd = {
                        if (accumulatedDrag.absoluteValue > 80f) {
                            val isForward = if (isRtl) accumulatedDrag > 0 else accumulatedDrag < 0
                            if (isForward) onSkipNext() else onSkipPrevious()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                    }
                )
            }
    ) {
        if (song == null) {
            // Empty state when nothing is loaded
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(OniSkin.spacing.screenHorizontal),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = OniSkin.colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "No Track Selected",
                        style = OniSkin.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose a song from your library to start playback",
                        style = OniSkin.typography.bodySmall,
                        color = OniSkin.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.primary),
                        shape = OniSkin.shapes.medium
                    ) {
                        Text(
                            text = "Go to Library",
                            style = OniSkin.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val isLandscape = maxWidth > maxHeight && maxWidth >= 540.dp
                val availableHeight = maxHeight
                val availableWidth = maxWidth

                if (!isLandscape) {
                    // Portrait Layout: Responsive vertical spacing & artwork constraint
                    val isCompactHeight = availableHeight < 720.dp
                    val isSmallScreen = availableHeight < 640.dp

                    val artworkMaxSize = when {
                        isSmallScreen -> (availableHeight * 0.33f).coerceIn(160.dp, 240.dp)
                        isCompactHeight -> (availableHeight * 0.37f).coerceIn(200.dp, 290.dp)
                        else -> minOf(availableWidth - (OniSkin.spacing.screenHorizontal * 2), 350.dp)
                    }

                    val topSpacer = if (isCompactHeight) 4.dp else 8.dp
                    val artSpacer = if (isCompactHeight) 8.dp else 14.dp
                    val infoSpacer = if (isCompactHeight) 6.dp else 12.dp
                    val lyricsSpacer = if (isCompactHeight) 6.dp else 12.dp
                    val progressSpacer = if (isCompactHeight) 6.dp else 10.dp
                    val controlsSpacer = if (isCompactHeight) 8.dp else 14.dp
                    val featureBtnHeight = 48.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .verticalScroll(scrollState)
                            .padding(bottom = if (isCompactHeight) 8.dp else OniSkin.spacing.screenVertical),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Top Bar
                        PlayerTopBar(
                            title = "NOW PLAYING",
                            subtitle = song.displayAlbum,
                            onNavigateBack = onNavigateBack,
                            onQueueClick = onOpenQueue
                        )

                        Spacer(modifier = Modifier.height(topSpacer))

                        // 2. Responsive Album Artwork with subtle playback motion
                        PlayerArtwork(
                            song = song,
                            isPlaying = uiState.isPlaying,
                            maxSize = artworkMaxSize,
                            onClick = onTogglePlayPause
                        )

                        Spacer(modifier = Modifier.height(artSpacer))

                        // 3. Track Title, Artist, and Favorite Toggle
                        PlayerTrackInfo(
                            title = song.displayTitle,
                            artist = song.displayArtist,
                            album = song.displayAlbum,
                            isFavorite = uiState.isFavorite,
                            onToggleFavorite = onToggleFavorite
                        )

                        Spacer(modifier = Modifier.height(infoSpacer))

                        // 4. Inline Lyrics / Karaoke Preview prompt
                        PlayerLyricsPreview(
                            currentLyricLine = currentLyricLine,
                            hasSynchronizedLyrics = uiState.hasSynchronizedLyrics,
                            onClick = onOpenKaraoke
                        )

                        Spacer(modifier = Modifier.height(lyricsSpacer))

                        // 5. Seekable Progress Bar & Timestamps
                        PlayerProgress(
                            positionMs = uiState.position,
                            durationMs = uiState.duration,
                            onSeek = onSeek
                        )

                        Spacer(modifier = Modifier.height(progressSpacer))

                        // 6. Playback Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
                        PlayerPlaybackControls(
                            isPlaying = uiState.isPlaying,
                            isPreparing = uiState.isPreparing,
                            isShuffle = uiState.isShuffle,
                            isRepeat = uiState.isRepeat,
                            playbackDelayCountdown = uiState.playbackDelayCountdown,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat
                        )

                        Spacer(modifier = Modifier.height(controlsSpacer))

                        // 7. Feature Actions Dock (Lyrics, Floating, Sleep Timer, Tag Editor, Delete)
                        PlayerFeatureActions(
                            onLyricsClick = onOpenKaraoke,
                            floatingLyricsEnabled = uiState.floatingLyricsEnabled,
                            onToggleFloatingLyrics = onToggleFloatingLyrics,
                            isSleepTimerRunning = uiState.isSleepTimerRunning,
                            sleepTimerMinutesLeft = uiState.sleepTimerMinutesLeft,
                            onSleepTimerClick = onOpenSleepTimer,
                            onEditTagsClick = onOpenTagEditor,
                            onDeleteClick = onDeleteClick,
                            buttonHeight = featureBtnHeight
                        )
                    }
                } else {
                    // Landscape Responsive Layout (Two-Pane)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = 4.dp)
                    ) {
                        PlayerTopBar(
                            title = "NOW PLAYING",
                            subtitle = song.displayAlbum,
                            onNavigateBack = onNavigateBack,
                            onQueueClick = onOpenQueue
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Pane: Artwork + optional Lyrics Preview
                            Column(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val landscapeArtMax = (availableHeight - 120.dp).coerceIn(140.dp, 280.dp)
                                PlayerArtwork(
                                    song = song,
                                    isPlaying = uiState.isPlaying,
                                    maxSize = landscapeArtMax,
                                    horizontalPadding = 0.dp,
                                    onClick = onTogglePlayPause
                                )

                                if (availableHeight >= 360.dp) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PlayerLyricsPreview(
                                        currentLyricLine = currentLyricLine,
                                        hasSynchronizedLyrics = uiState.hasSynchronizedLyrics,
                                        onClick = onOpenKaraoke,
                                        horizontalPadding = 0.dp
                                    )
                                }
                            }

                            // Right Pane: Track Info, Progress, Playback Controls, Feature Actions
                            val rightPaneScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .weight(0.58f)
                                    .fillMaxHeight()
                                    .verticalScroll(rightPaneScrollState),
                                verticalArrangement = Arrangement.SpaceEvenly,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PlayerTrackInfo(
                                    title = song.displayTitle,
                                    artist = song.displayArtist,
                                    album = song.displayAlbum,
                                    isFavorite = uiState.isFavorite,
                                    onToggleFavorite = onToggleFavorite,
                                    horizontalPadding = 0.dp
                                )

                                PlayerProgress(
                                    positionMs = uiState.position,
                                    durationMs = uiState.duration,
                                    onSeek = onSeek,
                                    horizontalPadding = 0.dp
                                )

                                PlayerPlaybackControls(
                                    isPlaying = uiState.isPlaying,
                                    isPreparing = uiState.isPreparing,
                                    isShuffle = uiState.isShuffle,
                                    isRepeat = uiState.isRepeat,
                                    playbackDelayCountdown = uiState.playbackDelayCountdown,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onSkipNext = onSkipNext,
                                    onSkipPrevious = onSkipPrevious,
                                    onToggleShuffle = onToggleShuffle,
                                    onToggleRepeat = onToggleRepeat,
                                    horizontalPadding = 0.dp
                                )

                                PlayerFeatureActions(
                                    onLyricsClick = onOpenKaraoke,
                                    floatingLyricsEnabled = uiState.floatingLyricsEnabled,
                                    onToggleFloatingLyrics = onToggleFloatingLyrics,
                                    isSleepTimerRunning = uiState.isSleepTimerRunning,
                                    sleepTimerMinutesLeft = uiState.sleepTimerMinutesLeft,
                                    onSleepTimerClick = onOpenSleepTimer,
                                    onEditTagsClick = onOpenTagEditor,
                                    onDeleteClick = onDeleteClick,
                                    horizontalPadding = 0.dp,
                                    buttonHeight = 48.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
