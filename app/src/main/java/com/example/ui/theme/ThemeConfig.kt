package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class OniTheme {
    COSMIC_OBSIDIAN,
    CYBERPUNK_NEON,
    AMBER_GOLD,
    FOREST_ZEN,
    CLASSIC_DARK,
    AERO_LIGHT,
    HIGH_DENSITY
}

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

object ThemeProvider {
    fun getThemeColors(theme: OniTheme): ThemeColors {
        return when (theme) {
            OniTheme.COSMIC_OBSIDIAN -> ThemeColors(
                background = Color(0xFF07070C),
                surface = Color(0xFF11111E),
                primary = Color(0xFF7C4DFF),
                secondary = Color(0xFF00E5FF),
                accent = Color(0xFFFF007F),
                textPrimary = Color(0xFFF5F5FA),
                textSecondary = Color(0xFF90A4AE),
                isDark = true
            )
            OniTheme.CYBERPUNK_NEON -> ThemeColors(
                background = Color(0xFF0F0E17),
                surface = Color(0xFF1F1E2D),
                primary = Color(0xFFFF007F),
                secondary = Color(0xFF00F0FF),
                accent = Color(0xFFFFFF00),
                textPrimary = Color(0xFFFFFFFE),
                textSecondary = Color(0xFFA7A9BE),
                isDark = true
            )
            OniTheme.AMBER_GOLD -> ThemeColors(
                background = Color(0xFF1C140E),
                surface = Color(0xFF2C2018),
                primary = Color(0xFFFF9100),
                secondary = Color(0xFFFFD600),
                accent = Color(0xFF795548),
                textPrimary = Color(0xFFFDFBF7),
                textSecondary = Color(0xFFD7CCC8),
                isDark = true
            )
            OniTheme.FOREST_ZEN -> ThemeColors(
                background = Color(0xFF0D1410),
                surface = Color(0xFF18241E),
                primary = Color(0xFF2E7D32),
                secondary = Color(0xFF4CAF50),
                accent = Color(0xFF81C784),
                textPrimary = Color(0xFFF1FDF5),
                textSecondary = Color(0xFFA4C2B2),
                isDark = true
            )
            OniTheme.CLASSIC_DARK -> ThemeColors(
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                primary = Color(0xFF2196F3),
                secondary = Color(0xFF03A9F4),
                accent = Color(0xFF00BCD4),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFB0BEC5),
                isDark = true
            )
            OniTheme.AERO_LIGHT -> ThemeColors(
                background = Color(0xFFF4F6F9),
                surface = Color(0xFFFFFFFF),
                primary = Color(0xFF6200EE),
                secondary = Color(0xFF03DAC6),
                accent = Color(0xFF3700B3),
                textPrimary = Color(0xFF1A1A1A),
                textSecondary = Color(0xFF757575),
                isDark = false
            )
            OniTheme.HIGH_DENSITY -> ThemeColors(
                background = Color(0xFFFEF7FF),
                surface = Color(0xFFF3EDF7),
                primary = Color(0xFF6750A4),
                secondary = Color(0xFFEADDFF),
                accent = Color(0xFF21005D),
                textPrimary = Color(0xFF1D1B20),
                textSecondary = Color(0xFF49454F),
                isDark = false
            )
        }
    }
}
