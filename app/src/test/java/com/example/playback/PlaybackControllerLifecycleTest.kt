package com.example.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.OniDatabase
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PlaybackControllerLifecycleTest {

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

    @Test
    fun release_isIdempotent() {
        // Calling release multiple times sequentially must not crash or throw exceptions
        controller.release()
        controller.release()
        controller.release()
    }

    @Test
    fun operationsAfterRelease_doNotMutateStateOrEmit() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        val stateBeforeRelease = controller.state.value

        controller.release()

        // Attempt various mutations after release
        controller.pause()
        controller.resume()
        controller.toggle()
        controller.seek(50000L)
        controller.next()
        controller.previous()
        controller.setRepeat(true)
        controller.setDelay(15)
        controller.cancelDelay()
        controller.triggerDelay()
        controller.setShuffle(true, ShuffleMode.DISCOVER)
        controller.addToQueue("song_2")
        controller.playNext("song_2")
        controller.updateSong("song_1")
        controller.toggleFavorite()

        val stateAfterRelease = controller.state.value
        assertEquals(stateBeforeRelease.currentSong?.id, stateAfterRelease.currentSong?.id)
        assertEquals(stateBeforeRelease.repeatMode, stateAfterRelease.repeatMode)
        assertEquals(stateBeforeRelease.shuffleEnabled, stateAfterRelease.shuffleEnabled)
        assertEquals(stateBeforeRelease.autoNextCountdownSeconds, stateAfterRelease.autoNextCountdownSeconds)
    }

    @Test
    fun restoreInterruptedByRelease_doesNotMutateState() = runBlocking {
        persistence.save(
            PersistedPlaybackState(
                currentSongId = "song_2",
                positionMs = 120000L,
                isPlaying = false,
                shuffleEnabled = true,
                shuffleMode = ShuffleMode.FAVORITES_BOOST,
                repeatMode = RepeatMode.ONE,
                queueIds = listOf("song_1", "song_2")
            )
        )

        // Instantiate new controller and immediately release it before IO restoration can settle
        val newController = PlaybackController(service)
        newController.release()

        // Wait a brief moment for any scheduled IO work to attempt running
        kotlinx.coroutines.delay(100)

        // Verify player and state are not active / mutated after release
        assertEquals(null, newController.state.value.currentSong)
        assertEquals(false, newController.state.value.isPlaying)
    }

    @Test
    fun delayedCallback_afterRelease_doesNotMutatePlayer() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        controller.setDelay(2) // 2 second delay

        controller.release()

        // Allow any timer ticks to elapse
        kotlinx.coroutines.delay(2500)

        // Verify controller state remained unchanged and player is released
        assertEquals(null, controller.state.value.autoNextCountdownSeconds)
    }

    @Test
    fun positionPersistenceLoop_stopsAfterRelease() = runBlocking {
        controller.setQueue(listOf("song_1", "song_2"), 0, false)
        val initialPosition = persistence.load()?.positionMs ?: 0L

        controller.release()

        // Wait past 1000ms interval of position loop
        kotlinx.coroutines.delay(1500)

        // Check that position is not written post-release
        val state = persistence.load()
        assertEquals(initialPosition, state?.positionMs ?: 0L)
    }

    @Test
    fun audioEffectsDelegation_afterRelease_returnsSafeDefaults() {
        controller.release()

        controller.setBand(0, 5f)
        controller.setBass(50f)
        controller.setVirtualizer(50f)
        controller.applyPreset(
            EqualizerPresetEntity(
                name = "Rock",
                isCustom = false,
                band60Hz = 4f,
                band230Hz = 2f,
                band910Hz = -1f,
                band4kHz = 2f,
                band14kHz = 5f,
                bassBoost = 20f,
                virtualizer = 15f
            )
        )

        assertEquals(0f, controller.getBass(), 0.001f)
        assertEquals(0f, controller.getVirtualizer(), 0.001f)
        assertEquals(5, controller.getBandGains().size)
    }

    @Test
    fun concurrentRelease_isThreadSafeAndAtomic() = runBlocking {
        val newController = PlaybackController(service)
        val threads = (1..10).map {
            Thread {
                newController.release()
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(newController.isReleased)
    }
}

