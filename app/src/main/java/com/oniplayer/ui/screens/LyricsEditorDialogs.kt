package com.oniplayer.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.oniplayer.data.entity.SongEntity
import com.oniplayer.ui.components.button.OniCircularActionButton
import com.oniplayer.ui.components.button.OniIconButton
import com.oniplayer.ui.components.button.OniIconButtonStyle
import com.oniplayer.ui.components.button.OniPrimaryButton
import com.oniplayer.ui.components.button.OniSecondaryButton
import com.oniplayer.ui.components.state.OniLoadingState
import com.oniplayer.ui.components.surface.OniSurface
import com.oniplayer.ui.components.surface.OniSurfaceVariant
import com.oniplayer.ui.lyrics.LyricsHelper
import com.oniplayer.ui.lyrics.LyricsLanguage
import com.oniplayer.ui.lyrics.LyricsSearchUiState
import com.oniplayer.ui.lyrics.LyricsSource
import com.oniplayer.ui.lyrics.LrcLine
import com.oniplayer.ui.lyrics.OnlineLyricsResult
import com.oniplayer.ui.theme.OniSkin
import com.oniplayer.ui.viewmodel.LyricsSearchViewModel
import com.oniplayer.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsEditorDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(song.lyrics ?: "") }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        OniSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.md),
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.dialog
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OniSkin.spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lyrics Editor", fontWeight = FontWeight.Bold, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                            Text(song.customTitle ?: song.title, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        }
                    }
                    OniIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        style = OniIconButtonStyle.Ghost
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = OniSkin.spacing.sm), color = OniSkin.colors.divider)

                // Guide
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    containerColor = OniSkin.colors.primaryContainer,
                    contentColor = OniSkin.colors.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(OniSkin.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Text(
                            "You can paste plain text or synchronized LRC text with [mm:ss.xx] timestamp tags.",
                            style = OniSkin.typography.caption,
                            color = OniSkin.colors.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

                // Text Area
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type or paste lyrics here...", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = OniSkin.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = OniSkin.shapes.button,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OniSkin.colors.textPrimary,
                        unfocusedTextColor = OniSkin.colors.textPrimary,
                        focusedContainerColor = OniSkin.colors.surfaceVariant,
                        unfocusedContainerColor = OniSkin.colors.surfaceVariant,
                        focusedBorderColor = OniSkin.colors.primary,
                        unfocusedBorderColor = OniSkin.colors.outline,
                        cursorColor = OniSkin.colors.primary,
                        selectionColors = TextSelectionColors(OniSkin.colors.primary, OniSkin.colors.primaryContainer)
                    )
                )

                Spacer(modifier = Modifier.height(OniSkin.spacing.md))

                // Actions
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
                    ) {
                        Text("Cancel", style = OniSkin.typography.labelLarge)
                    }
                    OniPrimaryButton(
                        text = "Save Changes",
                        onClick = {
                            viewModel.updateLyrics(song.id, text.trim().ifEmpty { null })
                            Toast.makeText(context, "Lyrics saved successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Sync Editor (unchanged)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyncEditorDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Playback state bindings
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val position by viewModel.audioEngine.position.collectAsState()
    val duration by viewModel.audioEngine.duration.collectAsState()

    val rawLyrics = song.lyrics ?: ""

    val initialLines = remember(rawLyrics) {
        val parsed = LyricsHelper.parseLrc(rawLyrics)
        if (parsed.isNotEmpty()) {
            parsed.map { it.text to it.timestampMs }.toMutableStateList()
        } else {
            rawLyrics.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it to -1L }
                .toMutableStateList()
        }
    }

    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        OniSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.md),
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.dialog
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OniSkin.spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interactive Sync Editor", fontWeight = FontWeight.Bold, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                            Text("Tap the sync icon on each line when it is sung!", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        }
                    }
                    OniIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        style = OniIconButtonStyle.Ghost
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = OniSkin.spacing.xs), color = OniSkin.colors.divider)

                // Progress Info Card
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(OniSkin.spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Position: ${LyricsHelper.formatLrcTime(position).replace("[", "").replace("]", "")}",
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Monospace,
                                style = OniSkin.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.primary
                            )

                            OniCircularActionButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                onClick = { viewModel.togglePlayPause() },
                                size = 48.dp,
                                iconSize = 20.dp,
                                elevation = OniSkin.elevation.flat
                            )
                        }

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                        ) {
                            OniSecondaryButton(
                                text = "Delay -0.5s",
                                onClick = {
                                    for (i in initialLines.indices) {
                                        val (text, time) = initialLines[i]
                                        if (time >= 0) {
                                            initialLines[i] = text to maxOf(0L, time - 500L)
                                        }
                                    }
                                    Toast.makeText(context, "Delayed all lines by -0.5s", Toast.LENGTH_SHORT).show()
                                }
                            )

                            OniSecondaryButton(
                                text = "Advance +0.5s",
                                onClick = {
                                    for (i in initialLines.indices) {
                                        val (text, time) = initialLines[i]
                                        if (time >= 0) {
                                            initialLines[i] = text to time + 500L
                                        }
                                    }
                                    Toast.makeText(context, "Advanced all lines by +0.5s", Toast.LENGTH_SHORT).show()
                                }
                            )

                            Button(
                                onClick = {
                                    for (i in initialLines.indices) {
                                        initialLines[i] = initialLines[i].first to -1L
                                    }
                                    Toast.makeText(context, "Cleared all timing tags", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = OniSkin.spacing.buttonHorizontal, vertical = OniSkin.spacing.buttonVertical),
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = OniSkin.shapes.button,
                                colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.error, contentColor = OniSkin.colors.onError)
                            ) {
                                Text("Reset All", style = OniSkin.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

                // List of Lyrics to Sync
                OniSurface(
                    modifier = Modifier.weight(1f),
                    variant = OniSurfaceVariant.Outlined,
                    shape = OniSkin.shapes.card
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(OniSkin.spacing.xxs)
                    ) {
                        itemsIndexed(initialLines) { index, linePair ->
                            val (lineText, timestamp) = linePair
                            val isSynced = timestamp >= 0L

                            OniSurface(
                                modifier = Modifier.fillMaxWidth(),
                                variant = OniSurfaceVariant.Flat,
                                shape = OniSkin.shapes.listItem,
                                containerColor = if (isSynced) OniSkin.colors.primaryContainer else OniSkin.colors.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OniIconButton(
                                        icon = if (isSynced) Icons.Default.CheckCircle else Icons.Default.Timer,
                                        contentDescription = "Sync line",
                                        onClick = {
                                            initialLines[index] = lineText to position
                                            if (index + 1 < initialLines.size) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(maxOf(0, index - 1))
                                                }
                                            }
                                        },
                                        selected = isSynced,
                                        style = OniIconButtonStyle.Neutral,
                                        iconSize = 18.dp
                                    )

                                    Spacer(modifier = Modifier.width(OniSkin.spacing.sm))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lineText,
                                            style = OniSkin.typography.bodyMedium,
                                            color = if (isSynced) OniSkin.colors.onPrimaryContainer else OniSkin.colors.textSecondary,
                                            fontWeight = if (isSynced) FontWeight.Medium else FontWeight.Normal
                                        )

                                        if (isSynced) {
                                            Text(
                                                text = LyricsHelper.formatLrcTime(timestamp).replace("[", "").replace("]", ""),
                                                style = OniSkin.typography.caption,
                                                fontFamily = FontFamily.Monospace,
                                                color = OniSkin.colors.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (isSynced) {
                                        OniIconButton(
                                            icon = Icons.Default.Clear,
                                            contentDescription = "Clear timestamp",
                                            onClick = { initialLines[index] = lineText to -1L },
                                            style = OniIconButtonStyle.Ghost,
                                            iconSize = 16.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.md))

                // Actions
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
                    ) {
                        Text("Cancel", style = OniSkin.typography.labelLarge)
                    }
                    OniPrimaryButton(
                        text = "Save & Apply",
                        onClick = {
                            val syncedLines = initialLines.filter { it.second >= 0L }.map { LrcLine(it.second, it.first) }
                            if (syncedLines.isEmpty()) {
                                val plainText = initialLines.joinToString("\n") { it.first }
                                viewModel.updateLyrics(song.id, plainText)
                                Toast.makeText(context, "Saved as plain lyrics", Toast.LENGTH_SHORT).show()
                            } else {
                                val lrcString = LyricsHelper.buildLrcString(syncedLines)
                                viewModel.updateLyrics(song.id, lrcString)
                                Toast.makeText(context, "Successfully saved synchronized LRC lyrics!", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}
