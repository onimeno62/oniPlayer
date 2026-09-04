package com.example.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 */
@Composable
fun OniFloatingNavigation(
    destinations: List<OniNavigationDestination>,
    selectedId: Int,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    surfaceVariant: OniSurfaceVariant = OniSurfaceVariant.Soft
) {
    OniSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = OniSkin.navigation.horizontalMargin,
                end = OniSkin.navigation.horizontalMargin,
                bottom = OniSkin.navigation.bottomPadding,
                top = 2.dp
            ),
        variant = surfaceVariant,
        shape = OniSkin.navigation.shape,
        elevation = OniSkin.navigation.elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OniSkin.navigation.barHeight),
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed && destination.enabled) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = OniSkin.motion.buttonPressDurationMs),
        label = "nav_item_press_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !destination.enabled -> OniSkin.colors.disabled
            isSelected -> OniSkin.navigation.selectedItemColor
            else -> OniSkin.navigation.unselectedItemColor
        },
        label = "nav_item_color"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) OniSkin.navigation.indicatorColor else Color.Transparent,
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 28.dp),
                enabled = destination.enabled,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill indicator around icon
        Box(
            modifier = Modifier
                .clip(OniSkin.navigation.indicatorShape)
                .background(indicatorColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = destination.label,
            style = OniSkin.typography.caption,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}
