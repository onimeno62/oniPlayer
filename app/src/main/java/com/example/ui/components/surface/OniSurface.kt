package com.example.ui.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin

/**
 * Surface styling variants defined by the oniPlayer Default Skin Design System.
 */
enum class OniSurfaceVariant {
    /** Low-contrast warm neutral foundation with soft border */
    Soft,
    /** Slightly stronger depth with raised elevation for floating elements */
    Elevated,
    /** Subtle translucent frosted appearance for floating overlays */
    Frosted,
    /** Minimal un-elevated flat container */
    Flat,
    /** Outlined surface with prominent border */
    Outlined
}

/**
 * Core surface primitive for oniPlayer Default Skin.
 * Consumes [OniSkin.surfaces], [OniSkin.shapes], and [OniSkin.elevation] tokens.
 */
@Composable
fun OniSurface(
    modifier: Modifier = Modifier,
    variant: OniSurfaceVariant = OniSurfaceVariant.Soft,
    shape: Shape = OniSkin.shapes.card,
    containerColor: Color? = null,
    contentColor: Color? = null,
    border: BorderStroke? = null,
    elevation: Dp? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val style = when (variant) {
        OniSurfaceVariant.Soft -> OniSkin.surfaces.soft
        OniSurfaceVariant.Elevated -> OniSkin.surfaces.elevated
        OniSurfaceVariant.Frosted -> OniSkin.surfaces.frosted
        OniSurfaceVariant.Flat -> OniSkin.surfaces.flat
        OniSurfaceVariant.Outlined -> OniSkin.surfaces.soft.copy(
            containerColor = Color.Transparent,
            borderStroke = BorderStroke(1.dp, OniSkin.colors.outline)
        )
    }

    val resolvedContainer = containerColor ?: style.containerColor
    val resolvedContent = contentColor ?: style.contentColor
    val resolvedBorder = border ?: style.borderStroke
    val resolvedElevation = elevation ?: style.elevation

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = resolvedContainer,
        contentColor = resolvedContent,
        tonalElevation = 0.dp,
        shadowElevation = resolvedElevation,
        border = resolvedBorder
    ) {
        Box {
            content()
        }
    }
}

/**
 * Convenience soft surface adhering to Default Skin soft depth tokens.
 */
@Composable
fun OniSoftSurface(
    modifier: Modifier = Modifier,
    shape: Shape = OniSkin.shapes.card,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OniSurface(
        modifier = modifier,
        variant = OniSurfaceVariant.Soft,
        shape = shape,
        onClick = onClick,
        enabled = enabled,
        content = content
    )
}

/**
 * Convenience elevated surface adhering to Default Skin raised elevation tokens.
 */
@Composable
fun OniElevatedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = OniSkin.shapes.card,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OniSurface(
        modifier = modifier,
        variant = OniSurfaceVariant.Elevated,
        shape = shape,
        onClick = onClick,
        enabled = enabled,
        content = content
    )
}

/**
 * Convenience frosted surface adhering to Default Skin frosted tokens.
 */
@Composable
fun OniFrostedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = OniSkin.shapes.card,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OniSurface(
        modifier = modifier,
        variant = OniSurfaceVariant.Frosted,
        shape = shape,
        onClick = onClick,
        enabled = enabled,
        content = content
    )
}
