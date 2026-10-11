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
    fun oldListeningHistoryIsRecognizedAsStaleAgainstCurrentTime() {
        val now = 1_800_000_000_000L
        val oldSong = song(
            id = "old",
            title = "Old Favorite",
            artist = "Artist A",
            genre = "Rock",
            playCount = 1,
            lastPlayedTimestamp = now - 45L * 24 * 60 * 60 * 1_000
        )

        val result = RecommendationEngine { now }.recommend(
            RecommendationSnapshot(listOf(oldSong)),
            limit = 1
        ).single()

        assertEquals("old", result.localSong?.id)
        // A 45-day-old play should receive the stale-history freshness bonus.
        // With the previous library-relative clock, it was treated as just played.
        assertTrue(result.score > 0.5)
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

    @Test
    fun recommendRemoteReturnsOnlyRemoteItemsAndDiversifiesArtists() {
        val candidates = (1..6).map { i ->
            remote("c$i", "Remote Track $i", "Popular Artist")
        } + listOf(
            remote("c7", "Another Track", "Second Artist"),
            remote("c8", "Third Track", "Third Artist")
        )

        val result = engine.recommendRemote(
            RecommendationSnapshot(
                localSongs = emptyList(),
                externalCandidates = candidates
            ),
            limit = 10
        )

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.remoteItem != null && it.localSong == null })
        val popularArtistCount = result.count { it.remoteItem?.artistName == "Popular Artist" }
        assertTrue("Expected artist count <= 2 but was $popularArtistCount", popularArtistCount <= 2)
    }

    @Test
    fun recommendRemoteExcludesLocalSongsMatchingTitleAndArtist() {
        val local = listOf(song("1", "Exact Match", "Daft Punk", "Electronic"))
        val duplicateCandidate = remote("rem1", "Exact Match", "Daft Punk")
        val uniqueCandidate = remote("rem2", "Around the World", "Daft Punk")

        val result = engine.recommendRemote(
            RecommendationSnapshot(
                localSongs = local,
                externalCandidates = listOf(duplicateCandidate, uniqueCandidate)
            ),
            limit = 5
        )

        val itemIds = result.mapNotNull { it.remoteItem?.identity?.itemId }
        assertTrue(!itemIds.contains("rem1"))
        assertTrue(itemIds.contains("rem2"))
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        genre: String,
        playCount: Int = 0,
        favorite: Boolean = false,
        lastPlayedTimestamp: Long = 0L
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
        lastPlayedTimestamp = lastPlayedTimestamp
    )

    private fun remote(id: String, title: String, artist: String) = RemoteMusicItem(
        identity = ProviderIdentity("test", id, RemoteMusicType.Track),
        title = title,
        artistName = artist
    )
}
