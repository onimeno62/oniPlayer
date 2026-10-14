package com.oniplayer.data.download

import com.oniplayer.data.online.MusicProvider
import com.oniplayer.data.online.ProviderCapabilities
import com.oniplayer.data.online.ProviderIdentity
import com.oniplayer.data.online.RemoteMusicItem
import com.oniplayer.data.online.RemoteMusicType
import com.oniplayer.data.online.RemoteTrack
import com.oniplayer.data.online.SearchFilter
import com.oniplayer.data.online.ProviderResult
import com.oniplayer.data.online.RemoteArtist
import com.oniplayer.data.online.RemoteAlbum
import com.oniplayer.data.online.RecommendationSeed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadManagerTest {
    private class FakeProvider(override val capabilities: ProviderCapabilities) : MusicProvider {
        override val id = "fake"
        override val displayName = "Fake"
        override suspend fun search(query: String, filter: SearchFilter) = ProviderResult.Success(emptyList<RemoteMusicItem>())
        override suspend fun getArtist(providerArtistId: String) = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getArtistReleases(providerArtistId: String) = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getAlbum(providerAlbumId: String) = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getTrack(providerTrackId: String) = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getRecommendations(seed: RecommendationSeed?) = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getNewReleases() = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getTrending() = ProviderResult.Failure(id, com.oniplayer.data.online.ProviderFailureKind.Unsupported)
    }

    @Test
    fun providerWithoutDownloadCapabilityCannotDownload() {
        assertFalse(DownloadManager.canDownload(FakeProvider(ProviderCapabilities.of())))
    }

    @Test
    fun taskRequiresExplicitDownloadUrl() {
        val track = RemoteTrack(
            RemoteMusicItem(ProviderIdentity("fake", "1", RemoteMusicType.Track), "Song"),
            streamUrl = "https://example.com/stream"
        )
        assertNull(DownloadManager.createTask(track, "fake"))
    }

    @Test
    fun taskCreatedOnlyWithDownloadUrl() {
        val track = RemoteTrack(
            RemoteMusicItem(ProviderIdentity("fake", "1", RemoteMusicType.Track), "Song"),
            downloadUrl = "https://example.com/download.mp3"
        )
        val task = DownloadManager.createTask(track, "fake")
        assertNotNull(task)
        org.junit.Assert.assertEquals("fake", task?.providerId)
        org.junit.Assert.assertEquals("1", task?.remoteId)
        org.junit.Assert.assertEquals("Song", task?.title)
        org.junit.Assert.assertEquals("https://example.com/download.mp3", task?.sourceUrl)
    }

    @Test
    fun taskIdIsDeterministicAndDistinct() {
        val id1 = DownloadManager.taskId("audius", "track-123")
        val id2 = DownloadManager.taskId("audius", "track-123")
        val id3 = DownloadManager.taskId("audius", "track-456")
        val idOtherProvider = DownloadManager.taskId("other", "track-123")

        org.junit.Assert.assertEquals(id1, id2)
        org.junit.Assert.assertNotEquals(id1, id3)
        org.junit.Assert.assertNotEquals(id1, idOtherProvider)
    }
}
