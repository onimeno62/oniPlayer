package com.example.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.entity.SongEntity
import com.example.ui.components.music.OniTrackRow
import com.example.ui.screens.PlayingEqualizerWave
import com.example.ui.theme.OniSkin

@Composable
fun SongRow(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    OniTrackRow(
        title = song.displayTitle,
        artist = song.displayArtist.ifBlank { "Unknown Artist" },
        album = song.displayAlbum.ifBlank { null },
        artworkUri = song.albumArtUri,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onMoreClick = onShowMenu,
        playingIndicator = if (isCurrent) {
            {
                if (isPlaying) {
                    PlayingEqualizerWave(
                        color = OniSkin.colors.primary,
                        modifier = Modifier.padding(end = OniSkin.spacing.sm)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = "Currently playing",
                        tint = OniSkin.colors.primary,
                        modifier = Modifier
                            .padding(end = OniSkin.spacing.sm)
                            .size(20.dp)
                    )
                }
            }
        } else null,
        modifier = modifier
    )
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
