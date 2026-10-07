package com.example.data.online

/**
 * Provider-neutral contract for online music sources.
 *
 * Implementations must not mutate the local library or control playback directly.
 * All provider-specific behavior stays behind this boundary.
 */
interface MusicProvider {
    val id: String
    val displayName: String
    val capabilities: ProviderCapabilities

    suspend fun search(query: String, filter: SearchFilter = SearchFilter.All): ProviderResult<List<RemoteMusicItem>>

    suspend fun getArtist(providerArtistId: String): ProviderResult<RemoteArtist>

    suspend fun getArtistReleases(providerArtistId: String): ProviderResult<List<RemoteAlbum>>

    suspend fun getAlbum(providerAlbumId: String): ProviderResult<RemoteAlbum>

    suspend fun getTrack(providerTrackId: String): ProviderResult<RemoteTrack>

    suspend fun getRecommendations(
        seed: RecommendationSeed? = null
    ): ProviderResult<List<RemoteMusicItem>>

    suspend fun getNewReleases(): ProviderResult<List<RemoteAlbum>>

    suspend fun getTrending(): ProviderResult<List<RemoteMusicItem>>
}
