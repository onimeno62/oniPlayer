package com.example.data.download

import com.example.data.online.MusicProvider
import com.example.data.online.ProviderCapabilities
import com.example.data.online.ProviderIdentity
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import com.example.data.online.RemoteTrack
import com.example.data.online.SearchFilter
import com.example.data.online.ProviderResult
import com.example.data.online.RemoteArtist
import com.example.data.online.RemoteAlbum
import com.example.data.online.RecommendationSeed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadManagerTest {
    private class FakeProvider(override val capabilities: ProviderCapabilities) : MusicProvider {
        override val id = "fake"
        override val displayName = "Fake"
        override suspend fun search(query: String, filter: SearchFilter) = ProviderResult.Success(emptyList<RemoteMusicItem>())
        override suspend fun getArtist(providerArtistId: String) = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getArtistReleases(providerArtistId: String) = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getAlbum(providerAlbumId: String) = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getTrack(providerTrackId: String) = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getRecommendations(seed: RecommendationSeed?) = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getNewReleases() = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
        override suspend fun getTrending() = ProviderResult.Failure(id, com.example.data.online.ProviderFailureKind.Unsupported)
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
        assertNotNull(DownloadManager.createTask(track, "fake"))
    }
}
