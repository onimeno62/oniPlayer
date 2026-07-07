package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun OniPlayerTheme(
    theme: OniTheme = OniTheme.COSMIC_OBSIDIAN,
    content: @Composable () -> Unit
) {
    val themeColors = ThemeProvider.getThemeColors(theme)
    
    val colorScheme = if (themeColors.isDark) {
        darkColorScheme(
            primary = themeColors.primary,
            secondary = themeColors.secondary,
            tertiary = themeColors.accent,
            background = themeColors.background,
            surface = themeColors.surface,
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = themeColors.textPrimary,
            onSurface = themeColors.textPrimary,
            surfaceVariant = themeColors.surface.copy(alpha = 0.8f),
            onSurfaceVariant = themeColors.textSecondary
        )
    } else {
        lightColorScheme(
            primary = themeColors.primary,
            secondary = themeColors.secondary,
            tertiary = themeColors.accent,
            background = themeColors.background,
            surface = themeColors.surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = themeColors.textPrimary,
            onSurface = themeColors.textPrimary,
            surfaceVariant = themeColors.surface.copy(alpha = 0.8f),
            onSurfaceVariant = themeColors.textSecondary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

