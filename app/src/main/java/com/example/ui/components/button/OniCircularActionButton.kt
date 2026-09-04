package com.example.ui.components.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable circular action component suitable for primary playback actions,
 * floating action buttons, and important focal controls.
 * Consumes [OniSkin.playbackControls], [OniSkin.colors], and [OniSkin.motion] tokens.
 */
@Composable
fun OniCircularActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = OniSkin.playbackControls.primaryControlSize,
    iconSize: Dp = (size.value * 0.45f).dp,
    containerColor: Color = OniSkin.colors.primary,
    contentColor: Color = OniSkin.colors.onPrimary,
    elevation: Dp = OniSkin.playbackControls.primaryElevation,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "circular_action_press_scale"
    )

    val effectiveContainerColor = if (enabled) containerColor else OniSkin.colors.disabled.copy(alpha = 0.5f)
    val effectiveContentColor = if (enabled) contentColor else OniSkin.colors.textSecondary.copy(alpha = 0.5f)

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
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 2.5.dp,
                color = effectiveContentColor
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = effectiveContentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
