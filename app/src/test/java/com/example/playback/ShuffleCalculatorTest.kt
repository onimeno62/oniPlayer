package com.example.playback

import com.example.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleCalculatorTest {

    private fun createSong(
        id: String,
        title: String = "Title $id",
        artist: String = "Artist",
        album: String = "Album",
        playCount: Int = 0,
        isFavorite: Boolean = false
    ): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            genre = "Rock",
            duration = 180000L,
            filePath = "/path/$id.mp3",
            albumArtUri = null,
            playCount = playCount,
            isFavorite = isFavorite
        )
    }

    @Test
    fun emptyQueue_returnsEmptyListAndEmptyIndices() {
        val emptyQueue = emptyList<SongEntity>()
        val shuffled = ShuffleCalculator.shuffleQueue(emptyQueue, 0, ShuffleMode.RANDOM)
        val indices = ShuffleCalculator.calculateShuffleIndices(emptyQueue, null, ShuffleMode.RANDOM)

        assertTrue(shuffled.isEmpty())
        assertEquals(0, indices.size)
    }

    @Test
    fun singleItemQueue_returnsSameItemAndSingleZeroIndex() {
        val song = createSong("1")
        val queue = listOf(song)

        val shuffled = ShuffleCalculator.shuffleQueue(queue, 0, ShuffleMode.RANDOM)
        val indices = ShuffleCalculator.calculateShuffleIndices(queue, "1", ShuffleMode.RANDOM)

        assertEquals(listOf(song), shuffled)
        assertEquals(1, indices.size)
        assertEquals(0, indices[0])
    }

    @Test
    fun randomMode_currentSongRemainsFirstAndContainsAllSongsExactlyOnce() {
        val songs = (1..5).map { createSong(it.toString()) }
        val currentId = "3"
        val currentIndex = 2 // "3" is at index 2

        val shuffled = ShuffleCalculator.shuffleQueue(songs, currentIndex, ShuffleMode.RANDOM, Random(42))
        val indices = ShuffleCalculator.calculateShuffleIndices(songs, currentId, ShuffleMode.RANDOM, Random(42))

        assertEquals("3", shuffled.first().id)
        assertEquals(songs.size, shuffled.size)
        assertEquals(songs.map { it.id }.toSet(), shuffled.map { it.id }.toSet())

        assertEquals(songs.size, indices.size)
        assertEquals(currentIndex, indices.first())
        assertEquals((0 until songs.size).toSet(), indices.toSet())
    }

    @Test
    fun currentSongMissing_fallsBackToFirstSong() {
        val songs = (1..4).map { createSong(it.toString()) }

        // currentId not in queue
        val indices = ShuffleCalculator.calculateShuffleIndices(songs, "non_existent_id", ShuffleMode.RANDOM, Random(42))
        val shuffled = ShuffleCalculator.shuffleQueue(songs, -1, ShuffleMode.RANDOM, Random(42))

        // Index 0 ("1") coerced at least 0
        assertEquals(0, indices.first())
        assertEquals("1", shuffled.first().id)
    }

    @Test
    fun discoverMode_weightsLowerPlayCountsHigher() {
        val songLowPlay = createSong("low", playCount = 0)
        val songHighPlay = createSong("high", playCount = 100)
        val currentSong = createSong("current", playCount = 10)
        val queue = listOf(currentSong, songLowPlay, songHighPlay)

        // Run multiple trials with deterministic seeds to verify statistical weighting
        var lowPickedFirstCount = 0
        val trials = 1000

        for (seed in 0 until trials) {
            val shuffled = ShuffleCalculator.shuffleQueue(queue, 0, ShuffleMode.DISCOVER, Random(seed))
            assertEquals("current", shuffled[0].id)
            if (shuffled[1].id == "low") {
                lowPickedFirstCount++
            }
        }

        // Weight for low = 1.0 / (1 + 0) = 1.0
        // Weight for high = 1.0 / (1 + 100) = ~0.0099
        // Low play song should be selected first ~99% of the time
        assertTrue("Low play song should be selected next majority of times ($lowPickedFirstCount / $trials)", lowPickedFirstCount > 900)
    }

    @Test
    fun favoritesBoostMode_weightsFavoritesHigher() {
        val songFavorite = createSong("fav", isFavorite = true)
        val songNormal = createSong("norm", isFavorite = false)
        val currentSong = createSong("current", isFavorite = false)
        val queue = listOf(currentSong, songFavorite, songNormal)

        var favPickedFirstCount = 0
        val trials = 1000

        for (seed in 0 until trials) {
            val shuffled = ShuffleCalculator.shuffleQueue(queue, 0, ShuffleMode.FAVORITES_BOOST, Random(seed))
            assertEquals("current", shuffled[0].id)
            if (shuffled[1].id == "fav") {
                favPickedFirstCount++
            }
        }

        // Weight for fav = 4.0
        // Weight for normal = 1.0
        // Favorite song should be chosen ~80% of the time (4 / 5)
        assertTrue("Favorite should be picked next ~80% of the time ($favPickedFirstCount / $trials)", favPickedFirstCount in 720..880)
    }
}
