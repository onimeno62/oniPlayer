package com.example.ui.library.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * Song list used by the library host (imported explicitly, so it shadows the legacy
 * `com.example.ui.screens.SongsListView` of the same signature).
 *
 * List style  : swipe right = Play Next, swipe left = Add to Queue (both spring back),
 *               draggable fast scroller with a letter bubble when sorted by title or artist.
 * Grid style  : delegates to the legacy grid implementation unchanged.
 */
@Composable
fun SongsListView(
    songs: List<SongEntity>,
    viewModel: MusicPlayerViewModel,
    sortBy: String,
    isSortAscending: Boolean,
    onShowTrackMenu: (SongEntity) -> Unit,
    layoutMode: String = "list"
) {
    if (layoutMode == "grid" || songs.isEmpty()) {
        com.example.ui.screens.SongsListView(
            songs, viewModel, sortBy, isSortAscending, onShowTrackMenu, layoutMode
        )
        return
    }

    val context = LocalContext.current
    val currentSong by viewModel.audioEngine.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsStateWithLifecycle()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.libraryScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.libraryScrollOffset
    )
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.libraryScrollIndex = listState.firstVisibleItemIndex
        viewModel.libraryScrollOffset = listState.firstVisibleItemScrollOffset
    }
    LaunchedEffect(currentSong) {
        val id = currentSong?.id ?: return@LaunchedEffect
        if (!listState.isScrollInProgress) {
            val index = songs.indexOfFirst { it.id == id }
            if (index >= 0) {
                val first = listState.firstVisibleItemIndex
                if (index < first || index > first + 8) listState.scrollToItem((index - 2).coerceAtLeast(0))
            }
        }
    }

    // Letters only make sense when the list is ordered alphabetically
    val letterSource: ((SongEntity) -> String)? = when (sortBy) {
        "title" -> { s -> s.displayTitle }
        "artist" -> { s -> s.displayArtist }
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SwipeableSongRow(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isPlaying = isPlaying,
                    onClick = { viewModel.playSong(song, songs) },
                    onShowMenu = { onShowTrackMenu(song) },
                    onPlayNext = {
                        viewModel.playNext(song)
                        Toast.makeText(context, "Playing next: ${song.displayTitle}", Toast.LENGTH_SHORT).show()
                    },
                    onAddToQueue = {
                        viewModel.addToQueue(song)
                        Toast.makeText(context, "Added to queue: ${song.displayTitle}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        if (letterSource != null) {
            LibraryFastScroller(
                listState = listState,
                itemCount = songs.size,
                letterAt = { i -> songs.getOrNull(i)?.let(letterSource).toBucketLetter() }
            )
        }
    }
}

private fun String?.toBucketLetter(): String {
    val c = this?.trim()?.firstOrNull() ?: return "#"
    return if (c.isLetter()) c.uppercaseChar().toString() else "#"
}

@Composable
private fun SwipeableSongRow(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onShowMenu: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    // Both actions are non-destructive: run the action, then always spring back (return false).
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onPlayNext()
                SwipeToDismissBoxValue.EndToStart -> onAddToQueue()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )
    val swiping = state.dismissDirection != SwipeToDismissBoxValue.Settled

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val toStart = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val toEnd = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            if (toStart || toEnd) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OniSkin.colors.primaryContainer)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (toStart) Arrangement.Start else Arrangement.End
                ) {
                    Icon(
                        imageVector = if (toStart) Icons.Default.SkipNext else Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = OniSkin.colors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (toStart) "Play next" else "Add to queue",
                        style = OniSkin.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = OniSkin.colors.primary
                    )
                }
            }
        }
    ) {
        Box(
            Modifier.then(if (swiping) Modifier.background(OniSkin.colors.surface) else Modifier)
        ) {
            SongRow(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                onClick = onClick,
                onShowMenu = onShowMenu
            )
        }
    }
}
