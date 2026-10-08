package com.example.data.radio

import com.example.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRadioEngineTest {
    private fun song(id: String, artist: String, genre: String, plays: Int) = SongEntity(
        id = id, title = id, artist = artist, album = "Album", genre = genre,
        duration = 180_000, filePath = "/music/" + id, albumArtUri = null,
        playCount = plays, lastPlayedTimestamp = plays.toLong()
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
}
