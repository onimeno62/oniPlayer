package com.example.data.radio

import com.example.data.entity.SongEntity
import com.example.data.online.ProviderIdentity
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRadioEngineTest {
    private fun song(id: String, artist: String, genre: String, plays: Int) = SongEntity(
        id = id, title = id, artist = artist, album = "Album", genre = genre,
        duration = 180_000, filePath = "/music/" + id, albumArtUri = null,
        playCount = plays, lastPlayedTimestamp = plays.toLong()
    )

    private fun remote(id: String, artist: String, genre: String = "Rock") = RemoteMusicItem(
        identity = ProviderIdentity("audius", id, RemoteMusicType.Track),
        title = id,
        artistName = artist,
        metadata = mapOf("genre" to genre)
    )

    @Test
    fun excludesSeedAndReturnsDeterministicLocalQueue() {
        val songs = listOf(
            song("seed", "Artist A", "Rock", 10),
            song("related", "Artist A", "Rock", 3),
            song("other", "Artist B", "Jazz", 1)
        )
        val result = SmartRadioEngine().buildQueue(RadioSeed(songId = "seed"), songs, limit = 2)
        assertEquals(2, result.size)
        assertTrue(result.none { it is RadioItem.Local && it.song.id == "seed" })
    }

    @Test
    fun excludesRemoteSeedWhenRemoteSeedProvided() {
        val remoteSeed = ProviderIdentity("audius", "seed-track", RemoteMusicType.Track)
        val seedCandidate = remote("seed-track", "Artist A")
        val otherCandidate = remote("other-track", "Artist A")

        val result = SmartRadioEngine().buildQueue(
            seed = RadioSeed(providerItem = remoteSeed),
            localSongs = emptyList(),
            remoteCandidates = listOf(seedCandidate, otherCandidate),
            limit = 2
        )
        assertTrue(result.isNotEmpty())
        assertTrue(result.none { it is RadioItem.Remote && it.item.identity == remoteSeed })
    }

    @Test
    fun emptyCandidatesAndLocalSongsReturnsEmptyQueue() {
        val result = SmartRadioEngine().buildQueue(
            seed = RadioSeed(songId = "any"),
            localSongs = emptyList(),
            remoteCandidates = emptyList(),
            limit = 10
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun remoteCandidatesPopulateQueueWhenLocalSongsEmpty() {
        val candidates = listOf(
            remote("track1", "Artist A"),
            remote("track2", "Artist B")
        )
        val result = SmartRadioEngine().buildQueue(
            seed = RadioSeed(genre = "Rock"),
            localSongs = emptyList(),
            remoteCandidates = candidates,
            limit = 2
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it is RadioItem.Remote })
    }
}

