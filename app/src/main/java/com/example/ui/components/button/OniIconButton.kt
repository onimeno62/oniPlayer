package com.example.ui.components.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Contextual styles for [OniIconButton].
 */
enum class OniIconButtonStyle {
    /** Subtle neutral button on transparent background */
    Ghost,
    /** Neutral bordered capsule/circle */
    Neutral,
    /** Accent tinted active control */
    Accent,
    /** Selected highlighted state */
    Selected,
    /** Elevated floating circular button */
    Floating
}

/**
 * Reusable icon button consuming Default Skin tokens.
 * Enforces a minimum 48dp accessibility touch target.
 */
@Composable
fun OniIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    style: OniIconButtonStyle = OniIconButtonStyle.Neutral,
    shape: Shape = CircleShape,
    iconSize: Dp = 22.dp,
    touchTargetSize: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "icon_button_scale"
    )

    val effectiveStyle = if (selected) OniIconButtonStyle.Selected else style

    val (containerColor, tintColor, borderStroke, shadowElevation) = when (effectiveStyle) {
        OniIconButtonStyle.Ghost -> {
            val tint = if (enabled) OniSkin.colors.textSecondary else OniSkin.colors.disabled
            Quad(Color.Transparent, tint, null, 0.dp)
        }
        OniIconButtonStyle.Neutral -> {
            val bg = OniSkin.colors.onSurface.copy(alpha = 0.04f)
            val tint = if (enabled) OniSkin.colors.textPrimary else OniSkin.colors.disabled
            val border = BorderStroke(1.dp, OniSkin.colors.outline.copy(alpha = 0.5f))
            Quad(bg, tint, border, 0.dp)
        }
        OniIconButtonStyle.Accent -> {
            val bg = OniSkin.colors.primary.copy(alpha = 0.12f)
            val tint = if (enabled) OniSkin.colors.primary else OniSkin.colors.disabled
            Quad(bg, tint, null, 0.dp)
        }
        OniIconButtonStyle.Selected -> {
            val bg = OniSkin.colors.primaryContainer
            val tint = if (enabled) OniSkin.colors.primary else OniSkin.colors.disabled
            val border = BorderStroke(1.dp, OniSkin.colors.primary.copy(alpha = 0.3f))
            Quad(bg, tint, border, 0.dp)
        }
        OniIconButtonStyle.Floating -> {
            val bg = OniSkin.surfaces.elevated.containerColor
            val tint = if (enabled) OniSkin.colors.textPrimary else OniSkin.colors.disabled
            val border = BorderStroke(1.dp, OniSkin.colors.outline.copy(alpha = 0.4f))
            Quad(bg, tint, border, OniSkin.elevation.subtle)
        }
    }

    Box(
        modifier = modifier
            .size(touchTargetSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape)
                else Modifier
            )
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderStroke != null) Modifier.border(borderStroke, shape)
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = touchTargetSize / 2),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
