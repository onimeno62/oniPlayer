package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.OniDatabase
import com.example.data.entity.SongEntity
import com.example.data.following.ArtistFollowRepository
import com.example.data.online.*
import com.example.data.recommendation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DiscoverLoadState {
    data object Idle : DiscoverLoadState
    data object Loading : DiscoverLoadState
    data class Success(val items: List<RemoteMusicItem>, val hasPartialFailures: Boolean = false) : DiscoverLoadState
    data class Error(val message: String?) : DiscoverLoadState
}

sealed interface DiscoverDetailState {
    data class RemoteAlbumDetail(
        val album: RemoteAlbum,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) : DiscoverDetailState

    data class RemoteArtistDetail(
        val artist: RemoteArtist,
        val releases: List<RemoteAlbum> = emptyList(),
        val tracks: List<RemoteMusicItem> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) : DiscoverDetailState
}

data class DiscoverUiState(
    val query: String = "",
    val searchFilter: SearchFilter = SearchFilter.All,
    val search: DiscoverLoadState = DiscoverLoadState.Idle,
    val trending: DiscoverLoadState = DiscoverLoadState.Idle,
    val newReleases: DiscoverLoadState = DiscoverLoadState.Idle,
    val becauseYouListenTo: DiscoverLoadState = DiscoverLoadState.Idle,
    val becauseYouListenToArtist: String? = null,
    val similarMusic: DiscoverLoadState = DiscoverLoadState.Idle,
    val discoverArtists: DiscoverLoadState = DiscoverLoadState.Idle,
    val selectedGenre: String? = "Electronic",
    val genreExplore: DiscoverLoadState = DiscoverLoadState.Idle,
    val isRefreshing: Boolean = false,
    val activeDetail: DiscoverDetailState? = null
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val localRepository = com.example.data.repository.MusicRepository(
        application,
        OniDatabase.getDatabase(application).songDao()
    )
    private val repository = OnlineMusicRepository(DefaultMusicProviders.create())
    private val recommendationEngine = RecommendationEngine()
    private val artistFollowRepository = ArtistFollowRepository(OniDatabase.getDatabase(application).songDao())
    private val songDao = OniDatabase.getDatabase(application).songDao()

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val _localSongs = MutableStateFlow<List<SongEntity>>(emptyList())

    val followedArtistIds: StateFlow<Set<String>> = artistFollowRepository.followedArtists()
        .map { artists -> artists.map { it.canonicalArtistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val followedArtistReleases: StateFlow<List<RemoteMusicItem>> = artistFollowRepository.followedReleases()
        .map { releases ->
            releases.take(30).map { release ->
                RemoteMusicItem(
                    identity = ProviderIdentity(release.providerId, release.releaseId, RemoteMusicType.Album),
                    title = release.title,
                    artistName = release.artistName,
                    artworkUrl = release.artworkUrl,
                    musicIdentity = MusicIdentity(canonicalArtistId = release.canonicalArtistId),
                    metadata = mapOf(
                        "release_date" to (release.releaseDate ?: ""),
                        "source" to "followed_artist"
                    )
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteSongIds: StateFlow<Set<String>> = songDao.getFavoriteSongs()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val availableGenres = listOf(
        "Electronic", "Hip-Hop", "Rock", "Pop", "Ambient", "Indie", "Jazz", "Metal", "Classical", "R&B", "Folk", "Lo-Fi"
    )

    init {
        viewModelScope.launch {
            localRepository.allSongs.collect { songs ->
                _localSongs.value = songs
                // When local songs update, trigger recommendation sections if idle
                if (_uiState.value.becauseYouListenTo is DiscoverLoadState.Idle) {
                    refreshBecauseYouListenTo()
                }
                if (_uiState.value.similarMusic is DiscoverLoadState.Idle) {
                    refreshSimilarMusic()
                }
            }
        }
        refreshTrending()
        refreshNewReleases()
        refreshDiscoverArtists()
        selectGenre("Electronic")
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            refreshTrending()
            refreshNewReleases()
            refreshBecauseYouListenTo()
            refreshSimilarMusic()
            refreshDiscoverArtists()
            _uiState.value.selectedGenre?.let { selectGenre(it) }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(trending = DiscoverLoadState.Loading) }
            val results = repository.trending()
            val items = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success -> value.value
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    trending = when {
                        items.isNotEmpty() -> DiscoverLoadState.Success(
                            items.distinctBy { item ->
                                item.identity.providerId + ":" + item.identity.type + ":" + item.identity.itemId
                            },
                            failures
                        )
                        failures -> DiscoverLoadState.Error("Trending tracks are temporarily unavailable.")
                        else -> DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun refreshNewReleases() {
        viewModelScope.launch {
            _uiState.update { it.copy(newReleases = DiscoverLoadState.Loading) }
            val results = repository.newReleases()
            val items = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success -> value.value.map { album ->
                        RemoteMusicItem(
                            identity = album.identity,
                            title = album.title,
                            artistName = album.artistName,
                            albumName = album.title,
                            artworkUrl = album.artworkUrl,
                            musicIdentity = album.musicIdentity,
                            metadata = mapOf("release_date" to (album.releaseDate ?: ""))
                        )
                    }
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    newReleases = when {
                        items.isNotEmpty() -> DiscoverLoadState.Success(
                            items.distinctBy { item -> item.identity.itemId },
                            failures
                        )
                        failures -> DiscoverLoadState.Error("New releases are temporarily unavailable.")
                        else -> DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun refreshBecauseYouListenTo() {
        viewModelScope.launch {
            _uiState.update { it.copy(becauseYouListenTo = DiscoverLoadState.Loading) }
            val songs = _localSongs.value
            val topArtist = songs
                .filter { it.playCount > 0 || it.isFavorite }
                .groupBy { it.displayArtist.trim() }
                .filterKeys { it.isNotBlank() && !it.equals("Unknown Artist", true) && !it.equals("Various Artists", true) }
                .maxByOrNull { (_, artistSongs) -> artistSongs.sumOf { it.playCount } + artistSongs.count { it.isFavorite } * 3 }
                ?.key
                ?: songs.map { it.displayArtist.trim() }
                    .filter { it.isNotBlank() && !it.equals("Unknown Artist", true) && !it.equals("Various Artists", true) }
                    .distinct()
                    .randomOrNull()

            if (topArtist.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        becauseYouListenTo = DiscoverLoadState.Success(emptyList()),
                        becauseYouListenToArtist = null
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(becauseYouListenToArtist = topArtist) }
            val searchResults = repository.search(topArtist, SearchFilter.Tracks)
            val items = searchResults.flatMap { it.result.getOrNull() ?: emptyList() }
                .filter { candidate ->
                    songs.none { local ->
                        local.displayTitle.equals(candidate.title, ignoreCase = true) &&
                            local.displayArtist.equals(candidate.artistName, ignoreCase = true)
                    }
                }
                .distinctBy { it.identity.providerId + ":" + it.identity.itemId }
                .take(15)

            val hasFailures = searchResults.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    becauseYouListenTo = if (items.isNotEmpty()) {
                        DiscoverLoadState.Success(items, hasFailures)
                    } else if (hasFailures) {
                        DiscoverLoadState.Error("Recommendations are temporarily unavailable.")
                    } else {
                        DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun refreshSimilarMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(similarMusic = DiscoverLoadState.Loading) }
            val songs = _localSongs.value
            val trendingItems = repository.trending().flatMap { it.result.getOrNull() ?: emptyList() }

            val topGenres = songs
                .map { it.displayGenre.trim() }
                .filter { it.isNotBlank() && !it.equals("Unknown Genre", true) && !it.equals("Local Audio", true) }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(2)
                .map { it.key }

            val genreCandidates = mutableListOf<RemoteMusicItem>()
            for (genre in topGenres) {
                val results = repository.search(genre, SearchFilter.Tracks)
                genreCandidates += results.flatMap { it.result.getOrNull() ?: emptyList() }
            }

            val allCandidates = (trendingItems + genreCandidates).distinctBy {
                it.identity.providerId + ":" + it.identity.itemId
            }

            val snapshot = RecommendationSnapshot(
                localSongs = songs,
                externalCandidates = allCandidates
            )
            val recommendations = recommendationEngine.recommendRemote(snapshot, limit = 15)
            val recommendedItems = recommendations.mapNotNull { rec ->
                rec.remoteItem?.let { item ->
                    val reasonText = when (rec.reason) {
                        RecommendationReason.Trending -> "Trending"
                        RecommendationReason.NewRelease -> "New Release"
                        RecommendationReason.SimilarTo -> "Similar to your taste"
                        RecommendationReason.BecauseYouPlayed -> "Based on listening history"
                        RecommendationReason.FavoriteAffinity -> "Matches favorites"
                        else -> "Recommended for you"
                    }
                    item.copy(metadata = item.metadata + ("recommendation_reason" to reasonText))
                }
            }

            _uiState.update {
                it.copy(
                    similarMusic = if (recommendedItems.isNotEmpty()) {
                        DiscoverLoadState.Success(recommendedItems)
                    } else {
                        DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun refreshDiscoverArtists() {
        viewModelScope.launch {
            _uiState.update { it.copy(discoverArtists = DiscoverLoadState.Loading) }
            val trendingResults = repository.trending().flatMap { it.result.getOrNull() ?: emptyList() }
            val newReleaseResults = repository.newReleases().flatMap { it.result.getOrNull() ?: emptyList() }

            val artistCandidates = mutableListOf<RemoteMusicItem>()
            for (item in trendingResults) {
                val name = item.artistName?.trim()
                if (!name.isNullOrBlank()) {
                    artistCandidates += RemoteMusicItem(
                        identity = ProviderIdentity(item.identity.providerId, item.identity.itemId, RemoteMusicType.Artist),
                        title = name,
                        artistName = name,
                        artworkUrl = item.artworkUrl,
                        musicIdentity = item.musicIdentity
                    )
                }
            }
            for (album in newReleaseResults) {
                val name = album.artistName?.trim()
                if (!name.isNullOrBlank()) {
                    artistCandidates += RemoteMusicItem(
                        identity = ProviderIdentity(album.identity.providerId, album.identity.itemId, RemoteMusicType.Artist),
                        title = name,
                        artistName = name,
                        artworkUrl = album.artworkUrl,
                        musicIdentity = album.musicIdentity
                    )
                }
            }

            val distinctArtists = artistCandidates
                .distinctBy { it.artistName?.lowercase() }
                .take(15)

            _uiState.update {
                it.copy(
                    discoverArtists = if (distinctArtists.isNotEmpty()) {
                        DiscoverLoadState.Success(distinctArtists)
                    } else {
                        DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun selectGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre, genreExplore = DiscoverLoadState.Loading) }
        viewModelScope.launch {
            val searchResults = repository.search(genre, SearchFilter.Tracks)
            val items = searchResults.flatMap { it.result.getOrNull() ?: emptyList() }
                .distinctBy { it.identity.providerId + ":" + it.identity.itemId }
                .take(20)
            val hasFailures = searchResults.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    genreExplore = if (items.isNotEmpty()) {
                        DiscoverLoadState.Success(items, hasFailures)
                    } else if (hasFailures) {
                        DiscoverLoadState.Error("Failed to explore $genre genre.")
                    } else {
                        DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun toggleFollowArtist(item: RemoteMusicItem) {
        val artistId = item.musicIdentity.canonicalArtistId ?: item.identity.itemId
        val artistName = item.artistName?.takeIf { it.isNotBlank() } ?: item.title
        toggleFollowArtist(artistId, artistName, item.artworkUrl)
    }

    fun toggleFollowArtist(artistId: String, artistName: String, artworkUrl: String?) {
        viewModelScope.launch {
            if (artistId in followedArtistIds.value) {
                artistFollowRepository.unfollow(artistId)
            } else {
                artistFollowRepository.follow(artistId, artistName, artworkUrl)
            }
        }
    }

    fun toggleFavoriteTrack(item: RemoteMusicItem) {
        viewModelScope.launch {
            val songId = "remote_${item.identity.providerId}_${item.identity.itemId}"
            val existing = songDao.getSongById(songId)
            if (existing != null) {
                songDao.updateSong(existing.copy(isFavorite = !existing.isFavorite))
            } else {
                val song = SongEntity(
                    id = songId,
                    title = item.title,
                    artist = item.artistName ?: "Unknown Artist",
                    album = item.albumName ?: "Online Music",
                    genre = item.metadata["genre"] ?: "Online",
                    duration = item.durationMs ?: 0L,
                    filePath = item.metadata["stream_url"] ?: "https://stream.offline",
                    albumArtUri = item.artworkUrl,
                    isFavorite = true,
                    dateAdded = System.currentTimeMillis()
                )
                songDao.insertSong(song)
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(query = "", search = DiscoverLoadState.Idle) }
    }

    fun setSearchFilter(filter: SearchFilter) {
        _uiState.update { it.copy(searchFilter = filter) }
        if (_uiState.value.query.isNotBlank()) search()
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(search = DiscoverLoadState.Idle) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(search = DiscoverLoadState.Loading) }
            val results = repository.search(query, _uiState.value.searchFilter)
            val successful = results.flatMap { result ->
                when (val value = result.result) {
                    is ProviderResult.Success<*> -> value.value as? List<RemoteMusicItem> ?: emptyList()
                    is ProviderResult.Failure -> emptyList()
                }
            }
            val failures = results.any { it.result is ProviderResult.Failure }
            _uiState.update {
                it.copy(
                    search = when {
                        successful.isNotEmpty() -> DiscoverLoadState.Success(
                            successful.distinctBy { item ->
                                item.identity.providerId + ":" + item.identity.type + ":" + item.identity.itemId
                            },
                            failures
                        )
                        failures -> DiscoverLoadState.Error("Online search is currently unavailable.")
                        else -> DiscoverLoadState.Success(emptyList())
                    }
                )
            }
        }
    }

    fun openAlbumDetail(item: RemoteMusicItem) {
        val initialAlbum = RemoteAlbum(
            identity = item.identity,
            title = item.title,
            artistName = item.artistName,
            artworkUrl = item.artworkUrl,
            musicIdentity = item.musicIdentity,
            releaseDate = item.metadata["release_date"]
        )
        _uiState.update {
            it.copy(
                activeDetail = DiscoverDetailState.RemoteAlbumDetail(initialAlbum, isLoading = true)
            )
        }

        viewModelScope.launch {
            val albumResult = repository.getAlbum(item.identity.providerId, item.identity.itemId)
            _uiState.update { current ->
                val detail = current.activeDetail as? DiscoverDetailState.RemoteAlbumDetail ?: return@update current
                when (albumResult) {
                    is ProviderResult.Success -> current.copy(
                        activeDetail = detail.copy(album = albumResult.value, isLoading = false)
                    )
                    is ProviderResult.Failure -> current.copy(
                        activeDetail = detail.copy(isLoading = false, errorMessage = albumResult.message)
                    )
                }
            }
        }
    }

    fun openArtistDetail(item: RemoteMusicItem) {
        val initialArtist = RemoteArtist(
            identity = item.identity,
            name = item.artistName ?: item.title,
            artworkUrl = item.artworkUrl,
            musicIdentity = item.musicIdentity
        )
        _uiState.update {
            it.copy(
                activeDetail = DiscoverDetailState.RemoteArtistDetail(initialArtist, isLoading = true)
            )
        }

        viewModelScope.launch {
            val releasesResult = repository.artistReleases(item.identity.providerId, item.identity.itemId)
            val releases = (releasesResult as? ProviderResult.Success)?.value ?: emptyList()
            val tracksResult = repository.search(initialArtist.name, SearchFilter.Tracks)
            val tracks = tracksResult.firstOrNull { it.providerId == item.identity.providerId }?.result?.getOrNull()
                ?: tracksResult.flatMap { it.result.getOrNull() ?: emptyList() }

            _uiState.update { current ->
                val detail = current.activeDetail as? DiscoverDetailState.RemoteArtistDetail ?: return@update current
                current.copy(
                    activeDetail = detail.copy(
                        releases = releases,
                        tracks = tracks,
                        isLoading = false
                    )
                )
            }
        }
    }

    fun closeDetail() {
        _uiState.update { it.copy(activeDetail = null) }
    }

    fun canStream(item: RemoteMusicItem): Boolean {
        val provider = repository.provider(item.identity.providerId) ?: return false
        return provider.capabilities.supports(ProviderCapability.STREAM)
    }

    fun canDownload(item: RemoteMusicItem): Boolean {
        val provider = repository.provider(item.identity.providerId) ?: return false
        return provider.capabilities.supports(ProviderCapability.DOWNLOAD)
    }

    private suspend fun resolveToSongEntity(item: RemoteMusicItem): SongEntity? {
        val provider = repository.provider(item.identity.providerId)
        val streamUrl = if (provider != null && provider.capabilities.supports(ProviderCapability.STREAM)) {
            when (val res = repository.getTrack(item.identity.providerId, item.identity.itemId)) {
                is ProviderResult.Success -> res.value.streamUrl
                is ProviderResult.Failure -> null
            }
        } else {
            item.metadata["stream_url"]
        }
        if (streamUrl.isNullOrBlank()) return null

        val songId = "remote_${item.identity.providerId}_${item.identity.itemId}"
        val existing = songDao.getSongById(songId)
        val song = existing?.copy(filePath = streamUrl) ?: SongEntity(
            id = songId,
            title = item.title,
            artist = item.artistName ?: "Unknown Artist",
            album = item.albumName ?: "Online Music",
            genre = item.metadata["genre"] ?: "Online",
            duration = item.durationMs ?: 0L,
            filePath = streamUrl,
            albumArtUri = item.artworkUrl,
            dateAdded = System.currentTimeMillis()
        )
        songDao.insertSong(song)
        return song
    }

    fun playTrack(
        item: RemoteMusicItem,
        musicPlayerViewModel: MusicPlayerViewModel,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (!canStream(item)) {
            onResult(false, "Streaming is not supported for ${item.identity.providerId}.")
            return
        }
        viewModelScope.launch {
            val song = resolveToSongEntity(item)
            if (song != null) {
                musicPlayerViewModel.playSong(song, listOf(song))
                onResult(true, null)
            } else {
                onResult(false, "Unable to resolve stream for ${item.title}.")
            }
        }
    }

    fun playNextTrack(
        item: RemoteMusicItem,
        musicPlayerViewModel: MusicPlayerViewModel,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (!canStream(item)) {
            onResult(false, "Streaming is not supported for ${item.identity.providerId}.")
            return
        }
        viewModelScope.launch {
            val song = resolveToSongEntity(item)
            if (song != null) {
                musicPlayerViewModel.playNext(song)
                onResult(true, null)
            } else {
                onResult(false, "Unable to resolve stream for ${item.title}.")
            }
        }
    }

    fun addToQueueTrack(
        item: RemoteMusicItem,
        musicPlayerViewModel: MusicPlayerViewModel,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (!canStream(item)) {
            onResult(false, "Streaming is not supported for ${item.identity.providerId}.")
            return
        }
        viewModelScope.launch {
            val song = resolveToSongEntity(item)
            if (song != null) {
                musicPlayerViewModel.addToQueue(song)
                onResult(true, null)
            } else {
                onResult(false, "Unable to resolve stream for ${item.title}.")
            }
        }
    }

    fun playAllAlbum(
        album: RemoteAlbum,
        musicPlayerViewModel: MusicPlayerViewModel,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (album.tracks.isEmpty()) {
            onResult(false, "No tracks available in this release.")
            return
        }
        viewModelScope.launch {
            val resolvedSongs = mutableListOf<SongEntity>()
            for (trackItem in album.tracks) {
                if (canStream(trackItem)) {
                    resolveToSongEntity(trackItem)?.let { resolvedSongs += it }
                }
            }
            if (resolvedSongs.isNotEmpty()) {
                musicPlayerViewModel.playSong(resolvedSongs.first(), resolvedSongs)
                onResult(true, null)
            } else {
                onResult(false, "No playable streaming tracks found for this release.")
            }
        }
    }
}
