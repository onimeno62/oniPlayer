package com.example.ui.components.playback

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Primary Play/Pause playback control.
 * Consumes [OniSkin.playbackControls], [OniSkin.colors], and [OniSkin.motion] tokens.
 * Pure presentation: accepts [isPlaying], [enabled], [loading], and emits [onClick].
 */
@Composable
fun OniPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: Dp = OniSkin.playbackControls.primaryControlSize,
    iconSize: Dp = (size.value * 0.5f).dp,
    containerColor: Color = OniSkin.playbackControls.primaryActiveTint,
    contentColor: Color = OniSkin.colors.onPrimary,
    elevation: Dp = OniSkin.playbackControls.primaryElevation
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "play_pause_press_scale"
    )

    val effectiveContainerColor = if (enabled) containerColor else OniSkin.colors.disabled.copy(alpha = 0.5f)
    val effectiveContentColor = if (enabled) contentColor else OniSkin.colors.textSecondary.copy(alpha = 0.6f)
    val description = if (isPlaying) "Pause" else "Play"

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (elevation > 0.dp && enabled) Modifier.shadow(elevation, CircleShape)
                else Modifier
            )
            .clip(CircleShape)
            .background(effectiveContainerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 2.5.dp,
                color = effectiveContentColor
            )
        } else {
            Crossfade(
                targetState = isPlaying,
                animationSpec = tween(durationMillis = OniSkin.motion.quickDurationMs),
                label = "play_pause_crossfade"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = description,
                    tint = effectiveContentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
