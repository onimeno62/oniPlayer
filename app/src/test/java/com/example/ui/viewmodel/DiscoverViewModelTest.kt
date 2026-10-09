package com.example.ui.viewmodel

import com.example.data.entity.SongEntity
import com.example.data.online.MusicProvider
import com.example.data.online.OnlineMusicRepository
import com.example.data.online.ProviderCapabilities
import com.example.data.online.ProviderCapability
import com.example.data.online.ProviderFailureKind
import com.example.data.online.ProviderIdentity
import com.example.data.online.ProviderResult
import com.example.data.online.RecommendationSeed
import com.example.data.online.RemoteAlbum
import com.example.data.online.RemoteArtist
import com.example.data.online.RemoteMusicItem
import com.example.data.online.RemoteMusicType
import com.example.data.online.RemoteTrack
import com.example.data.online.SearchFilter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- search ---------------------------------------------------------------------------

    @Test
    fun clearing_query_cancels_in_flight_search_and_stays_idle() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val provider = TestProvider(
            id = "audius",
            capabilities = ProviderCapabilities.of(ProviderCapability.SEARCH, ProviderCapability.STREAM),
            searchHandler = {
                gate.await()
                ProviderResult.Success(listOf(track("audius", "t1")))
            }
        )
        val vm = viewModel(listOf(provider))

        vm.setQuery("lofi")
        vm.search()
        assertEquals(DiscoverLoadState.Loading, vm.uiState.value.search)

        vm.setQuery("")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(DiscoverLoadState.Idle, vm.uiState.value.search)
    }

    @Test
    fun older_search_response_never_overwrites_newer_one() = runTest(dispatcher) {
        val slowGate = CompletableDeferred<Unit>()
        val provider = TestProvider(
            id = "audius",
            capabilities = ProviderCapabilities.of(ProviderCapability.SEARCH),
            searchHandler = { query ->
                if (query == "old") {
                    slowGate.await()
                    ProviderResult.Success(listOf(track("audius", "old-1")))
                } else {
                    ProviderResult.Success(listOf(track("audius", "new-1")))
                }
            }
        )
        val vm = viewModel(listOf(provider))

        vm.setQuery("old")
        vm.search()
        vm.setQuery("new")
        vm.search()
        slowGate.complete(Unit)
        advanceUntilIdle()

        val state = vm.uiState.value.search as DiscoverLoadState.Success
        assertEquals(listOf("new-1"), state.items.map { it.item.identity.itemId })
        assertEquals("new", vm.uiState.value.submittedQuery)
    }

    @Test
    fun stream_flag_and_provider_name_come_from_capabilities() = runTest(dispatcher) {
        val streaming = TestProvider(
            id = "stream-source",
            displayName = "Stream Source",
            capabilities = ProviderCapabilities.of(ProviderCapability.SEARCH, ProviderCapability.STREAM),
            searchHandler = { ProviderResult.Success(listOf(track("stream-source", "a"))) }
        )
        val catalog = TestProvider(
            id = "catalog-source",
            displayName = "Catalog Source",
            capabilities = ProviderCapabilities.of(ProviderCapability.SEARCH),
            searchHandler = { ProviderResult.Success(listOf(track("catalog-source", "b"))) }
        )
        val vm = viewModel(listOf(streaming, catalog))

        vm.setQuery("x")
        vm.search()
        advanceUntilIdle()

        val cards = (vm.uiState.value.search as DiscoverLoadState.Success).items.associateBy { it.item.identity.providerId }
        assertTrue(cards.getValue("stream-source").canStream)
        assertEquals("Stream Source", cards.getValue("stream-source").providerName)
        assertFalse(cards.getValue("catalog-source").canStream)
        assertEquals("Catalog", cards.getValue("catalog-source").sourceLabel)
    }

    // ---- remote sections ---------------------------------------------------------------

    @Test
    fun new_releases_keep_same_item_id_from_different_providers() = runTest(dispatcher) {
        val first = TestProvider(
            id = "p1",
            capabilities = ProviderCapabilities.of(ProviderCapability.NEW_RELEASES),
            newReleasesHandler = { ProviderResult.Success(listOf(album("p1", "same-id"))) }
        )
        val second = TestProvider(
            id = "p2",
            capabilities = ProviderCapabilities.of(ProviderCapability.NEW_RELEASES),
            newReleasesHandler = { ProviderResult.Success(listOf(album("p2", "same-id"), album("p2", "same-id"))) }
        )
        val vm = viewModel(listOf(first, second))
        advanceUntilIdle()

        val state = vm.uiState.value.newReleases as DiscoverLoadState.Success
        assertEquals(setOf("p1", "p2"), state.items.map { it.item.identity.providerId }.toSet())
        assertEquals(2, state.items.size)
    }

    @Test
    fun one_failing_trending_provider_keeps_healthy_results() = runTest(dispatcher) {
        val healthy = TestProvider(
            id = "healthy",
            capabilities = ProviderCapabilities.of(ProviderCapability.TRENDING),
            trendingHandler = { ProviderResult.Success(listOf(track("healthy", "t"))) }
        )
        val failing = TestProvider(
            id = "failing",
            capabilities = ProviderCapabilities.of(ProviderCapability.TRENDING),
            trendingHandler = { ProviderResult.Failure("failing", ProviderFailureKind.Network) }
        )
        val vm = viewModel(listOf(healthy, failing))
        advanceUntilIdle()

        val state = vm.uiState.value.trending as DiscoverLoadState.Success
        assertEquals(1, state.items.size)
        assertTrue(state.hasPartialFailures)
    }

    @Test
    fun trending_error_recovers_on_retry() = runTest(dispatcher) {
        var fail = true
        val provider = TestProvider(
            id = "p",
            capabilities = ProviderCapabilities.of(ProviderCapability.TRENDING),
            trendingHandler = {
                if (fail) ProviderResult.Failure("p", ProviderFailureKind.Network)
                else ProviderResult.Success(listOf(track("p", "t")))
            }
        )
        val vm = viewModel(listOf(provider))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.trending is DiscoverLoadState.Error)

        fail = false
        vm.refreshTrending()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.trending is DiscoverLoadState.Success)
    }

    @Test
    fun no_supporting_provider_yields_empty_not_error() = runTest(dispatcher) {
        val vm = viewModel(emptyList())
        advanceUntilIdle()

        assertEquals(DiscoverLoadState.Empty, vm.uiState.value.trending)
        assertEquals(DiscoverLoadState.Empty, vm.uiState.value.newReleases)
    }

    // ---- local sections ----------------------------------------------------------------

    @Test
    fun local_sections_build_offline_with_reasons() = runTest(dispatcher) {
        val songs = listOf(
            song("1", playCount = 5, lastPlayed = 2_000L, favorite = true),
            song("2", playCount = 1, lastPlayed = 1_000L),
            song("3")
        )
        val vm = viewModel(emptyList(), songs = flowOf(songs))
        backgroundScope.launch { vm.localSections.collect {} }
        advanceUntilIdle()

        val local = vm.localSections.value
        assertFalse(local.libraryIsEmpty)
        val madeForYou = local.madeForYou as DiscoverLoadState.Success
        assertTrue(madeForYou.items.all { it.reason != null })
        assertEquals(listOf("1", "2"), (local.continueListening as DiscoverLoadState.Success).items.map { it.song.id })
        assertEquals(listOf("1"), (local.favorites as DiscoverLoadState.Success).items.map { it.song.id })
    }

    @Test
    fun empty_library_is_reported_explicitly() = runTest(dispatcher) {
        val vm = viewModel(emptyList(), songs = flowOf(emptyList()))
        backgroundScope.launch { vm.localSections.collect {} }
        advanceUntilIdle()

        assertTrue(vm.localSections.value.libraryIsEmpty)
        assertEquals(DiscoverLoadState.Empty, vm.localSections.value.madeForYou)
    }

    @Test
    fun local_failure_is_isolated_and_retryable() = runTest(dispatcher) {
        var attempts = 0
        val songs: Flow<List<SongEntity>> = flow {
            attempts++
            if (attempts == 1) throw IllegalStateException("db unavailable")
            emit(listOf(song("1")))
        }
        val trending = TestProvider(
            id = "p",
            capabilities = ProviderCapabilities.of(ProviderCapability.TRENDING),
            trendingHandler = { ProviderResult.Success(listOf(track("p", "t"))) }
        )
        val vm = viewModel(listOf(trending), songs = songs)
        backgroundScope.launch { vm.localSections.collect {} }
        advanceUntilIdle()

        assertTrue(vm.localSections.value.madeForYou is DiscoverLoadState.Error)
        assertTrue("remote sections unaffected", vm.uiState.value.trending is DiscoverLoadState.Success)

        vm.retryLocal()
        advanceUntilIdle()
        assertTrue(vm.localSections.value.madeForYou is DiscoverLoadState.Success)
    }

    @Test
    fun followed_release_failure_is_isolated() = runTest(dispatcher) {
        val gateway = FakeFollowGateway(
            releases = flow { throw IllegalStateException("db") }
        )
        val vm = viewModel(emptyList(), gateway = gateway)
        backgroundScope.launch { vm.followedReleases.collect {} }
        advanceUntilIdle()

        assertTrue(vm.followedReleases.value is DiscoverLoadState.Error)
    }

    @Test
    fun follow_toggle_uses_canonical_artist_id() = runTest(dispatcher) {
        val gateway = FakeFollowGateway()
        val vm = viewModel(emptyList(), gateway = gateway)
        backgroundScope.launch { vm.followedArtistIds.collect {} }
        val item = track("p", "t").copy(
            artistName = "Artist",
            musicIdentity = com.example.data.online.MusicIdentity(canonicalArtistId = "mbid-1")
        )

        vm.toggleFollowArtist(item)
        advanceUntilIdle()
        assertEquals(setOf("mbid-1"), gateway.ids.value)

        vm.toggleFollowArtist(item)
        advanceUntilIdle()
        assertEquals(emptySet<String>(), gateway.ids.value)
    }

    // ---- helpers -----------------------------------------------------------------------

    private fun viewModel(
        providers: List<MusicProvider>,
        songs: Flow<List<SongEntity>> = flowOf(emptyList()),
        gateway: DiscoverFollowGateway = FakeFollowGateway()
    ) = DiscoverViewModel(
        localSongs = songs,
        onlineRepository = OnlineMusicRepository(providers, cacheTtlMs = -1L),
        followGateway = gateway,
        computeDispatcher = dispatcher
    )

    private fun track(providerId: String, id: String) = RemoteMusicItem(
        identity = ProviderIdentity(providerId, id, RemoteMusicType.Track),
        title = "Track $id"
    )

    private fun album(providerId: String, id: String) = RemoteAlbum(
        identity = ProviderIdentity(providerId, id, RemoteMusicType.Album),
        title = "Album $id",
        releaseDate = "2026-10-01"
    )

    private fun song(
        id: String,
        playCount: Int = 0,
        lastPlayed: Long = 0L,
        favorite: Boolean = false
    ) = SongEntity(
        id = id,
        title = "Song $id",
        artist = "Artist $id",
        album = "Album $id",
        genre = "Ambient",
        duration = 180_000L,
        filePath = "/music/$id.mp3",
        albumArtUri = null,
        playCount = playCount,
        isFavorite = favorite,
        lastPlayedTimestamp = lastPlayed,
        dateAdded = id.toLong()
    )

    private class FakeFollowGateway(
        val ids: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet()),
        private val releases: Flow<List<RemoteMusicItem>> = flowOf(emptyList())
    ) : DiscoverFollowGateway {
        override fun followedArtistIds(): Flow<Set<String>> = ids
        override fun followedReleases(): Flow<List<RemoteMusicItem>> = releases
        override suspend fun follow(canonicalArtistId: String, artistName: String, artworkUrl: String?) {
            ids.value = ids.value + canonicalArtistId
        }
        override suspend fun unfollow(canonicalArtistId: String) {
            ids.value = ids.value - canonicalArtistId
        }
    }

    private class TestProvider(
        override val id: String,
        override val displayName: String = id,
        override val capabilities: ProviderCapabilities,
        private val searchHandler: suspend (String) -> ProviderResult<List<RemoteMusicItem>> = { ProviderResult.Success(emptyList()) },
        private val trendingHandler: suspend () -> ProviderResult<List<RemoteMusicItem>> = { ProviderResult.Success(emptyList()) },
        private val newReleasesHandler: suspend () -> ProviderResult<List<RemoteAlbum>> = { ProviderResult.Success(emptyList()) }
    ) : MusicProvider {
        override suspend fun search(query: String, filter: SearchFilter): ProviderResult<List<RemoteMusicItem>> =
            searchHandler(query)

        override suspend fun getArtist(providerArtistId: String): ProviderResult<RemoteArtist> =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getArtistReleases(providerArtistId: String): ProviderResult<List<RemoteAlbum>> =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getAlbum(providerAlbumId: String): ProviderResult<RemoteAlbum> =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getTrack(providerTrackId: String): ProviderResult<RemoteTrack> =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getRecommendations(seed: RecommendationSeed?): ProviderResult<List<RemoteMusicItem>> =
            ProviderResult.Failure(id, ProviderFailureKind.Unsupported)

        override suspend fun getNewReleases(): ProviderResult<List<RemoteAlbum>> = newReleasesHandler()

        override suspend fun getTrending(): ProviderResult<List<RemoteMusicItem>> = trendingHandler()
    }
}
