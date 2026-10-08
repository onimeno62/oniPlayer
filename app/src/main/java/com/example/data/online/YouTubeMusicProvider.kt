package com.example.data.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional official YouTube Data API adapter.
 *
 * It intentionally exposes metadata/search only. The official API does not provide
 * a general-purpose playable/downloadable audio URL, so STREAM/DOWNLOAD are not claimed.
 */
class YouTubeMusicProvider(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : MusicProvider {
    override val id = "youtube_music"
    override val displayName = "YouTube Music"
    override val capabilities = ProviderCapabilities.of(
        ProviderCapability.SEARCH,
        ProviderCapability.ARTIST,
        ProviderCapability.TRACK,
        ProviderCapability.ARTWORK
    )

    override suspend fun search(query: String, filter: SearchFilter): ProviderResult<List<RemoteMusicItem>> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext ProviderResult.Failure(id, ProviderFailureKind.Unauthorized, "YouTube API key is not configured.")
            val type = when (filter) {
                SearchFilter.Artists -> "channel"
                else -> "video"
            }
            request("search", mapOf("part" to "snippet", "q" to query.trim(), "type" to type, "maxResults" to "25"))
                .map { root ->
                    val items = root.optJSONArray("items") ?: return@map emptyList()
                    buildList {
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val snippet = item.optJSONObject("snippet") ?: continue
                            val idObject = item.optJSONObject("id")
                            val remoteId = idObject?.optString(if (type == "channel") "channelId" else "videoId").orEmpty()
                            if (remoteId.isBlank()) continue
                            add(
                                RemoteMusicItem(
                                    identity = ProviderIdentity(id, remoteId, if (type == "channel") RemoteMusicType.Artist else RemoteMusicType.Track),
                                    title = snippet.optString("title"),
                                    artistName = snippet.optString("channelTitle").takeIf { it.isNotBlank() },
                                    artworkUrl = snippet.optJSONObject("thumbnails")?.optJSONObject("high")?.optString("url"),
                                    metadata = mapOf("description" to snippet.optString("description"))
                                )
                            )
                        }
                    }
                }
        }

    override suspend fun getArtist(providerArtistId: String): ProviderResult<RemoteArtist> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "Artist lookup requires an additional channels request.")

    override suspend fun getArtistReleases(providerArtistId: String): ProviderResult<List<RemoteAlbum>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

    override suspend fun getAlbum(providerAlbumId: String): ProviderResult<RemoteAlbum> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

    override suspend fun getTrack(providerTrackId: String): ProviderResult<RemoteTrack> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext ProviderResult.Failure(id, ProviderFailureKind.Unauthorized, "YouTube API key is not configured.")
            request("videos", mapOf("part" to "snippet,contentDetails", "id" to providerTrackId)).map { root ->
                val item = root.optJSONArray("items")?.optJSONObject(0)
                    ?: return@map RemoteTrack(RemoteMusicItem(ProviderIdentity(id, providerTrackId, RemoteMusicType.Track), "Unknown"))
                val snippet = item.optJSONObject("snippet")
                RemoteTrack(
                    RemoteMusicItem(
                        identity = ProviderIdentity(id, providerTrackId, RemoteMusicType.Track),
                        title = snippet?.optString("title").orEmpty(),
                        artistName = snippet?.optString("channelTitle"),
                        artworkUrl = snippet?.optJSONObject("thumbnails")?.optJSONObject("high")?.optString("url")
                    )
                )
            }
        }

    override suspend fun getRecommendations(seed: RecommendationSeed?): ProviderResult<List<RemoteMusicItem>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

    override suspend fun getNewReleases(): ProviderResult<List<RemoteAlbum>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

    override suspend fun getTrending(): ProviderResult<List<RemoteMusicItem>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

    private fun request(
        endpoint: String,
        parameters: Map<String, String>
    ): ProviderResult<JSONObject> {
        val urlBuilder = "https://www.googleapis.com/youtube/v3/$endpoint".toHttpUrl().newBuilder()
        parameters.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        urlBuilder.addQueryParameter("key", apiKey)
        return try {
            client.newCall(Request.Builder().url(urlBuilder.build()).get().build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return when (response.code) {
                        400, 401, 403 -> ProviderResult.Failure(id, ProviderFailureKind.Unauthorized, "YouTube API rejected the request.")
                        404 -> ProviderResult.Failure(id, ProviderFailureKind.NotFound)
                        429 -> ProviderResult.Failure(id, ProviderFailureKind.RateLimited)
                        in 500..599 -> ProviderResult.Failure(id, ProviderFailureKind.Unavailable)
                        else -> ProviderResult.Failure(id, ProviderFailureKind.Network, "HTTP " + response.code)
                    }
                }
                ProviderResult.Success(JSONObject(body))
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            ProviderResult.Failure(id, ProviderFailureKind.Network, error.message, error)
        }
    }

    private inline fun <T> ProviderResult<JSONObject>.map(transform: (JSONObject) -> T): ProviderResult<T> =
        when (this) {
            is ProviderResult.Success<*> -> runCatching { ProviderResult.Success(transform(value)) }
                .getOrElse { ProviderResult.Failure(id, ProviderFailureKind.InvalidResponse, it.message, it) }
            is ProviderResult.Failure -> this
        }
}
