package com.example.ui.components.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Reusable Default Skin secondary button.
 * Subordinates to [OniPrimaryButton] using soft surface tokens.
 */
@Composable
fun OniSecondaryButton(
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
        label = "secondary_button_press_scale"
    )

    OutlinedButton(
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
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = OniSkin.surfaces.soft.containerColor,
            contentColor = OniSkin.colors.textPrimary,
            disabledContainerColor = OniSkin.surfaces.soft.containerColor.copy(alpha = 0.5f),
            disabledContentColor = OniSkin.colors.textSecondary.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (enabled && !loading) OniSkin.colors.outline else OniSkin.colors.outline.copy(alpha = 0.4f)
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = OniSkin.spacing.buttonHorizontal, vertical = OniSkin.spacing.buttonVertical)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = OniSkin.colors.primary
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
        } else if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
        }

        Text(
            text = text,
            style = OniSkin.typography.labelLarge,
            color = if (enabled && !loading) OniSkin.colors.textPrimary else OniSkin.colors.textSecondary.copy(alpha = 0.5f)
        )

        if (trailingIcon != null && !loading) {
            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
            trailingIcon()
        }
    }
}
