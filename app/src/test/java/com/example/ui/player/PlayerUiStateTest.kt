package com.example.ui.player

import com.example.data.entity.SongEntity
import com.example.ui.player.model.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUiStateTest {

    @Test
    fun defaultState_hasExpectedDefaults() {
        val state = PlayerUiState()

        assertEquals(null, state.currentSong)
        assertFalse(state.isPlaying)
        assertFalse(state.isPreparing)
        assertEquals(0L, state.position)
        assertEquals(0L, state.duration)
        assertEquals(0f, state.progressFraction, 0.001f)
        assertFalse(state.isShuffle)
        assertFalse(state.isRepeat)
        assertFalse(state.isFavorite)
        assertTrue(state.queue.isEmpty())
        assertFalse(state.floatingLyricsEnabled)
        assertFalse(state.isSleepTimerRunning)
        assertEquals(0, state.sleepTimerMinutesLeft)
        assertFalse(state.hasSynchronizedLyrics)
    }

    @Test
    fun progressFraction_calculatesCorrectly() {
        val state = PlayerUiState(
            position = 30_000L,
            duration = 120_000L
        )
        assertEquals(0.25f, state.progressFraction, 0.001f)

        // Zero duration edge case
        val zeroDurationState = PlayerUiState(position = 5000L, duration = 0L)
        assertEquals(0f, zeroDurationState.progressFraction, 0.001f)

        // Position exceeding duration edge case
        val overflowState = PlayerUiState(position = 200_000L, duration = 100_000L)
        assertEquals(1.0f, overflowState.progressFraction, 0.001f)
    }

    @Test
    fun hasSynchronizedLyrics_detectsSyncedAndPlainLyrics() {
        val songWithSynced = createSampleSong(
            lyrics = "[00:12.34] Hello darkness my old friend\n[00:16.00] I've come to talk with you again"
        )
        val stateWithSynced = PlayerUiState(currentSong = songWithSynced)
        assertTrue(stateWithSynced.hasSynchronizedLyrics)

        val songWithPlain = createSampleSong(
            lyrics = "Just plain text lyrics without timestamps"
        )
        val stateWithPlain = PlayerUiState(currentSong = songWithPlain)
        assertFalse(stateWithPlain.hasSynchronizedLyrics)

        val songWithoutLyrics = createSampleSong(lyrics = null)
        val stateWithoutLyrics = PlayerUiState(currentSong = songWithoutLyrics)
        assertFalse(stateWithoutLyrics.hasSynchronizedLyrics)
    }

    private fun createSampleSong(
        id: String = "1",
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: Long = 180_000L,
        lyrics: String? = null
    ): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            genre = "Rock",
            duration = duration,
            filePath = "/storage/emulated/0/Music/test.mp3",
            albumArtUri = null,
            lyrics = lyrics
        )
    }
}
