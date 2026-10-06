package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.playback.RepeatMode
import com.example.playback.ShuffleType
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.components.music.OniArtwork
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.player.AudioSpectrumVisualizer
import com.example.ui.player.AudioVisualizerMode
import com.example.ui.player.LyricLineMode
import com.example.ui.player.SeekBarStyle
import com.example.ui.player.components.*
import com.example.ui.player.components.lyrics.LyricEmphasis
import com.example.ui.player.components.lyrics.rememberLyricsAppearance
import com.example.ui.player.model.PlayerUiState
import com.example.ui.theme.OniSkin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
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
private fun SongEntity.audioQualityLabel(): String {
    val normalizedFormat = format.trim().uppercase().ifBlank {
        filePath.substringAfterLast('.', "").uppercase()
    }
    val bitrateLabel = bitrate.takeIf { it > 0 }?.let { "$it kbps" }
    return when {
        normalizedFormat.isNotBlank() && bitrateLabel != null -> "$normalizedFormat • $bitrateLabel"
        normalizedFormat.isNotBlank() -> normalizedFormat
        bitrateLabel != null -> bitrateLabel
        else -> "Unknown quality"
    }
}

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
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showVisualizerPicker by remember { mutableStateOf(false) }
    var pendingVisualizerMode by remember { mutableStateOf<Int?>(null) }

    val audioVisualizerMode by viewModel.playerAudioVisualizer.collectAsStateWithLifecycle()
    val audioSessionId by viewModel.audioSessionId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val visualizerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val requestedMode = pendingVisualizerMode
        pendingVisualizerMode = null
        if (granted && requestedMode != null) {
            viewModel.setPlayerAudioVisualizer(requestedMode)
        }
    }
    val currentSeekBarStyle by viewModel.playerSeekBarStyle.collectAsStateWithLifecycle()
    val currentLyricMode by viewModel.playerLyricMode.collectAsStateWithLifecycle()
    val currentLineCount by viewModel.playerLyricLines.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

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
        seekBarStyle = when (currentSeekBarStyle) {
            1 -> SeekBarStyle.WAVY
            2 -> SeekBarStyle.DOT_LINE
            3 -> SeekBarStyle.AUDIO_SPECTRUM
            else -> SeekBarStyle.SIMPLE
        },
        lyricLineMode = when (currentLyricMode) {
            1 -> LyricLineMode.OVER_BOTTOM
            2 -> LyricLineMode.HIDDEN
            else -> LyricLineMode.UNDERNEATH
        },
        lineCount = currentLineCount,
        audioVisualizerMode = audioVisualizerMode,
        audioSessionId = audioSessionId,
        modifier = modifier
    )

    // Contextual Action Menu Bottom Sheet (⋮ Menu)
    if (showMenuModal) {
        PlayerMenuModal(
            song = song,
            floatingLyricsEnabled = uiState.floatingLyricsEnabled,
            isSleepTimerRunning = uiState.isSleepTimerRunning,
            sleepTimerMinutesLeft = uiState.sleepTimerMinutesLeft,
            isFavorite = uiState.isFavorite,
            audioVisualizerMode = audioVisualizerMode,
            onToggleFavorite = {
                song?.id?.let { viewModel.toggleFavorite(it) }
                showMenuModal = false
            },
            onOpenPlaylistPicker = {
                showMenuModal = false
                showPlaylistPicker = true
            },
            onOpenFileLocation = {
                song?.let {
                    val folderName = java.io.File(it.filePath).parentFile?.name ?: "Internal"
                    viewModel.setActiveCategoryIndex(1)
                    viewModel.setSelectedGroup(folderName)
                    viewModel.selectTab(0)
                }
                showMenuModal = false
            },
            onOpenAlbum = {
                song?.let {
                    val albumKey = "${it.displayAlbum.ifBlank { "Unknown Album" }}|${it.displayAlbumArtist}"
                    viewModel.setActiveCategoryIndex(2)
                    viewModel.setSelectedGroup(albumKey)
                    viewModel.selectTab(0)
                }
                showMenuModal = false
            },
            onOpenArtist = {
                song?.let {
                    val artistKey = it.displayArtist.ifBlank { "Unknown Artist" }
                    viewModel.setActiveCategoryIndex(3)
                    viewModel.setSelectedGroup(artistKey)
                    viewModel.selectTab(0)
                }
                showMenuModal = false
            },
            onToggleFloatingLyrics = { viewModel.setFloatingLyricsEnabled(!uiState.floatingLyricsEnabled) },
            onOpenSleepTimer = { showSleepTimer = true },
            onOpenKaraoke = { showKaraoke = true },
            onOpenTagEditor = { showTagEditor = true },
            onDeleteClick = { showDeleteDialog = true },
            onOpenAudioVisualizerPicker = { showMenuModal = false; showVisualizerPicker = true },
            onDismiss = { showMenuModal = false }
        )
    }

    if (showVisualizerPicker) {
        AudioVisualizerPickerDialog(
            selectedMode = audioVisualizerMode,
            onSelect = { mode ->
                if (mode == 0 || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.setPlayerAudioVisualizer(mode)
                    showVisualizerPicker = false
                    showMenuModal = false
                } else {
                    pendingVisualizerMode = mode
                    showVisualizerPicker = false
                    showMenuModal = false
                    visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onDismiss = { showVisualizerPicker = false }
        )
    }

    if (showPlaylistPicker && song != null) {
        PlayerPlaylistPickerSheet(
            song = song,
            playlists = playlists,
            onAddToPlaylist = { playlist ->
                viewModel.addSongToPlaylist(song.id, playlist.id)
                showPlaylistPicker = false
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name, listOf(song.id))
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false }
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
    audioVisualizerMode: Int = 0,
    audioSessionId: Int = -1
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
                    val visibleLyricLines = lineCount.coerceIn(1, 3)
                    val artworkHeight = (availableHeight * 0.62f).coerceIn(360.dp, 560.dp)
                    val metadataBottomGap = 4.dp

                    // Portrait composition:
                    // 1) black OLED canvas
                    // 2) borderless full-bleed artwork in the upper visual region
                    // 3) artwork dissolves into a pitch-black negative-space fade
                    // 4) metadata is bottom-anchored immediately above the controller
                    // 5) controller is anchored directly above Scaffold's navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        if (audioVisualizerMode == AudioVisualizerMode.OFF.ordinal) {
                            OniArtwork(
                                artworkUri = song.albumArtUri,
                                contentDescription = "Album art for ${song.displayTitle}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(artworkHeight)
                                    .align(Alignment.TopCenter),
                                shape = RectangleShape,
                                elevation = 0.dp,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AudioSpectrumVisualizer(
                                mode = AudioVisualizerMode.entries.getOrElse(audioVisualizerMode) { AudioVisualizerMode.BARS },
                                audioSessionId = audioSessionId,
                                isPlaying = uiState.isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(artworkHeight)
                                    .align(Alignment.TopCenter)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.88f),
                                        0.35f to Color.Black.copy(alpha = 0.50f),
                                        0.72f to Color.Black.copy(alpha = 0.14f),
                                        1f to Color.Transparent
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (artworkHeight - 300.dp).coerceAtLeast(0.dp))
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.18f to Color.Black.copy(alpha = 0.18f),
                                        0.36f to Color.Black.copy(alpha = 0.46f),
                                        0.56f to Color.Black.copy(alpha = 0.72f),
                                        0.74f to Color.Black.copy(alpha = 0.91f),
                                        0.88f to Color.Black.copy(alpha = 0.98f),
                                        1f to Color.Black
                                    )
                                )
                        )

                        PlayerTopBar(
                            title = "",
                            onNavigateBack = onNavigateBack,
                            onQueueClick = onOpenQueue,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = OniSkin.spacing.screenHorizontal)
                        )

                        // Metadata is anchored to the artwork boundary, not to the controller.
                        // The controller has its own bottom anchor so changing lyric count / card height
                        // can never move the metadata or change its distance from the navigation bar.
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height((artworkHeight - metadataBottomGap).coerceAtLeast(0.dp))
                                .padding(
                                    start = OniSkin.spacing.screenHorizontal,
                                    end = OniSkin.spacing.screenHorizontal
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            PlayerTrackInfo(
                                title = song.displayTitle,
                                artist = song.displayArtist,
                                album = song.displayAlbum,
                                isFavorite = uiState.isFavorite,
                                onToggleFavorite = onToggleFavorite,
                                horizontalPadding = 0.dp,
                                useSerifFont = true,
                                qualityBadge = song.audioQualityLabel(),
                                onMenuClick = onOpenMenuModal,
                                highContrast = true
                            )
                        }

                        // Controller is independently pinned to the bottom of the Player content area.
                        // Scaffold's bottom inset already ends this area immediately above the navigation bar.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(
                                    start = OniSkin.spacing.screenHorizontal,
                                    end = OniSkin.spacing.screenHorizontal,
                                    bottom = 4.dp
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(OniSkin.surfaces.soft.containerColor)
                                    .border(
                                        BorderStroke(1.dp, OniSkin.colors.onSurface.copy(alpha = 0.14f)),
                                        RoundedCornerShape(28.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.02f),
                                                    Color.White.copy(alpha = 0.10f),
                                                    Color.White.copy(alpha = 0.02f)
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 12.dp)
                                ) {
                                    if (lyricLineMode != LyricLineMode.HIDDEN) {
                                        PlayerLyricsPreview(
                                            currentLyricLine = currentLyricLine,
                                            nextLyricLine = nextLyricLine,
                                            thirdLyricLine = thirdLyricLine,
                                            hasSynchronizedLyrics = uiState.hasSynchronizedLyrics,
                                            onClick = onOpenKaraoke,
                                            isFetchingLyrics = uiState.isFetchingLyrics,
                                            lyricLineMode = lyricLineMode,
                                            lineCount = visibleLyricLines,
                                            horizontalPadding = 0.dp
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .height(1.dp)
                                                .background(OniSkin.colors.outline.copy(alpha = 0.28f))
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    PlayerProgress(
                                        positionMs = uiState.position,
                                        durationMs = uiState.duration,
                                        onSeek = onSeek,
                                        horizontalPadding = 0.dp,
                                        seekBarStyle = seekBarStyle
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

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
                                }
                            }
                        }
                    }                } else {
                    // Landscape Responsive Two-Pane Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
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
                                if (audioVisualizerMode == AudioVisualizerMode.OFF.ordinal) {
                                    PlayerArtwork(
                                        song = song,
                                        isPlaying = uiState.isPlaying,
                                        maxSize = landscapeArtMax,
                                        horizontalPadding = 0.dp,
                                        onClick = onTogglePlayPause
                                    )
                                } else {
                                    AudioSpectrumVisualizer(
                                        mode = AudioVisualizerMode.entries.getOrElse(audioVisualizerMode) { AudioVisualizerMode.BARS },
                                        audioSessionId = audioSessionId,
                                        isPlaying = uiState.isPlaying,
                                        modifier = Modifier
                                            .size(landscapeArtMax)
                                            .clip(OniSkin.artwork.shape)
                                    )
                                }
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
