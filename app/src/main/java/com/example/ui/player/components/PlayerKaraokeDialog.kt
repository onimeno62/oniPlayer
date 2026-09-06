package com.example.ui.player.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.screens.LyricsEditorDialog
import com.example.ui.screens.ManualSearchDialog
import com.example.ui.screens.SyncEditorDialog
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * Full-screen Synchronized Lyrics & Karaoke Sing-Along dialog in Default Skin.
 *
 * Preserves the full real-time mic engine, vocal visualizer, audio amplitude scaling,
 * seek-on-tap, and manual lyrics management without regressions.
 * Migrated to Default Skin design system tokens.
 */
@Composable
fun PlayerKaraokeDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentPosition by viewModel.position.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    var showManualSearch by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showSyncEditor by remember { mutableStateOf(false) }

    val rawLyrics = song.lyrics ?: ""
    val parsedLrc = remember(rawLyrics) { LyricsHelper.parseLrc(rawLyrics) }
    val activeLrcIndex: Int = remember(currentPosition, parsedLrc) {
        LyricsHelper.getActiveLineIndex(parsedLrc, currentPosition)
    }

    Dialog(
        onDismissRequest = {
            viewModel.karaokeMicEngine.stopMic()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OniSkin.colors.background
        ) {
            var autoScrollEnabled by remember { mutableStateOf(true) }
            val listState = rememberLazyListState()
            var showPlainTextMode by remember(song.lyrics) {
                mutableStateOf(!LyricsHelper.isSynced(song.lyrics))
            }

            val isMicEnabled by viewModel.karaokeMicEngine.isMicEnabled.collectAsStateWithLifecycle()
            val micAmplitude by viewModel.karaokeMicEngine.amplitude.collectAsStateWithLifecycle()
            val micGain by viewModel.karaokeMicEngine.micGain.collectAsStateWithLifecycle()

            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.karaokeMicEngine.startMic()
                    Toast.makeText(context, "Microphone enabled! Sing along!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Microphone permission is required to sing along", Toast.LENGTH_SHORT).show()
                }
            }

            // Auto-scroll logic: scroll active highlighted line into view
            LaunchedEffect(activeLrcIndex) {
                if (autoScrollEnabled && activeLrcIndex >= 0 && parsedLrc.isNotEmpty()) {
                    listState.animateScrollToItem(activeLrcIndex)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.karaokeMicEngine.stopMic()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OniSkin.colors.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close karaoke",
                            tint = OniSkin.colors.textPrimary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = song.customTitle ?: song.title,
                            style = OniSkin.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.customArtist ?: song.artist,
                            style = OniSkin.typography.caption,
                            color = OniSkin.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!showPlainTextMode) {
                        IconButton(
                            onClick = { autoScrollEnabled = !autoScrollEnabled },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (autoScrollEnabled) OniSkin.colors.primary.copy(alpha = 0.15f)
                                    else OniSkin.colors.surfaceVariant
                                )
                        ) {
                            Icon(
                                imageVector = if (autoScrollEnabled) Icons.Default.CompassCalibration else Icons.Default.ExploreOff,
                                contentDescription = "Toggle auto-scroll",
                                tint = if (autoScrollEnabled) OniSkin.colors.primary else OniSkin.colors.textSecondary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector Capsule Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            val syncActive = !showPlainTextMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (syncActive) OniSkin.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { showPlainTextMode = false }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (syncActive) OniSkin.colors.primary else OniSkin.colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Synced Karaoke",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = if (syncActive) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (showPlainTextMode) OniSkin.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { showPlainTextMode = true }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Subject,
                                        contentDescription = null,
                                        tint = if (showPlainTextMode) OniSkin.colors.primary else OniSkin.colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Plain Text",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showPlainTextMode) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Search, Edit, Sync, Sing Along
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KaraokeActionChip(
                        icon = Icons.Default.CloudDownload,
                        label = "Search",
                        onClick = { showManualSearch = true },
                        modifier = Modifier.weight(1f)
                    )

                    KaraokeActionChip(
                        icon = Icons.Default.Edit,
                        label = "Edit",
                        onClick = { showLyricsEditor = true },
                        modifier = Modifier.weight(1f)
                    )

                    KaraokeActionChip(
                        icon = Icons.Default.Sync,
                        label = "Sync",
                        onClick = { showSyncEditor = true },
                        modifier = Modifier.weight(1f)
                    )

                    KaraokeActionChip(
                        icon = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        label = "Sing Along",
                        isActive = isMicEnabled,
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
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mic Mixer & Vocal Visualizer Panel
                AnimatedVisibility(
                    visible = isMicEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = OniSkin.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vocal Monitor & Gain",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = OniSkin.colors.primary
                                    )
                                }

                                Text(
                                    text = "${(micGain * 100).toInt()}%",
                                    style = OniSkin.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Vocal frequency/amplitude visualizer canvas
                            val primaryColor = OniSkin.colors.primary
                            val outlineColor = OniSkin.colors.outline
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            ) {
                                val barCount = 24
                                val barWidth = size.width / (barCount * 1.5f)
                                val spacing = barWidth * 0.5f

                                for (i in 0 until barCount) {
                                    val distanceFromCenter = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                                    val heightFactor = (1f - distanceFromCenter * 0.6f) * (micAmplitude / 100f).coerceIn(0.1f, 1f)
                                    val barHeight = (size.height * heightFactor).coerceAtLeast(4f)
                                    val x = i * (barWidth + spacing) + spacing / 2

                                    drawLine(
                                        color = if (heightFactor > 0.3f) primaryColor else outlineColor.copy(alpha = 0.4f),
                                        start = Offset(x, size.height / 2 - barHeight / 2),
                                        end = Offset(x, size.height / 2 + barHeight / 2),
                                        strokeWidth = barWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Gain Slider
                            Slider(
                                value = micGain,
                                onValueChange = { viewModel.karaokeMicEngine.setMicGain(it) },
                                valueRange = 0.2f..3.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = OniSkin.colors.primary,
                                    activeTrackColor = OniSkin.colors.primary,
                                    inactiveTrackColor = OniSkin.colors.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Main Lyrics View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (showPlainTextMode) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayText = if (rawLyrics.isNotBlank()) {
                                LyricsHelper.stripLrcTags(rawLyrics)
                            } else {
                                "No lyrics found for this song.\nTap 'Search Online' or 'Edit' to add lyrics."
                            }

                            Text(
                                text = displayText,
                                style = OniSkin.typography.bodyLarge,
                                color = OniSkin.colors.textPrimary,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )
                        }
                    } else {
                        if (parsedLrc.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                OniSurface(
                                    variant = OniSurfaceVariant.Soft,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lyrics,
                                            contentDescription = null,
                                            tint = OniSkin.colors.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "No Synchronized Lyrics",
                                    style = OniSkin.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Search online or paste timestamped lyrics to unlock real-time scrolling karaoke mode.",
                                    style = OniSkin.typography.bodySmall,
                                    color = OniSkin.colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { showManualSearch = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Search Online", style = OniSkin.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = { showLyricsEditor = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Paste Manually", style = OniSkin.typography.labelMedium)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(parsedLrc) { index, line ->
                                    val isActive = index == activeLrcIndex
                                    val isPassed = index < activeLrcIndex

                                    val textColor = when {
                                        isActive && isMicEnabled -> OniSkin.colors.primary
                                        isActive -> OniSkin.colors.textPrimary
                                        isPassed -> OniSkin.colors.textTertiary.copy(alpha = 0.5f)
                                        else -> OniSkin.colors.textSecondary
                                    }

                                    val micPulseScale = if (isActive && isMicEnabled) {
                                        1.0f + (micAmplitude / 100f) * 0.12f
                                    } else 1.0f

                                    val scale by animateFloatAsState(
                                        targetValue = (if (isActive) 1.1f else 0.95f) * micPulseScale,
                                        animationSpec = tween(durationMillis = 200),
                                        label = "karaoke_line_scale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.seekTo(line.timestampMs)
                                                autoScrollEnabled = true
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = line.text,
                                            style = if (isActive) OniSkin.typography.titleLarge else OniSkin.typography.bodyLarge,
                                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 34.sp,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                        )
                                    }
                                }
                            }

                            if (!autoScrollEnabled) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(OniSkin.colors.primary)
                                        .clickable { autoScrollEnabled = true }
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Resume Auto-Scroll",
                                        style = OniSkin.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OniSkin.colors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Inline Controls
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skipPrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = OniSkin.colors.textPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OniSkin.colors.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or pause",
                                tint = OniSkin.colors.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.skipNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next track",
                                tint = OniSkin.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Secondary Dialogs
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

@Composable
private fun KaraokeActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val variant = if (isActive) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft
    val tint = if (isActive) OniSkin.colors.primary else OniSkin.colors.textSecondary

    OniSurface(
        variant = variant,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        modifier = modifier.height(44.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = OniSkin.typography.caption,
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
