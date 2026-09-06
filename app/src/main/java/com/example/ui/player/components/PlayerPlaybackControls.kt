package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.playback.OniNextButton
import com.example.ui.components.playback.OniPlayPauseButton
import com.example.ui.components.playback.OniPreviousButton
import com.example.ui.components.playback.OniRepeatButton
import com.example.ui.components.playback.OniShuffleButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Main playback controls for the Player screen in Default Skin.
 *
 * Consolidates Shuffle, Previous, Play/Pause, Next, and Repeat buttons using
 * shared [Oni*] playback components.
 *
 * Replaces the legacy Aurora Glass player controls.
 */
@Composable
fun PlayerPlaybackControls(
    isPlaying: Boolean,
    isPreparing: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    playbackDelayCountdown: Int? = null,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playback delay countdown banner (e.g., crossfade/gap countdown)
        AnimatedVisibility(
            visible = playbackDelayCountdown != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (playbackDelayCountdown != null) {
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = OniSkin.spacing.xs)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = OniSkin.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next track starting in $playbackDelayCountdown s...",
                            style = OniSkin.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniShuffleButton(
                isActive = isShuffle,
                onClick = onToggleShuffle,
                enabled = enabled,
                modifier = Modifier.testTag("player_shuffle_button")
            )

            OniPreviousButton(
                onClick = onSkipPrevious,
                enabled = enabled,
                modifier = Modifier.testTag("player_previous_button")
            )

            OniPlayPauseButton(
                isPlaying = isPlaying,
                loading = isPreparing,
                onClick = onTogglePlayPause,
                enabled = enabled,
                modifier = Modifier.testTag("player_play_pause_button")
            )

            OniNextButton(
                onClick = onSkipNext,
                enabled = enabled,
                modifier = Modifier.testTag("player_next_button")
            )

            OniRepeatButton(
                isRepeat = isRepeat,
                onClick = onToggleRepeat,
                enabled = enabled,
                modifier = Modifier.testTag("player_repeat_button")
            )
        }
    }
}
