package com.example.ui.components.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Repeat modes supported by oniPlayer.
 */
enum class OniRepeatMode {
    OFF,
    ALL,
    ONE
}

/**
 * Reusable shuffle toggle control.
 * Consumes [OniSkin.playbackControls] tertiary tokens.
 */
@Composable
fun OniShuffleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = OniSkin.playbackControls.tertiaryControlSize,
    iconSize: Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "shuffle_press_scale"
    )

    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> OniSkin.colors.disabled
            isActive -> OniSkin.playbackControls.tertiaryActiveTint
            else -> OniSkin.playbackControls.tertiaryInactiveTint
        },
        label = "shuffle_tint"
    )

    val description = if (isActive) "Shuffle is on" else "Shuffle is off"

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                selected = isActive
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Reusable repeat toggle control supporting OFF, ALL, and ONE.
 * Consumes [OniSkin.playbackControls] tertiary tokens.
 */
@Composable
fun OniRepeatButton(
    repeatMode: OniRepeatMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = OniSkin.playbackControls.tertiaryControlSize,
    iconSize: Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "repeat_press_scale"
    )

    val isActive = repeatMode != OniRepeatMode.OFF

    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> OniSkin.colors.disabled
            isActive -> OniSkin.playbackControls.tertiaryActiveTint
            else -> OniSkin.playbackControls.tertiaryInactiveTint
        },
        label = "repeat_tint"
    )

    val (icon, description) = when (repeatMode) {
        OniRepeatMode.OFF -> Pair(Icons.Default.Repeat, "Repeat is off")
        OniRepeatMode.ALL -> Pair(Icons.Default.Repeat, "Repeat all tracks")
        OniRepeatMode.ONE -> Pair(Icons.Default.RepeatOne, "Repeat current track")
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                selected = isActive
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Convenience overload for boolean repeat state.
 */
@Composable
fun OniRepeatButton(
    isRepeat: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = OniSkin.playbackControls.tertiaryControlSize
) {
    OniRepeatButton(
        repeatMode = if (isRepeat) OniRepeatMode.ALL else OniRepeatMode.OFF,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size
    )
}
