package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.LocalCornerRadius
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun dashboardRadiusMedium(): Dp = LocalCornerRadius.current.dp

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

fun greetingForTime(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "LATE NIGHT TUNES"
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        hour < 21 -> "GOOD EVENING"
        else -> "GOOD NIGHT"
    }
}

@Composable
fun PlayingEqualizerWave(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "playing_equalizer")
    val scales = listOf(
        transition.animateFloat(0.25f, 0.9f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "bar1"),
        transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(310, easing = LinearEasing), RepeatMode.Reverse), label = "bar2"),
        transition.animateFloat(0.15f, 0.75f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "bar3"),
        transition.animateFloat(0.2f, 0.95f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "bar4")
    )
    Row(modifier = modifier.height(18.dp).width(20.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        scales.forEach { scale ->
            val height by scale
            Box(modifier = Modifier.weight(1f).fillMaxHeight(height).clip(RoundedCornerShape(1.dp)).background(color))
        }
    }
}

@Composable
fun PlaylistPickerBottomSheet(
    song: SongEntity,
    playlists: List<PlaylistEntity>,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = OniSkin.colors.surface, shape = OniSkin.shapes.bottomSheet) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.screenHorizontal)) {
            Text("Add to playlist", style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
            Text(song.displayTitle, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = OniSkin.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("New playlist") })
                IconButton(onClick = {
                    val name = newPlaylistName.trim()
                    if (name.isNotEmpty()) {
                        viewModel.createPlaylist(name, listOf(song.id))
                        onDismiss()
                    }
                }, enabled = newPlaylistName.trim().isNotEmpty()) {
                    Icon(Icons.Default.Add, contentDescription = "Create playlist")
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = OniSkin.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val containsSong = playlist.songIdsJson.contains("\"" + song.id + "\"")
                    OniSurface(variant = if (containsSong) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft, shape = OniSkin.shapes.card, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = OniSkin.colors.primary)
                            Spacer(Modifier.width(OniSkin.spacing.md))
                            Text(playlist.name, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary, modifier = Modifier.weight(1f))
                            if (containsSong) Icon(Icons.Default.Check, contentDescription = "Already in playlist", tint = OniSkin.colors.primary)
                            else TextButton(onClick = { viewModel.addSongToPlaylist(song.id, playlist.id); onDismiss() }) { Text("Add") }
                        }
                    }
                }
            }
        }
    }
}
