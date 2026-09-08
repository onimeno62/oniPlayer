package com.example.ui.screens

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlin.math.roundToInt

@Composable
fun FloatingLyricsCard(viewModel: MusicPlayerViewModel) {
    val floatingEnabled by viewModel.floatingLyricsEnabled.collectAsState()
    if (!floatingEnabled) return
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val position by viewModel.audioEngine.position.collectAsState()
    var offsetX by remember { mutableStateOf(30f) }
    var offsetY by remember { mutableStateOf(150f) }
    val lrcLines = remember(currentSong?.lyrics) { LyricsHelper.parseLrc(currentSong?.lyrics) }
    val activeIndex = remember(lrcLines, position) { LyricsHelper.getActiveLineIndex(lrcLines, position) }

    Box(modifier = Modifier.fillMaxSize()) {
        OniSurface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .widthIn(min = 240.dp, max = 320.dp)
                .heightIn(min = 130.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.card,
            elevation = OniSkin.elevation.floating
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(OniSkin.spacing.sm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DragIndicator, contentDescription = "Drag", tint = OniSkin.colors.textTertiary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
                        Icon(Icons.Default.Lyrics, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Text(currentSong?.customTitle ?: currentSong?.title ?: "No Song Playing", style = OniSkin.typography.labelMedium, color = OniSkin.colors.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { viewModel.setFloatingLyricsEnabled(false) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OniSkin.colors.textSecondary)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), contentAlignment = Alignment.Center) {
                    when {
                        currentSong == null -> Text("Play a track to view lyrics", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        currentSong?.lyrics.isNullOrBlank() -> Text("No lyrics found for this song", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        lrcLines.isEmpty() -> Text(currentSong!!.lyrics!!.take(80) + "...", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary, maxLines = 4)
                        else -> {
                            val activeLine = lrcLines.getOrNull(activeIndex)
                            val nextLine = lrcLines.getOrNull(activeIndex + 1)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(activeLine?.text ?: "(Intro / Instrumental)", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.primary, maxLines = 2)
                                nextLine?.let { Text(it.text, style = OniSkin.typography.caption, color = OniSkin.colors.textTertiary, maxLines = 1) }
                            }
                        }
                    }
                }
            }
        }
    }
}
