package com.example.data.recommendation

import com.example.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val engine = RecommendationEngine()

    @Test
    fun becauseYouPlayed_prefersUnplayedSongsByKnownArtist() {
        val songs = listOf(
            song("1", "Played", "Artist A", playCount = 10),
            song("2", "Same Artist", "Artist A"),
            song("3", "Other", "Artist B")
        )

        val result = engine.becauseYouPlayed(songs)

        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun similarToFavorites_matchesPreferredGenreWithoutRepeatingArtist() {
        val songs = listOf(
            song("1", "Favorite", "Artist A", genre = "Rock", favorite = true, rating = 5),
            song("2", "Similar", "Artist B", genre = "Rock"),
            song("3", "Same Artist", "Artist A", genre = "Rock"),
            song("4", "Different", "Artist C", genre = "Jazz")
        )

        val result = engine.similarToFavorites(songs)

        assertTrue(result.any { it.id == "2" })
        assertTrue(result.none { it.id == "3" })
        assertTrue(result.none { it.id == "4" })
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        genre: String = "Rock",
        playCount: Int = 0,
        favorite: Boolean = false,
        rating: Int = 0
    ) = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        genre = genre,
        duration = 180_000,
        filePath = "/music/$id.mp3",
        albumArtUri = null,
        playCount = playCount,
        isFavorite = favorite,
        rating = rating
    )
}
