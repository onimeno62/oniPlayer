package com.example.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.OniDatabase
import com.example.data.entity.SongEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackQueueConsistencyTest {

    private lateinit var service: MusicPlaybackService
    private lateinit var controller: PlaybackController
    private lateinit var database: OniDatabase
    private lateinit var persistence: PlaybackPersistence

    private val sampleSongs = listOf(
        SongEntity(
            id = "song_1",
            title = "Track One",
            artist = "Artist A",
            album = "Album 1",
            genre = "Rock",
            duration = 180000L,
            filePath = "/storage/emulated/0/Music/track1.mp3",
            albumArtUri = null,
            playCount = 5,
            isFavorite = false
        ),
        SongEntity(
            id = "song_2",
            title = "Track Two",
            artist = "Artist B",
            album = "Album 2",
            genre = "Pop",
            duration = 240000L,
            filePath = "/storage/emulated/0/Music/track2.mp3",
            albumArtUri = null,
            playCount = 10,
            isFavorite = true
        ),
        SongEntity(
            id = "song_3",
            title = "Track Three",
            artist = "Artist C",
            album = "Album 3",
            genre = "Jazz",
            duration = 200000L,
            filePath = "/storage/emulated/0/Music/track3.mp3",
            albumArtUri = null,
            playCount = 2,
            isFavorite = false
        ),
        SongEntity(
            id = "song_4",
            title = "Track Four",
            artist = "Artist D",
            album = "Album 4",
            genre = "Classical",
            duration = 210000L,
            filePath = "/storage/emulated/0/Music/track4.mp3",
            albumArtUri = null,
            playCount = 1,
            isFavorite = false
        )
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = OniDatabase.getDatabase(context)
        persistence = PlaybackPersistence(context)
        persistence.clear()

        runBlocking {
            database.songDao().insertSongs(sampleSongs)
        }

        val serviceController = Robolectric.buildService(MusicPlaybackService::class.java)
        service = serviceController.get()
        controller = PlaybackController(service)
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized) {
            controller.release()
        }
        persistence.clear()
    }

    private suspend fun awaitPersistedQueue(expectedIds: List<String>, timeoutMs: Long = 1000): PersistedPlaybackState? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val state = persistence.load()
            if (state?.queueIds == expectedIds) return state
            kotlinx.coroutines.delay(20)
        }
        return persistence.load()
    }

    @Test
    fun setQueue_updatesQueueAndPersistsState() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)

        val state = controller.state.value
        assertEquals(2, state.queue.size)
        assertEquals(listOf("song_1", "song_2"), state.queue.map { it.id })
        assertEquals("song_1", state.currentSong?.id)

        val persisted = awaitPersistedQueue(listOf("song_1", "song_2"))
        assertNotNull(persisted)
        assertEquals(listOf("song_1", "song_2"), persisted?.queueIds)
        assertEquals("song_1", persisted?.currentSongId)
    }

    @Test
    fun addToQueue_includesNewItemInPlaybackStateQueueAndPersistedIds() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        controller.addToQueue("song_3")

        val state = controller.state.value
        assertEquals(3, state.queue.size)
        assertEquals(listOf("song_1", "song_2", "song_3"), state.queue.map { it.id })

        val persisted = awaitPersistedQueue(listOf("song_1", "song_2", "song_3"))
        assertNotNull(persisted)
        assertEquals(listOf("song_1", "song_2", "song_3"), persisted?.queueIds)
    }

    @Test
    fun addToQueue_duplicateIsRejected() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        controller.addToQueue("song_1")

        val state = controller.state.value
        assertEquals(2, state.queue.size)
        assertEquals(listOf("song_1", "song_2"), state.queue.map { it.id })
    }

    @Test
    fun playSong_whenNotInQueue_insertsInConsistentOrderAndPersists() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        controller.playSong("song_3")

        val state = controller.state.value
        assertEquals(3, state.queue.size)
        assertEquals("song_3", state.currentSong?.id)
        // song_3 was inserted after song_1 (the current item at index 0)
        assertEquals(listOf("song_1", "song_3", "song_2"), state.queue.map { it.id })

        val persisted = awaitPersistedQueue(listOf("song_1", "song_3", "song_2"))
        assertNotNull(persisted)
        assertEquals(listOf("song_1", "song_3", "song_2"), persisted?.queueIds)
        assertEquals("song_3", persisted?.currentSongId)
    }

    @Test
    fun playSong_whenAlreadyInQueue_seeksAndPreservesQueue() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        controller.playSong("song_2")

        val state = controller.state.value
        assertEquals(2, state.queue.size)
        assertEquals("song_2", state.currentSong?.id)
        assertEquals(listOf("song_1", "song_2"), state.queue.map { it.id })
    }

    @Test
    fun playNext_movesExistingSongImmediatelyAfterCurrent() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)
        controller.playNext("song_3")

        val state = controller.state.value
        assertEquals(3, state.queue.size)
        assertEquals("song_1", state.currentSong?.id)
        assertEquals(listOf("song_1", "song_3", "song_2"), state.queue.map { it.id })

        val persisted = awaitPersistedQueue(listOf("song_1", "song_3", "song_2"))
        assertNotNull(persisted)
        assertEquals(listOf("song_1", "song_3", "song_2"), persisted?.queueIds)
    }

    @Test
    fun restorePlaybackState_whenCurrentSongDeleted_fallsBackWithZeroPosition() = runBlocking {
        persistence.save(
            PersistedPlaybackState(
                currentSongId = "deleted_song",
                positionMs = 60000L,
                isPlaying = false,
                shuffleEnabled = false,
                shuffleMode = ShuffleMode.RANDOM,
                repeatMode = RepeatMode.ALL,
                queueIds = listOf("deleted_song", "song_1", "song_2")
            )
        )

        val newController = PlaybackController(service)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 2000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (newController.state.value.queue.isNotEmpty()) break
            kotlinx.coroutines.delay(20)
        }

        val state = newController.state.value
        assertEquals(2, state.queue.size)
        assertEquals(listOf("song_1", "song_2"), state.queue.map { it.id })
        assertEquals("song_1", state.currentSong?.id)
        assertEquals(0L, state.positionMs)

        newController.release()
    }

    @Test
    fun setShuffle_togglePreservesQueueMembershipAndLogicalOrdering() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)

        controller.setShuffle(true, ShuffleMode.RANDOM)
        assertTrue(controller.state.value.shuffleEnabled)
        assertEquals(listOf("song_1", "song_2", "song_3"), controller.state.value.queue.map { it.id })

        controller.setShuffle(false, ShuffleMode.RANDOM)
        org.junit.Assert.assertFalse(controller.state.value.shuffleEnabled)
        assertEquals(listOf("song_1", "song_2", "song_3"), controller.state.value.queue.map { it.id })
    }

    @Test
    fun setShuffle_withInvalidSuppliedOrder_fallsBackSafelyWithoutCrashing() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)

        // Pass invalid order with incorrect size (e.g. from stale client state)
        controller.setShuffle(true, ShuffleMode.RANDOM, suppliedOrder = intArrayOf(0, 1))
        assertTrue(controller.state.value.shuffleEnabled)
        assertEquals(3, controller.state.value.queue.size)
    }

    @Test
    fun initialQueueEmpty_hasNullCurrentSong() {
        val state = controller.state.value
        assertEquals(0, state.queue.size)
        assertNull(state.currentSong)
    }

    private fun getShuffleTraversalIndices(player: androidx.media3.common.Player): List<Int> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val result = mutableListOf<Int>()
        var windowIndex = timeline.getFirstWindowIndex(player.shuffleModeEnabled)
        while (windowIndex != androidx.media3.common.C.INDEX_UNSET) {
            result.add(windowIndex)
            windowIndex = timeline.getNextWindowIndex(
                windowIndex,
                androidx.media3.common.Player.REPEAT_MODE_OFF,
                player.shuffleModeEnabled
            )
        }
        return result
    }

    @Test
    fun scenario1_addToQueue_whileShuffleEnabled() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)
        controller.setShuffle(true, ShuffleMode.RANDOM)

        assertEquals("song_1", controller.state.value.currentSong?.id)
        assertTrue(controller.state.value.shuffleEnabled)

        controller.addToQueue("song_4")

        val playerMediaIds = (0 until controller.player.mediaItemCount).map {
            controller.player.getMediaItemAt(it).mediaId
        }
        assertEquals(listOf("song_1", "song_2", "song_3", "song_4"), playerMediaIds)
        assertEquals(4, playerMediaIds.distinct().size)

        val stateIds = controller.state.value.queue.map { it.id }
        assertEquals(listOf("song_1", "song_2", "song_3", "song_4"), stateIds)
        assertEquals(4, stateIds.distinct().size)

        assertTrue(controller.state.value.shuffleEnabled)
        assertTrue(controller.player.shuffleModeEnabled)

        val traversalIndices = getShuffleTraversalIndices(controller.player)
        assertEquals(4, traversalIndices.size)
        assertEquals((0 until 4).toSet(), traversalIndices.toSet())
        assertEquals(4, traversalIndices.distinct().size)

        assertEquals("song_1", controller.state.value.currentSong?.id)
        assertEquals("song_1", controller.player.currentMediaItem?.mediaId)
    }

    @Test
    fun scenario2_playNext_whileShuffleEnabled() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3", "song_4"), 1, false)
        controller.setShuffle(true, ShuffleMode.RANDOM)

        assertEquals("song_2", controller.state.value.currentSong?.id)
        assertEquals("song_2", controller.player.currentMediaItem?.mediaId)
        assertTrue(controller.state.value.shuffleEnabled)

        controller.playNext("song_4")

        val playerMediaIds = (0 until controller.player.mediaItemCount).map {
            controller.player.getMediaItemAt(it).mediaId
        }
        assertEquals(1, playerMediaIds.count { it == "song_4" })

        val expectedSet = setOf("song_1", "song_2", "song_3", "song_4")
        assertEquals(expectedSet, playerMediaIds.toSet())
        assertEquals(4, playerMediaIds.distinct().size)

        assertEquals(listOf("song_1", "song_2", "song_4", "song_3"), playerMediaIds)
        assertEquals(listOf("song_1", "song_2", "song_4", "song_3"), controller.state.value.queue.map { it.id })

        val traversalIndices = getShuffleTraversalIndices(controller.player)
        assertEquals(4, traversalIndices.size)
        assertEquals((0 until 4).toSet(), traversalIndices.toSet())
        assertEquals(4, traversalIndices.distinct().size)

        assertEquals("song_2", controller.state.value.currentSong?.id)
        assertEquals("song_2", controller.player.currentMediaItem?.mediaId)

        val traversedMediaIds = traversalIndices.map { controller.player.getMediaItemAt(it).mediaId }
        assertEquals(expectedSet, traversedMediaIds.toSet())
        assertEquals(4, traversedMediaIds.distinct().size)
    }

    @Test
    fun scenario3_playSong_outsideQueue_whileShuffleEnabled() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)
        controller.setShuffle(true, ShuffleMode.RANDOM)

        assertEquals("song_1", controller.state.value.currentSong?.id)
        assertTrue(controller.state.value.shuffleEnabled)

        controller.playSong("song_4")

        val playerMediaIds = (0 until controller.player.mediaItemCount).map {
            controller.player.getMediaItemAt(it).mediaId
        }
        assertEquals(1, playerMediaIds.count { it == "song_4" })
        assertEquals(4, playerMediaIds.size)
        assertEquals(4, playerMediaIds.distinct().size)

        assertEquals("song_4", controller.state.value.currentSong?.id)
        assertEquals("song_4", controller.player.currentMediaItem?.mediaId)

        val expectedSet = setOf("song_1", "song_2", "song_3", "song_4")
        assertEquals(expectedSet, playerMediaIds.toSet())

        assertTrue(controller.state.value.shuffleEnabled)
        assertTrue(controller.player.shuffleModeEnabled)

        val traversalIndices = getShuffleTraversalIndices(controller.player)
        assertEquals(4, traversalIndices.size)
        assertEquals((0 until 4).toSet(), traversalIndices.toSet())
        assertEquals(4, traversalIndices.distinct().size)

        val stateIds = controller.state.value.queue.map { it.id }
        assertEquals(expectedSet, stateIds.toSet())
        assertEquals(4, stateIds.distinct().size)

        val persisted = awaitPersistedQueue(playerMediaIds)
        assertNotNull(persisted)
        assertEquals(expectedSet, persisted?.queueIds?.toSet())
        assertEquals("song_4", persisted?.currentSongId)
    }

    @Test
    fun scenario4_shuffleToggle_afterQueueMutation() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)
        org.junit.Assert.assertFalse(controller.state.value.shuffleEnabled)

        controller.addToQueue("song_4")
        assertEquals(listOf("song_1", "song_2", "song_3", "song_4"), controller.state.value.queue.map { it.id })

        controller.setShuffle(true, ShuffleMode.RANDOM)
        assertTrue(controller.state.value.shuffleEnabled)

        controller.playNext("song_3")

        controller.setShuffle(false, ShuffleMode.RANDOM)
        org.junit.Assert.assertFalse(controller.state.value.shuffleEnabled)

        val expectedOrder = listOf("song_1", "song_3", "song_2", "song_4")
        val stateQueue = controller.state.value.queue.map { it.id }
        val playerQueue = (0 until controller.player.mediaItemCount).map {
            controller.player.getMediaItemAt(it).mediaId
        }

        assertEquals(expectedOrder, stateQueue)
        assertEquals(expectedOrder, playerQueue)
        assertEquals(4, stateQueue.distinct().size)
        assertEquals(setOf("song_1", "song_2", "song_3", "song_4"), stateQueue.toSet())
    }
}
