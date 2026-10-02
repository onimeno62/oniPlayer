package com.example.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

data class ListeningStatsUi(
    val totalListening: String,
    val topArtist: String?,
    val tracksThisWeek: Int
)

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Derives lightweight listening stats from data we already store (playCount, duration,
 * lastPlayedTimestamp). Total time is an estimate: play count x track length.
 * Returns null when nothing has been played yet so the section can hide itself.
 */
fun computeListeningStats(songs: List<SongEntity>, now: Long = System.currentTimeMillis()): ListeningStatsUi? {
    val played = songs.filter { it.playCount > 0 }
    if (played.isEmpty()) return null
    val totalMs = played.sumOf { it.playCount.toLong() * it.duration.coerceAtLeast(0L) }
    val hours = totalMs / 3_600_000L
    val total = if (hours >= 1) "$hours h" else "${(totalMs / 60_000L).coerceAtLeast(1L)} min"
    val topArtist = played
        .groupBy { it.displayArtist.ifBlank { "Unknown Artist" } }
        .maxByOrNull { (_, list) -> list.sumOf { it.playCount } }
        ?.key
    val week = songs.count { it.lastPlayedTimestamp > 0 && now - it.lastPlayedTimestamp <= WEEK_MS }
    return ListeningStatsUi(total, topArtist, week)
}

@Composable
fun ListeningStatsRow(stats: ListeningStatsUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
    ) {
        StatChip(Icons.Default.Schedule, stats.totalListening, "Total listening", Modifier.weight(1f))
        StatChip(Icons.Default.Person, stats.topArtist ?: "None yet", "Top artist", Modifier.weight(1f))
        StatChip(Icons.Default.MusicNote, stats.tracksThisWeek.toString(), "This week", Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    OniSurface(
        modifier = modifier
            .height(64.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.xs, vertical = OniSkin.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            OniSurface(
                variant = OniSurfaceVariant.Flat,
                shape = OniSkin.shapes.full,
                containerColor = OniSkin.colors.primaryContainer
            ) {
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = OniSkin.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
