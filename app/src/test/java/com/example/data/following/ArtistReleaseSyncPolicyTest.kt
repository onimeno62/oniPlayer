package com.example.data.following

import com.example.data.online.MusicIdentity
import com.example.data.online.ProviderIdentity
import com.example.data.online.RemoteAlbum
import com.example.data.online.RemoteMusicType
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistReleaseSyncPolicyTest {

    @Test
    fun newReleasesSince_returnsOnlyItemsAboveLastSeenCursor() {
        val releases = listOf(
            album("newer", "2026-10-08"),
            album("middle", "2026-10-05"),
            album("seen", "2026-10-01"),
            album("older", "2025-01-01")
        )

        val result = ArtistReleaseSyncPolicy.newReleasesSince(releases, "seen")

        assertEquals(listOf("newer", "middle"), result.map { it.identity.itemId })
    }

    @Test
    fun newReleasesSince_returnsEmptyWhenCursorIsMissing() {
        val releases = listOf(
            album("newer", "2026-10-08"),
            album("older", "2025-01-01")
        )

        val result = ArtistReleaseSyncPolicy.newReleasesSince(releases, "deleted")

        assertEquals(emptyList<String>(), result.map { it.identity.itemId })
    }

    @Test
    fun order_deduplicatesAndSortsByReleaseDate() {
        val releases = listOf(
            album("b", "2026-01-01"),
            album("a", "2026-10-01"),
            album("a", "2026-10-01")
        )

        val result = ArtistReleaseSyncPolicy.order(releases)

        assertEquals(listOf("a", "b"), result.map { it.identity.itemId })
    }

    private fun album(id: String, date: String): RemoteAlbum =
        RemoteAlbum(
            identity = ProviderIdentity("musicbrainz", id, RemoteMusicType.Album),
            title = id,
            artistName = "Artist",
            releaseDate = date,
            musicIdentity = MusicIdentity(canonicalArtistId = "artist")
        )
}
