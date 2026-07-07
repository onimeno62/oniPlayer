package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import com.example.ui.lyrics.LrcLine
import com.example.ui.lyrics.LyricsHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.MusicPlayerViewModel
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun PlayerScreen(viewModel: MusicPlayerViewModel) {
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val position by viewModel.audioEngine.position.collectAsState()
    val duration by viewModel.audioEngine.duration.collectAsState()

    val isShuffle by viewModel.isShuffle.collectAsState()
    val isRepeat by viewModel.isRepeat.collectAsState()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsState()

    val context = LocalContext.current

    // Rotation angle state for spinning vinyl art
    var rotationAngle by remember { mutableStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "Vinyl rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Vinyl rotation angle"
    )

    // Scroll state for lyrics
    val lyricsScrollState = rememberScrollState()

    // Dialog state for Tag Editor inside player
    var showTagEditor by remember { mutableStateOf(false) }

    // Enhanced lyrics feature states
    var showManualSearch by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showSyncEditor by remember { mutableStateOf(false) }
    var isKaraokeModeFull by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            rotationAngle = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        if (totalDragY > 100f) { // Dragged down significantly
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
            // Drag-down visual handle indicator
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
                    .clickable { viewModel.selectTab(0) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Header matching HTML: "Now Playing / OniPlayer"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "OniPlayer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Palette theme selector button
                    IconButton(
                        onClick = { viewModel.selectTab(3) }, // Switch to Themes screen
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Themes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (currentSong == null) {
                // Empty state
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a song to play",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val song = currentSong!!

                // 2. Beautiful Artwork Container matching High Density (rounded-[40px] with elegant gradient)
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                    )
                )

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(gradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Spinning Vinyl Ring
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D1B20)) // Deep Obsidian Vinyl Body
                            .border(8.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .rotate(if (isPlaying) angle else rotationAngle)
                    ) {
                        // Embedded Album Cover art
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Vinyl Cover Art",
                            modifier = Modifier
                                .size(130.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                        )

                        // Center Spindle Hole
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .align(Alignment.Center)
                        )
                    }

                    // 320kbps FLAC Badge at bottom right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "320kbps • FLAC",
                            color = Color(0xFF1D1B20),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Song Title & Artist info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = song.customTitle ?: song.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (song.customArtist ?: song.artist) + " • " + (song.customAlbum ?: song.album),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 4. Compact Lyrics Container / Drawer
                val parsedLrc: List<LrcLine> = remember(song.lyrics) { LyricsHelper.parseLrc(song.lyrics) }
                val activeLrcIndex = remember(parsedLrc, position) { LyricsHelper.getActiveLineIndex(parsedLrc, position) }
                val isSynced = remember(song.lyrics) { LyricsHelper.isSynced(song.lyrics) }
                val floatingEnabled by viewModel.floatingLyricsEnabled.collectAsState()
                val isAutoDownload by viewModel.isAutoDownloadEnabled.collectAsState()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Action Lyrics Tool Bar (Source + Edit + Sync + Search + Float)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source chip / pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sourceLabel = if (song.lyrics.isNullOrBlank()) "None" else if (isSynced) "LRC Sync" else "Plain Text"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "Src: $sourceLabel",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Auto Download Toggle Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isAutoDownload) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .clickable { viewModel.setAutoDownloadEnabled(!isAutoDownload) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    if (isAutoDownload) "Auto-DL On" else "Auto-DL Off",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoDownload) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Toolbar Action Icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Online Action
                            IconButton(
                                onClick = { showManualSearch = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search Online Databases",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Text Edit Action
                            IconButton(
                                onClick = { showLyricsEditor = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Lyrics",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Sync Editor Action
                            IconButton(
                                onClick = { showSyncEditor = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.SyncAlt,
                                    contentDescription = "Sync Timings",
                                    tint = if (isSynced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Floating window toggle Action
                            IconButton(
                                onClick = {
                                    val targetState = !floatingEnabled
                                    if (targetState) {
                                        if (android.provider.Settings.canDrawOverlays(context)) {
                                            viewModel.setFloatingLyricsEnabled(true)
                                            android.widget.Toast.makeText(context, "Floating Lyrics Overlay Started!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Please enable 'Display over other apps' to show lyrics on top of other apps!",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                                context.startActivity(intent)
                                            }
                                        }
                                    } else {
                                        viewModel.setFloatingLyricsEnabled(false)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lyrics,
                                    contentDescription = "Toggle Floating Lyrics",
                                    tint = if (floatingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Clear / Delete Lyrics Action
                            IconButton(
                                onClick = {
                                    viewModel.updateLyrics(song.id, null)
                                    Toast.makeText(context, "Lyrics cleared offline", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Clear Offline Lyrics",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Compact Lyrics Card (Click opens Full-screen Karaoke Mode)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isKaraokeModeFull = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        border = borderStrokeDefault()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.lyrics.isNullOrBlank()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        "No offline lyrics found",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        "Search global sync repositories or type manually",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { showManualSearch = true },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Search Online", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { showLyricsEditor = true },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Type Manually", fontSize = 10.sp)
                                        }
                                    }
                                }
                            } else if (parsedLrc.isEmpty()) {
                                // Plain text lyrics showing first couple of lines
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = song.lyrics,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                // Synced lyrics - display current active line + next line
                                val currentLine = if (activeLrcIndex >= 0 && activeLrcIndex < parsedLrc.size) parsedLrc[activeLrcIndex] else null
                                val nextLine = if (activeLrcIndex + 1 >= 0 && activeLrcIndex + 1 < parsedLrc.size) parsedLrc[activeLrcIndex + 1] else null

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (currentLine != null) {
                                        Text(
                                            text = currentLine.text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "• • • (Instrumental Intro) • • •",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    if (nextLine != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = nextLine.text,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Progress Seekbar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = position.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { viewModel.audioEngine.seekTo(it.toLong()) },
                        valueRange = 0f..maxOf(1f, duration.toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(position),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(duration),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 6. Media Control Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
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
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play / Pause FAB - Custom styling: rounded corners [24.dp], background `#21005D`
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF21005D))
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next
                        IconButton(onClick = { viewModel.skipNext() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Repeat
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        Icon(
                            if (isRepeat) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 7. High Density bottom features shortcut row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lyrics shortcut
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (song.lyrics.isNullOrBlank()) {
                                    viewModel.searchLyricsOnline()
                                    Toast.makeText(context, "Searching Gemini for lyrics...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Lyrics loaded offline!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lyrics",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Equalizer shortcut
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.selectTab(2) // Jump to Equalizer screen
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Equalizer",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Edit Tags shortcut
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showTagEditor = true // Open tag editor dialog
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = "Edit Tags",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Edit Tags",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tag Editor dialog integration
                if (showTagEditor) {
                    TagEditorDialog(
                        song = song,
                        viewModel = viewModel,
                        onDismiss = { showTagEditor = false }
                    )
                }

                // 8. FullScreen Scrolling Karaoke overlay dialog
                if (isKaraokeModeFull) {
                    Dialog(
                        onDismissRequest = { isKaraokeModeFull = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.96f) // deep immersive dark background
                        ) {
                            var autoScrollEnabled by remember { mutableStateOf(true) }
                            val listState = rememberLazyListState()

                            // Auto-scroll logic when active index changes
                            LaunchedEffect(activeLrcIndex) {
                                if (autoScrollEnabled && activeLrcIndex >= 0 && parsedLrc.isNotEmpty()) {
                                    listState.animateScrollToItem(maxOf(0, activeLrcIndex - 2))
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .padding(24.dp)
                            ) {
                                // Fullscreen Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.customTitle ?: song.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.customArtist ?: song.artist,
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Auto-scroll lock toggle icon
                                        IconButton(
                                            onClick = { autoScrollEnabled = !autoScrollEnabled }
                                        ) {
                                            Icon(
                                                imageVector = if (autoScrollEnabled) Icons.Default.CompassCalibration else Icons.Default.ExploreOff,
                                                contentDescription = "Toggle Auto-Scroll",
                                                tint = if (autoScrollEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                            )
                                        }

                                        IconButton(onClick = { isKaraokeModeFull = false }) {
                                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                                        }
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                                // Main Lyrics body
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    if (parsedLrc.isEmpty()) {
                                        // Plain text fallback
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = song.lyrics ?: "No lyrics text",
                                                fontSize = 18.sp,
                                                lineHeight = 28.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.9f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    } else {
                                        // Synced Scrolling Karaoke column
                                        androidx.compose.foundation.lazy.LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(vertical = 140.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            itemsIndexed(parsedLrc) { index, line ->
                                                val isActive = index == activeLrcIndex
                                                val isPassed = index < activeLrcIndex

                                                val textColor = if (isActive) {
                                                    MaterialTheme.colorScheme.primary
                                                } else if (isPassed) {
                                                    Color.White.copy(alpha = 0.35f)
                                                } else {
                                                    Color.White.copy(alpha = 0.75f)
                                                }

                                                val textScale = if (isActive) 1.25f else 1.0f
                                                val textWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            // Click to seek to this timestamp!
                                                            viewModel.audioEngine.seekTo(line.timestampMs)
                                                            // Resume auto-scroll if clicked
                                                            autoScrollEnabled = true
                                                        }
                                                        .padding(vertical = 14.dp, horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = line.text,
                                                        fontSize = (16 * textScale).sp,
                                                        fontWeight = textWeight,
                                                        color = textColor,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 24.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Overlay toast-like button if autoscroll is manually interrupted
                                        if (!autoScrollEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 20.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .clickable { autoScrollEnabled = true }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Resume Auto-Scroll", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Simple Media player inline preview controller
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.skipPrevious() }) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                                        }

                                        IconButton(
                                            onClick = { viewModel.togglePlayPause() },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }

                                        IconButton(onClick = { viewModel.skipNext() }) {
                                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Enhanced lyrics dialogues integration
                if (showManualSearch) {
                    ManualSearchDialog(
                        song = song,
                        viewModel = viewModel,
                        onDismiss = { showManualSearch = false }
                    )
                }

                if (showLyricsEditor) {
                    LyricsEditorDialog(
                        song = song,
                        viewModel = viewModel,
                        onDismiss = { showLyricsEditor = false }
                    )
                }

                if (showSyncEditor) {
                    SyncEditorDialog(
                        song = song,
                        viewModel = viewModel,
                        onDismiss = { showSyncEditor = false }
                    )
                }
            }
        }
    }
}
