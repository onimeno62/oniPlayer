package com.example.data.online

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicProviderContractTest {
    @Test
    fun capabilities_are_the_only_source_of_supported_actions() {
        val provider = FakeProvider(
            capabilities = ProviderCapabilities.of(
                ProviderCapability.SEARCH,
                ProviderCapability.STREAM
            )
        )

        assertTrue(provider.capabilities.supports(ProviderCapability.SEARCH))
        assertTrue(provider.capabilities.supports(ProviderCapability.STREAM))
        assertTrue(!provider.capabilities.supports(ProviderCapability.DOWNLOAD))
    }

    @Test
    fun default_providers_are_optional_and_capability_driven() {
        val providers = DefaultMusicProviders.create()

        assertEquals(setOf("audius", "musicbrainz"), providers.map { it.id }.toSet())
        assertTrue(providers.first { it.id == "audius" }.capabilities.supports(ProviderCapability.STREAM))
        assertTrue(!providers.first { it.id == "musicbrainz" }.capabilities.supports(ProviderCapability.STREAM))
    }

    @Test
    fun repository_isolates_provider_failure() = runTest {
        val failing = FakeProvider(
            id = "failing",
            failure = ProviderFailureKind.RateLimited
        )
        val healthy = FakeProvider(
            id = "healthy",
            searchResult = listOf(
                RemoteMusicItem(
                    identity = ProviderIdentity("healthy", "track-1", RemoteMusicType.Track),
                    title = "Example"
                )
            )
        )

        val results = OnlineMusicRepository(listOf(failing, healthy)).search("example")

        assertEquals(2, results.size)
        assertTrue(results.first { it.providerId == "failing" }.result is ProviderResult.Failure)
        assertEquals(
            listOf("Example"),
            (results.first { it.providerId == "healthy" }.result as ProviderResult.Success).value.map { it.title }
        )
    }

    private class FakeProvider(
        override val id: String = "fake",
        override val displayName: String = "Fake",
        override val capabilities: ProviderCapabilities =
            ProviderCapabilities.of(ProviderCapability.SEARCH),
        private val failure: ProviderFailureKind? = null,
        private val searchResult: List<RemoteMusicItem> = emptyList()
    ) : MusicProvider {
        override suspend fun search(
            query: String,
            filter: SearchFilter
        ): ProviderResult<List<RemoteMusicItem>> =
            failure?.let { ProviderResult.Failure(id, it) }
                ?: ProviderResult.Success(searchResult)

        override suspend fun getArtist(providerArtistId: String) =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getAlbum(providerAlbumId: String) =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getTrack(providerTrackId: String) =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getRecommendations(seed: RecommendationSeed?) =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getNewReleases() =
            ProviderResult.Failure<List<RemoteAlbum>>(id, ProviderFailureKind.Unsupported)

        override suspend fun getTrending() =
            ProviderResult.Failure<List<RemoteMusicItem>>(id, ProviderFailureKind.Unsupported)
    }
}
