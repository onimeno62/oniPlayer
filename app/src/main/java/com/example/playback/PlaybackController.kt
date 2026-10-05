package com.example.playback

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.common.util.UnstableApi
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.example.data.database.OniDatabase
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import com.example.data.preferences.PlayerSettings
import com.example.data.preferences.PlayerSettingsStore
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Service-owned playback engine. The Activity and ViewModels never own ExoPlayer. */
private val Context.playbackSettingsStore by preferencesDataStore(name = "oni_settings")

@OptIn(UnstableApi::class)
class PlaybackController(private val service: MediaSessionService) {
    companion object {
        const val SET_QUEUE = "com.example.oniplayer.SET_QUEUE"
        const val PLAY_SONG = "com.example.oniplayer.PLAY_SONG"
        const val SET_SHUFFLE = "com.example.oniplayer.SET_SHUFFLE"
        const val SET_REPEAT = "com.example.oniplayer.SET_REPEAT"
        const val ADD_TO_QUEUE = "com.example.oniplayer.ADD_TO_QUEUE"
        const val PLAY_NEXT = "com.example.oniplayer.PLAY_NEXT"
        const val UPDATE_SONG = "com.example.oniplayer.UPDATE_SONG"
        const val TOGGLE_FAVORITE = "com.example.oniplayer.TOGGLE_FAVORITE"
        const val EQ_BAND = "com.example.oniplayer.EQ_BAND"
        const val BASS = "com.example.oniplayer.BASS"
        const val VIRTUALIZER = "com.example.oniplayer.VIRTUALIZER"
        const val PRESET = "com.example.oniplayer.PRESET"
        const val SET_DELAY = "com.example.oniplayer.SET_DELAY"
        const val CANCEL_DELAY = "com.example.oniplayer.CANCEL_DELAY"
        const val TRIGGER_DELAY = "com.example.oniplayer.TRIGGER_DELAY"
        const val NEXT = "com.example.oniplayer.NEXT"
        const val PREVIOUS = "com.example.oniplayer.PREVIOUS"
        const val REMOVE_FROM_QUEUE = "com.example.oniplayer.REMOVE_FROM_QUEUE"
        const val MOVE_IN_QUEUE = "com.example.oniplayer.MOVE_IN_QUEUE"
        const val END_SESSION = "com.example.oniplayer.END_SESSION"
        const val SET_CROSSFADE_ENABLED = "com.example.oniplayer.SET_CROSSFADE_ENABLED"
        const val SET_CROSSFADE_DURATION = "com.example.oniplayer.SET_CROSSFADE_DURATION"

        const val SONG_IDS = "song_ids"
        const val SONG_ID = "song_id"
        const val START_INDEX = "start_index"
        const val PLAY = "play"
        const val ENABLED = "enabled"
        const val MODE = "mode"
        const val VALUE = "value"
        const val BAND = "band"
        const val DELAY_SECONDS = "delay_seconds"
        const val PRESET_DATA = "preset"
        const val SHUFFLE_ENABLED = "shuffle_enabled"
        const val SHUFFLE_MODE = "shuffle_mode"
        const val SHUFFLE_TYPE = "shuffle_type"
        const val REPEAT_MODE = "repeat_mode"
        const val SHUFFLE_ORDER = "shuffle_order"
        const val FROM_INDEX = "from_index"
        const val TO_INDEX = "to_index"
        const val CROSSFADE_ENABLED = "crossfade_enabled"
        const val CROSSFADE_DURATION_SECONDS = "crossfade_duration_seconds"

        private const val RESTART_ON_PREVIOUS_THRESHOLD_MS = 3_000L
    }

    private val context: Context = service.applicationContext
    private val dao = OniDatabase.getDatabase(context).songDao()
    private val persistence = PlaybackPersistence(context)
    private val audioEffectsController = AudioEffectsController(context)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    internal val commandMutex = Mutex()
    private val _isReleased = AtomicBoolean(false)
    private val persistenceRevision = AtomicLong(0L)
    val isReleased: Boolean get() = _isReleased.get()
    private val playbackDelayController = PlaybackDelayController(
        scope = scope,
        onDelayFinished = {
            if (isReleased) return@PlaybackDelayController
            delayedAdvancePending = true
            player.seekToNextMediaItem()
            player.play()
        }
    )

    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildVideoRenderers(
            context: Context,
            extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            enableDecoderFallback: Boolean,
            eventHandler: Handler,
            eventListener: VideoRendererEventListener,
            allowedVideoJoiningTimeMs: Long,
            out: ArrayList<Renderer>
        ) {
        }

        override fun buildCameraMotionRenderers(
            context: Context,
            extensionRendererMode: Int,
            out: ArrayList<Renderer>
        ) {
        }

