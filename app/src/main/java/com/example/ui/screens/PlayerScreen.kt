package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.playback.RepeatMode
import com.example.playback.ShuffleType
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.player.LyricLineMode
import com.example.ui.player.SeekBarStyle
import com.example.ui.player.components.*
import com.example.ui.player.model.PlayerUiState
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Primary Player Screen for oniPlayer in Default Skin.
 *
 * Coordinates player presentation components, local dialog states, modal actions,
 * dynamic seekbar styles, and audio/artwork effects.
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
    var showMenuModal by remember { mutableStateOf(false) }

    // Dynamic preferences & effects state
    var audioAnalyzerActive by remember { mutableStateOf(false) }
    var artworkEffectsActive by remember { mutableStateOf(true) }
    var currentSeekBarStyle by remember { mutableStateOf(SeekBarStyle.SIMPLE) }
    var currentLyricMode by remember { mutableStateOf(LyricLineMode.UNDERNEATH) }
    var currentLineCount by remember { mutableIntStateOf(2) }

    // Intercept hardware back button to return to library screen context
    BackHandler(enabled = true) {
        viewModel.goBackToLibraryContext()
    }

    val song = uiState.currentSong

    // Calculate active and upcoming line preview if synced lyrics are present.
    val (currentLyricLine, nextLyricLine, thirdLyricLine) = remember(song?.lyrics, uiState.position) {
        val lyrics = song?.lyrics
        if (!lyrics.isNullOrBlank() && LyricsHelper.isSynced(lyrics)) {
            val parsed = LyricsHelper.parseLrc(lyrics).filter { it.text.isNotBlank() }
            if (parsed.isEmpty()) {
                Triple(null, null, null)
            } else {
                val idx = LyricsHelper.getActiveLineIndex(parsed, uiState.position)
                val activeIdx = if (idx >= 0) idx else 0
                val curr = parsed.getOrNull(activeIdx)?.text
                val next = parsed.getOrNull(activeIdx + 1)?.text
                val third = parsed.getOrNull(activeIdx + 2)?.text
                Triple(curr, next, third)
            }
        } else if (!lyrics.isNullOrBlank()) {
            val plainLines = lyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
            Triple(plainLines.getOrNull(0), plainLines.getOrNull(1), plainLines.getOrNull(2))
        } else {
            Triple(null, null, null)
        }
    }

    PlayerContent(
        uiState = uiState,
        currentLyricLine = currentLyricLine,
        nextLyricLine = nextLyricLine,
        thirdLyricLine = thirdLyricLine,
        onNavigateBack = { viewModel.goBackToLibraryContext() },
        onOpenQueue = { showQueueSheet = true },
        onOpenMenuModal = { showMenuModal = true },
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
        onSelectShuffleType = { viewModel.setShuffleType(it) },
        onSelectRepeatMode = { viewModel.setRepeatMode(it) },
        seekBarStyle = currentSeekBarStyle,
        lyricLineMode = currentLyricMode,
        lineCount = currentLineCount,
        audioAnalyzerEnabled = audioAnalyzerActive,
        artworkEffectsEnabled = artworkEffectsActive,
        modifier = modifier
    )

    // Contextual Action Menu Bottom Sheet (⋮ Menu)
    if (showMenuModal) {
        PlayerMenuModal(
            song = song,
            floatingLyricsEnabled = uiState.floatingLyricsEnabled,
            isSleepTimerRunning = uiState.isSleepTimerRunning,
            sleepTimerMinutesLeft = uiState.sleepTimerMinutesLeft,
            audioAnalyzerEnabled = audioAnalyzerActive,
            artworkEffectsEnabled = artworkEffectsActive,
            onToggleFloatingLyrics = { viewModel.setFloatingLyricsEnabled(!uiState.floatingLyricsEnabled) },
            onOpenSleepTimer = { showSleepTimer = true },
            onOpenKaraoke = { showKaraoke = true },
            onOpenTagEditor = { showTagEditor = true },
            onDeleteClick = { showDeleteDialog = true },
            onToggleAudioAnalyzer = { audioAnalyzerActive = !audioAnalyzerActive },
            onToggleArtworkEffects = { artworkEffectsActive = !artworkEffectsActive },
            onDismiss = { showMenuModal = false }
        )
    }

    // Dialogs & Sheets
    if (showQueueSheet) {
        PlayerQueueSheet(
            queue = uiState.queue,
            currentSong = song,
            onPlaySong = { selectedSong ->
                viewModel.playSong(selectedSong, uiState.queue)
            },
            onRemoveFromQueue = { songToRemove ->
                viewModel.removeFromQueue(songToRemove)
            },
            onMoveInQueue = { fromIndex, toIndex ->
                viewModel.moveInQueue(fromIndex, toIndex)
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
 * Implemented with Default Skin tokens, Canvas editorial styling options,
 * and contextual menu modal integration.
 */
@Composable
fun PlayerContent(
    uiState: PlayerUiState,
    currentLyricLine: String?,
    nextLyricLine: String? = null,
    thirdLyricLine: String? = null,
    onNavigateBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenMenuModal: () -> Unit,
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
    modifier: Modifier = Modifier,
    onSelectShuffleType: (ShuffleType?) -> Unit = {},
    onSelectRepeatMode: (RepeatMode) -> Unit = {},
    seekBarStyle: SeekBarStyle = SeekBarStyle.SIMPLE,
    lyricLineMode: LyricLineMode = LyricLineMode.UNDERNEATH,
    lineCount: Int = 2,
    audioAnalyzerEnabled: Boolean = false,
    artworkEffectsEnabled: Boolean = true
) {
    val song = uiState.currentSong
    val scrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val artworkDragOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 120.dp.toPx() }

    Surface(
        color = OniSkin.colors.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("player_screen")
            .pointerInput(isRtl) {
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

                    val dragProgress = (artworkDragOffsetY.value / dismissThresholdPx).coerceIn(0f, 1.5f)
                    val artworkTranslateY = artworkDragOffsetY.value.coerceAtLeast(0f)
                    val artworkDragScale = 1f - (dragProgress * 0.08f).coerceAtMost(0.12f)
                    val artworkDragAlpha = 1f - (dragProgress * 0.3f).coerceAtMost(0.45f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .verticalScroll(scrollState)
                            .padding(bottom = if (isCompactHeight) 8.dp else OniSkin.spacing.screenVertical),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Top Bar with Queue and Menu triggers
                        PlayerTopBar(
                            title = "NOW PLAYING",
                            subtitle = song.displayAlbum,
                            onNavigateBack = onNavigateBack,
                            onQueueClick = onOpenQueue
                        )

                        Spacer(modifier = Modifier.height(topSpacer))

                        // 2. Album Artwork with effects & gestures
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = artworkTranslateY
                                    scaleX = artworkDragScale
                                    scaleY = artworkDragScale
                                    alpha = artworkDragAlpha
                                }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (artworkDragOffsetY.value > dismissThresholdPx) {
                                                onNavigateBack()
                                                scope.launch { artworkDragOffsetY.snapTo(0f) }
                                            } else {
                                                scope.launch {
                                                    artworkDragOffsetY.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                                    )
                                                }
                                            }
                                        },
                                        onDragCancel = { scope.launch { artworkDragOffsetY.animateTo(0f, spring()) } },
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount > 0 || artworkDragOffsetY.value > 0) {
                                                change.consume()
                                                scope.launch {
                                                    val newValue = (artworkDragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                                    artworkDragOffsetY.snapTo(newValue)
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            PlayerArtwork(
                                song = song,
                                isPlaying = uiState.isPlaying,
                                maxSize = artworkMaxSize,
                                onClick = onTogglePlayPause,
                                artworkEffectsEnabled = artworkEffectsEnabled,
                                audioAnalyzerEnabled = audioAnalyzerEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(artSpacer))

                        // 3. Track Title, Artist, and Favorite Toggle
                        PlayerTrackInfo(
                            title = song.displayTitle,
                            artist = song.displayArtist,
                            album = song.displayAlbum,
                            isFavorite = uiState.isFavorite,
                            onToggleFavorite = onToggleFavorite,
                            useSerifFont = true
                        )

                        Spacer(modifier = Modifier.height(infoSpacer))

                        // 4. Live Lyrics Preview (supports 1, 2, or 3 lines and placement modes)
                        if (lyricLineMode != LyricLineMode.HIDDEN) {
                            PlayerLyricsPreview(
                                currentLyricLine = currentLyricLine,
                                nextLyricLine = nextLyricLine,
                                thirdLyricLine = thirdLyricLine,
                                hasSynchronizedLyrics = uiState.hasSynchronizedLyrics,
                                onClick = onOpenKaraoke,
                                isFetchingLyrics = uiState.isFetchingLyrics,
                                lyricLineMode = lyricLineMode,
                                lineCount = lineCount
                            )
                            Spacer(modifier = Modifier.height(lyricsSpacer))
                        }

                        // 5. Configurable Progress Bar (Simple, Wavy, Dot Line, Spectrum)
                        PlayerProgress(
                            positionMs = uiState.position,
                            durationMs = uiState.duration,
                            onSeek = onSeek,
                            seekBarStyle = seekBarStyle
                        )

                        Spacer(modifier = Modifier.height(progressSpacer))

                        // 6. Playback Controls
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
                            shuffleType = uiState.shuffleType,
                            repeatMode = uiState.repeatMode,
                            onSelectShuffleType = onSelectShuffleType,
                            onSelectRepeatMode = onSelectRepeatMode
                        )

                        Spacer(modifier = Modifier.height(controlsSpacer))

                        // 7. Cleaner Contextual Row with Contextual Menu (⋮)
                        PlayerFeatureActions(
                            onLyricsClick = onOpenKaraoke,
                            floatingLyricsEnabled = uiState.floatingLyricsEnabled,
                            onToggleFloatingLyrics = onToggleFloatingLyrics,
                            isSleepTimerRunning = uiState.isSleepTimerRunning,
                            sleepTimerMinutesLeft = uiState.sleepTimerMinutesLeft,
                            onSleepTimerClick = onOpenSleepTimer,
                            onEditTagsClick = onOpenTagEditor,
                            onDeleteClick = onDeleteClick,
                            buttonHeight = 48.dp
                        )
                    }
                } else {
                    // Landscape Responsive Two-Pane Layout
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
                                    onClick = onTogglePlayPause,
                                    artworkEffectsEnabled = artworkEffectsEnabled,
                                    audioAnalyzerEnabled = audioAnalyzerEnabled
                                )
                            }

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
                                    horizontalPadding = 0.dp,
                                    useSerifFont = true
                                )

                                PlayerProgress(
                                    positionMs = uiState.position,
                                    durationMs = uiState.duration,
                                    onSeek = onSeek,
                                    horizontalPadding = 0.dp,
                                    seekBarStyle = seekBarStyle
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
                                    horizontalPadding = 0.dp,
                                    shuffleType = uiState.shuffleType,
                                    repeatMode = uiState.repeatMode,
                                    onSelectShuffleType = onSelectShuffleType,
                                    onSelectRepeatMode = onSelectRepeatMode
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
