package com.example.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.screens.dashboardRadiusMedium
import com.example.ui.theme.LocalBackgroundTransparency
import com.example.ui.theme.LocalGlassEffectEnabled

/**
 * Returns [CardColors] implementing the established Oni glassmorphism treatment:
 * very low-alpha container fill (0.05f baseline modulated slightly by [LocalBackgroundTransparency])
 * when glass is enabled via [LocalGlassEffectEnabled], falling back to a solid surface
 * color when glass is disabled.
 */
@Composable
fun glassCardColors(
    defaultSolidColor: Color = MaterialTheme.colorScheme.surface
): CardColors {
    val glassEnabled = LocalGlassEffectEnabled.current
    val transparencyVal = LocalBackgroundTransparency.current
    val scale = (transparencyVal / 50f).coerceIn(0.5f, 1.5f)
    return CardDefaults.cardColors(
        containerColor = if (glassEnabled) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f * scale)
        } else {
            defaultSolidColor
        }
    )
}

/**
 * Returns a thin low-alpha [BorderStroke] (1.dp, onSurface at 0.10f alpha baseline)
 * when glass is enabled, or null when glass is disabled.
 */
@Composable
fun glassCardBorder(): BorderStroke? {
    val glassEnabled = LocalGlassEffectEnabled.current
    val transparencyVal = LocalBackgroundTransparency.current
    val scale = (transparencyVal / 50f).coerceIn(0.5f, 1.5f)
    return if (glassEnabled) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f * scale))
    } else {
        null
    }
}

/**
 * Returns [CardElevation] with 0.dp when glass is enabled (relying on border and
 * translucent fill), or [solidElevation] when glass is disabled.
 */
@Composable
fun glassCardElevation(
    solidElevation: Dp = 2.dp
): CardElevation {
    val glassEnabled = LocalGlassEffectEnabled.current
    return CardDefaults.cardElevation(
        defaultElevation = if (glassEnabled) 0.dp else solidElevation
    )
}

/**
 * A reusable Modifier extension that applies the established Oni glass card treatment
 * to any Composable container (background fill + border when enabled, solid fill when disabled).
 */
@Composable
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(16.dp),
    defaultSolidColor: Color = MaterialTheme.colorScheme.surface
): Modifier {
    val glassEnabled = LocalGlassEffectEnabled.current
    val transparencyVal = LocalBackgroundTransparency.current
    val scale = (transparencyVal / 50f).coerceIn(0.5f, 1.5f)
    val containerColor = if (glassEnabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f * scale)
    } else {
        defaultSolidColor
    }
    val borderStroke = if (glassEnabled) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f * scale))
    } else {
        null
    }
    return this
        .clip(shape)
        .background(containerColor)
        .then(
            if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier
        )
}

@Preview(showBackground = true)
@Composable
private fun GlassCardStylePreview() {
    Box(
        modifier = Modifier
            .size(160.dp, 100.dp)
            .glassCard(RoundedCornerShape(dashboardRadiusMedium())),
        contentAlignment = Alignment.Center
    ) {
        Text("Glass Card")
    }
}
