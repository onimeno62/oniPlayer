package com.example.data.recommendation

import com.example.data.entity.SongEntity
import com.example.data.online.ProviderIdentity
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val engine = RecommendationEngine()

    @Test
    fun coldStartReturnsDeterministicLocalCandidates() {
        val songs = listOf(
            song("1", "Alpha", "Artist A", "Rock"),
            song("2", "Beta", "Artist B", "Pop"),
            song("3", "Gamma", "Artist C", "Jazz")
        )

        val first = engine.recommend(RecommendationSnapshot(songs), limit = 3)
        val second = engine.recommend(RecommendationSnapshot(songs), limit = 3)

        assertEquals(first.map { it.localSong?.id }, second.map { it.localSong?.id })
        assertTrue(first.all { it.localSong != null })
    }

    @Test
    fun favoriteAndListeningHistoryInfluenceRanking() {
        val favorite = song("fav", "Favorite", "Artist A", "Rock", favorite = true)
        val history = song("history", "History", "Artist A", "Rock", playCount = 10)
        val unrelated = song("other", "Other", "Artist B", "Jazz")

        val result = engine.recommend(
            RecommendationSnapshot(listOf(favorite, history, unrelated)),
            limit = 3
        )

        val ids = result.mapNotNull { it.localSong?.id }
        assertTrue(ids.indexOf("fav") < ids.indexOf("other"))
        assertTrue(result.any { it.localSong?.id == "fav" && it.reason == RecommendationReason.FavoriteAffinity })
        assertTrue(result.any { it.localSong?.id == "history" && it.reason == RecommendationReason.BecauseYouPlayed })
    }

    @Test
    fun diversityLimitsSingleArtistSaturation() {
        val songs = (1..6).map { index ->
            song(index.toString(), "Track $index", "Same Artist", "Rock", playCount = 5)
        } + song("other", "Other", "Different Artist", "Rock", playCount = 1)

        val result = engine.recommend(RecommendationSnapshot(songs), limit = 7)
        assertTrue(result.count { it.localSong?.displayArtist == "Same Artist" } <= 3)
    }

    @Test
    fun remoteCandidatesExcludeRecentlyPlayedAndDeduplicate() {
        val first = remote("1", "One", "Artist A")
        val duplicate = remote("1", "One duplicate", "Artist A")
        val recent = remote("2", "Recent", "Artist B")

        val result = engine.recommend(
            RecommendationSnapshot(
                localSongs = emptyList(),
                externalCandidates = listOf(first, duplicate, recent),
                recentExternalIds = setOf(recent.identity)
            ),
            limit = 10
        )

        assertEquals(listOf("1"), result.mapNotNull { it.remoteItem?.identity?.itemId })
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        genre: String,
        playCount: Int = 0,
        favorite: Boolean = false
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
        isFavorite = favorite
    )

    private fun remote(id: String, title: String, artist: String) = RemoteMusicItem(
        identity = ProviderIdentity("test", id, RemoteMusicType.Track),
        title = title,
        artistName = artist
    )
}
