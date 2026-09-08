package com.example.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Destination model for [OniFloatingNavigation].
 */
data class OniNavigationDestination(
    val id: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String? = null,
    val enabled: Boolean = true
)

/**
 * Reusable Default Skin floating navigation surface.
 * Consumes [OniSkin.navigation], [OniSkin.surfaces], and [OniSkin.shapes] tokens.
 * The caller owns system insets; this component only adds its tokenized margins.
 * Optional surface overrides support existing user appearance preferences.
 */
@Composable
fun OniFloatingNavigation(
    destinations: List<OniNavigationDestination>,
    selectedId: Int,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    surfaceVariant: OniSurfaceVariant = OniSurfaceVariant.Soft,
    shape: Shape = OniSkin.navigation.shape,
    containerColor: Color? = null
) {
    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = OniSkin.navigation.horizontalMargin,
                end = OniSkin.navigation.horizontalMargin,
                bottom = OniSkin.navigation.bottomPadding,
                top = OniSkin.spacing.xxs
            ),
        variant = surfaceVariant,
        shape = shape,
        containerColor = containerColor,
        elevation = OniSkin.navigation.elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 64dp remains the baseline. Large-font/wrapped labels may need more
                // height; fixed height clipped them. Intrinsics keep all tabs equal.
                .heightIn(min = OniSkin.navigation.barHeight)
                .height(IntrinsicSize.Min)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                OniNavigationItem(
                    destination = destination,
                    isSelected = destination.id == selectedId,
                    onClick = { onDestinationSelected(destination.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Reusable individual navigation destination item.
 * Consumes [OniSkin.navigation], [OniSkin.colors], and [OniSkin.motion] tokens.
 */
@Composable
fun OniNavigationItem(
    destination: OniNavigationDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val motion = OniSkin.motion

    val scale by animateFloatAsState(
        targetValue = if (isPressed && destination.enabled) 0.92f else 1.0f,
        animationSpec = tween(
            durationMillis = motion.buttonPressDurationMs,
            easing = motion.standardEasing
        ),
        label = "nav_item_press_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !destination.enabled -> OniSkin.colors.disabled
            isSelected -> OniSkin.navigation.selectedItemColor
            else -> OniSkin.navigation.unselectedItemColor
        },
        animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing),
        label = "nav_item_color"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) OniSkin.navigation.indicatorColor else Color.Transparent,
        animationSpec = tween(motion.componentStateDurationMs, easing = motion.standardEasing),
        label = "nav_indicator_color"
    )

    val itemModifier = if (destination.testTag != null) {
        modifier.testTag(destination.testTag)
    } else {
        modifier
    }

    Column(
        modifier = itemModifier
            .fillMaxHeight()
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = destination.enabled,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = OniSkin.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Only the visual indicator scales; the tab's interactive bounds never shrink.
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(OniSkin.navigation.indicatorShape)
                .background(indicatorColor)
                .padding(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xxs),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = null, // The visible label names the selectable tab.
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

        Text(
            text = destination.label,
            style = OniSkin.typography.labelMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
