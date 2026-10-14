package com.oniplayer.data.playlist

import com.oniplayer.data.entity.SongEntity
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

    @Test
    fun emptyLibraryReturnsEmptyList() {
        val result = SmartPlaylistEngine().build(SmartPlaylistSpec(setOf(SmartPlaylistRule.MostPlayed)), emptyList())
        assertEquals(emptyList<SongEntity>(), result)
    }

    @Test
    fun genreAndArtistFilteringMatchesCorrectly() {
        val songs = listOf(
            SongEntity("1", "S1", "Queen", "Album", "Rock", 100, "/1", null),
            SongEntity("2", "S2", "Queen", "Album", "Pop", 100, "/2", null),
            SongEntity("3", "S3", "ABBA", "Album", "Pop", 100, "/3", null)
        )
        val rockQueen = SmartPlaylistEngine().build(
            SmartPlaylistSpec(rules = emptySet(), artist = "Queen", genre = "Rock"),
            songs
        )
        assertEquals(listOf("1"), rockQueen.map { it.id })
    }

    @Test
    fun minimumRatingAndFavoritesRulesEnforced() {
        val songs = listOf(
            SongEntity("1", "S1", "A", "Alb", "G", 100, "/1", null, rating = 3, isFavorite = true),
            SongEntity("2", "S2", "A", "Alb", "G", 100, "/2", null, rating = 5, isFavorite = false),
            SongEntity("3", "S3", "A", "Alb", "G", 100, "/3", null, rating = 1, isFavorite = false)
        )
        val favoritesMin3 = SmartPlaylistEngine().build(
            SmartPlaylistSpec(rules = setOf(SmartPlaylistRule.Favorites), minimumRating = 3),
            songs
        )
        assertEquals(listOf("1"), favoritesMin3.map { it.id })
    }

    @Test
    fun limitTruncatesResult() {
        val songs = (1..20).map {
            SongEntity(it.toString(), "S$it", "A", "Alb", "G", 100, "/$it", null, playCount = it)
        }
        val result = SmartPlaylistEngine().build(
            SmartPlaylistSpec(rules = setOf(SmartPlaylistRule.MostPlayed), limit = 5),
            songs
        )
        assertEquals(5, result.size)
        assertEquals("20", result.first().id)
    }
}
