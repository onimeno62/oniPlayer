package com.example.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniArtwork
import com.example.ui.theme.OniSkin

@Composable
fun HorizontalSongCard(
    song: SongEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = song.displayTitle
    val artist = song.displayArtist

    Column(
        modifier = modifier
            .width(120.dp)
            .defaultMinSize(minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title by $artist"
            }
            .clickable(onClick = onClick)
    ) {
        OniArtwork(
            artworkUri = song.albumArtUri,
            size = 120.dp,
            shape = OniSkin.artwork.shape,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
        Text(
            text = title,
            style = OniSkin.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = OniSkin.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = artist,
            style = OniSkin.typography.caption,
            color = OniSkin.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
