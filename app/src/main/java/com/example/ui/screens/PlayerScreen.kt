package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.lyrics.LrcLine
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.viewmodel.MusicPlayerViewModel
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import com.example.data.entity.SongEntity
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalSecondaryColor
import com.example.ui.theme.LocalAccentGlowColor

@Composable
fun PlayerScreen(viewModel: MusicPlayerViewModel) {
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val position by viewModel.audioEngine.position.collectAsState()
    val duration by viewModel.audioEngine.duration.collectAsState()

    val isShuffle by viewModel.isShuffle.collectAsState()
    val isRepeat by viewModel.isRepeat.collectAsState()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val floatingLyricsEnabled by viewModel.floatingLyricsEnabled.collectAsState()

    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val secondaryColor = LocalSecondaryColor.current
    val accentGlowColor = LocalAccentGlowColor.current
    val density = LocalDensity.current

    BackHandler(enabled = true) {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }

    // Proper conversion of DP to PX for shadow effects
    val shadowElevationPx = with(density) { 8.dp.toPx() }

    // Pre-calculate lyrics variables at the root function level to ensure visibility inside dialogues/sheets
    val song = currentSong
    val parsedLrc = remember(song?.lyrics) {
        if (song != null && !song.lyrics.isNullOrBlank()) {
            LyricsHelper.parseLrc(song.lyrics)
        } else {
            emptyList()
        }
    }
    val activeLrcIndex = remember(parsedLrc, position) {
        LyricsHelper.getActiveLineIndex(parsedLrc, position)
    }

    // Rotation angle state for floating and drifting animation
    val infiniteTransition = rememberInfiniteTransition(label = "Aurora floating")
    
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = SineHeightEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Vinyl breathing pulse"
    )

    val driftingRotation by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = SineHeightEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Vinyl drift"
    )

    // Animated scale for tactile buttons
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.06f else 1.00f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "Play button bounce"
    )

    // Local Sleep Timer state
    var sleepTimerMinutesLeft by remember { mutableStateOf(0) }
    var isSleepTimerRunning by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSleepTimerRunning, sleepTimerMinutesLeft) {
        if (isSleepTimerRunning && sleepTimerMinutesLeft > 0) {
            delay(60000L) // Wait 1 minute
            sleepTimerMinutesLeft--
            if (sleepTimerMinutesLeft == 0) {
                viewModel.audioEngine.pause()
                isSleepTimerRunning = false
                Toast.makeText(context, "Sleep timer expired. Playback paused.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Scroll state for lyrics
    val lyricsScrollState = rememberScrollState()

    // Dialog/Drawer states
    var showTagEditor by remember { mutableStateOf(false) }
    var showManualSearch by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showSyncEditor by remember { mutableStateOf(false) }
    var isKaraokeModeFull by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        if (totalDragY > 100f) { // Swipe down to exit full player
                            viewModel.selectTab(0)
                        }
                    },
                    onDragCancel = { totalDragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag-down handle
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                    .clickable { viewModel.selectTab(0) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Premium Header with Outlined Circle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.goBackToLibraryContext()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.8.sp
                    )
                    Text(
                        text = "Aurora Glass",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (song == null) {
                // Empty state
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a song to play",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val isFavorite = favoriteSongs.any { it.id == song.id }

                // 2. Large Album Artwork with floating scale & subtle rotation drift
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxWidth(0.82f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Accent color-matched ambient radial drop-shadow
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .graphicsLayer {
                                scaleX = if (isPlaying) breathingScale else 1.0f
                                scaleY = if (isPlaying) breathingScale else 1.0f
                                alpha = if (isPlaying) 0.32f else 0.15f
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(accentColor, Color.Transparent)
                                ),
                                CircleShape
                            )
                    )

                    // Rounded artwork card with subtle reflection & floating motion
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = if (isPlaying) breathingScale else 1.0f
                                scaleY = if (isPlaying) breathingScale else 1.0f
                                rotationZ = if (isPlaying) driftingRotation else 0f
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = "Album Artwork",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                            )

                            // Subtle overlay reflection gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.3f))
                                        )
                                    )
                            )
                        }
                    }

                    // Luxury Audio Quality Badge (frosted capsule at bottom right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "High Fidelity • 24-bit FLAC",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // 3. Song Information (Exact Typography requested)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = song.customTitle ?: song.title,
                        fontSize = 26.sp, // Song Title 26sp SemiBold
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = (song.customArtist ?: song.artist),
                        fontSize = 17.sp, // Artist 17sp Medium
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (song.customAlbum ?: song.album ?: "Unknown Album"),
                        fontSize = 15.sp, // Album 15sp Regular
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isKaraokeModeFull = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.lyrics.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "No lyrics cached offline. Tap to search.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else if (parsedLrc.isEmpty()) {
                            Text(
                                text = LyricsHelper.stripLrcTags(song.lyrics),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            val currentLine = if (activeLrcIndex >= 0 && activeLrcIndex < parsedLrc.size) parsedLrc[activeLrcIndex] else null
                            if (currentLine != null) {
                                Text(
                                    text = currentLine.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "• • • Instrumental • • •",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Draggable & Animated Waveform Seekbar (Peak Craft)
                Column(modifier = Modifier.fillMaxWidth()) {
                    WaveformSeekBar(
                        position = position,
                        duration = duration,
                        accentColor = accentColor,
                        onSeek = { viewModel.audioEngine.seekTo(it) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatPlayerDuration(position),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = formatPlayerDuration(duration),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 6. Tactile Playback controls row (Material Symbols Weights + outlines)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        IconButton(onClick = { viewModel.skipPrevious() }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Neumorphic tactile Play/Pause Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    scaleX = playButtonScale
                                    scaleY = playButtonScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(accentColor, secondaryColor)
                                    )
                                )
                                .clickable { viewModel.togglePlayPause() }
                                .testTag("play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next
                        IconButton(onClick = { viewModel.skipNext() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Repeat Toggle
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        Icon(
                            if (isRepeat) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Glassmorphism Floating Bottom Features Dock
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lyrics manager
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isKaraokeModeFull = true }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Lyrics,
                                contentDescription = "Lyrics Manager",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lyrics",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Floating Lyrics Window Toggle
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setFloatingLyricsEnabled(!floatingLyricsEnabled) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PictureInPicture,
                                contentDescription = "Floating Window",
                                tint = if (floatingLyricsEnabled) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Float Window",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (floatingLyricsEnabled) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Sleep Timer Action
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSleepTimerDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (isSleepTimerRunning) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSleepTimerRunning) "${sleepTimerMinutesLeft}m" else "Sleep Timer",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSleepTimerRunning) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Favorite Action (Filled only if active)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.toggleFavorite(song.id) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Favorite",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Edit Tags dialog trigger
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTagEditor = true }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = "Edit Tags",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Edit Tags",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Custom Frosted Sliding Glass Bottom Sheet for Queue
        AnimatedVisibility(
            visible = showQueueSheet,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showQueueSheet = false }
            ) {
                // Floating Glass sheet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .align(Alignment.BottomCenter)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .clickable(enabled = false) {}, // prevent clicks leaking
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val playlist by viewModel.currentPlaylist.collectAsState()
                            Text(
                                text = "Playing Queue (${playlist.size} tracks)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = { showQueueSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val playlist by viewModel.currentPlaylist.collectAsState()
                        if (playlist.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("Queue is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(playlist) { index, item ->
                                    val isCurrent = song?.id == item.id
                                    val rowBg = if (isCurrent) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                    val rowBorder = if (isCurrent) BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)) else null

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(rowBg)
                                            .then(if (rowBorder != null) Modifier.border(rowBorder, RoundedCornerShape(14.dp)) else Modifier)
                                            .clickable {
                                                viewModel.playSong(item, playlist)
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Mini index / Visualizer dots
                                            if (isCurrent) {
                                                // Live visualizer dot
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(accentColor)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.width(16.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            AsyncImage(
                                                model = item.albumArtUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop,
                                                error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.customTitle ?: item.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.customArtist ?: item.artist,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Sleep Timer setting selector dialog
        if (showSleepTimerDialog) {
            Dialog(onDismissRequest = { showSleepTimerDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Configure Sleep Timer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Playback will automatically pause after the duration ends.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Timer pills
                        listOf(0, 5, 15, 30, 45, 60).chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { mins ->
                                    val isSelected = if (mins == 0) !isSleepTimerRunning else isSleepTimerRunning && sleepTimerMinutesLeft == mins
                                    val btnBg = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(btnBg)
                                            .clickable {
                                                if (mins == 0) {
                                                    isSleepTimerRunning = false
                                                    sleepTimerMinutesLeft = 0
                                                } else {
                                                    sleepTimerMinutesLeft = mins
                                                    isSleepTimerRunning = true
                                                }
                                                showSleepTimerDialog = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (mins == 0) "Disable" else "${mins} Min",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tag Editor dialog integration
        if (showTagEditor && song != null) {
            TagEditorDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { showTagEditor = false }
            )
        }

        // 8. FullScreen Immersive Scrolling Karaoke lyrics dialog (Exact specs requested)
        if (isKaraokeModeFull && song != null) {
            Dialog(
                onDismissRequest = { 
                    viewModel.karaokeMicEngine.stopMic()
                    isKaraokeModeFull = false 
                },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                // Large Gaussian blur effect background
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f) // deep premium translucent surface
                ) {
                    var autoScrollEnabled by remember { mutableStateOf(true) }
                    val listState = rememberLazyListState()
                    var showPlainTextMode by remember(song.lyrics) {
                        mutableStateOf(!LyricsHelper.isSynced(song.lyrics))
                    }

                    val isMicEnabled by viewModel.karaokeMicEngine.isMicEnabled.collectAsState()
                    val micAmplitude by viewModel.karaokeMicEngine.amplitude.collectAsState()
                    val micGain by viewModel.karaokeMicEngine.micGain.collectAsState()

                    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    val micPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            viewModel.karaokeMicEngine.startMic()
                            Toast.makeText(context, "Mic enabled! Sing along!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Microphone permission is required to sing along", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Auto-scroll logic: always scroll the active highlighted lyric line to the top of the visible list
                    LaunchedEffect(activeLrcIndex) {
                        if (autoScrollEnabled && activeLrcIndex >= 0 && parsedLrc.isNotEmpty()) {
                            listState.animateScrollToItem(activeLrcIndex)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Fullscreen Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    viewModel.karaokeMicEngine.stopMic()
                                    isKaraokeModeFull = false 
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.customTitle ?: song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.customArtist ?: song.artist,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!showPlainTextMode) {
                                IconButton(
                                    onClick = { autoScrollEnabled = !autoScrollEnabled },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(
                                            1.dp,
                                            if (autoScrollEnabled) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (autoScrollEnabled) Icons.Default.CompassCalibration else Icons.Default.ExploreOff,
                                        contentDescription = "Toggle Auto-Scroll",
                                        tint = if (autoScrollEnabled) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(40.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mode Selector Capsule Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(4.dp)
                            ) {
                                val syncActive = !showPlainTextMode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (syncActive) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { showPlainTextMode = false }
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = if (syncActive) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Synced Karaoke",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (syncActive) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (showPlainTextMode) secondaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { showPlainTextMode = true }
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Subject,
                                            contentDescription = null,
                                            tint = if (showPlainTextMode) secondaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Plain Text",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (showPlainTextMode) secondaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. Controller Bar for Actions (Search, Edit, Sync, Sing Along)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButtonWithText(
                                icon = Icons.Default.CloudDownload,
                                label = "Search Online",
                                onClick = { showManualSearch = true },
                                modifier = Modifier.weight(1f),
                                contentColor = accentColor
                            )

                            IconButtonWithText(
                                icon = Icons.Default.Edit,
                                label = "Edit Lyrics",
                                onClick = { showLyricsEditor = true },
                                modifier = Modifier.weight(1f),
                                contentColor = secondaryColor
                            )

                            IconButtonWithText(
                                icon = Icons.Default.SyncAlt,
                                label = "Sync Editor",
                                onClick = { showSyncEditor = true },
                                modifier = Modifier.weight(1f),
                                contentColor = Color(0xFFFF9800)
                            )

                            IconButtonWithText(
                                icon = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                label = "Sing Along",
                                onClick = {
                                    if (isMicEnabled) {
                                        viewModel.karaokeMicEngine.stopMic()
                                    } else {
                                        if (hasMicPermission) {
                                            viewModel.karaokeMicEngine.startMic()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentColor = if (isMicEnabled) Color(0xFF00FFCC) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                isSelected = isMicEnabled
                            )
                        }

                        // 2. Karaoke Mic Mixer & Vocal Visualizer Panel
                        AnimatedVisibility(
                            visible = isMicEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.04f)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Hearing,
                                                contentDescription = null,
                                                tint = Color(0xFF00FFCC),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "VOCAL MIXER & POWER MONITOR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00FFCC),
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Text(
                                            text = "Vocal Level: ${micAmplitude.toInt()}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FFCC),
                                            modifier = Modifier
                                                .background(Color(0xFF00FFCC).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Real-time Canvas Soundwave Visualizer
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    ) {
                                        val width = size.width
                                        val height = size.height
                                        val barCount = 30
                                        val barWidth = width / barCount
                                        val ampFactor = (micAmplitude / 100f).coerceIn(0.05f, 1f)

                                        for (i in 0 until barCount) {
                                            val x = i * barWidth + (barWidth / 4)
                                            val progress = i.toFloat() / barCount
                                            val sinFactor = kotlin.math.sin(progress * Math.PI).toFloat()
                                            val noise = (0.7f + 0.3f * kotlin.math.sin((progress * 15f) + (micAmplitude * 0.1f)).toFloat())
                                            val barHeight = height * ampFactor * sinFactor * noise

                                            drawRoundRect(
                                                color = Color(0xFF00FFCC).copy(alpha = 0.2f + (0.8f * ampFactor)),
                                                topLeft = Offset(x, (height - barHeight) / 2),
                                                size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, barHeight),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Gain Control Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Mic Gain",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.width(60.dp)
                                        )

                                        Slider(
                                            value = micGain,
                                            onValueChange = { viewModel.karaokeMicEngine.setMicGain(it) },
                                            valueRange = 0.5f..2.5f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF00FFCC),
                                                activeTrackColor = Color(0xFF00FFCC),
                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = "${String.format("%.1f", micGain)}x",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FFCC),
                                            modifier = Modifier.width(30.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                        // Large Typography scrolling view
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (showPlainTextMode) {
                                val cleanLyrics = remember(song.lyrics) {
                                    LyricsHelper.stripLrcTags(song.lyrics)
                                }
                                if (cleanLyrics.isBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lyrics,
                                                contentDescription = null,
                                                tint = secondaryColor.copy(alpha = 0.8f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "No Lyrics Saved",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "This song has no lyrics cached offline yet. Search online or type them in manually.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 32.dp)
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = { showManualSearch = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Search Online", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { showLyricsEditor = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Paste Manually", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(vertical = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Top
                                        ) {
                                            Text(
                                                text = cleanLyrics,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                                textAlign = TextAlign.Center,
                                                lineHeight = 36.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (parsedLrc.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lyrics,
                                                contentDescription = null,
                                                tint = accentColor.copy(alpha = 0.8f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "No Synchronized Lyrics",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Search our online database or sync your lyrics manually to unlock real-time scrolling karaoke mode.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 32.dp)
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = { showManualSearch = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Search Online", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { showLyricsEditor = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Paste Manually", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 16.dp, bottom = 480.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        itemsIndexed(parsedLrc) { index, line ->
                                            val isActive = index == activeLrcIndex
                                            val isPassed = index < activeLrcIndex

                                            // Inactive items: Gray. Active item: Bright white, bold, mic-responsive scaling
                                            val textColor = if (isActive) {
                                                if (isMicEnabled) Color(0xFF00FFCC) else MaterialTheme.colorScheme.onSurface
                                            } else if (isPassed) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            }

                                            val textWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                                            
                                            // When mic is enabled, vocal input scales active text dynamically
                                            val micPulseScale = if (isActive && isMicEnabled) {
                                                1.0f + (micAmplitude / 100f) * 0.15f
                                            } else {
                                                1.0f
                                            }
                                            val textScale = (if (isActive) 1.15f else 0.95f) * micPulseScale

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.audioEngine.seekTo(line.timestampMs)
                                                        autoScrollEnabled = true
                                                    }
                                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = line.text,
                                                    fontSize = (26 * textScale).sp,
                                                    fontWeight = textWeight,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 38.sp,
                                                    modifier = if (isActive) {
                                                        Modifier.graphicsLayer {
                                                            shadowElevation = shadowElevationPx
                                                        }
                                                    } else Modifier
                                                )
                                            }
                                        }
                                    }

                                    if (!autoScrollEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 24.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(accentColor)
                                                .clickable { autoScrollEnabled = true }
                                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                        ) {
                                            Text("Resume Sync Scroll", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // Inline full screen player controllers
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.skipPrevious() }) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface)
                                }

                                IconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(accentColor, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                IconButton(onClick = { viewModel.skipNext() }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Secondary Dialogs
        if (showManualSearch && song != null) {
            ManualSearchDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { showManualSearch = false }
            )
        }

        if (showLyricsEditor && song != null) {
            LyricsEditorDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { showLyricsEditor = false }
            )
        }

        if (showSyncEditor && song != null) {
            SyncEditorDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { showSyncEditor = false }
            )
        }
    }
}

// Peak Craft Interactive WaveformSeekbar Composable
@Composable
fun WaveformSeekBar(
    position: Long,
    duration: Long,
    accentColor: Color,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((fraction * duration).toLong())
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek((fraction * duration).toLong())
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val unplayedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = 3.dp.toPx()
            val spacing = 2.dp.toPx()
            val totalBarWidth = barWidth + spacing
            val barCount = (size.width / totalBarWidth).toInt()

            for (i in 0 until barCount) {
                val barFraction = i.toFloat() / barCount.toFloat()
                val isPlayed = barFraction <= progress

                // Structured high craft visual waveforms
                val sinVal = kotlin.math.sin(barFraction * Math.PI * 3.8).toFloat()
                val cosVal = kotlin.math.cos(barFraction * Math.PI * 7.2).toFloat()
                val rawHeight = (sinVal.absoluteValue * 0.65f + cosVal.absoluteValue * 0.35f)

                val barHeight = (height * rawHeight * 0.82f).coerceAtLeast(5.dp.toPx())
                val x = i * totalBarWidth
                val yStart = (height - barHeight) / 2

                drawRoundRect(
                    color = if (isPlayed) accentColor else unplayedColor,
                    topLeft = Offset(x, yStart),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f.dp.toPx(), 1.5f.dp.toPx())
                )
            }
        }
    }
}

private fun formatPlayerDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

private val SineHeightEasing = Easing { fraction ->
    val t = fraction * Math.PI
    kotlin.math.sin(t).toFloat()
}

@Composable
fun IconButtonWithText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color,
    isSelected: Boolean = false
) {
    val bgCol = if (isSelected) contentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    val borderCol = if (isSelected) contentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

