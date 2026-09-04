package com.example.ui.components.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable Default Skin primary button.
 * Consumes [OniSkin.colors.primary], [OniSkin.shapes.button], and [OniSkin.motion] tokens.
 */
@Composable
fun OniPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "primary_button_press_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { role = Role.Button },
        enabled = enabled && !loading,
        shape = OniSkin.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = OniSkin.colors.primary,
            contentColor = OniSkin.colors.onPrimary,
            disabledContainerColor = OniSkin.colors.disabled.copy(alpha = 0.35f),
            disabledContentColor = OniSkin.colors.textSecondary.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = OniSkin.elevation.subtle,
            pressedElevation = OniSkin.elevation.flat,
            disabledElevation = OniSkin.elevation.flat
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = OniSkin.spacing.buttonHorizontal, vertical = OniSkin.spacing.buttonVertical)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = OniSkin.colors.onPrimary
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
        } else if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
        }

        Text(
            text = text,
            style = OniSkin.typography.labelLarge,
            color = if (enabled && !loading) OniSkin.colors.onPrimary else OniSkin.colors.textSecondary.copy(alpha = 0.6f)
        )

        if (trailingIcon != null && !loading) {
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
            trailingIcon()
        }
    }
}