        override fun buildImageRenderers(
            context: Context,
            out: ArrayList<Renderer>
        ) {
        }

        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }
    }.apply {
        setEnableDecoderFallback(true)
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
    }

    val player = ExoPlayer.Builder(context, renderersFactory).setHandleAudioBecomingNoisy(true).build()
    val mediaSession = MediaSession.Builder(service, player)
        .setId("oni_session_${System.identityHashCode(this)}")
        .setCallback(SessionCallback())
        .setMediaButtonPreferences(
            listOf(
                CommandButton.Builder(CommandButton.ICON_STOP)
                    .setDisplayName("End oniPlayer")
                    .setSessionCommand(SessionCommand(END_SESSION, Bundle.EMPTY))
                    .build()
            )
        )
        .build()

    private val _state = MutableStateFlow(PlaybackState(null, false, 0, 0, 0, 0f, false, null, false, ShuffleMode.RANDOM, RepeatMode.ALL, emptyList()))
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var baseQueue = emptyList<SongEntity>()
    private var shuffleEnabled = false
    private var shuffleMode = ShuffleMode.RANDOM
    private var shuffleType = ShuffleType.SONGS
    private var repeatMode = RepeatMode.ALL
    private var preparing = false

    private var preShuffleAllQueue: List<String> = emptyList()

    private var publishSuppressed = false

    private var playerSettings = PlayerSettings()
    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 0L
    private var crossfadeJob: Job? = null
    private var crossfadeAtBoundary = false
    private var delayedAdvancePending = false
    private var appliedAudioFocus: Boolean? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var loudnessSessionId = C.AUDIO_SESSION_ID_UNSET
    private val musicAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isReleased) return
            if (!isPlaying && !crossfadeAtBoundary) {
                player.volume = 1f
                crossfadeJob?.cancel()
                crossfadeJob = null
            }
            publish()
            savePlaybackState()
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (isReleased) return
            preparing = state == Player.STATE_BUFFERING
            publish()
            if (state == Player.STATE_ENDED && playbackDelayController.hasDelay && player.hasNextMediaItem()) {
                crossfadeAtBoundary = true
                player.volume = 0f
                playbackDelayController.startDelay()
            }
            savePlaybackState()
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            if (isReleased) return
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !delayedAdvancePending) {
                cancelDelay(publishState = false)
                crossfadeAtBoundary = false
                crossfadeJob?.cancel()
                player.volume = 1f
            } else if (delayedAdvancePending || (crossfadeEnabled && crossfadeDurationMs > 0L)) {
                delayedAdvancePending = false
                crossfadeAtBoundary = true
                player.volume = 0f
                startFadeIn()
            } else {
                crossfadeAtBoundary = false
                player.volume = 1f
            }
            publish()
            if (item != null && player.playWhenReady && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                scope.launch { recordPlay(item.mediaId) }
            }
            savePlaybackState()
        }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (isReleased) return
            val itemChanged = oldPosition.mediaItem?.mediaId != newPosition.mediaItem?.mediaId
            if (!itemChanged) publish()
            savePlaybackState()
        }
        override fun onAudioSessionIdChanged(id: Int) {
            if (isReleased) return
            audioEffectsController.attachToAudioSession(id)
            applyLoudnessBoost(id)
            publish()
        }
        override fun onRepeatModeChanged(playerRepeatMode: Int) {
            if (isReleased) return
            val reconciled = RepeatMode.reconcile(playerRepeatMode, repeatMode, keepAllWhenOff = playbackDelayController.hasDelay)
            if (reconciled != repeatMode) {
                repeatMode = reconciled
                player.pauseAtEndOfMediaItems = repeatMode == RepeatMode.SINGLE || playbackDelayController.hasDelay
            }
            publish()
            savePlaybackState()
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (isReleased) return
            this@PlaybackController.shuffleEnabled = shuffleModeEnabled
            publish()
            savePlaybackState()
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            if (isReleased) return
            preparing = false
            Log.e("PlaybackController", "ExoPlayer error: ${error.errorCodeName}", error)
            publish()
        }
    }

    init {
        player.addListener(playerListener)
        scope.launch {
            PlayerSettingsStore.settings(context).collect { settings ->
                if (!isReleased) applyPlayerSettings(settings)
            }
        }
        scope.launch {
            while (isActive && !isReleased) {
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    withContext(Dispatchers.IO) {
                        if (!isReleased) persistence.savePosition(pos)
                    }
                }
                delay(5000)
            }
        }
        scope.launch {
            playbackDelayController.countdown.collect {
                if (!isReleased) publish()
            }
        }
        scope.launch {
            context.playbackSettingsStore.data.collect { prefs ->
                if (isReleased) return@collect
                crossfadeEnabled = prefs[booleanPreferencesKey("crossfade_enabled")] ?: false
                crossfadeDurationMs = (prefs[intPreferencesKey("crossfade_duration_seconds")] ?: 5)
                    .coerceIn(0, 30) * 1000L
            }
        }
        scope.launch {
            while (isActive && !isReleased) {
                updateCrossfadeVolume()
                delay(50L)
            }
        }
        restorePlaybackState()
    }

    private fun applyPlayerSettings(settings: PlayerSettings) {
        playerSettings = settings
        try {
            val params = PlaybackParameters(settings.playbackSpeed, settings.playbackPitch)
            if (player.playbackParameters != params) player.playbackParameters = params
            if (player.skipSilenceEnabled != settings.skipSilence) player.skipSilenceEnabled = settings.skipSilence
            if (appliedAudioFocus != settings.handleAudioFocus) {
                player.setAudioAttributes(musicAudioAttributes, settings.handleAudioFocus)
                appliedAudioFocus = settings.handleAudioFocus
            }
            player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
        } catch (t: Throwable) {
            Log.w("PlaybackController", "Failed to apply player settings", t)
        }
        applyLoudnessBoost(player.audioSessionId)
    }

    private fun applyLoudnessBoost(sessionId: Int) {
        val gainMb = (playerSettings.loudnessBoostDb * 100f).toInt()
        try {
            if (gainMb <= 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) {
                loudnessEnhancer?.setEnabled(false)
                return
            }
            if (loudnessEnhancer == null || loudnessSessionId != sessionId) {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                loudnessSessionId = sessionId
            }
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.setEnabled(true)
        } catch (t: Throwable) {
            Log.w("PlaybackController", "LoudnessEnhancer unavailable", t)
            runCatching { loudnessEnhancer?.release() }
            loudnessEnhancer = null
            loudnessSessionId = C.AUDIO_SESSION_ID_UNSET
        }
    }

    suspend fun setQueue(ids: List<String>, startIndex: Int, playImmediately: Boolean) {
        if (isReleased) return
        val songs = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext emptyList()
            val byId = dao.getSongsByIds(ids).associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }
        if (isReleased || songs.isEmpty()) return
        baseQueue = songs
        preShuffleAllQueue = emptyList()
        val sourceIndex = startIndex.coerceIn(0, songs.lastIndex)
        val startId = songs[sourceIndex].id
        preparing = true
        player.setMediaItems(songs.map(::mediaItem), sourceIndex, 0L)
        if (shuffleEnabled && shuffleType == ShuffleType.ALL) {
            expandToLibrary()
        }
        if (shuffleEnabled) {
            player.setShuffleOrder(buildShuffleOrder(currentQueue(), startId))
        } else {
            player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(player.mediaItemCount))
        }
        player.setShuffleModeEnabled(shuffleEnabled)
        applyRepeatMode()
        player.prepare()
        if (playImmediately) {
            player.play()
            scope.launch { recordPlay(startId) }
        } else player.pause()
        publish(currentQueue())
        savePlaybackState()
    }

    suspend fun playSong(id: String) {
        if (isReleased) return
        val currentItem = player.currentMediaItem
        if (currentItem?.mediaId == id) {
            if (!player.isPlaying) {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
            return
        }
        val index = indexOf(id)
        if (index >= 0) {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.seekTo(index, 0L)
            player.play()
            scope.launch { recordPlay(id) }
            savePlaybackState()
            return
        }
        val song = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext null
            dao.getSongById(id)
        } ?: return
        if (isReleased) return
        val insertIndex = if (player.mediaItemCount == 0) 0 else (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        val currentId = player.currentMediaItem?.mediaId ?: if (player.mediaItemCount > 0) _state.value.currentSong?.id else null
        val baseIndex = baseQueue.indexOfFirst { it.id == currentId }
        baseQueue = if (baseIndex >= 0) {
            baseQueue.toMutableList().apply { add(baseIndex + 1, song) }
        } else {
            baseQueue + song
        }
        player.addMediaItem(insertIndex, mediaItem(song))
        player.seekTo(insertIndex, 0L)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
        scope.launch { recordPlay(id) }
        publish()
        savePlaybackState()
    }

    fun pause() { if (!isReleased) player.pause() }
    fun resume() {
        if (!isReleased) {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }
    fun toggle() {
        if (!isReleased) {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
        }
    }
    fun seek(positionMs: Long) { if (!isReleased) player.seekTo(positionMs.coerceAtLeast(0)) }
    fun next() {
        if (!isReleased) {
            cancelDelay(publishState = false)
            crossfadeAtBoundary = false
            delayedAdvancePending = false
            crossfadeJob?.cancel()
            player.volume = 1f
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.seekToNextMediaItem()
            player.play()
        }
    }
    fun previous() {
        if (!isReleased) {
            cancelDelay(publishState = false)
            crossfadeAtBoundary = false
            crossfadeJob?.cancel()
            player.volume = 1f
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            if (playerSettings.rewindOnPrevious && player.currentPosition > RESTART_ON_PREVIOUS_THRESHOLD_MS) {
                player.seekTo(0L)
            } else {
                player.seekToPreviousMediaItem()
            }
            player.play()
        }
    }
    fun stop() { if (!isReleased) player.stop() }

    suspend fun setShuffle(
        enabled: Boolean,
        mode: ShuffleMode,
        type: ShuffleType = shuffleType,
        suppliedOrder: IntArray? = null
    ) {
        if (isReleased) return
        val wasShuffleAll = shuffleEnabled && shuffleType == ShuffleType.ALL
        val willShuffleAll = enabled && type == ShuffleType.ALL
        shuffleEnabled = enabled
        shuffleMode = mode
        shuffleType = type
        if (player.mediaItemCount > 0) {
            if (willShuffleAll && !wasShuffleAll) {
                expandToLibrary()
            } else if (wasShuffleAll && !willShuffleAll) {
                restorePreShuffleAllQueue()
            }
            if (isReleased) return
            val currentId = player.currentMediaItem?.mediaId
            val count = player.mediaItemCount
            val isValidSuppliedOrder = type == ShuffleType.SONGS &&
                suppliedOrder != null &&
                suppliedOrder.size == count &&
                suppliedOrder.toSet().size == count &&
                suppliedOrder.all { it in 0 until count }
            val shuffleOrder = if (enabled) {
                if (isValidSuppliedOrder) {
                    ShuffleOrder.DefaultShuffleOrder(suppliedOrder, System.nanoTime())
                } else {
                    buildShuffleOrder(currentQueue(), currentId)
                }
            } else {
                ShuffleOrder.UnshuffledShuffleOrder(count)
            }
            player.setShuffleOrder(shuffleOrder)
            player.setShuffleModeEnabled(enabled)
        }
        publish(currentQueue())
        savePlaybackState()
    }

    private suspend fun expandToLibrary() {
        val currentId = player.currentMediaItem?.mediaId ?: return
        val before = currentQueue()
        val library = withContext(Dispatchers.IO) {
            if (isReleased) emptyList() else runCatching { dao.getAllSongs().first() }.getOrDefault(emptyList())
        }
        if (isReleased || library.isEmpty()) return
        val currentSong = before.find { it.id == currentId } ?: library.find { it.id == currentId } ?: return
        if (preShuffleAllQueue.isEmpty()) preShuffleAllQueue = before.map { it.id }
        replaceQueueKeepingCurrent(listOf(currentSong) + library.filter { it.id != currentId })
    }

    private suspend fun restorePreShuffleAllQueue() {
        val ids = preShuffleAllQueue
        preShuffleAllQueue = emptyList()
        if (ids.isEmpty()) return
        val currentId = player.currentMediaItem?.mediaId ?: return
        val songs = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext emptyList()
            val byId = dao.getSongsByIds(ids).associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }
        if (isReleased || songs.isEmpty()) return
        val target = if (songs.any { it.id == currentId }) {
            songs
        } else {
            val current = currentQueue().find { it.id == currentId } ?: return
            listOf(current) + songs
        }
        replaceQueueKeepingCurrent(target)
    }

    private fun replaceQueueKeepingCurrent(target: List<SongEntity>) {
        val currentId = player.currentMediaItem?.mediaId ?: return
        val pivot = target.indexOfFirst { it.id == currentId }
        if (pivot < 0) return
        publishSuppressed = true
        try {
            val currentIndex = player.currentMediaItemIndex
            val count = player.mediaItemCount
            if (currentIndex + 1 < count) player.removeMediaItems(currentIndex + 1, count)
            if (currentIndex > 0) player.removeMediaItems(0, currentIndex)
            val before = target.subList(0, pivot)
            val after = target.subList(pivot + 1, target.size)
            if (after.isNotEmpty()) player.addMediaItems(1, after.map(::mediaItem))
            if (before.isNotEmpty()) player.addMediaItems(0, before.map(::mediaItem))
            baseQueue = target
        } finally {
            publishSuppressed = false
        }
    }

    suspend fun addToQueue(id: String) {
        if (isReleased) return
        val song = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext null
            dao.getSongById(id)
        } ?: return
        if (isReleased || indexOf(id) >= 0) return
        baseQueue = baseQueue + song
        player.addMediaItem(mediaItem(song))
        publish(currentQueue())
        savePlaybackState()
    }

    /** Reorders the active Media3 playlist without interrupting the current track. */
    private fun endPlaybackSession() {
        if (isReleased) return
        player.stop()
        player.clearMediaItems()
        baseQueue = emptyList()
        publish(emptyList())
        service.stopSelf()
    }

    suspend fun moveInQueue(fromIndex: Int, toIndex: Int) {
        if (isReleased) return
        val count = player.mediaItemCount
        if (fromIndex !in 0 until count || toIndex !in 0 until count || fromIndex == toIndex) return
        player.moveMediaItem(fromIndex, toIndex)
        val reorderedIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val byId = baseQueue.associateBy { it.id }
        baseQueue = reorderedIds.mapNotNull { byId[it] }
        publish(currentQueue())
        savePlaybackState()
    }

    /** Removes a song from the queue. If it's currently playing, advances to next. */
    suspend fun removeFromQueue(id: String) {
        if (isReleased) return
        val index = indexOf(id)
        if (index < 0) return
        val isCurrentlyPlaying = player.currentMediaItem?.mediaId == id
        baseQueue = baseQueue.filter { it.id != id }
        if (isCurrentlyPlaying) {
            if (player.mediaItemCount <= 1) {
                player.stop()
                player.clearMediaItems()
                baseQueue = emptyList()
            } else {
                player.removeMediaItem(index)
            }
        } else {
            player.removeMediaItem(index)
        }
        publish(currentQueue())
        savePlaybackState()
    }

    suspend fun playNext(id: String) {
        val currentId = player.currentMediaItem?.mediaId ?: if (player.mediaItemCount > 0) _state.value.currentSong?.id else null
        if (isReleased || id == currentId) return
        val song = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext null
            dao.getSongById(id)
        } ?: return
        if (isReleased) return
        val old = indexOf(id)
        val currentIndex = if (player.mediaItemCount > 0) player.currentMediaItemIndex.coerceIn(0, player.mediaItemCount - 1) else 0
        val filtered = baseQueue.filterNot { it.id == id }
        val baseIndex = filtered.indexOfFirst { it.id == currentId }
        baseQueue = if (baseIndex >= 0) filtered.toMutableList().apply { add(baseIndex + 1, song) } else filtered + song

        val insertIndex = if (player.mediaItemCount == 0) {
            0
        } else if (old in 0 until currentIndex) {
            player.removeMediaItem(old)
            currentIndex.coerceAtMost(player.mediaItemCount)
        } else {
            if (old > currentIndex) player.removeMediaItem(old)
            (currentIndex + 1).coerceAtMost(player.mediaItemCount)
        }
        player.addMediaItem(insertIndex, mediaItem(song))
        publish(currentQueue())
        savePlaybackState()
    }

    suspend fun updateSong(id: String) {
        if (isReleased) return
        val song = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext null
            dao.getSongById(id)
        } ?: return
        if (isReleased) return
        val index = indexOf(id)
        if (index >= 0) player.replaceMediaItem(index, mediaItem(song))
        baseQueue = baseQueue.map { if (it.id == id) song else it }
        if (player.currentMediaItem?.mediaId == id) _state.value = _state.value.copy(currentSong = song)
        publish(currentQueue())
    }

    suspend fun toggleFavorite() {
        if (isReleased) return
        val id = player.currentMediaItem?.mediaId ?: return
        val song = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext null
            dao.getSongById(id)
        } ?: return
        if (isReleased) return
        withContext(Dispatchers.IO) {
            if (!isReleased) {
                dao.updateSong(song.copy(isFavorite = !song.isFavorite))
            }
        }
        if (isReleased) return
        updateSong(id)
    }

    fun setRepeat(one: Boolean) {
        setRepeatMode(if (one) RepeatMode.ONE else RepeatMode.ALL)
    }

    fun setRepeatMode(mode: RepeatMode) {
        if (isReleased) return
        repeatMode = mode
        applyRepeatMode()
        publish()
        savePlaybackState()
    }

    fun setDelay(seconds: Int) {
        if (isReleased) return
        playbackDelayController.setDelay(seconds)
        applyRepeatMode()
        publish()
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        if (isReleased) return
        crossfadeEnabled = enabled
        if (!enabled) {
            crossfadeJob?.cancel()
            crossfadeJob = null
            crossfadeAtBoundary = false
            player.volume = 1f
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        if (isReleased) return
        crossfadeDurationMs = seconds.coerceIn(0, 30) * 1000L
        if (crossfadeDurationMs == 0L) {
            crossfadeJob?.cancel()
            crossfadeJob = null
            crossfadeAtBoundary = false
            player.volume = 1f
        }
    }

    private fun updateCrossfadeVolume() {
        if (!crossfadeEnabled || crossfadeDurationMs <= 0L || !player.isPlaying || (playbackDelayController.hasDelay && crossfadeAtBoundary)) return
        val duration = player.duration
        if (duration <= 0L || duration == C.TIME_UNSET || !player.hasNextMediaItem()) return
        val remaining = duration - player.currentPosition
        if (remaining in 1L..crossfadeDurationMs) {
            player.volume = (remaining.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    private fun startFadeIn() {
        crossfadeJob?.cancel()
        if (!crossfadeEnabled || crossfadeDurationMs <= 0L) {
            crossfadeAtBoundary = false
            player.volume = 1f
            return
        }
        val duration = crossfadeDurationMs
        crossfadeJob = scope.launch {
            val startedAt = System.currentTimeMillis()
            while (isActive && !isReleased && player.isPlaying) {
                val progress = ((System.currentTimeMillis() - startedAt).toFloat() / duration).coerceIn(0f, 1f)
                player.volume = progress
                if (progress >= 1f) break
                delay(40L)
            }
            if (!isReleased) {
                player.volume = 1f
                crossfadeAtBoundary = false
            }
        }
    }

    fun cancelDelay(publishState: Boolean = true) {
        if (isReleased) return
        playbackDelayController.cancelDelay()
        delayedAdvancePending = false
        crossfadeAtBoundary = false
        crossfadeJob?.cancel()
        crossfadeJob = null
        player.volume = 1f
        if (publishState) publish()
    }

    fun triggerDelay() {
        if (isReleased) return
        if (playbackDelayController.hasDelay) playbackDelayController.startDelay() else next()
    }

    fun setBand(index: Int, gain: Float) { if (!isReleased) audioEffectsController.setBand(index, gain) }
    fun setBass(level: Float) { if (!isReleased) audioEffectsController.setBass(level) }
    fun setVirtualizer(level: Float) { if (!isReleased) audioEffectsController.setVirtualizer(level) }
    fun applyPreset(p: EqualizerPresetEntity) { if (!isReleased) audioEffectsController.applyPreset(p) }
    fun getBandGains(): FloatArray = if (!isReleased) audioEffectsController.getBandGains() else FloatArray(AudioEffectsController.NUM_BANDS)
    fun getBass(): Float = if (!isReleased) audioEffectsController.getBass() else 0f
    fun getVirtualizer(): Float = if (!isReleased) audioEffectsController.getVirtualizer() else 0f

    private suspend fun recordPlay(id: String) {
        if (isReleased) return
        val song = withContext(Dispatchers.IO) {
            if (isReleased) return@withContext null
            dao.getSongById(id)
        } ?: return
        if (isReleased) return
        withContext(Dispatchers.IO) {
            if (!isReleased) {
                dao.updateSong(song.copy(playCount = song.playCount + 1, lastPlayedTimestamp = System.currentTimeMillis()))
            }
        }
    }

    private fun buildShuffleOrder(queue: List<SongEntity>, currentId: String?): ShuffleOrder {
        if (queue.isEmpty() || queue.size != player.mediaItemCount) {
            return ShuffleOrder.DefaultShuffleOrder(player.mediaItemCount, System.nanoTime())
        }
        val indices = ShuffleCalculator.calculateOrder(queue, currentId, shuffleMode, shuffleType)
        return ShuffleOrder.DefaultShuffleOrder(indices, System.nanoTime())
    }

    private fun indexOf(id: String?) = if (id == null) -1 else (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == id } ?: -1
    private fun currentQueue(): List<SongEntity> {
        val byId = baseQueue.associateBy { it.id }
        return (0 until player.mediaItemCount).mapNotNull { i ->
            val mediaId = player.getMediaItemAt(i).mediaId
            byId[mediaId] ?: _state.value.queue.find { it.id == mediaId }
        }
    }

    private fun applyRepeatMode() {
        player.repeatMode = when {
            repeatMode == RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            repeatMode == RepeatMode.ALL && !playbackDelayController.hasDelay -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.pauseAtEndOfMediaItems = repeatMode == RepeatMode.SINGLE
    }

    private fun updateCurrentMetadata() {
        val i = player.currentMediaItemIndex
        val song = _state.value.currentSong ?: baseQueue.find { it.id == player.currentMediaItem?.mediaId } ?: return
        if (i in 0 until player.mediaItemCount) player.replaceMediaItem(i, mediaItem(song))
    }

    private fun mediaItem(song: SongEntity) = MediaItem.Builder().setMediaId(song.id).setUri(Uri.parse(song.filePath)).setMediaMetadata(
        MediaMetadata.Builder().setTitle(song.displayTitle).setArtist(song.displayArtist).setAlbumTitle(song.displayAlbum).setArtworkUri(song.albumArtUri?.let(Uri::parse))
            .setExtras(Bundle().apply {
                putBoolean(SHUFFLE_ENABLED, shuffleEnabled)
                putInt(SHUFFLE_MODE, shuffleMode.ordinal)
                putInt(SHUFFLE_TYPE, shuffleType.ordinal)
                putInt(REPEAT_MODE, repeatMode.ordinal)
            }).build()
    ).build()

    private fun savePlaybackState() {
        if (isReleased) return
        val revision = persistenceRevision.incrementAndGet()
        val currentId = player.currentMediaItem?.mediaId
        val currentPosition = player.currentPosition
        val isPlaying = player.isPlaying
        val queueIds = if (player.mediaItemCount > 0) {
            (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        } else {
            baseQueue.map { it.id }
        }
        val currentShuffleEnabled = shuffleEnabled
        val currentShuffleMode = shuffleMode
        val currentShuffleType = shuffleType
        val currentRepeatMode = repeatMode
        val currentPreShuffleAll = preShuffleAllQueue
        scope.launch(Dispatchers.IO) {
            if (isReleased || revision != persistenceRevision.get()) return@launch
            persistence.save(
                PersistedPlaybackState(
                    currentSongId = currentId,
                    positionMs = currentPosition,
                    isPlaying = isPlaying,
                    shuffleEnabled = currentShuffleEnabled,
                    shuffleMode = currentShuffleMode,
                    repeatMode = currentRepeatMode,
                    queueIds = queueIds,
                    shuffleType = currentShuffleType,
                    preShuffleAllQueueIds = currentPreShuffleAll
                )
            )
        }
    }

    private fun restorePlaybackState() {
        scope.launch {
            val persisted = withContext(Dispatchers.IO) {
                if (isReleased) return@withContext null
                persistence.load()
            } ?: return@launch
            if (isReleased) return@launch
            val rememberPosition = runCatching { PlayerSettingsStore.current(context).rememberPosition }.getOrDefault(true)
            if (isReleased) return@launch
            val currentSongId = persisted.currentSongId?.takeIf { it.isNotEmpty() }
                ?: persisted.queueIds.firstOrNull()
                ?: return@launch
            val queueIds = persisted.queueIds.ifEmpty { listOf(currentSongId) }
            val songs = withContext(Dispatchers.IO) {
                if (isReleased) return@withContext emptyList()
                val byId = dao.getSongsByIds(queueIds).associateBy { it.id }
                queueIds.mapNotNull { byId[it] }
            }
            if (isReleased || songs.isEmpty()) return@launch
            baseQueue = songs
            shuffleEnabled = persisted.shuffleEnabled
            shuffleMode = persisted.shuffleMode
            shuffleType = persisted.shuffleType
            repeatMode = persisted.repeatMode
            preShuffleAllQueue = persisted.preShuffleAllQueueIds
            val matchedIndex = songs.indexOfFirst { it.id == currentSongId }
            val currentIndex = if (matchedIndex >= 0) matchedIndex else 0
            val restoredPositionMs = if (matchedIndex >= 0 && rememberPosition) persisted.positionMs else 0L
            val actualCurrentId = songs[currentIndex].id
            preparing = false
            player.setMediaItems(songs.map(::mediaItem), currentIndex, restoredPositionMs)
            if (shuffleEnabled) {
                player.setShuffleOrder(buildShuffleOrder(songs, actualCurrentId))
            } else {
                player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(songs.size))
            }
            player.setShuffleModeEnabled(shuffleEnabled)
            applyRepeatMode()
            player.pause()
            publish(songs)
            if (songs.size != queueIds.size || matchedIndex < 0) {
                savePlaybackState()
            }
        }
    }

    private var nextRevision = 1L
    private var lastPublishedRevision = 0L

    private data class PlaybackSnapshot(
        val revision: Long,
        val currentSong: SongEntity?,
        val currentSongId: String?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val bufferedPositionMs: Long,
        val beatEnergy: Float,
        val isPreparing: Boolean,
        val autoNextCountdownSeconds: Int?,
        val shuffleEnabled: Boolean,
        val shuffleMode: ShuffleMode,
        val shuffleType: ShuffleType,
        val repeatMode: RepeatMode,
        val queue: List<SongEntity>,
        val audioSessionId: Int
    )

    private fun publish(queueOverride: List<SongEntity>? = null) {
        if (isReleased || publishSuppressed) return
        val q = queueOverride ?: currentQueue()
        val revision = nextRevision++
        if (revision < lastPublishedRevision) return
        val currentId = player.currentMediaItem?.mediaId
        val currentSong = if (currentId == null) {
            null
        } else {
            q.find { it.id == currentId }
                ?: baseQueue.find { it.id == currentId }
                ?: _state.value.currentSong?.takeIf { it.id == currentId }
        }
        val currentBeatEnergy = audioEffectsController.beatEnergy.value
        val isPlaying = player.isPlaying
        val positionMs = player.currentPosition.coerceAtLeast(0)
        val durationMs = player.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET && it >= 0 } ?: 0
        val bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0)
        val isPreparing = preparing
        val countdownSeconds = playbackDelayController.countdown.value
        val currentShuffleEnabled = shuffleEnabled
        val currentShuffleMode = shuffleMode
        val currentShuffleType = shuffleType
        val currentRepeatMode = repeatMode

        val snapshot = PlaybackSnapshot(
            revision = revision,
            currentSong = currentSong,
            currentSongId = currentId,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPositionMs,
            beatEnergy = currentBeatEnergy,
            isPreparing = isPreparing,
            autoNextCountdownSeconds = countdownSeconds,
            shuffleEnabled = currentShuffleEnabled,
            shuffleMode = currentShuffleMode,
            shuffleType = currentShuffleType,
            repeatMode = currentRepeatMode,
            queue = q,
            audioSessionId = player.audioSessionId
        )

        lastPublishedRevision = snapshot.revision
        _state.value = _state.value.copy(
            currentSong = snapshot.currentSong,
            isPlaying = snapshot.isPlaying,
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
            bufferedPositionMs = snapshot.bufferedPositionMs,
            beatEnergy = snapshot.beatEnergy,
            isPreparing = snapshot.isPreparing,
            autoNextCountdownSeconds = snapshot.autoNextCountdownSeconds,
            shuffleEnabled = snapshot.shuffleEnabled,
            shuffleMode = snapshot.shuffleMode,
            shuffleType = snapshot.shuffleType,
            repeatMode = snapshot.repeatMode,
            queue = snapshot.queue,
            audioSessionId = snapshot.audioSessionId
        )
        val bundle = Bundle().apply {
            putLong("state_revision", snapshot.revision)
            putString("current_song_id", snapshot.currentSongId)
            putBoolean("is_playing", snapshot.isPlaying)
            putLong("position_ms", snapshot.positionMs)
            putLong("duration_ms", snapshot.durationMs)
            putLong("buffered_position_ms", snapshot.bufferedPositionMs)
            putFloat("beat_energy", snapshot.beatEnergy)
            putBoolean("is_preparing", snapshot.isPreparing)
            putInt("auto_next_countdown", snapshot.autoNextCountdownSeconds ?: -1)
            putBoolean(SHUFFLE_ENABLED, snapshot.shuffleEnabled)
            putInt(SHUFFLE_MODE, snapshot.shuffleMode.ordinal)
            putInt(SHUFFLE_TYPE, snapshot.shuffleType.ordinal)
            putInt(REPEAT_MODE, snapshot.repeatMode.ordinal)
            putStringArrayList("queue_ids", ArrayList(snapshot.queue.map { it.id }))
            putInt("audio_session_id", snapshot.audioSessionId)
        }
        if (!isReleased) {
            mediaSession.broadcastCustomCommand(SessionCommand("STATE_CHANGED", Bundle.EMPTY), bundle)
        }
    }

    internal fun handleCustomCommand(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
        val result = SettableFuture.create<SessionResult>()
        if (isReleased) {
            result.set(SessionResult(SessionResult.RESULT_ERROR_SESSION_DISCONNECTED))
            return result
        }
        scope.launch {
            try {
                commandMutex.withLock {
                    if (isReleased) {
                        result.set(SessionResult(SessionResult.RESULT_ERROR_SESSION_DISCONNECTED))
                        return@launch
                    }
                    when (command.customAction) {
                        SET_QUEUE -> setQueue(args.getStringArrayList(SONG_IDS).orEmpty(), args.getInt(START_INDEX), args.getBoolean(PLAY, true))
                        PLAY_SONG -> playSong(args.getString(SONG_ID) ?: return@withLock)
                        SET_SHUFFLE -> setShuffle(
                            enabled = args.getBoolean(ENABLED),
                            mode = ShuffleMode.entries.getOrElse(args.getInt(MODE)) { shuffleMode },
                            type = ShuffleType.entries.getOrElse(args.getInt(SHUFFLE_TYPE, shuffleType.ordinal)) { shuffleType },
                            suppliedOrder = args.getIntArray(SHUFFLE_ORDER)
                        )
                        SET_REPEAT -> setRepeatMode(RepeatMode.entries.getOrElse(args.getInt(MODE, repeatMode.ordinal)) { repeatMode })
                        ADD_TO_QUEUE -> addToQueue(args.getString(SONG_ID) ?: return@withLock)
                        PLAY_NEXT -> playNext(args.getString(SONG_ID) ?: return@withLock)
                        REMOVE_FROM_QUEUE -> removeFromQueue(args.getString(SONG_ID) ?: return@withLock)
                        MOVE_IN_QUEUE -> moveInQueue(args.getInt(FROM_INDEX), args.getInt(TO_INDEX))
                        END_SESSION -> endPlaybackSession()
                        UPDATE_SONG -> updateSong(args.getString(SONG_ID) ?: return@withLock)
                        TOGGLE_FAVORITE -> toggleFavorite()
                        EQ_BAND -> setBand(args.getInt(BAND), args.getFloat(VALUE))
                        BASS -> setBass(args.getFloat(VALUE))
                        VIRTUALIZER -> setVirtualizer(args.getFloat(VALUE))
                        SET_DELAY -> setDelay(args.getInt(DELAY_SECONDS))
                        CANCEL_DELAY -> cancelDelay()
                        TRIGGER_DELAY -> triggerDelay()
                        NEXT -> next()
                        PREVIOUS -> previous()
                        SET_CROSSFADE_ENABLED -> setCrossfadeEnabled(args.getBoolean(CROSSFADE_ENABLED))
                        SET_CROSSFADE_DURATION -> setCrossfadeDuration(args.getInt(CROSSFADE_DURATION_SECONDS))
                        PRESET -> args.getBundle(PRESET_DATA)?.let { p -> applyPreset(EqualizerPresetEntity(p.getString("name", "Custom"), p.getBoolean("isCustom"), p.getFloat("band60Hz"), p.getFloat("band230Hz"), p.getFloat("band910Hz"), p.getFloat("band4kHz"), p.getFloat("band14kHz"), p.getFloat("bassBoost"), p.getFloat("virtualizer"))) }
                    }
                    if (isReleased) {
                        result.set(SessionResult(SessionResult.RESULT_ERROR_SESSION_DISCONNECTED))
                    } else {
                        result.set(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
            } catch (t: Throwable) {
                if (isReleased || t is kotlinx.coroutines.CancellationException) {
                    result.set(SessionResult(SessionResult.RESULT_ERROR_SESSION_DISCONNECTED))
                } else {
                    result.setException(t)
                }
            }
        }
        return result
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(SET_QUEUE, Bundle.EMPTY)).add(SessionCommand(PLAY_SONG, Bundle.EMPTY)).add(SessionCommand(SET_SHUFFLE, Bundle.EMPTY))
                .add(SessionCommand(SET_REPEAT, Bundle.EMPTY))
                .add(SessionCommand(ADD_TO_QUEUE, Bundle.EMPTY)).add(SessionCommand(PLAY_NEXT, Bundle.EMPTY)).add(SessionCommand(UPDATE_SONG, Bundle.EMPTY))
                .add(SessionCommand(TOGGLE_FAVORITE, Bundle.EMPTY)).add(SessionCommand(EQ_BAND, Bundle.EMPTY)).add(SessionCommand(BASS, Bundle.EMPTY))
                .add(SessionCommand(VIRTUALIZER, Bundle.EMPTY)).add(SessionCommand(PRESET, Bundle.EMPTY)).add(SessionCommand(SET_DELAY, Bundle.EMPTY))
                .add(SessionCommand(CANCEL_DELAY, Bundle.EMPTY)).add(SessionCommand(TRIGGER_DELAY, Bundle.EMPTY)).add(SessionCommand(SET_CROSSFADE_ENABLED, Bundle.EMPTY)).add(SessionCommand(SET_CROSSFADE_DURATION, Bundle.EMPTY)).add(SessionCommand(NEXT, Bundle.EMPTY)).add(SessionCommand(PREVIOUS, Bundle.EMPTY)).add(SessionCommand(REMOVE_FROM_QUEUE, Bundle.EMPTY)).add(SessionCommand(MOVE_IN_QUEUE, Bundle.EMPTY)).add(SessionCommand(END_SESSION, Bundle.EMPTY)).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands).build()
        }

        override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            return handleCustomCommand(command, args)
        }
    }

    fun release() {
        if (!_isReleased.compareAndSet(false, true)) return
        player.removeListener(playerListener)
        playbackDelayController.release()
        audioEffectsController.release()
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        mediaSession.release()
        player.release()
        scope.cancel()
    }
}
