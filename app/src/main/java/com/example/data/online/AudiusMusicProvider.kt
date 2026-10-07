package com.example.data.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AudiusMusicProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : MusicProvider {
    override val id = "audius"
    override val displayName = "Audius"

    override val capabilities = ProviderCapabilities.of(
        ProviderCapability.SEARCH,
        ProviderCapability.ARTIST,
        ProviderCapability.TRACK,
        ProviderCapability.STREAM,
        ProviderCapability.TRENDING,
        ProviderCapability.ARTWORK
    )

    override suspend fun search(
        query: String,
        filter: SearchFilter
    ): ProviderResult<List<RemoteMusicItem>> = withContext(Dispatchers.IO) {
        when (filter) {
            SearchFilter.Artists -> searchArtists(query)
            SearchFilter.Albums, SearchFilter.Playlists -> ProviderResult.Success(emptyList())
            SearchFilter.All, SearchFilter.Tracks -> requestList("tracks/search", query) { parseTrack(it) }
        }
    }

    override suspend fun getArtist(providerArtistId: String): ProviderResult<RemoteArtist> =
        withContext(Dispatchers.IO) { requestObject("users/$providerArtistId") { parseArtist(it) } }

    override suspend fun getAlbum(providerAlbumId: String): ProviderResult<RemoteAlbum> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "Audius does not expose albums as a first-class catalog entity.")

    override suspend fun getTrack(providerTrackId: String): ProviderResult<RemoteTrack> =
        withContext(Dispatchers.IO) {
            requestObject("tracks/$providerTrackId") { json ->
                parseTrack(json)?.let { RemoteTrack(it, "$BASE_URL/tracks/$providerTrackId/stream") }
            }
        }

    override suspend fun getRecommendations(seed: RecommendationSeed?): ProviderResult<List<RemoteMusicItem>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "Personalized Audius recommendations require a resolved Audius user identity.")

    override suspend fun getNewReleases(): ProviderResult<List<RemoteAlbum>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "Audius has no canonical album-release model.")

    override suspend fun getTrending(): ProviderResult<List<RemoteMusicItem>> =
        withContext(Dispatchers.IO) { requestList("tracks/trending") { parseTrack(it) } }

    private fun searchArtists(query: String): ProviderResult<List<RemoteMusicItem>> =
        requestList("users/search", query) { json ->
            parseArtist(json)?.let { artist ->
                RemoteMusicItem(
                    identity = artist.identity,
                    title = artist.name,
                    artistName = artist.name,
                    artworkUrl = artist.artworkUrl,
                    musicIdentity = artist.musicIdentity,
                    metadata = artist.metadata
                )
            }
        }

    private fun <T> requestList(
        path: String,
        query: String? = null,
        parser: (JSONObject) -> T?
    ): ProviderResult<List<T>> {
        val builder = "$BASE_URL/$path".toHttpUrl().newBuilder().addQueryParameter("limit", "25")
        query?.takeIf { it.isNotBlank() }?.let { builder.addQueryParameter("query", it.trim()) }
        return execute(Request.Builder().url(builder.build()).get().build()) { root ->
            val data = root.optJSONArray("data") ?: return@execute emptyList()
            buildList {
                for (index in 0 until data.length()) parser(data.optJSONObject(index))?.let(::add)
            }
        }
    }

    private fun <T> requestObject(path: String, parser: (JSONObject) -> T?): ProviderResult<T> =
        execute(Request.Builder().url("$BASE_URL/$path").get().build()) { root ->
            parser(root.optJSONObject("data") ?: root)
        }

    private fun <T> execute(
        request: Request,
        transform: (JSONObject) -> T?
    ): ProviderResult<T> =
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return when (response.code) {
                        401, 403 -> ProviderResult.Failure(id, ProviderFailureKind.Unauthorized)
                        404 -> ProviderResult.Failure(id, ProviderFailureKind.NotFound)
                        429 -> ProviderResult.Failure(id, ProviderFailureKind.RateLimited)
                        in 500..599 -> ProviderResult.Failure(id, ProviderFailureKind.Unavailable)
                        else -> ProviderResult.Failure(id, ProviderFailureKind.Network, "HTTP " + response.code)
                    }
                }
                if (body.isBlank()) {
                    return ProviderResult.Failure(id, ProviderFailureKind.InvalidResponse, "Empty Audius response.")
                }
                transform(JSONObject(body))?.let(ProviderResult::Success)
                    ?: ProviderResult.Failure(id, ProviderFailureKind.InvalidResponse, "Audius returned an invalid response.")
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            ProviderResult.Failure(id, ProviderFailureKind.Network, error.message, error)
        }

    private fun parseTrack(json: JSONObject): RemoteMusicItem? {
        val trackId = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
        val user = json.optJSONObject("user")
        return RemoteMusicItem(
            identity = ProviderIdentity(id, trackId, RemoteMusicType.Track),
            title = title,
            artistName = user?.optString("name")?.takeIf { it.isNotBlank() },
            artworkUrl = artworkUrl(json),
            durationMs = json.optLong("duration", 0L).takeIf { it > 0 }?.times(1000L),
            metadata = buildMap {
                user?.optString("id")?.takeIf { it.isNotBlank() }?.let { put("artist_provider_id", it) }
                json.optString("genre").takeIf { it.isNotBlank() }?.let { put("genre", it) }
                json.optString("permalink").takeIf { it.isNotBlank() }?.let { put("permalink", it) }
            }
        )
    }

    private fun parseArtist(json: JSONObject): RemoteArtist? {
        val artistId = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = json.optString("name").takeIf { it.isNotBlank() } ?: return null
        return RemoteArtist(
            identity = ProviderIdentity(id, artistId, RemoteMusicType.Artist),
            name = name,
            artworkUrl = artworkUrl(json),
            metadata = buildMap {
                json.optString("permalink").takeIf { it.isNotBlank() }?.let { put("permalink", it) }
            }
        )
    }

    private fun artworkUrl(json: JSONObject): String? {
        val artwork = json.optJSONObject("artwork") ?: return null
        return listOf("1000x1000", "480x480", "150x150")
            .firstNotNullOfOrNull { key -> artwork.optString(key).takeIf { it.isNotBlank() } }
    }

    private companion object {
        const val BASE_URL = "https://api.audius.co/v1"
    }
}
