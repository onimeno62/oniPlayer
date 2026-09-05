package com.example.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.model.ArtistUiModel
import com.example.ui.theme.OniSkin

@Composable
fun ArtistRow(
    artist: ArtistUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniArtwork(
                artworkUri = artist.artworkUri,
                size = 48.dp,
                shape = CircleShape,
                contentDescription = "Avatar for ${artist.name}",
                placeholder = {
                    val initial = artist.name.trim().take(1).uppercase().ifEmpty { "?" }
                    Text(
                        text = initial,
                        style = OniSkin.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary
                    )
                }
            )

            Spacer(modifier = Modifier.width(OniSkin.spacing.md))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.name,
                    style = OniSkin.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val albumText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
                val songText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                Text(
                    text = "$albumText • $songText",
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OniSkin.colors.textTertiary
            )
        }
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
