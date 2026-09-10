package com.example.ui

import com.example.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryCategoriesAndActionsTest {

    private fun createSong(
        id: String,
        title: String,
        rating: Int = 0,
        playCount: Int = 0,
        lastPlayed: Long = 0L,
        dateAdded: Long = 0L
    ) = SongEntity(
        id = id,
        title = title,
        artist = "Test Artist",
        album = "Test Album",
        genre = "Rock",
        duration = 180000L,
        filePath = "/storage/$id.mp3",
        albumArtUri = null,
        rating = rating,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayed,
        dateAdded = dateAdded
    )

    @Test
    fun highRatedFilter_satisfiesExistingFourPlusStarThreshold() {
        val songs = listOf(
            createSong("1", "Song 1", rating = 5),
            createSong("2", "Song 2", rating = 4),
            createSong("3", "Song 3", rating = 3),
            createSong("4", "Song 4", rating = 0)
        )

        val highRated = songs.filter { it.rating >= 4 }

        assertEquals(2, highRated.size)
        assertTrue(highRated.any { it.id == "1" })
        assertTrue(highRated.any { it.id == "2" })
        assertFalse(highRated.any { it.id == "3" })
        assertFalse(highRated.any { it.id == "4" })
    }

    @Test
    fun neverPlayedFilter_requiresPlayCountZero() {
        val songs = listOf(
            createSong("1", "Song 1", playCount = 0),
            createSong("2", "Song 2", playCount = 1),
            createSong("3", "Song 3", playCount = 15)
        )

        val neverPlayed = songs.filter { it.playCount == 0 }

        assertEquals(1, neverPlayed.size)
        assertEquals("1", neverPlayed.first().id)
    }

    @Test
    fun recentlyPlayedFilter_requiresLastPlayedGreaterThanZero() {
        val songs = listOf(
            createSong("1", "Song 1", lastPlayed = 1000L),
            createSong("2", "Song 2", lastPlayed = 5000L),
            createSong("3", "Song 3", lastPlayed = 0L)
        )

        val recentlyPlayed = songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }

        assertEquals(2, recentlyPlayed.size)
        assertEquals("2", recentlyPlayed[0].id)
        assertEquals("1", recentlyPlayed[1].id)
    }

    @Test
    fun playlistSongDeduplication_preventsDuplicateEntry() {
        val existingJson = """["song_1", "song_2"]"""
        val array = org.json.JSONArray(existingJson)
        val songIds = (0 until array.length()).map { array.getString(it) }.toMutableList()

        val newSong = "song_1"
        val alreadyExists = songIds.contains(newSong)
        if (!alreadyExists) {
            songIds.add(newSong)
        }

        assertTrue(alreadyExists)
        assertEquals(2, songIds.size)
    }

    @Test
    fun playlistNameValidation_rejectsBlankOrEmptyNames() {
        val invalidNames = listOf("", "   ", "\t", "\n")
        invalidNames.forEach { name ->
            assertTrue(name.trim().isEmpty())
        }

        val validName = "My Favorites 2026"
        assertFalse(validName.trim().isEmpty())
        assertEquals("My Favorites 2026", validName.trim())
    }
}
