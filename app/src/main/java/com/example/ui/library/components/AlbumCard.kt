package com.example.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.model.AlbumUiModel
import com.example.ui.screens.formatDuration
import com.example.ui.theme.OniSkin

@Composable
fun AlbumCard(
    album: AlbumUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OniSurface(
        modifier = modifier
            .width(160.dp)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick),
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OniSkin.spacing.xs)
        ) {
            OniArtwork(
                artworkUri = album.artworkUri,
                shape = OniSkin.artwork.shape,
                contentDescription = "Cover art for ${album.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

            Text(
                text = album.title,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = album.artist,
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${album.songCount} songs • ${formatDuration(album.totalDurationMs)}",
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumCardPreview() {
    AlbumCard(
        album = AlbumUiModel(
            albumKey = "sample_key",
            title = "Sample Album",
            artist = "Sample Artist",
            artworkUri = null,
            songCount = 12,
            totalDurationMs = 2400000L
        ),
        onClick = {}
    )
}
