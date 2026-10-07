package com.example.data.online

enum class ProviderCapability {
    SEARCH,
    ARTIST,
    ALBUM,
    TRACK,
    PLAYLIST,
    STREAM,
    DOWNLOAD,
    RECOMMENDATIONS,
    NEW_RELEASES,
    TRENDING,
    RADIO,
    ARTWORK,
    LYRICS
}

data class ProviderCapabilities(
    private val supported: Set<ProviderCapability> = emptySet()
) {
    fun supports(capability: ProviderCapability): Boolean = capability in supported

    fun asSet(): Set<ProviderCapability> = supported.toSet()

    companion object {
        fun of(vararg capabilities: ProviderCapability): ProviderCapabilities =
            ProviderCapabilities(capabilities.toSet())
    }
}

enum class SearchFilter {
    All,
    Tracks,
    Artists,
    Albums,
    Playlists
}

enum class RemoteMusicType {
    Track,
    Artist,
    Album,
    Playlist
}

data class MusicIdentity(
    val canonicalArtistId: String? = null,
    val canonicalReleaseGroupId: String? = null,
    val canonicalReleaseId: String? = null,
    val canonicalRecordingId: String? = null
)

data class ProviderIdentity(
    val providerId: String,
    val itemId: String,
    val type: RemoteMusicType
)

data class RemoteMusicItem(
    val identity: ProviderIdentity,
    val title: String,
    val artistName: String? = null,
    val albumName: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val musicIdentity: MusicIdentity = MusicIdentity(),
    val isExplicit: Boolean? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class RemoteTrack(
    val item: RemoteMusicItem,
    val streamUrl: String? = null,
    val downloadUrl: String? = null
)

data class RemoteArtist(
    val identity: ProviderIdentity,
    val name: String,
    val artworkUrl: String? = null,
    val musicIdentity: MusicIdentity = MusicIdentity(),
    val metadata: Map<String, String> = emptyMap()
)

data class RemoteAlbum(
    val identity: ProviderIdentity,
    val title: String,
    val artistName: String? = null,
    val artworkUrl: String? = null,
    val releaseDate: String? = null,
    val tracks: List<RemoteMusicItem> = emptyList(),
    val musicIdentity: MusicIdentity = MusicIdentity()
)

data class ProviderSearchResult(
    val providerId: String,
    val result: ProviderResult<List<RemoteMusicItem>>
)

data class ProviderSectionResult<T>(
    val providerId: String,
    val result: ProviderResult<T>
)

data class RecommendationSeed(
    val trackProviderIds: List<ProviderIdentity> = emptyList(),
    val artistProviderIds: List<ProviderIdentity> = emptyList(),
    val genre: String? = null
)

sealed interface ProviderResult<out T> {
    data class Success<T>(val value: T) : ProviderResult<T>

    data class Failure(
        val providerId: String,
        val kind: ProviderFailureKind,
        val message: String? = null,
        val cause: Throwable? = null
    ) : ProviderResult<Nothing>
}

enum class ProviderFailureKind {
    Network,
    Unauthorized,
    RateLimited,
    NotFound,
    Unsupported,
    InvalidResponse,
    Unavailable,
    Unknown
}

fun <T> ProviderResult<T>.getOrNull(): T? =
    (this as? ProviderResult.Success<T>)?.value
