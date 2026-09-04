package com.example.ui.components.music

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable favorite action presentation component.
 * Consumes [OniSkin.colors] and [OniSkin.motion] tokens.
 * Pure presentation: does not modify repository state.
 */
@Composable
fun OniFavoriteAction(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = OniSkin.colors.accent,
    inactiveColor: Color = OniSkin.colors.textSecondary,
    touchTargetSize: Dp = 48.dp,
    iconSize: Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.85f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "favorite_press_scale"
    )

    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> OniSkin.colors.disabled
            isFavorite -> activeColor
            else -> inactiveColor
        },
        label = "favorite_tint"
    )

    val description = if (isFavorite) "Remove from favorites" else "Add to favorites"

    Box(
        modifier = modifier
            .size(touchTargetSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = touchTargetSize / 2),
                enabled = enabled,
                role = Role.Button,
                onClick = onToggle
            )
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Reusable overflow / more-options action presentation component.
 * Consumes [OniSkin.colors] tokens.
 */
@Composable
fun OniOverflowAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "More options",
    enabled: Boolean = true,
    tint: Color = OniSkin.colors.textSecondary,
    touchTargetSize: Dp = 48.dp,
    iconSize: Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "overflow_press_scale"
    )

    Box(
        modifier = modifier
            .size(touchTargetSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = touchTargetSize / 2),
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
            imageVector = Icons.Filled.MoreVert,
            contentDescription = contentDescription,
            tint = if (enabled) tint else OniSkin.colors.disabled,
            modifier = Modifier.size(iconSize)
        )
    }
}
