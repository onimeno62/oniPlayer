package com.example.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.entity.SongEntity
import com.example.ui.screens.PlayingEqualizerWave
import com.example.ui.screens.dashboardRadiusMedium
import com.example.ui.screens.dashboardRadiusSmall
import com.example.ui.theme.LocalAccentColor

@Composable
fun SongRow(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(dashboardRadiusMedium()))
            .background(
                if (isCurrent) LocalAccentColor.current.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = LibrarySpacing.md, vertical = LibrarySpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = "Cover art for ${song.displayTitle}",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(dashboardRadiusSmall()))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
        )

        Spacer(modifier = Modifier.width(LibrarySpacing.md))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) LocalAccentColor.current else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(LibrarySpacing.xs))

            val artistName = song.displayArtist.ifBlank { "Unknown Artist" }
            val albumName = song.displayAlbum.ifBlank { "Unknown Album" }
            Text(
                text = "$artistName • $albumName",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) LocalAccentColor.current.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(LibrarySpacing.sm))

        if (isCurrent) {
            if (isPlaying) {
                PlayingEqualizerWave(
                    color = LocalAccentColor.current,
                    modifier = Modifier.padding(end = LibrarySpacing.sm)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "Currently playing",
                    tint = LocalAccentColor.current,
                    modifier = Modifier
                        .padding(end = LibrarySpacing.sm)
                        .size(20.dp)
                )
            }
        }

        IconButton(
            onClick = onShowMenu,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Track menu for ${song.displayTitle}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SongRowPreview() {
    val sampleSong = SongEntity(
        id = "sample_song_1",
        title = "Sample Song Title",
        artist = "Sample Artist",
        album = "Sample Album",
        genre = "Sample Genre",
        duration = 210000L,
        filePath = "/sample/path.mp3",
        albumArtUri = null
    )
    Column {
        SongRow(
            song = sampleSong,
            isCurrent = false,
            isPlaying = false,
            onClick = {},
            onShowMenu = {}
        )
        SongRow(
            song = sampleSong,
            isCurrent = true,
            isPlaying = true,
            onClick = {},
            onShowMenu = {}
        )
    }
}
