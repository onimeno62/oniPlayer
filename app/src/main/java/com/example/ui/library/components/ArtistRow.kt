package com.example.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.theme.LocalAccentColor

@Composable
fun ArtistRow(
    artist: ArtistUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = LibrarySpacing.lg, vertical = LibrarySpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LocalAccentColor.current.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUri != null) {
                AsyncImage(
                    model = artist.artworkUri,
                    contentDescription = "Avatar for ${artist.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalAccentColor.current
                )
            }
        }

        Spacer(modifier = Modifier.width(LibrarySpacing.md))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(LibrarySpacing.xs))

            val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
            val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
            Text(
                text = "$albumText • $songText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArtistRowPreview() {
    ArtistRow(
        artist = ArtistUiModel(
            artistKey = "sample_artist",
            name = "Sample Artist",
            albumCount = 3,
            songCount = 28,
            artworkUri = null
        ),
        onClick = {}
    )
}
