package com.example.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackPersistenceTest {

    private lateinit var context: Context
    private lateinit var persistence: PlaybackPersistence

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        persistence = PlaybackPersistence(context)
        persistence.clear()
    }

    @Test
    fun saveAndLoadState_restoresAllFieldsCorrectly() {
        val state = PersistedPlaybackState(
            currentSongId = "song-123",
            positionMs = 45000L,
            isPlaying = true,
            shuffleEnabled = true,
            shuffleMode = ShuffleMode.FAVORITES_BOOST,
            repeatMode = RepeatMode.ONE,
            queueIds = listOf("song-123", "song-456", "song-789")
        )

        persistence.save(state)
        val loaded = persistence.load()

        assertNotNull(loaded)
        assertEquals("song-123", loaded?.currentSongId)
        assertEquals(45000L, loaded?.positionMs)
        assertTrue(loaded?.isPlaying == true)
        assertTrue(loaded?.shuffleEnabled == true)
        assertEquals(ShuffleMode.FAVORITES_BOOST, loaded?.shuffleMode)
        assertEquals(RepeatMode.ONE, loaded?.repeatMode)
        assertEquals(listOf("song-123", "song-456", "song-789"), loaded?.queueIds)
    }

    @Test
    fun savePosition_updatesOnlyPosition() {
        val state = PersistedPlaybackState(
            currentSongId = "song-1",
            positionMs = 1000L,
            isPlaying = true,
            shuffleEnabled = false,
            shuffleMode = ShuffleMode.RANDOM,
            repeatMode = RepeatMode.ALL,
            queueIds = listOf("song-1")
        )

        persistence.save(state)
        persistence.savePosition(50000L)

        val loaded = persistence.load()
        assertNotNull(loaded)
        assertEquals(50000L, loaded?.positionMs)
        assertEquals("song-1", loaded?.currentSongId)
    }

    @Test
    fun load_returnsNullWhenEmpty() {
        persistence.clear()
        assertNull(persistence.load())
    }

    @Test
    fun deserializeQueueIds_handlesCorruptJsonGracefully() {
        val malformedJson = "{not-an-array}"
        val result = persistence.deserializeQueueIds(malformedJson)
        assertTrue(result.isEmpty())
    }

    @Test
    fun deserializeQueueIds_handlesEmptyAndNull() {
        assertTrue(persistence.deserializeQueueIds(null).isEmpty())
        assertTrue(persistence.deserializeQueueIds("").isEmpty())
    }

    @Test
    fun save_coercesNegativePositionToZero() {
        val state = PersistedPlaybackState(
            currentSongId = "song-1",
            positionMs = -500L,
            isPlaying = false,
            shuffleEnabled = false,
            shuffleMode = ShuffleMode.RANDOM,
            repeatMode = RepeatMode.ALL,
            queueIds = listOf("song-1")
        )

        persistence.save(state)
        val loaded = persistence.load()

        assertNotNull(loaded)
        assertEquals(0L, loaded?.positionMs)
    }
}
