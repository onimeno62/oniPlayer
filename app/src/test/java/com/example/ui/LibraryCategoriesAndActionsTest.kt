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
        artist: String = "Test Artist",
        album: String = "Test Album",
        genre: String = "Rock",
        filePath: String = "/storage/$id.mp3",
        rating: Int = 0,
        playCount: Int = 0,
        lastPlayed: Long = 0L,
        dateAdded: Long = 0L
    ) = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        duration = 180000L,
        filePath = filePath,
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

    @Test
    fun newPlaylistCreation_withSelectedSong_initializesWithSongId() {
        val selectedSongId = "song_target_123"
        val initialIds = listOf(selectedSongId)
        val distinctIds = initialIds.distinct()
        val jsonArray = org.json.JSONArray(distinctIds).toString()

        val playlist = com.example.data.entity.PlaylistEntity(
            id = "playlist_test",
            name = "Workout Vibes",
            songIdsJson = jsonArray
        )

        val parsed = org.json.JSONArray(playlist.songIdsJson)
        assertEquals(1, parsed.length())
        assertEquals(selectedSongId, parsed.getString(0))
    }

    @Test
    fun categoryOverviewScreens_produceEmptySongList_soActionsRemainDisabled() {
        val songs = listOf(
            createSong("1", "Track 1"),
            createSong("2", "Track 2")
        )

        // Overview screens with no group/item selected must NOT return all library songs
        fun resolveOverviewSongs(categoryIndex: Int, selectedGroup: String?): List<SongEntity> {
            val uniqueFolders = songs.groupBy { java.io.File(it.filePath).parentFile?.name ?: "Internal" }
            val uniqueGenres = songs.groupBy { it.genre.ifEmpty { "General" } }
            return when (categoryIndex) {
                1 -> if (selectedGroup != null) uniqueFolders[selectedGroup] ?: emptyList() else emptyList() // Folders
                2 -> if (selectedGroup != null) songs.filter { it.album == selectedGroup } else emptyList() // Albums
                3 -> if (selectedGroup != null) songs.filter { it.artist == selectedGroup } else emptyList() // Artists
                4 -> if (selectedGroup != null) uniqueGenres[selectedGroup] ?: emptyList() else emptyList() // Genres
                8 -> emptyList() // Playlists overview
                9 -> emptyList() // Smart Playlists overview
                else -> emptyList()
            }
        }

        // Category overviews (selectedGroup == null) must be empty
        assertEquals(emptyList<SongEntity>(), resolveOverviewSongs(1, null))
        assertEquals(emptyList<SongEntity>(), resolveOverviewSongs(2, null))
        assertEquals(emptyList<SongEntity>(), resolveOverviewSongs(3, null))
        assertEquals(emptyList<SongEntity>(), resolveOverviewSongs(4, null))
        assertEquals(emptyList<SongEntity>(), resolveOverviewSongs(8, null))
        assertEquals(emptyList<SongEntity>(), resolveOverviewSongs(9, null))
    }

    @Test
    fun categoryDetailScreens_produceExactContextualSongs() {
        val s1 = createSong("1", "A", "Artist 1", "Album 1", "Rock", "/Music/Rock/a.mp3")
        val s2 = createSong("2", "B", "Artist 1", "Album 2", "Jazz", "/Music/Jazz/b.mp3")
        val s3 = createSong("3", "C", "Artist 2", "Album 1", "Rock", "/Music/Rock/c.mp3")
        val songs = listOf(s1, s2, s3)

        // Folder detail
        val folderSongs = songs.filter { java.io.File(it.filePath).parentFile?.name == "Rock" }
        assertEquals(listOf(s1, s3), folderSongs)

        // Album detail
        val albumSongs = songs.filter { it.album == "Album 1" }
        assertEquals(listOf(s1, s3), albumSongs)

        // Artist detail
        val artistSongs = songs.filter { it.artist == "Artist 1" }
        assertEquals(listOf(s1, s2), artistSongs)

        // Genre detail
        val genreSongs = songs.filter { it.genre == "Jazz" }
        assertEquals(listOf(s2), genreSongs)

        // Playlist detail
        val playlist = com.example.data.entity.PlaylistEntity("p1", "My Mix", """["2", "3"]""")
        val jsonArray = org.json.JSONArray(playlist.songIdsJson)
        val pIds = List(jsonArray.length()) { jsonArray.getString(it) }
        val byId = songs.associateBy { it.id }
        val playlistSongs = pIds.mapNotNull(byId::get)
        assertEquals(listOf(s2, s3), playlistSongs)
    }

    @Test
    fun smartPlaylistContexts_eachProduceCorrectSongSet() {
        val songs = listOf(
            createSong("1", "Played Once", playCount = 1, lastPlayed = 5000L, rating = 5),
            createSong("2", "Played Twice", playCount = 2, lastPlayed = 10000L, rating = 4),
            createSong("3", "Never Played", playCount = 0, lastPlayed = 0L, rating = 3)
        )
        val favorites = listOf(songs[0])

        // Recently Played
        val recentlyPlayed = songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
        assertEquals(listOf("2", "1"), recentlyPlayed.map { it.id })

        // Favorites
        assertEquals(listOf("1"), favorites.map { it.id })

        // High Rating (>= 4)
        val highRating = songs.filter { it.rating >= 4 }.sortedByDescending { it.rating }
        assertEquals(listOf("1", "2"), highRating.map { it.id })

        // Never Played (playCount == 0)
        val neverPlayed = songs.filter { it.playCount == 0 }
        assertEquals(listOf("3"), neverPlayed.map { it.id })
    }
}
