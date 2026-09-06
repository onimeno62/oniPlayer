package com.example.ui.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.example.ui.components.playback.OniPlaybackProgress
import com.example.ui.theme.OniSkin

/**
 * Playback progress slider and time indicator for the Player screen.
 *
 * Uses the shared [OniPlaybackProgress] component to ensure consistent slider
 * behavior, scrubber interaction, and time formatting throughout oniPlayer.
 */
@Composable
fun PlayerProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    horizontalPadding: Dp = OniSkin.spacing.screenHorizontal
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .testTag("player_progress_bar")
    ) {
        OniPlaybackProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = onSeek,
            enabled = enabled,
            showTimeLabels = true,
            activeTrackColor = OniSkin.colors.primary,
            thumbColor = OniSkin.colors.primary
        )
    }
}
