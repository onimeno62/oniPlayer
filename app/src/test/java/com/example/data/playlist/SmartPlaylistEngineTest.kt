package com.example.data.playlist

import com.example.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPlaylistEngineTest {
    private fun song(id: String, rating: Int, plays: Int) = SongEntity(
        id = id, title = id, artist = "Artist", album = "Album", genre = "Rock",
        duration = if (id == "long") 300_000 else 120_000,
        filePath = "/music/" + id, albumArtUri = null,
        rating = rating, playCount = plays
    )

    @Test
    fun highlyRatedFiltersAndSortsByRating() {
        val songs = listOf(song("low", 2, 10), song("high", 5, 1), song("mid", 4, 3))
        val result = SmartPlaylistEngine().build(SmartPlaylistSpec(setOf(SmartPlaylistRule.HighlyRated)), songs)
        assertEquals(listOf("high", "mid"), result.map { it.id })
    }

    @Test
    fun longestSortsByDuration() {
        val songs = listOf(song("short", 0, 0), song("long", 0, 0))
        val result = SmartPlaylistEngine().build(SmartPlaylistSpec(setOf(SmartPlaylistRule.Longest)), songs)
        assertEquals(listOf("long", "short"), result.map { it.id })
    }
}
