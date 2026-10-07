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
                provider.id to runCatching { provider.search(query.trim(), filter) }
                    .getOrElse {
                        ProviderResult.Failure(
                            providerId = provider.id,
                            kind = ProviderFailureKind.Unknown,
                            message = it.message,
                            cause = it
                        )
                    }
            }
            .map { (providerId, result) -> ProviderSearchResult(providerId, result) }
    }

    suspend fun newReleases(): List<ProviderSectionResult<List<RemoteAlbum>>> =
        providersSupporting(ProviderCapability.NEW_RELEASES)
            .map { provider ->
                ProviderSectionResult(
                    providerId = provider.id,
                    result = runCatching { provider.getNewReleases() }
                        .getOrElse {
                            ProviderResult.Failure(
                                providerId = provider.id,
                                kind = ProviderFailureKind.Unknown,
                                message = it.message,
                                cause = it
                            )
                        }
                )
            }

    suspend fun trending(): List<ProviderSectionResult<List<RemoteMusicItem>>> =
        providersSupporting(ProviderCapability.TRENDING)
            .map { provider ->
                ProviderSectionResult(
                    providerId = provider.id,
                    result = runCatching { provider.getTrending() }
                        .getOrElse {
                            ProviderResult.Failure(
                                providerId = provider.id,
                                kind = ProviderFailureKind.Unknown,
                                message = it.message,
                                cause = it
                            )
                        }
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
