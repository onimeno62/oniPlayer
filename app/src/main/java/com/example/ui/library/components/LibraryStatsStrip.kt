package com.example.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

@Composable
fun LibraryStatsStrip(
    songCount: Int,
    artistCount: Int,
    albumCount: Int,
    favoriteCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = OniSkin.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
    ) {
        StatItem(label = "Songs", value = songCount.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "Artists", value = artistCount.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "Albums", value = albumCount.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "Favorites", value = favoriteCount.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OniSurface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            },
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.xs, vertical = OniSkin.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = OniSkin.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
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

@Preview(showBackground = true)
@Composable
private fun LibraryStatsStripPreview() {
    LibraryStatsStrip(
        songCount = 142,
        artistCount = 28,
        albumCount = 16,
        favoriteCount = 35
    )
}
