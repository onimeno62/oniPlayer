package com.example.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Navigation visual tokens for oniPlayer skins.
 * Defines dimensions, shapes, and colors for floating navigation bars,
 * active indicator pills, and destination states specified in Section 18.
 */
data class OniNavigationTokens(
    val barHeight: Dp = 64.dp,
    val shape: CornerBasedShape = RoundedCornerShape(24.dp),
    val elevation: Dp = 0.dp,
    val marginHorizontal: Dp = 24.dp,
    val marginBottom: Dp = 12.dp,
    val indicatorShape: CornerBasedShape = RoundedCornerShape(14.dp),
    val selectedItemColor: Color,
    val unselectedItemColor: Color,
    val indicatorColor: Color
) {
    companion object {
        fun defaultNavigation(colors: OniColorTokens, shapes: OniShapeTokens): OniNavigationTokens = OniNavigationTokens(
            barHeight = 64.dp,
            shape = shapes.xl,
            elevation = 0.dp,
            marginHorizontal = 24.dp,
            marginBottom = 12.dp,
            indicatorShape = shapes.medium,
            selectedItemColor = colors.primary,
            unselectedItemColor = colors.onSurfaceVariant,
            indicatorColor = colors.primary.copy(alpha = 0.14f)
        )
    }
}
