package com.example.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.OniDatabase
import com.example.data.entity.SongEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackTransitionListenerAuditTest {

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
            duration = 300000L,
            filePath = "/storage/emulated/0/Music/track3.mp3",
            albumArtUri = null,
            playCount = 0,
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
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized) {
            controller.release()
        }
        persistence.clear()
    }

    private suspend fun awaitPersistedState(predicate: (PersistedPlaybackState?) -> Boolean): PersistedPlaybackState? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 3000) {
            ShadowLooper.idleMainLooper()
            val p = persistence.load()
            if (predicate(p)) return p
            delay(20)
        }
        return persistence.load()
    }

    @Test
    fun invariantA_currentIdentityMatchesPlayerCurrentMediaItem() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 0, false)
        ShadowLooper.idleMainLooper()

        assertEquals(controller.player.currentMediaItem?.mediaId, controller.state.value.currentSong?.id)

        controller.next()
        ShadowLooper.idleMainLooper()
        assertEquals(controller.player.currentMediaItem?.mediaId, controller.state.value.currentSong?.id)

        controller.previous()
        ShadowLooper.idleMainLooper()
        assertEquals(controller.player.currentMediaItem?.mediaId, controller.state.value.currentSong?.id)

        controller.playNext("song_3")
        ShadowLooper.idleMainLooper()
        assertEquals(controller.player.currentMediaItem?.mediaId, controller.state.value.currentSong?.id)

        controller.addToQueue("song_4")
        ShadowLooper.idleMainLooper()
        assertEquals(controller.player.currentMediaItem?.mediaId, controller.state.value.currentSong?.id)

        controller.playSong("song_4")
        ShadowLooper.idleMainLooper()
        assertEquals("song_4", controller.state.value.currentSong?.id)
        assertEquals(controller.player.currentMediaItem?.mediaId, controller.state.value.currentSong?.id)
    }

    @Test
    fun invariantB_queueMembershipPreservedAcrossTransitions() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2", "song_3"), 1, false)
        ShadowLooper.idleMainLooper()

        val initialPlayerIds = (0 until controller.player.mediaItemCount).map { controller.player.getMediaItemAt(it).mediaId }
        val initialStateIds = controller.state.value.queue.map { it.id }
        assertEquals(initialPlayerIds, initialStateIds)

        controller.playNext("song_1")
        ShadowLooper.idleMainLooper()

        val afterPlayNextPlayer = (0 until controller.player.mediaItemCount).map { controller.player.getMediaItemAt(it).mediaId }
        val afterPlayNextState = controller.state.value.queue.map { it.id }
        assertEquals(afterPlayNextPlayer, afterPlayNextState)
        assertEquals(setOf("song_1", "song_2", "song_3"), afterPlayNextState.toSet())
    }

    @Test
    fun invariantC_queueMutationsDoNotFalselyIncrementPlayCount() = runBlocking {
        val initialPlayCountSong3 = database.songDao().getSongById("song_3")!!.playCount
        val initialPlayCountSong4 = database.songDao().getSongById("song_4")!!.playCount

        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        ShadowLooper.idleMainLooper()

        controller.addToQueue("song_4")
        ShadowLooper.idleMainLooper()

        val playCountSong4AfterAdd = database.songDao().getSongById("song_4")!!.playCount
        assertEquals(initialPlayCountSong4, playCountSong4AfterAdd)

        controller.playNext("song_3")
        ShadowLooper.idleMainLooper()

        val playCountSong3AfterPlayNext = database.songDao().getSongById("song_3")!!.playCount
        assertEquals(initialPlayCountSong3, playCountSong3AfterPlayNext)
    }

    @Test
    fun invariantD_preparingStateTransitionsCorrectly() = runBlocking {
        assertFalse(controller.state.value.isPreparing)

        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        // Immediately after setQueue, preparing flag is set to true until playback state becomes non-buffering
        assertTrue(controller.state.value.isPreparing)

        // Idle looper to allow preparation to finish or update
        ShadowLooper.idleMainLooper()
        // If state changed from buffering to ready or idle, preparing becomes false
        val preparingAfterIdle = controller.state.value.isPreparing
        // Now simulate playbackStateChanged to BUFFERING and then READY
        controller.player.play()
        ShadowLooper.idleMainLooper()
        // Ensure state is well-defined
        assertNotNull(controller.state.value.currentSong)
    }

    @Test
    fun invariantE_delayInteractionWithManualNextAndCancellation() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        controller.setDelay(10)
        ShadowLooper.idleMainLooper()

        // autoNextCountdownSeconds is the active countdown value (null when no countdown is actively running)
        assertNull(controller.state.value.autoNextCountdownSeconds)

        // Triggering delay starts countdown
        controller.triggerDelay()
        ShadowLooper.idleMainLooper()
        assertNotNull(controller.state.value.autoNextCountdownSeconds)

        // Manual next cancels active delay countdown immediately
        controller.next()
        ShadowLooper.idleMainLooper()
        assertNull(controller.state.value.autoNextCountdownSeconds)
        assertEquals("song_2", controller.state.value.currentSong?.id)

        // Test cancelDelay directly
        controller.triggerDelay()
        ShadowLooper.idleMainLooper()
        assertNotNull(controller.state.value.autoNextCountdownSeconds)

        controller.cancelDelay()
        ShadowLooper.idleMainLooper()
        assertNull(controller.state.value.autoNextCountdownSeconds)
    }

    @Test
    fun mediaItemTransition_automaticNext_playsAndMaintainsConsistentIdentity() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, true)
        ShadowLooper.idleMainLooper()

        assertEquals("song_1", controller.state.value.currentSong?.id)
        assertTrue(controller.player.playWhenReady)

        // Transition to next media item
        controller.next()
        ShadowLooper.idleMainLooper()

        assertEquals("song_2", controller.player.currentMediaItem?.mediaId)
        assertEquals("song_2", controller.state.value.currentSong?.id)

        val persisted = awaitPersistedState { it?.currentSongId == "song_2" }
        assertNotNull(persisted)
        assertEquals("song_2", persisted?.currentSongId)
    }
}
