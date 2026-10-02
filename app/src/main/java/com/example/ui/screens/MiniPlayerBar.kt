package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.playback.OniMiniPlayer
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * App-level mini-player host. Collects playback state itself so per-tick position updates only
 * recompose this bar, not the whole app container. Renders the existing [OniMiniPlayer]
 * (skin component) and never owns playback state.
 */
@Composable
fun MiniPlayerBar(
    viewModel: MusicPlayerViewModel,
    surfaceVariant: OniSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val song by viewModel.audioEngine.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.audioEngine.position.collectAsStateWithLifecycle()
    val duration by viewModel.audioEngine.duration.collectAsStateWithLifecycle()
    val current = song ?: return

    OniMiniPlayer(
        title = current.displayTitle,
        artist = current.displayArtist,
        artworkUri = current.albumArtUri,
        isPlaying = isPlaying,
        progressFraction = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
        onPlayPauseClick = { viewModel.togglePlayPause() },
        onPlayerClick = { viewModel.selectTab(1) },
        onNextClick = { viewModel.skipNext() },
        surfaceVariant = surfaceVariant,
        modifier = modifier
    )
}
