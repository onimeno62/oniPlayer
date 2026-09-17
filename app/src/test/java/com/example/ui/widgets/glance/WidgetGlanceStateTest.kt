package com.example.ui.widgets.glance

import androidx.datastore.preferences.core.preferencesOf
import com.example.ui.widgets.core.OniWidgetPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetGlanceStateTest {
    @Test
    fun roundTripPreservesPlaybackState() {
        val original = OniWidgetPlaybackState(
            songId = "song-42",
            title = "Track",
            artist = "Artist",
            album = "Album",
            albumArtworkUri = "content://artwork/42",
            isPlaying = true,
            positionMs = 12_345L,
            durationMs = 98_765L,
            isShuffle = true,
            isRepeat = false,
            activeLyric = "Current",
            previousLyric = "Previous",
            nextLyric = "Next",
            hasLyrics = true
        )

        val preferences = preferencesOf().toMutablePreferences()
        WidgetGlanceState.writeTo(preferences, original)

        assertEquals(original, preferences.toPreferences().toPlaybackState())
    }

    @Test
    fun emptyStateUsesSafeDefaults() {
        val state = preferencesOf().toPlaybackState()

        assertTrue(state.isBlank)
        assertFalse(state.isPlaying)
        assertEquals("No track playing", state.title)
        assertEquals("oniPlayer", state.artist)
        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.durationMs)
    }

    @Test
    fun nullableMetadataIsRemovedFromState() {
        val preferences = preferencesOf().toMutablePreferences()
        WidgetGlanceState.writeTo(
            preferences,
            OniWidgetPlaybackState(songId = "song-1", albumArtworkUri = null)
        )

        val state = preferences.toPreferences().toPlaybackState()

        assertEquals("song-1", state.songId)
        assertEquals(null, state.albumArtworkUri)
    }
}
