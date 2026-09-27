package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Horizontal alignment of synced lyric lines. */
enum class LyricsAlignment { CENTER, START }

/**
 * Visual preferences for the synced lyrics viewport.
 *
 * @property fontSizeSp size of the highlighted (current) line.
 * @property inactiveScale size of every other line relative to the highlighted one (0.6..1.0).
 * @property highlightColorHex colour of the current line, `null` = theme accent.
 * @property textColorHex colour of the other lines, `null` = theme text colours.
 */
data class LyricsAppearance(
    val fontSizeSp: Float = DEFAULT_FONT_SIZE_SP,
    val inactiveScale: Float = DEFAULT_INACTIVE_SCALE,
    val highlightColorHex: String? = null,
    val textColorHex: String? = null,
    val boldActiveLine: Boolean = true,
    val alignment: LyricsAlignment = LyricsAlignment.CENTER
) {
    fun sanitized(): LyricsAppearance = copy(
        fontSizeSp = fontSizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP),
        inactiveScale = inactiveScale.coerceIn(MIN_INACTIVE_SCALE, 1f),
        highlightColorHex = highlightColorHex?.takeIf { it.isNotBlank() },
        textColorHex = textColorHex?.takeIf { it.isNotBlank() }
    )

    companion object {
        const val DEFAULT_FONT_SIZE_SP = 24f
        const val MIN_FONT_SIZE_SP = 16f
        const val MAX_FONT_SIZE_SP = 36f
        const val DEFAULT_INACTIVE_SCALE = 0.82f
        const val MIN_INACTIVE_SCALE = 0.6f
    }
}

private val Context.lyricsDataStore: DataStore<Preferences> by preferencesDataStore(name = "oni_lyrics_prefs")

/** Persists [LyricsAppearance]. Separate file from `oni_settings` so it never collides with the ViewModel store. */
object LyricsSettingsStore {
    private val FONT_SIZE = floatPreferencesKey("font_size_sp")
    private val INACTIVE_SCALE = floatPreferencesKey("inactive_scale")
    private val HIGHLIGHT_COLOR = stringPreferencesKey("highlight_color_hex")
    private val TEXT_COLOR = stringPreferencesKey("text_color_hex")
    private val BOLD_ACTIVE = booleanPreferencesKey("bold_active_line")
    private val ALIGNMENT = stringPreferencesKey("alignment")

    fun appearance(context: Context): Flow<LyricsAppearance> =
        context.applicationContext.lyricsDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { read(it) }

    suspend fun update(context: Context, transform: (LyricsAppearance) -> LyricsAppearance) {
        context.applicationContext.lyricsDataStore.edit { prefs ->
            val next = transform(read(prefs)).sanitized()
            prefs[FONT_SIZE] = next.fontSizeSp
            prefs[INACTIVE_SCALE] = next.inactiveScale
            prefs[BOLD_ACTIVE] = next.boldActiveLine
            prefs[ALIGNMENT] = next.alignment.name
            if (next.highlightColorHex == null) prefs.remove(HIGHLIGHT_COLOR) else prefs[HIGHLIGHT_COLOR] = next.highlightColorHex
            if (next.textColorHex == null) prefs.remove(TEXT_COLOR) else prefs[TEXT_COLOR] = next.textColorHex
        }
    }

    suspend fun reset(context: Context) {
        context.applicationContext.lyricsDataStore.edit { it.clear() }
    }

    private fun read(prefs: Preferences): LyricsAppearance = LyricsAppearance(
        fontSizeSp = prefs[FONT_SIZE] ?: LyricsAppearance.DEFAULT_FONT_SIZE_SP,
        inactiveScale = prefs[INACTIVE_SCALE] ?: LyricsAppearance.DEFAULT_INACTIVE_SCALE,
        highlightColorHex = prefs[HIGHLIGHT_COLOR],
        textColorHex = prefs[TEXT_COLOR],
        boldActiveLine = prefs[BOLD_ACTIVE] ?: true,
        alignment = prefs[ALIGNMENT]?.let { name -> LyricsAlignment.entries.firstOrNull { it.name == name } }
            ?: LyricsAlignment.CENTER
    ).sanitized()
}
