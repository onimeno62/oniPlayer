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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.data.entity.SongEntity
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.lyrics.LrcLine
import com.example.ui.lyrics.PitchStatus
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
 * Phase 7.2 Features:
 * - Playback-driven auto-scrolling anchored at top reading position
 * - Stable sync button without internal state oscillation or blinking
 * - Distinct user-scroll interaction tracking via interactionSource
 * - Noise-gated pitch detection & PitchStatus monitoring
 * - Safe headphone monitor and microphone lifecycle management
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val isVocalReductionActive by viewModel.isVocalReductionActive.collectAsStateWithLifecycle()

    var showManualSearch by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showSyncEditor by remember { mutableStateOf(false) }
    var showOffsetControls by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    // Language & Translation State
    var selectedLanguage by remember(song.id) { mutableStateOf("Original") }
    var isTranslatingLyrics by remember { mutableStateOf(false) }
    var translatedLyricsText by remember(song.id) { mutableStateOf<String?>(null) }

    // Manual sync offset nudge (in milliseconds)
    var manualOffsetMs by remember(song.id) { mutableStateOf(0L) }

    val rawLyrics = song.lyrics ?: ""
    val activeLyricsText = remember(selectedLanguage, translatedLyricsText, rawLyrics) {
        if (selectedLanguage == "Original" || translatedLyricsText.isNullOrBlank()) {
            rawLyrics
        } else {
            translatedLyricsText!!
        }
    }

    val parsedLrc = remember(activeLyricsText) {
        LyricsHelper.parseLrc(activeLyricsText).filter { it.text.isNotBlank() }
    }

    // Effective playback position with manual calibration offset
    val effectivePosition = remember(currentPosition, manualOffsetMs) {
        maxOf(0L, currentPosition + manualOffsetMs)
    }

    val activeLrcIndex: Int = remember(effectivePosition, parsedLrc) {
        LyricsHelper.getActiveLineIndex(parsedLrc, effectivePosition)
    }

    // Ensure microphone is stopped when dialog leaves composition
    DisposableEffect(Unit) {
        onDispose {
            viewModel.karaokeMicEngine.stopMic()
        }
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
            var showPlainTextMode by remember(activeLyricsText) {
                mutableStateOf(!LyricsHelper.isSynced(activeLyricsText))
            }

            val isMicEnabled by viewModel.karaokeMicEngine.isMicEnabled.collectAsStateWithLifecycle()
            val micAmplitude by viewModel.karaokeMicEngine.amplitude.collectAsStateWithLifecycle()
            val micGain by viewModel.karaokeMicEngine.micGain.collectAsStateWithLifecycle()
            val isAudioPassThroughEnabled by viewModel.karaokeMicEngine.isAudioPassThroughEnabled.collectAsStateWithLifecycle()
            val vocalPitchNote by viewModel.karaokeMicEngine.vocalPitchNote.collectAsStateWithLifecycle()
            val pitchStatus by viewModel.karaokeMicEngine.pitchStatus.collectAsStateWithLifecycle()

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

            // Distinct User-scroll detection: Listen ONLY to user drag interactions,
            // never conflating programmatic animateScrollToItem() with user touches!
            LaunchedEffect(listState.interactionSource) {
                listState.interactionSource.interactions.collectLatest { interaction ->
                    when (interaction) {
                        is DragInteraction.Start -> {
                            autoScrollEnabled = false
                        }
                    }
                }
            }

            // Top-anchored reading scroll: automatically aligns active lyric to top reading zone
            LaunchedEffect(activeLrcIndex, autoScrollEnabled) {
                if (autoScrollEnabled && activeLrcIndex >= 0 && parsedLrc.isNotEmpty()) {
                    listState.animateScrollToItem(
                        index = activeLrcIndex,
                        scrollOffset = 0
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
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
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showLanguageSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedLanguage != "Original") OniSkin.colors.primary.copy(alpha = 0.18f)
                                    else OniSkin.colors.surfaceVariant
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Select Lyrics Language",
                                tint = if (selectedLanguage != "Original") OniSkin.colors.primary else OniSkin.colors.textSecondary
                            )
                        }

                        // Fixed Non-Blinking Sync Button with clear stable visual states
                        if (!showPlainTextMode) {
                            IconButton(
                                onClick = {
                                    val newState = !autoScrollEnabled
                                    autoScrollEnabled = newState
                                    if (newState && activeLrcIndex >= 0 && parsedLrc.isNotEmpty()) {
                                        // Immediately jump or animate to current active lyric
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (autoScrollEnabled) OniSkin.colors.primary.copy(alpha = 0.15f)
                                        else OniSkin.colors.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = if (autoScrollEnabled) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                    contentDescription = if (autoScrollEnabled) "Auto-sync active (Tap to pause)" else "Auto-sync paused (Tap to resume)",
                                    tint = if (autoScrollEnabled) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Selector Capsule Switcher & Language Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            val syncActive = !showPlainTextMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (syncActive) OniSkin.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { showPlainTextMode = false }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (syncActive) OniSkin.colors.primary else OniSkin.colors.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Synced",
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
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Subject,
                                        contentDescription = null,
                                        tint = if (showPlainTextMode) OniSkin.colors.primary else OniSkin.colors.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Plain",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showPlainTextMode) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Active Language Pill & Offset indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedLanguage != "Original") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OniSkin.colors.primary.copy(alpha = 0.15f))
                                    .clickable { showLanguageSheet = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedLanguage,
                                    style = OniSkin.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.primary
                                )
                            }
                        }

                        // Timing Nudge Toggle Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (manualOffsetMs != 0L || showOffsetControls) OniSkin.colors.primary.copy(alpha = 0.15f)
                                    else OniSkin.colors.surfaceVariant
                                )
                                .clickable { showOffsetControls = !showOffsetControls }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sync Offset",
                                    tint = if (manualOffsetMs != 0L) OniSkin.colors.primary else OniSkin.colors.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (manualOffsetMs == 0L) "Sync Nudge" else "${if (manualOffsetMs > 0) "+" else ""}${manualOffsetMs}ms",
                                    style = OniSkin.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = if (manualOffsetMs != 0L) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Quick Timing Calibration Bar
                AnimatedVisibility(
                    visible = showOffsetControls,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Offset: ${if (manualOffsetMs > 0) "+" else ""}${manualOffsetMs}ms",
                                style = OniSkin.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.textPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                NudgeButton("-0.5s") { manualOffsetMs -= 500L }
                                NudgeButton("-0.1s") { manualOffsetMs -= 100L }
                                NudgeButton("+0.1s") { manualOffsetMs += 100L }
                                NudgeButton("+0.5s") { manualOffsetMs += 500L }

                                if (manualOffsetMs != 0L) {
                                    NudgeButton("Reset") { manualOffsetMs = 0L }
                                    NudgeButton("Save", isPrimary = true) {
                                        viewModel.shiftSongLyricsTiming(song.id, rawLyrics, manualOffsetMs)
                                        manualOffsetMs = 0L
                                        Toast.makeText(context, "Saved calibrated timing!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Search, Edit, Sync, Vocal Cut, Sing Along
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        label = "Sync Editor",
                        onClick = { showSyncEditor = true },
                        modifier = Modifier.weight(1f)
                    )

                    KaraokeActionChip(
                        icon = if (isVocalReductionActive) Icons.Default.HearingDisabled else Icons.Default.Hearing,
                        label = if (isVocalReductionActive) "Vocal Cut: ON" else "Vocal Cut",
                        isActive = isVocalReductionActive,
                        onClick = {
                            viewModel.toggleVocalReduction()
                            val msg = if (!isVocalReductionActive) "Vocal reduction activated!" else "Original vocals restored"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
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

                // Mic Mixer & Pitch Monitoring Panel
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
                            .padding(vertical = 6.dp)
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
                                        text = "Vocal Pitch & Monitor",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = OniSkin.colors.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Pitch Status Indicator
                                    val statusText = when (pitchStatus) {
                                        is PitchStatus.Detected -> "Pitch: ${(pitchStatus as PitchStatus.Detected).note}"
                                        is PitchStatus.Detecting -> "Detecting..."
                                        is PitchStatus.NoClearPitch -> "No Clear Pitch"
                                        is PitchStatus.Listening -> "Listening"
                                        is PitchStatus.Idle -> "Idle"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (pitchStatus is PitchStatus.Detected) OniSkin.colors.primary.copy(alpha = 0.2f)
                                                else OniSkin.colors.surfaceVariant
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = OniSkin.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pitchStatus is PitchStatus.Detected) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Gain: ${(micGain * 100).toInt()}%",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = OniSkin.colors.textSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Vocal amplitude visualizer canvas
                            val primaryColor = OniSkin.colors.primary
                            val outlineColor = OniSkin.colors.outline
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
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
                                        color = if (heightFactor > 0.25f) primaryColor else outlineColor.copy(alpha = 0.4f),
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
                                modifier = Modifier.height(24.dp)
                            )

                            // Live Headphone Monitor Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Live Ear Monitor (Passthrough)",
                                        style = OniSkin.typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OniSkin.colors.textPrimary
                                    )
                                    Text(
                                        text = "Headphones strongly recommended to prevent feedback",
                                        style = OniSkin.typography.caption,
                                        fontSize = 10.sp,
                                        color = OniSkin.colors.textTertiary
                                    )
                                }

                                Switch(
                                    checked = isAudioPassThroughEnabled,
                                    onCheckedChange = { viewModel.karaokeMicEngine.setAudioPassThrough(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OniSkin.colors.primary,
                                        checkedTrackColor = OniSkin.colors.primary.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }
                }

                // Main Lyrics View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isTranslatingLyrics) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = OniSkin.colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Translating lyrics to $selectedLanguage with AI...",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preserving musical timestamps and rhythm",
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary
                            )
                        }
                    } else if (showPlainTextMode) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayText = if (activeLyricsText.isNotBlank()) {
                                LyricsHelper.stripLrcTags(activeLyricsText)
                            } else {
                                "No lyrics found for this song.\nTap 'Search' or 'Edit' to add lyrics."
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
                            // Top-reading position: contentPadding top=16.dp and generous bottom padding
                            // so even the final lines can cleanly reach the top reading position!
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 320.dp),
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
                                        targetValue = (if (isActive) 1.08f else 0.95f) * micPulseScale,
                                        animationSpec = tween(durationMillis = 180),
                                        label = "karaoke_line_scale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.seekTo(line.timestampMs)
                                                autoScrollEnabled = true
                                            }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
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

                            // Manual scroll paused floating resume button
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = OniSkin.colors.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Resume Auto-Sync",
                                            style = OniSkin.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OniSkin.colors.onPrimary
                                        )
                                    }
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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

    // Language Translation Sheet
    if (showLanguageSheet) {
        val supportedLanguages = listOf(
            "Original",
            "English",
            "Romanized / Romaji",
            "Japanese (日本語)",
            "Spanish (Español)",
            "French (Français)",
            "German (Deutsch)",
            "Korean (한국어)",
            "Chinese (中文)"
        )

        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            containerColor = OniSkin.colors.background,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Translate Current Lyrics",
                        style = OniSkin.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textPrimary
                    )
                    IconButton(onClick = { showLanguageSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OniSkin.colors.textSecondary)
                    }
                }

                Text(
                    text = "AI translation feature: translate or romanize loaded lyrics while preserving timestamps.",
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                supportedLanguages.forEach { lang ->
                    val isSelected = selectedLanguage == lang
                    OniSurface(
                        variant = if (isSelected) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft,
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            showLanguageSheet = false
                            if (lang == "Original") {
                                selectedLanguage = "Original"
                                translatedLyricsText = null
                            } else {
                                selectedLanguage = lang
                                isTranslatingLyrics = true
                                viewModel.translateSongLyrics(song.id, rawLyrics, lang) { result ->
                                    isTranslatingLyrics = false
                                    result.onSuccess { translated ->
                                        translatedLyricsText = translated
                                        Toast.makeText(context, "Translated to $lang!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Translation failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang,
                                style = OniSkin.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = OniSkin.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (translatedLyricsText != null && selectedLanguage != "Original") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.updateLyrics(song.id, translatedLyricsText)
                            Toast.makeText(context, "Saved $selectedLanguage as song lyrics!", Toast.LENGTH_SHORT).show()
                            showLanguageSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Permanently Save Translated Lyrics", style = OniSkin.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
private fun NudgeButton(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPrimary) OniSkin.colors.primary
                else OniSkin.colors.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OniSkin.typography.caption,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) OniSkin.colors.onPrimary else OniSkin.colors.textPrimary
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
