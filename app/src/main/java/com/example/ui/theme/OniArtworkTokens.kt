package com.example.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Artwork presentation tokens for oniPlayer skins.
 * Defines corner radius, shapes, shadow, glow, blur, halo, and reflection
 * as specified in Section 16.
 */
data class OniArtworkTokens(
    val cornerRadius: Dp = 18.dp,
    val shape: CornerBasedShape = RoundedCornerShape(18.dp),
    val shadowElevation: Dp = 4.dp,
    val glowAlpha: Float = 0.20f,
    val blurStrength: Float = 20f,
    val haloRadiusFactor: Float = 0.9f,
    val reflectionEnabled: Boolean = false
) {
    companion object {
        fun defaultArtwork(shapes: OniShapeTokens): OniArtworkTokens = OniArtworkTokens(
            cornerRadius = 18.dp,
            shape = shapes.artwork
        )
    }
}
