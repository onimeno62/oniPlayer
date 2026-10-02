package com.example.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Queue sheet for the Player screen in Default Skin.
 *
 * Displays the active queue with current playing track indicator,
 * remove-from-queue buttons and current-track indication.
 */
@Composable
fun PlayerQueueSheet(
    queue: List<SongEntity>,
    currentSong: SongEntity?,
    onPlaySong: (SongEntity) -> Unit,
    onRemoveFromQueue: ((SongEntity) -> Unit)? = null,
    onMoveInQueue: ((Int, Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val localQueue = remember(queue) { mutableStateListOf<SongEntity>().apply { addAll(queue) } }
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(queue) {
        if (draggedIndex == null) {
            localQueue.clear()
            localQueue.addAll(queue)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OniSkin.colors.background.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            OniSurface(
                variant = OniSurfaceVariant.Elevated,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .fillMaxHeight(0.75f)
                    .clickable(enabled = false) {}
                    .testTag("player_queue_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(top = 12.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(OniSkin.colors.outline.copy(alpha = 0.5f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OniSkin.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = OniSkin.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Playing Queue",
                                style = OniSkin.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${queue.size})",
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary
                            )
                        }

                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close queue",
                                tint = OniSkin.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (queue.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Queue is empty",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = OniSkin.spacing.screenHorizontal,
                                end = OniSkin.spacing.screenHorizontal,
                                bottom = OniSkin.spacing.screenVertical,
                                top = 4.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(
                                items = localQueue,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                val isCurrent = song.id == currentSong?.id
                                val containerVariant = if (isCurrent) OniSurfaceVariant.Soft else OniSurfaceVariant.Flat

                                OniSurface(
                                    variant = containerVariant,
                                    shape = OniSkin.shapes.medium,
                                    onClick = { onPlaySong(song) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { translationY = if (draggedIndex == index) draggedOffset else 0f }
                                        .semantics {
                                            selected = isCurrent
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            tint = OniSkin.colors.textSecondary,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .pointerInput(index) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { draggedIndex = index; draggedOffset = 0f },
                                                        onDragCancel = { draggedIndex = null; draggedOffset = 0f },
                                                        onDragEnd = {
                                                            val from = draggedIndex
                                                            val target = listState.layoutInfo.visibleItemsInfo
                                                                .filter { it.index != from }
                                                                .minByOrNull { info -> kotlin.math.abs((info.offset + info.size / 2) - (draggedOffset + (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == from }?.let { it.offset + it.size / 2 } ?: 0))) }
                                                                ?.index
                                                            if (from != null && target != null && from != target) {
                                                                val moved = localQueue.removeAt(from)
                                                                val adjustedTarget = if (target > from) target - 1 else target
                                                                localQueue.add(adjustedTarget.coerceIn(0, localQueue.size), moved)
                                                                onMoveInQueue?.invoke(from, target)
                                                            }
                                                            draggedIndex = null; draggedOffset = 0f
                                                        },
                                                        onDrag = { change, amount -> change.consume(); draggedOffset += amount.y }
                                                    )
                                                }
                                        )
                                        OniArtwork(
                                            artworkUri = song.albumArtUri,
                                            contentDescription = null,
                                            size = 44.dp,
                                            shape = OniSkin.shapes.small
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                style = OniSkin.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) OniSkin.colors.primary else OniSkin.colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = song.artist,
                                                style = OniSkin.typography.caption,
                                                color = OniSkin.colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Equalizer,
                                                contentDescription = "Currently playing",
                                                tint = OniSkin.colors.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // The playback controller handles current-track removal by advancing safely.
                                        if (onRemoveFromQueue != null) {
                                            IconButton(
                                                onClick = { onRemoveFromQueue(song) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RemoveCircleOutline,
                                                    contentDescription = "Remove from queue",
                                                    tint = OniSkin.colors.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(8.dp))
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
}
