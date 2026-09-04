package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens for oniPlayer skins.
 * Defines both the dimensional scale and semantic roles specified in Section 11:
 *
 * Scale:
 * - xs: 6dp
 * - small: 10dp
 * - medium: 14dp
 * - large: 18dp
 * - xl: 24dp
 * - xxl: 32dp
 * - full: 999dp / CircleShape
 *
 * Semantic Roles:
 * - button: 14dp (medium)
 * - card: 18dp (large)
 * - listItem: 14dp (medium)
 * - dialog: 24dp (xl)
 * - bottomSheet: 24dp (xl)
 * - artwork: 18dp (large)
 * - chip: 10dp (small)
 * - pill: 999dp (full)
 */
data class OniShapeTokens(
    // Scale
    val xs: CornerBasedShape = RoundedCornerShape(6.dp),
    val small: CornerBasedShape = RoundedCornerShape(10.dp),
    val medium: CornerBasedShape = RoundedCornerShape(14.dp),
    val large: CornerBasedShape = RoundedCornerShape(18.dp),
    val xl: CornerBasedShape = RoundedCornerShape(24.dp),
    val xxl: CornerBasedShape = RoundedCornerShape(32.dp),
    val full: CornerBasedShape = CircleShape,

    // Semantic Roles
    val button: CornerBasedShape = medium,
    val card: CornerBasedShape = large,
    val listItem: CornerBasedShape = medium,
    val dialog: CornerBasedShape = xl,
    val bottomSheet: CornerBasedShape = xl,
    val artwork: CornerBasedShape = large,
    val chip: CornerBasedShape = small,
    val pill: CornerBasedShape = full
) {
    /**
     * Maps these tokens to Material 3 [Shapes]
     * for standard Material components.
     */
    fun toMaterialShapes(): Shapes = Shapes(
        extraSmall = xs,
        small = small,
        medium = medium,
        large = large,
        extraLarge = xl
    )

    companion object {
        fun defaultShapes(): OniShapeTokens = OniShapeTokens()
    }
}
