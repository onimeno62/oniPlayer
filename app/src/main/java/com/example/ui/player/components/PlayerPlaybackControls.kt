package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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
import com.example.ui.components.playback.OniNextButton
import com.example.ui.components.playback.OniPlayPauseButton
import com.example.ui.components.playback.OniPreviousButton
import com.example.ui.components.playback.OniRepeatButton
import com.example.ui.components.playback.OniShuffleButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

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
    enabled: Boolean = true,
    horizontalPadding: androidx.compose.ui.unit.Dp = OniSkin.spacing.screenHorizontal
) {
    val motion = OniSkin.motion
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = playbackDelayCountdown != null,
            enter = fadeIn(animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing)),
            exit = fadeOut(animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing))
        ) {
            if (playbackDelayCountdown != null) {
                OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.medium, modifier = Modifier.fillMaxWidth().padding(bottom = OniSkin.spacing.xs)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Text("Next track starting in $playbackDelayCountdown s...", style = OniSkin.typography.caption, fontWeight = FontWeight.Bold, color = OniSkin.colors.primary)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OniShuffleButton(isActive = isShuffle, onClick = onToggleShuffle, enabled = enabled, modifier = Modifier.testTag("player_shuffle_button"))
            OniPreviousButton(onClick = onSkipPrevious, enabled = enabled, modifier = Modifier.testTag("player_previous_button"))
            OniPlayPauseButton(isPlaying = isPlaying, loading = isPreparing, onClick = onTogglePlayPause, enabled = enabled, modifier = Modifier.testTag("player_play_pause_button"))
            OniNextButton(onClick = onSkipNext, enabled = enabled, modifier = Modifier.testTag("player_next_button"))
            OniRepeatButton(isRepeat = isRepeat, onClick = onToggleRepeat, enabled = enabled, modifier = Modifier.testTag("player_repeat_button"))
        }
    }
}
