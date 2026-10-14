package com.oniplayer.data.following

import com.oniplayer.data.online.MusicIdentity
import com.oniplayer.data.online.ProviderIdentity
import com.oniplayer.data.online.RemoteAlbum
import com.oniplayer.data.online.RemoteMusicType
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

    @Test
    fun newReleasesSince_returnsEmptyWhenReleasesOrCursorEmpty() {
        val emptyResult = ArtistReleaseSyncPolicy.newReleasesSince(emptyList(), "any")
        assertEquals(emptyList<RemoteAlbum>(), emptyResult)

        val nullCursorResult = ArtistReleaseSyncPolicy.newReleasesSince(listOf(album("a", "2026-01-01")), null)
        assertEquals(emptyList<RemoteAlbum>(), nullCursorResult)
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
