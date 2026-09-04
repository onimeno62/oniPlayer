package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for oniPlayer skins.
 * Defines semantic roles rather than hardcoded palette entries.
 * Adheres to .ai/skills/oniplayer-default-skin-design-system.md.
 */
data class OniColorTokens(
    // Semantic Roles
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,

    // Supporting Semantic Tokens
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val disabled: Color,

    // Accent Foundations
    val accent: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val isDark: Boolean
) {
    /**
     * Maps these semantic tokens into a Material 3 [ColorScheme]
     * so standard Material 3 components render in complete harmony with the active skin.
     */
    fun toMaterialColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = accentSecondary,
                onSecondary = Color.Black,
                tertiary = accentGlow,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outlineVariant,
                error = error,
                onError = onError
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = accentSecondary,
                onSecondary = Color.White,
                tertiary = accentGlow,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outlineVariant,
                error = error,
                onError = onError
            )
        }
    }
}
