package com.example.ui.components.playback

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable previous/next secondary playback button primitive.
 */
@Composable
private fun OniSkipButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = OniSkin.playbackControls.secondaryControlSize,
    iconSize: Dp = 32.dp,
    tint: Color = OniSkin.playbackControls.secondaryTint
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "skip_button_press_scale"
    )

    val effectiveTint = if (enabled) tint else OniSkin.colors.disabled

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
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Previous track playback control.
 * Consumes [OniSkin.playbackControls] tokens.
 */
@Composable
fun OniPreviousButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = OniSkin.playbackControls.secondaryControlSize,
    tint: Color = OniSkin.playbackControls.secondaryTint
) {
    OniSkipButton(
        icon = Icons.Filled.SkipPrevious,
        contentDescription = "Previous track",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        tint = tint
    )
}

/**
 * Next track playback control.
 * Consumes [OniSkin.playbackControls] tokens.
 */
@Composable
fun OniNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = OniSkin.playbackControls.secondaryControlSize,
    tint: Color = OniSkin.playbackControls.secondaryTint
) {
    OniSkipButton(
        icon = Icons.Filled.SkipNext,
        contentDescription = "Next track",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        tint = tint
    )
}
