package com.example.data.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * MusicBrainz identity/metadata provider.
 *
 * It deliberately does not provide playback. Its role is canonical identity
 * resolution and release metadata.
 */
class MusicBrainzProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : MusicProvider {
    override val id = "musicbrainz"
    override val displayName = "MusicBrainz"

    override val capabilities = ProviderCapabilities.of(
        ProviderCapability.SEARCH,
        ProviderCapability.ARTIST,
        ProviderCapability.ALBUM,
        ProviderCapability.TRACK
    )

    private val requestLimiter = RequestLimiter()

    override suspend fun search(
        query: String,
        filter: SearchFilter
    ): ProviderResult<List<RemoteMusicItem>> = withContext(Dispatchers.IO) {
        when (filter) {
            SearchFilter.Artists -> searchEntity("artist", query) { parseArtist(it) }
            SearchFilter.Albums -> searchEntity("release", query) { parseRelease(it) }
            SearchFilter.Tracks, SearchFilter.All -> searchEntity("recording", query) { parseRecording(it) }
            SearchFilter.Playlists -> ProviderResult.Success(emptyList())
        }
    }

    override suspend fun getArtist(providerArtistId: String): ProviderResult<RemoteArtist> =
        withContext(Dispatchers.IO) {
            lookup("artist", providerArtistId, inc = "aliases+tags+genres") { parseArtist(it) }
        }

    override suspend fun getAlbum(providerAlbumId: String): ProviderResult<RemoteAlbum> =
        withContext(Dispatchers.IO) {
            lookup("release", providerAlbumId, inc = "artist-credits+recordings+labels") { parseRelease(it) }
        }

    override suspend fun getTrack(providerTrackId: String): ProviderResult<RemoteTrack> =
        withContext(Dispatchers.IO) {
            lookup("recording", providerTrackId, inc = "artist-credits+releases") {
                parseRecording(it)?.let(::RemoteTrack)
            }
        }

    override suspend fun getRecommendations(seed: RecommendationSeed?): ProviderResult<List<RemoteMusicItem>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "MusicBrainz does not rank recommendations.")

    override suspend fun getNewReleases(): ProviderResult<List<RemoteAlbum>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "Release discovery belongs to the discovery layer.")

    override suspend fun getTrending(): ProviderResult<List<RemoteMusicItem>> =
        ProviderResult.Failure(id, ProviderFailureKind.Unsupported, "MusicBrainz does not provide popularity ranking.")

    private fun <T> searchEntity(
        entity: String,
        query: String,
        parser: (JSONObject) -> T?
    ): ProviderResult<List<T>> {
        val url = "$BASE_URL/$entity".toHttpUrl().newBuilder()
            .addQueryParameter("query", query.trim())
            .addQueryParameter("fmt", "json")
            .addQueryParameter("limit", "25")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        return executeRateLimited(request) { root ->
            val data = root.optJSONArray("$entity-list") ?: return@execute emptyList()
            buildList {
                for (index in 0 until data.length()) parser(data.optJSONObject(index))?.let(::add)
            }
        }
    }

    private suspend fun <T> lookup(
        entity: String,
        mbid: String,
        inc: String? = null,
        parser: (JSONObject) -> T?
    ): ProviderResult<T> {
        val builder = "$BASE_URL/$entity/$mbid".toHttpUrl().newBuilder()
            .addQueryParameter("fmt", "json")
        inc?.let { builder.addQueryParameter("inc", it) }

        val request = Request.Builder()
            .url(builder.build())
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return executeRateLimited(request, parser)
    }

    private suspend fun <T> executeRateLimited(
        request: Request,
        transform: (JSONObject) -> T
    ): ProviderResult<T> {
        requestLimiter.awaitTurn()
        return execute(request, transform)
    }

    private fun <T> execute(
        request: Request,
        transform: (JSONObject) -> T
    ): ProviderResult<T> =
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return when (response.code) {
                        401, 403 -> ProviderResult.Failure(id, ProviderFailureKind.Unauthorized)
                        404 -> ProviderResult.Failure(id, ProviderFailureKind.NotFound)
                        429 -> ProviderResult.Failure(id, ProviderFailureKind.RateLimited)
                        in 500..599 -> ProviderResult.Failure(id, ProviderFailureKind.Unavailable)
                        else -> ProviderResult.Failure(id, ProviderFailureKind.Network, "HTTP " + response.code)
                    }
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return ProviderResult.Failure(id, ProviderFailureKind.InvalidResponse, "Empty MusicBrainz response.")
                }
                ProviderResult.Success(transform(JSONObject(body)))
            }
        } catch (error: Exception) {
            ProviderResult.Failure(id, ProviderFailureKind.Network, error.message, error)
        }

    private fun parseArtist(json: JSONObject): RemoteArtist? {
        val mbid = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = json.optString("name").takeIf { it.isNotBlank() } ?: return null
        return RemoteArtist(
            identity = ProviderIdentity(id, mbid, RemoteMusicType.Artist),
            name = name,
            musicIdentity = MusicIdentity(canonicalArtistId = mbid),
            metadata = mapOf(
                "sort_name" to json.optString("sort-name"),
                "country" to json.optString("country")
            ).filterValues { it.isNotBlank() }
        )
    }

    private fun parseRecording(json: JSONObject): RemoteMusicItem? {
        val mbid = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
        val artist = json.optJSONArray("artist-credit")?.optJSONObject(0)?.optJSONObject("artist")
        val release = json.optJSONArray("release-list")?.optJSONObject(0)
        return RemoteMusicItem(
            identity = ProviderIdentity(id, mbid, RemoteMusicType.Track),
            title = title,
            artistName = artist?.optString("name")?.takeIf { it.isNotBlank() },
            albumName = release?.optString("title")?.takeIf { it.isNotBlank() },
            durationMs = json.optLong("length", 0L).takeIf { it > 0 },
            musicIdentity = MusicIdentity(
                canonicalArtistId = artist?.optString("id")?.takeIf { it.isNotBlank() },
                canonicalRecordingId = mbid,
                canonicalReleaseId = release?.optString("id")?.takeIf { it.isNotBlank() }
            )
        )
    }

    private fun parseRelease(json: JSONObject): RemoteAlbum? {
        val mbid = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
        val artist = json.optJSONArray("artist-credit")?.optJSONObject(0)?.optJSONObject("artist")
        return RemoteAlbum(
            identity = ProviderIdentity(id, mbid, RemoteMusicType.Album),
            title = title,
            artistName = artist?.optString("name")?.takeIf { it.isNotBlank() },
            releaseDate = json.optString("date").takeIf { it.isNotBlank() },
            musicIdentity = MusicIdentity(
                canonicalArtistId = artist?.optString("id")?.takeIf { it.isNotBlank() },
                canonicalReleaseId = mbid
            )
        )
    }

    private class RequestLimiter {
        private val mutex = Mutex()
        private var lastRequestAt = 0L

        suspend fun awaitTurn() {
            mutex.withLock {
                val waitMs = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastRequestAt)
                if (waitMs > 0) delay(waitMs)
                lastRequestAt = System.currentTimeMillis()
            }
        }

        private companion object {
            const val MIN_INTERVAL_MS = 1_000L
        }
    }

    private companion object {
        const val BASE_URL = "https://musicbrainz.org/ws/2"
        const val USER_AGENT = "oniPlayer/1.0 (https://github.com/onimeno62/oniPlayer)"
    }
}
