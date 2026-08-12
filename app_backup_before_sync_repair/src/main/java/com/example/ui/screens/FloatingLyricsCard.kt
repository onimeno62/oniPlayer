package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlin.math.roundToInt

@Composable
fun FloatingLyricsCard(viewModel: MusicPlayerViewModel) {
    val floatingEnabled by viewModel.floatingLyricsEnabled.collectAsState()
    if (!floatingEnabled) return

    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val position by viewModel.audioEngine.position.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()

    // Drag positions state
    var offsetX by remember { mutableStateOf(30f) }
    var offsetY by remember { mutableStateOf(150f) }

    // Parse LRC lines
    val lrcLines = remember(currentSong?.lyrics) {
        LyricsHelper.parseLrc(currentSong?.lyrics)
    }

    // Active lines
    val activeIndex = remember(lrcLines, position) {
        LyricsHelper.getActiveLineIndex(lrcLines, position)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {} // Block some raw screen clicks if required, or let it pass
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(280.dp)
                .height(130.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Handle Row (Drag handle & Controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Floating Lyrics",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentSong?.customTitle ?: currentSong?.title ?: "No Song Playing",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(150.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setFloatingLyricsEnabled(false) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Lyrics Screen content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentSong == null) {
                        Text(
                            text = "Play a track to view lyrics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else if (currentSong?.lyrics.isNullOrBlank()) {
                        Text(
                            text = "No lyrics found for this song",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else if (lrcLines.isEmpty()) {
                        // Plain text fallback (scroll or show snippet)
                        Text(
                            text = currentSong!!.lyrics!!.take(80) + "...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    } else {
                        // Karaoke synchronized!
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val activeLine = if (activeIndex >= 0 && activeIndex < lrcLines.size) lrcLines[activeIndex] else null
                            val nextLine = if (activeIndex + 1 >= 0 && activeIndex + 1 < lrcLines.size) lrcLines[activeIndex + 1] else null

                            if (activeLine != null) {
                                Text(
                                    text = activeLine.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "(Intro / Instrumental)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (nextLine != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nextLine.text,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
