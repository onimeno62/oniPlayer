package com.example.ui.widgets.glance

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ui.widgets.core.OniWidgetPlaybackState

/**
 * Durable, per-GlanceId playback snapshot.
 *
 * Glance Preferences state is scoped to an individual widget instance, so
 * multiple launcher instances cannot overwrite one another.
 */
object WidgetGlanceState {
    private val songIdKey = stringPreferencesKey("song_id")
    private val titleKey = stringPreferencesKey("title")
    private val artistKey = stringPreferencesKey("artist")
    private val albumKey = stringPreferencesKey("album")
    private val artworkUriKey = stringPreferencesKey("artwork_uri")
    private val isPlayingKey = booleanPreferencesKey("is_playing")
    private val positionMsKey = longPreferencesKey("position_ms")
    private val durationMsKey = longPreferencesKey("duration_ms")
    private val shuffleKey = booleanPreferencesKey("shuffle")
    private val repeatKey = booleanPreferencesKey("repeat")
    private val activeLyricKey = stringPreferencesKey("active_lyric")
    private val previousLyricKey = stringPreferencesKey("previous_lyric")
    private val nextLyricKey = stringPreferencesKey("next_lyric")
    private val hasLyricsKey = booleanPreferencesKey("has_lyrics")

    fun Preferences.toPlaybackState(): OniWidgetPlaybackState =
        OniWidgetPlaybackState(
            songId = this[songIdKey],
            title = this[titleKey] ?: "No track playing",
            artist = this[artistKey] ?: "oniPlayer",
            album = this[albumKey] ?: "",
            albumArtworkUri = this[artworkUriKey],
            isPlaying = this[isPlayingKey] ?: false,
            positionMs = (this[positionMsKey] ?: 0L).coerceAtLeast(0L),
            durationMs = (this[durationMsKey] ?: 0L).coerceAtLeast(0L),
            isShuffle = this[shuffleKey] ?: false,
            isRepeat = this[repeatKey] ?: false,
            activeLyric = this[activeLyricKey],
            previousLyric = this[previousLyricKey],
            nextLyric = this[nextLyricKey],
            hasLyrics = this[hasLyricsKey] ?: false
        )

    fun writeTo(preferences: MutablePreferences, state: OniWidgetPlaybackState) {
        writeNullable(preferences, songIdKey, state.songId)
        writeNullable(preferences, titleKey, state.title)
        writeNullable(preferences, artistKey, state.artist)
        writeNullable(preferences, albumKey, state.album)
        writeNullable(preferences, artworkUriKey, state.albumArtworkUri)
        preferences[isPlayingKey] = state.isPlaying
        preferences[positionMsKey] = state.positionMs
        preferences[durationMsKey] = state.durationMs
        preferences[shuffleKey] = state.isShuffle
        preferences[repeatKey] = state.isRepeat
        writeNullable(preferences, activeLyricKey, state.activeLyric)
        writeNullable(preferences, previousLyricKey, state.previousLyric)
        writeNullable(preferences, nextLyricKey, state.nextLyric)
        preferences[hasLyricsKey] = state.hasLyrics
    }

    private fun writeNullable(
        preferences: MutablePreferences,
        key: Preferences.Key<String>,
        value: String?
    ) {
        if (value == null) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }
}

fun Preferences.toPlaybackState(): OniWidgetPlaybackState =
    WidgetGlanceState.run { this@toPlaybackState.toPlaybackState() }
