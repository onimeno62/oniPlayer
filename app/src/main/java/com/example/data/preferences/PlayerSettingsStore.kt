package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

/**
 * Engine-level player preferences. Lives in its own DataStore file so it never collides
 * with the existing `oni_settings` store (which already has two delegates).
 * Read by the service-owned PlaybackController and by the settings UI.
 */
private val Context.oniPlayerPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "oni_player_prefs")

data class PlayerSettings(
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val skipSilence: Boolean = false,
    val handleAudioFocus: Boolean = true,
    val pauseOnDisconnect: Boolean = true,
    val rewindOnPrevious: Boolean = true,
    val rememberPosition: Boolean = true,
    val loudnessBoostDb: Float = 0f,
    val keepScreenOn: Boolean = false,
    val startTab: Int = 1
)

object PlayerSettingsStore {
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
    val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
    val HANDLE_AUDIO_FOCUS = booleanPreferencesKey("handle_audio_focus")
    val PAUSE_ON_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
    val REWIND_ON_PREVIOUS = booleanPreferencesKey("rewind_on_previous")
    val REMEMBER_POSITION = booleanPreferencesKey("remember_position")
    val LOUDNESS_BOOST_DB = floatPreferencesKey("loudness_boost_db")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val START_TAB = intPreferencesKey("start_tab")

    private val floatKeys = listOf(PLAYBACK_SPEED, PLAYBACK_PITCH, LOUDNESS_BOOST_DB)
    private val booleanKeys = listOf(SKIP_SILENCE, HANDLE_AUDIO_FOCUS, PAUSE_ON_DISCONNECT, REWIND_ON_PREVIOUS, REMEMBER_POSITION, KEEP_SCREEN_ON)
    private val intKeys = listOf(START_TAB)

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun store(context: Context): DataStore<Preferences> = context.applicationContext.oniPlayerPrefsStore

    fun settings(context: Context): Flow<PlayerSettings> = store(context).data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun current(context: Context): PlayerSettings = settings(context).first()

    /** Fire-and-forget write that survives the calling screen leaving composition. */
    fun <T> update(context: Context, key: Preferences.Key<T>, value: T) {
        val app = context.applicationContext
        writeScope.launch { runCatching { store(app).edit { it[key] = value } } }
    }

    fun resetSpeedAndPitch(context: Context) {
        val app = context.applicationContext
        writeScope.launch {
            runCatching {
                store(app).edit {
                    it.remove(PLAYBACK_SPEED)
                    it.remove(PLAYBACK_PITCH)
                }
            }
        }
    }

    suspend fun resetAll(context: Context) {
        store(context).edit { it.clear() }
    }

    suspend fun exportJson(context: Context): JSONObject {
        val prefs = store(context).data.first()
        val out = JSONObject()
        floatKeys.forEach { key -> prefs[key]?.let { out.put(key.name, it.toDouble()) } }
        booleanKeys.forEach { key -> prefs[key]?.let { out.put(key.name, it) } }
        intKeys.forEach { key -> prefs[key]?.let { out.put(key.name, it) } }
        return out
    }

    suspend fun importJson(context: Context, source: JSONObject) {
        store(context).edit { prefs ->
            floatKeys.forEach { key -> if (source.has(key.name)) prefs[key] = source.getDouble(key.name).toFloat() }
            booleanKeys.forEach { key -> if (source.has(key.name)) prefs[key] = source.getBoolean(key.name) }
            intKeys.forEach { key -> if (source.has(key.name)) prefs[key] = source.getInt(key.name) }
        }
    }

    private fun Preferences.toSettings(): PlayerSettings = PlayerSettings(
        playbackSpeed = (this[PLAYBACK_SPEED] ?: 1f).coerceIn(0.25f, 3f),
        playbackPitch = (this[PLAYBACK_PITCH] ?: 1f).coerceIn(0.25f, 3f),
        skipSilence = this[SKIP_SILENCE] ?: false,
        handleAudioFocus = this[HANDLE_AUDIO_FOCUS] ?: true,
        pauseOnDisconnect = this[PAUSE_ON_DISCONNECT] ?: true,
        rewindOnPrevious = this[REWIND_ON_PREVIOUS] ?: true,
        rememberPosition = this[REMEMBER_POSITION] ?: true,
        loudnessBoostDb = (this[LOUDNESS_BOOST_DB] ?: 0f).coerceIn(0f, 15f),
        keepScreenOn = this[KEEP_SCREEN_ON] ?: false,
        startTab = (this[START_TAB] ?: 1).coerceIn(0, 2)
    )
}
