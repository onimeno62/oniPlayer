package com.example.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.library.components.ArtistRow
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.model.ArtistUiModel

@Composable
fun ArtistsScreen(
    artists: List<ArtistUiModel>,
    onArtistClick: (ArtistUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        LibraryEmptyState(
            title = "No Artists",
            message = "No artists found in your library",
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(artists, key = { it.artistKey }) { artist ->
                ArtistRow(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}
