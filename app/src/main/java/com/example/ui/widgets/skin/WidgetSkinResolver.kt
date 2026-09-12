package com.example.ui.widgets.skin

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ui.theme.DefaultSkin
import com.example.ui.theme.OniSkinTokens
import com.example.ui.theme.OniTheme
import com.example.ui.theme.ThemeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.preferencesDataStore

private val Context.widgetSettingsDataStore by preferencesDataStore(name = "oni_settings")
private val THEME_OPTION_KEY = stringPreferencesKey("theme_option")
private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color_hex")
private val MATERIAL_YOU_KEY = booleanPreferencesKey("material_you_enabled")

/**
 * Resolves the active [OniSkinTokens] in widget rendering environments
 * without requiring the full Activity / Composable tree hierarchy.
 */
object WidgetSkinResolver {
    fun resolveActiveSkin(context: Context): OniSkinTokens {
        return try {
            val prefs = runBlocking {
                context.widgetSettingsDataStore.data.first()
            }
            val themeOption = prefs[THEME_OPTION_KEY] ?: "Dark"
            val accentHex = prefs[ACCENT_COLOR_KEY] ?: "#3B73E3"
            val parsedAccent = try {
                Color(android.graphics.Color.parseColor(accentHex))
            } catch (e: Exception) {
                DefaultSkin.DefaultPrimaryBlue
            }

            val isDark = when (themeOption) {
                "Light" -> false
                "Dark", "AMOLED" -> true
                else -> {
                    val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }

            DefaultSkin.createSkin(
                isDark = isDark,
                accentColor = parsedAccent
            )
        } catch (e: Exception) {
            DefaultSkin.createSkin(isDark = true)
        }
    }
}
