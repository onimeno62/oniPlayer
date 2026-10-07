package com.example.data.online

/**
 * Provider-agnostic entry point used by ViewModels/use cases.
 *
 * This class deliberately exposes providers as capabilities rather than requiring
 * callers to know which provider implements a feature.
 */
class OnlineMusicRepository(
    providers: List<MusicProvider>,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS
) {
    private val providersById = providers.associateBy { it.id }
    private val searchCache = mutableMapOf<SearchCacheKey, CacheEntry<List<RemoteMusicItem>>>()
    private val trendingCache = mutableMapOf<String, CacheEntry<List<RemoteMusicItem>>>()

    fun providers(): List<MusicProvider> = providersById.values.toList()

    fun provider(providerId: String): MusicProvider? = providersById[providerId]

    fun providersSupporting(capability: ProviderCapability): List<MusicProvider> =
        providersById.values.filter { it.capabilities.supports(capability) }

    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.All
    ): List<ProviderSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        return providersSupporting(ProviderCapability.SEARCH)
            .map { provider ->
                val key = SearchCacheKey(provider.id, normalizedQuery.lowercase(), filter)
                val cached = cached(searchCache, key)
                val result = cached ?: provider.search(normalizedQuery, filter).also {
                    put(searchCache, key, it)
                }
                ProviderSearchResult(provider.id, result)
            }
    }

    suspend fun newReleases(): List<ProviderSectionResult<List<RemoteAlbum>>> =
        providersSupporting(ProviderCapability.NEW_RELEASES).map { provider ->
            ProviderSectionResult(provider.id, provider.getNewReleases())
        }

    suspend fun trending(): List<ProviderSectionResult<List<RemoteMusicItem>>> =
        providersSupporting(ProviderCapability.TRENDING).map { provider ->
            val cached = cached(trendingCache, provider.id)
            val result = cached ?: provider.getTrending().also { put(trendingCache, provider.id, it) }
            ProviderSectionResult(provider.id, result)
        }

    @Synchronized
    private fun <K, V> cached(cache: MutableMap<K, CacheEntry<V>>, key: K): V? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.createdAt > cacheTtlMs) {
            cache.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    private fun <K, V> put(cache: MutableMap<K, CacheEntry<V>>, key: K, value: V) {
        cache[key] = CacheEntry(System.currentTimeMillis(), value)
    }

    private data class SearchCacheKey(
        val providerId: String,
        val query: String,
        val filter: SearchFilter
    )

    private data class CacheEntry<V>(val createdAt: Long, val value: V)

    private companion object {
        const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1_000L
    }
}
