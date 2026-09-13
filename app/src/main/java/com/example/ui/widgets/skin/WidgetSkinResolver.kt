package com.example.ui.widgets.skin

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.DefaultSkin
import com.example.ui.theme.OniSkinTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.widgetSettingsDataStore by preferencesDataStore(name = "oni_settings")
private val THEME_OPTION_KEY = stringPreferencesKey("theme_option")
private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color_hex")
private val MATERIAL_YOU_KEY = booleanPreferencesKey("material_you_enabled")

/**
 * Resolves the active skin for AppWidget rendering.
 *
 * Widgets cannot rely on the Activity composition tree, so the same persisted
 * skin settings are resolved here. The widget presentation then applies a
 * restrained translucent treatment to the semantic surface tokens, giving the
 * Default Skin its glass/neumorphic widget character without introducing a
 * second widget-only color system.
 */
object WidgetSkinResolver {
    fun resolveActiveSkin(context: Context): OniSkinTokens {
        return try {
            val prefs = runBlocking { context.widgetSettingsDataStore.data.first() }
            val themeOption = prefs[THEME_OPTION_KEY] ?: "Dark"
            val accentHex = prefs[ACCENT_COLOR_KEY] ?: "#3B73E3"
            val parsedAccent = try {
                Color(android.graphics.Color.parseColor(accentHex))
            } catch (_: Exception) {
                DefaultSkin.DefaultPrimaryBlue
            }

            val isDark = when (themeOption) {
                "Light" -> false
                "Dark", "AMOLED" -> true
                else -> {
                    val flags = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    flags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }

            val base = DefaultSkin.createSkin(
                isDark = isDark,
                accentColor = parsedAccent
            )

            // Keep all widget styling derived from the active skin. These alpha
            // adjustments are presentation modifiers, not replacement colors.
            val colors = base.colors.copy(
                surface = base.colors.surface.copy(alpha = if (isDark) 0.92f else 0.94f),
                surfaceVariant = base.colors.surfaceVariant.copy(alpha = if (isDark) 0.78f else 0.88f),
                surfaceElevated = base.colors.surfaceElevated.copy(alpha = if (isDark) 0.94f else 0.96f),
                primaryContainer = base.colors.primaryContainer.copy(alpha = if (isDark) 0.78f else 0.86f),
                outline = base.colors.outline.copy(alpha = if (isDark) 0.82f else 0.88f),
                outlineVariant = base.colors.outlineVariant.copy(alpha = if (isDark) 0.72f else 0.80f),
                accentGlow = base.colors.accentGlow.copy(alpha = if (isDark) 0.30f else 0.20f)
            )

            base.copy(colors = colors)
        } catch (_: Exception) {
            DefaultSkin.createSkin(isDark = true)
        }
    }
}
