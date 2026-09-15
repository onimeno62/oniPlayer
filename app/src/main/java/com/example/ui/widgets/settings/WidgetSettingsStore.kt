package com.example.ui.widgets.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetPreferences by preferencesDataStore(name = "oni_widget_settings")

/** Persisted, user-facing widget behavior preferences. */
data class WidgetSettings(
    val widgetsEnabled: Boolean = true,
    val nowPlayingEnabled: Boolean = true,
    val miniPlayerEnabled: Boolean = true,
    val lyricsEnabled: Boolean = true,
    val dynamicAlbumEnabled: Boolean = true,
    val liveLyricsUpdates: Boolean = true,
    val playerRefreshSeconds: Int = 2,
    val lyricsRefreshSeconds: Int = 1
)

object WidgetSettingsStore {
    private val WIDGETS_ENABLED = booleanPreferencesKey("widgets_enabled")
    private val NOW_PLAYING_ENABLED = booleanPreferencesKey("now_playing_enabled")
    private val MINI_PLAYER_ENABLED = booleanPreferencesKey("mini_player_enabled")
    private val LYRICS_ENABLED = booleanPreferencesKey("lyrics_enabled")
    private val DYNAMIC_ALBUM_ENABLED = booleanPreferencesKey("dynamic_album_enabled")
    private val LIVE_LYRICS_UPDATES = booleanPreferencesKey("live_lyrics_updates")
    private val PLAYER_REFRESH_SECONDS = intPreferencesKey("player_refresh_seconds")
    private val LYRICS_REFRESH_SECONDS = intPreferencesKey("lyrics_refresh_seconds")

    fun settings(context: Context): Flow<WidgetSettings> = context.widgetPreferences.data.map { prefs ->
        WidgetSettings(
            widgetsEnabled = prefs[WIDGETS_ENABLED] ?: true,
            nowPlayingEnabled = prefs[NOW_PLAYING_ENABLED] ?: true,
            miniPlayerEnabled = prefs[MINI_PLAYER_ENABLED] ?: true,
            lyricsEnabled = prefs[LYRICS_ENABLED] ?: true,
            dynamicAlbumEnabled = prefs[DYNAMIC_ALBUM_ENABLED] ?: true,
            liveLyricsUpdates = prefs[LIVE_LYRICS_UPDATES] ?: true,
            playerRefreshSeconds = prefs[PLAYER_REFRESH_SECONDS] ?: 2,
            lyricsRefreshSeconds = prefs[LYRICS_REFRESH_SECONDS] ?: 1
        )
    }

    suspend fun update(context: Context, transform: (WidgetSettings) -> WidgetSettings) {
        context.widgetPreferences.edit { prefs ->
            val current = WidgetSettings(
                widgetsEnabled = prefs[WIDGETS_ENABLED] ?: true,
                nowPlayingEnabled = prefs[NOW_PLAYING_ENABLED] ?: true,
                miniPlayerEnabled = prefs[MINI_PLAYER_ENABLED] ?: true,
                lyricsEnabled = prefs[LYRICS_ENABLED] ?: true,
                dynamicAlbumEnabled = prefs[DYNAMIC_ALBUM_ENABLED] ?: true,
                liveLyricsUpdates = prefs[LIVE_LYRICS_UPDATES] ?: true,
                playerRefreshSeconds = prefs[PLAYER_REFRESH_SECONDS] ?: 2,
                lyricsRefreshSeconds = prefs[LYRICS_REFRESH_SECONDS] ?: 1
            )
            val next = transform(current)
            prefs[WIDGETS_ENABLED] = next.widgetsEnabled
            prefs[NOW_PLAYING_ENABLED] = next.nowPlayingEnabled
            prefs[MINI_PLAYER_ENABLED] = next.miniPlayerEnabled
            prefs[LYRICS_ENABLED] = next.lyricsEnabled
            prefs[DYNAMIC_ALBUM_ENABLED] = next.dynamicAlbumEnabled
            prefs[LIVE_LYRICS_UPDATES] = next.liveLyricsUpdates
            prefs[PLAYER_REFRESH_SECONDS] = next.playerRefreshSeconds.coerceIn(1, 10)
            prefs[LYRICS_REFRESH_SECONDS] = next.lyricsRefreshSeconds.coerceIn(1, 10)
        }
    }
}