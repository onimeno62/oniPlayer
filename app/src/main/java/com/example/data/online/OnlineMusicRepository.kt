package com.example.data.online

/**
 * Provider-agnostic entry point used by ViewModels/use cases.
 *
 * This class deliberately exposes providers as capabilities rather than requiring
 * callers to know which provider implements a feature.
 */
class OnlineMusicRepository(
    providers: List<MusicProvider>
) {
    private val providersById = providers.associateBy { it.id }

    fun providers(): List<MusicProvider> = providersById.values.toList()

    fun provider(providerId: String): MusicProvider? = providersById[providerId]

    fun providersSupporting(capability: ProviderCapability): List<MusicProvider> =
        providersById.values.filter { it.capabilities.supports(capability) }

    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.All
    ): List<ProviderSearchResult> {
        if (query.isBlank()) return emptyList()

        return providersSupporting(ProviderCapability.SEARCH)
            .map { provider ->
                ProviderSearchResult(
                    providerId = provider.id,
                    result = safeProviderCall(provider) {
                        provider.search(query.trim(), filter)
                    }
                )
            }
    }

    suspend fun newReleases(): List<ProviderSectionResult<List<RemoteAlbum>>> =
        providersSupporting(ProviderCapability.NEW_RELEASES)
            .map { provider ->
                ProviderSectionResult(
                    providerId = provider.id,
                    result = safeProviderCall(provider) { provider.getNewReleases() }
                )
            }

    suspend fun trending(): List<ProviderSectionResult<List<RemoteMusicItem>>> =
        providersSupporting(ProviderCapability.TRENDING)
            .map { provider ->
                ProviderSectionResult(
                    providerId = provider.id,
                    result = safeProviderCall(provider) { provider.getTrending() }
                )
            }

    private suspend fun <T> safeProviderCall(
        provider: MusicProvider,
        block: suspend () -> ProviderResult<T>
    ): ProviderResult<T> =
        try {
            block()
        } catch (error: Throwable) {
            ProviderResult.Failure(
                providerId = provider.id,
                kind = ProviderFailureKind.Unknown,
                message = error.message,
                cause = error
            )
        }
}

data class ProviderSearchResult(
    val providerId: String,
    val result: ProviderResult<List<RemoteMusicItem>>
)

data class ProviderSectionResult<T>(
    val providerId: String,
    val result: ProviderResult<T>
)
