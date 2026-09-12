package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.database.OniDatabase
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.ArtistSummaryEntity
import com.example.data.api.GeminiMusicService
import com.example.data.repository.MusicRepository
import com.example.playback.OniAudioEngine
import com.example.playback.ShuffleMode
import com.example.ui.player.model.PlayerUiState
import com.example.ui.theme.OniTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

private val android.content.Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oni_settings")
private val THEME_OPTION_KEY = stringPreferencesKey("theme_option")
private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color_hex")
private val MATERIAL_YOU_KEY = booleanPreferencesKey("material_you_enabled")
private val GLASS_EFFECT_KEY = booleanPreferencesKey("glass_effect_enabled")
private val BLUR_STRENGTH_KEY = floatPreferencesKey("blur_strength")
private val CORNER_RADIUS_KEY = floatPreferencesKey("corner_radius")
private val BACKGROUND_TRANSPARENCY_KEY = floatPreferencesKey("background_transparency")
private val REDUCE_MOTION_KEY = booleanPreferencesKey("reduce_motion_enabled")
private val AUTO_SEARCH_ARTIST_DATA_KEY = booleanPreferencesKey("auto_search_artist_data")
private val AUTO_SEARCH_WIFI_ONLY_KEY = booleanPreferencesKey("auto_search_wifi_only")
private val AUTO_DOWNLOAD_LYRICS_KEY = booleanPreferencesKey("auto_download_lyrics")
private val PLAYBACK_DELAY_KEY = intPreferencesKey("playback_delay_seconds")
private val CROSSFADE_ENABLED_KEY = booleanPreferencesKey("crossfade_enabled")
private val CROSSFADE_DURATION_KEY = intPreferencesKey("crossfade_duration_seconds")

class EngineStateFlowDelegate<T>(
    private val flow: StateFlow<T>,
    private val setter: (T) -> Unit
) {
    var value: T
        get() = flow.value
        set(v) { setter(v) }
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MusicPlayerViewModel"

    private val database = OniDatabase.getDatabase(application)
    private val repository = MusicRepository(application, database.songDao())
    val audioEngine = OniAudioEngine.getInstance(application)
    val karaokeMicEngine = com.example.ui.lyrics.KaraokeMicEngine()

    // Library scroll position persistence
    var libraryScrollIndex: Int = 0
    var libraryScrollOffset: Int = 0

    // Scroll positions for ArtistsScreen
    var artistsGridIndex: Int = 0
    var artistsGridOffset: Int = 0
    var artistsListIndex: Int = 0
    var artistsListOffset: Int = 0

    // Scroll positions for AlbumsScreen
    var albumsGridIndex: Int = 0
    var albumsGridOffset: Int = 0
    var albumsListIndex: Int = 0
    var albumsListOffset: Int = 0

    // Scroll positions for SongsListView Grid
    var songsGridIndex: Int = 0
    var songsGridOffset: Int = 0

    // Scroll positions for GroupedListView (Folders, Genres, etc.)
    var groupedGridIndex: Int = 0
    var groupedGridOffset: Int = 0
    var groupedListIndex: Int = 0
    var groupedListOffset: Int = 0

    // Scroll positions for PlaylistsScreen
    var playlistsGridIndex: Int = 0
    var playlistsGridOffset: Int = 0
    var playlistsListIndex: Int = 0
    var playlistsListOffset: Int = 0

    // User selected theme state
    private val _currentTheme = MutableStateFlow(OniTheme.HIGH_DENSITY)
    val currentTheme: StateFlow<OniTheme> = _currentTheme.asStateFlow()

    // User selected theme option name ("Light", "Dark", "AMOLED", "Follow System")
    private val _selectedThemeOption = MutableStateFlow("Dark")
    val selectedThemeOption: StateFlow<String> = _selectedThemeOption.asStateFlow()

    // Custom Accent Color hex string (defaulting to Default Skin Blue: #3B73E3)
    private val _customAccentColor = MutableStateFlow("#3B73E3")
    val customAccentColor: StateFlow<String> = _customAccentColor.asStateFlow()

    // Material You toggle state (default false)
    private val _materialYouEnabled = MutableStateFlow(false)
    val materialYouEnabled: StateFlow<Boolean> = _materialYouEnabled.asStateFlow()

    // Glass Effect toggle state (default true)
    private val _glassEffectEnabled = MutableStateFlow(true)
    val glassEffectEnabled: StateFlow<Boolean> = _glassEffectEnabled.asStateFlow()

    // Blur strength state (default 20f, range 0-100)
    private val _blurStrength = MutableStateFlow(20f)
    val blurStrength: StateFlow<Float> = _blurStrength.asStateFlow()

    // Corner radius state (default 16f, range 8-32)
    private val _cornerRadius = MutableStateFlow(16f)
    val cornerRadius: StateFlow<Float> = _cornerRadius.asStateFlow()

    // Reduce Motion state (default false)
    private val _reduceMotionEnabled = MutableStateFlow(false)
    val reduceMotionEnabled: StateFlow<Boolean> = _reduceMotionEnabled.asStateFlow()

    private val _nextSongDelaySeconds = MutableStateFlow(0)
    val nextSongDelaySeconds: StateFlow<Int> = _nextSongDelaySeconds.asStateFlow()

    private val _playbackDelayCountdown = MutableStateFlow<Int?>(null)
    val playbackDelayCountdown: StateFlow<Int?> = _playbackDelayCountdown.asStateFlow()

    private var delayJob: Job? = null

    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(5)
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    // Background transparency state (default 50f, range 0-100)
    private val _backgroundTransparency = MutableStateFlow(50f)
    val backgroundTransparency: StateFlow<Float> = _backgroundTransparency.asStateFlow()

    // Screen selection / Tab state
    private val _currentTab = MutableStateFlow(1) // 0: Songs/Library, 1: Player, 2: Equalizer, 3: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Library category selection & hierarchical navigation states
    private val _activeCategoryIndex = MutableStateFlow<Int?>(null)
    val activeCategoryIndex: StateFlow<Int?> = _activeCategoryIndex.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _activePlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val activePlaylist: StateFlow<PlaylistEntity?> = _activePlaylist.asStateFlow()

    private val _activeSmartPlaylistType = MutableStateFlow<String?>(null)
    val activeSmartPlaylistType: StateFlow<String?> = _activeSmartPlaylistType.asStateFlow()

    // Store the context where the currently playing song was selected from
    private val _playedCategoryIndex = MutableStateFlow<Int?>(null)
    val playedCategoryIndex: StateFlow<Int?> = _playedCategoryIndex.asStateFlow()

    private val _playedSelectedGroup = MutableStateFlow<String?>(null)
    val playedSelectedGroup: StateFlow<String?> = _playedSelectedGroup.asStateFlow()

    private val _playedActivePlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val playedActivePlaylist: StateFlow<PlaylistEntity?> = _playedActivePlaylist.asStateFlow()

    private val _playedActiveSmartPlaylistType = MutableStateFlow<String?>(null)
    val playedActiveSmartPlaylistType: StateFlow<String?> = _playedActiveSmartPlaylistType.asStateFlow()

    fun setActiveCategoryIndex(index: Int?) {
        _activeCategoryIndex.value = index
        _selectedGroup.value = null
        _activePlaylist.value = null
        _activeSmartPlaylistType.value = null
    }

    fun setSelectedGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setActivePlaylist(playlist: PlaylistEntity?) {
        _activePlaylist.value = playlist
    }

    fun setActiveSmartPlaylistType(type: String?) {
        _activeSmartPlaylistType.value = type
    }

    fun goBackToLibraryContext() {
        val playedCategory = _playedCategoryIndex.value
        if (playedCategory != null) {
            _activeCategoryIndex.value = playedCategory
            _selectedGroup.value = _playedSelectedGroup.value
            _activePlaylist.value = _playedActivePlaylist.value
            _activeSmartPlaylistType.value = _playedActiveSmartPlaylistType.value
        } else {
            inferAndSetLibraryContextForCurrentSong()
        }
        _currentTab.value = 0
    }

    fun inferAndSetLibraryContextForCurrentSong() {
        val current = audioEngine.currentSong.value ?: return
        if (_activeCategoryIndex.value != null) return

        val artist = current.customArtist ?: current.artist
        if (artist.isNotBlank() && artist != "Unknown Artist" && artist != "<unknown>") {
            _activeCategoryIndex.value = 3
            _selectedGroup.value = artist
            return
        }

        val filePath = current.filePath
        val file = File(filePath)
        val folderName = file.parentFile?.name ?: "Internal Storage"
        if (folderName.isNotBlank()) {
            _activeCategoryIndex.value = 1
            _selectedGroup.value = folderName
            return
        }

        _activeCategoryIndex.value = 0
        _selectedGroup.value = null
    }

    // Scanning status
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Lyrics fetching state
    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()

    // Lyrics configuration states
    private val _isAutoDownloadEnabled = MutableStateFlow(true)
    val isAutoDownloadEnabled: StateFlow<Boolean> = _isAutoDownloadEnabled.asStateFlow()

    private val _floatingLyricsEnabled = EngineStateFlowDelegate(audioEngine.floatingLyricsEnabled) { audioEngine.setFloatingLyricsEnabled(it) }
    val floatingLyricsEnabled: StateFlow<Boolean> = audioEngine.floatingLyricsEnabled

    // Online Tag Edit state
    private val _isOptimizingTags = MutableStateFlow(false)
    val isOptimizingTags: StateFlow<Boolean> = _isOptimizingTags.asStateFlow()

    // Active playlist being played
    private val _currentPlaylist = EngineStateFlowDelegate(audioEngine.currentPlaylist) { audioEngine.setPlaylist(it) }
    val currentPlaylist: StateFlow<List<SongEntity>> = audioEngine.currentPlaylist

    private val _pendingDeleteRequest = MutableSharedFlow<android.content.IntentSender>(extraBufferCapacity = 1)
    val pendingDeleteRequest: SharedFlow<android.content.IntentSender> = _pendingDeleteRequest.asSharedFlow()

    // Shuffle and Repeat modes
    private val _isShuffle = EngineStateFlowDelegate(audioEngine.isShuffle) { audioEngine.setShuffle(it) }
    val isShuffle: StateFlow<Boolean> = audioEngine.isShuffle

    private val _isRepeat = EngineStateFlowDelegate(audioEngine.isRepeat) { audioEngine.setRepeat(it) }
    val isRepeat: StateFlow<Boolean> = audioEngine.isRepeat

    private val _shuffleMode = EngineStateFlowDelegate(audioEngine.shuffleMode) { audioEngine.setShuffleMode(it) }
    val shuffleMode: StateFlow<ShuffleMode> = audioEngine.shuffleMode

    // Search query for library
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPresets: StateFlow<List<EqualizerPresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtistSummaries: StateFlow<List<ArtistSummaryEntity>> = repository.allArtistSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSong: StateFlow<SongEntity?> = audioEngine.currentSong
    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val position: StateFlow<Long> = audioEngine.position
    val duration: StateFlow<Long> = audioEngine.duration
    val isPreparing: StateFlow<Boolean> = audioEngine.isPreparing

    // Sleep Timer
    private val _sleepTimerMinutesLeft = MutableStateFlow(0)
    val sleepTimerMinutesLeft: StateFlow<Int> = _sleepTimerMinutesLeft.asStateFlow()

    private val _isSleepTimerRunning = MutableStateFlow(false)
    val isSleepTimerRunning: StateFlow<Boolean> = _isSleepTimerRunning.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _isSleepTimerRunning.value = false
            _sleepTimerMinutesLeft.value = 0
            return
        }
        _isSleepTimerRunning.value = true
        _sleepTimerMinutesLeft.value = minutes
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerMinutesLeft.value > 0) {
                kotlinx.coroutines.delay(60_000L)
                val current = _sleepTimerMinutesLeft.value
                if (current <= 1) {
                    _sleepTimerMinutesLeft.value = 0
                    _isSleepTimerRunning.value = false
                    audioEngine.pause()
                    break
                } else {
                    _sleepTimerMinutesLeft.value = current - 1
                }
            }
        }
    }

    fun cancelSleepTimer() {
        setSleepTimer(0)
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }

    fun pausePlayback() {
        audioEngine.pause()
    }

    fun resumePlayback() {
        audioEngine.resume()
    }

    private data class EnginePlaybackState(
        val song: SongEntity?,
        val isPlaying: Boolean,
        val position: Long,
        val duration: Long,
        val isPreparing: Boolean
    )

    private data class EngineQueueState(
        val isShuffle: Boolean,
        val isRepeat: Boolean,
        val queue: List<SongEntity>,
        val favoriteIds: Set<String>
    )

    private data class PlayerFeaturesState(
        val floatingLyrics: Boolean,
        val delayCountdown: Int?,
        val timerMinutes: Int,
        val timerRunning: Boolean,
        val isFetchingLyrics: Boolean
    )

    val playerUiState: StateFlow<PlayerUiState> = combine(
        combine(audioEngine.currentSong, audioEngine.isPlaying, audioEngine.position, audioEngine.duration, audioEngine.isPreparing) { song, playing, pos, dur, prep ->
            EnginePlaybackState(song, playing, pos, dur, prep)
        },
        combine(audioEngine.isShuffle, audioEngine.isRepeat, audioEngine.currentPlaylist, favoriteSongs) { shuf, rep, q, favs ->
            EngineQueueState(shuf, rep, q, favs.map { it.id }.toSet())
        },
        combine(floatingLyricsEnabled, playbackDelayCountdown, _sleepTimerMinutesLeft, _isSleepTimerRunning, isFetchingLyrics) { floatLrc, delaySec, timerMin, timerRun, fetching ->
            PlayerFeaturesState(floatLrc, delaySec, timerMin, timerRun, fetching)
        }
    ) { playback, queueState, features ->
        PlayerUiState(
            currentSong = playback.song,
            isPlaying = playback.isPlaying,
            isPreparing = playback.isPreparing,
            position = playback.position,
            duration = playback.duration,
            isShuffle = queueState.isShuffle,
            isRepeat = queueState.isRepeat,
            isFavorite = playback.song != null && queueState.favoriteIds.contains(playback.song.id),
            queue = queueState.queue,
            floatingLyricsEnabled = features.floatingLyrics,
            playbackDelayCountdown = features.delayCountdown,
            sleepTimerMinutesLeft = features.timerMinutes,
            isSleepTimerRunning = features.timerRunning,
            isFetchingLyrics = features.isFetchingLyrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

    // Equalizer UI Binding states
    private val _currentPresetName = MutableStateFlow("Flat")
    val currentPresetName: StateFlow<String> = _currentPresetName.asStateFlow()

    private val _eqBand60Hz = MutableStateFlow(0f)
    val eqBand60Hz: StateFlow<Float> = _eqBand60Hz.asStateFlow()

    private val _eqBand230Hz = MutableStateFlow(0f)
    val eqBand230Hz: StateFlow<Float> = _eqBand230Hz.asStateFlow()

    private val _eqBand910Hz = MutableStateFlow(0f)
    val eqBand910Hz: StateFlow<Float> = _eqBand910Hz.asStateFlow()

    private val _eqBand4kHz = MutableStateFlow(0f)
    val eqBand4kHz: StateFlow<Float> = _eqBand4kHz.asStateFlow()

    private val _eqBand14kHz = MutableStateFlow(0f)
    val eqBand14kHz: StateFlow<Float> = _eqBand14kHz.asStateFlow()

    private val _eqBassBoost = MutableStateFlow(0f)
    val eqBassBoost: StateFlow<Float> = _eqBassBoost.asStateFlow()

    private val _eqVirtualizer = MutableStateFlow(0f)
    val eqVirtualizer: StateFlow<Float> = _eqVirtualizer.asStateFlow()

    private val _isVocalReductionActive = MutableStateFlow(false)
    val isVocalReductionActive: StateFlow<Boolean> = _isVocalReductionActive.asStateFlow()
    private var preVocalReductionGains: FloatArray? = null

    private val lyricsTranslationCache = mutableMapOf<String, String>()

    init {
        scanAndLoad()
        
        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[THEME_OPTION_KEY] ?: "Dark" }
                    .collect { savedOption ->
                        _selectedThemeOption.value = savedOption
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved theme: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[ACCENT_COLOR_KEY] ?: "#3B73E3" }
                    .collect { savedAccent ->
                        _customAccentColor.value = savedAccent
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved accent color: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[MATERIAL_YOU_KEY] ?: false }
                    .collect { savedMaterialYou ->
                        _materialYouEnabled.value = savedMaterialYou
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading material you preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[GLASS_EFFECT_KEY] ?: true }
                    .collect { savedGlass ->
                        _glassEffectEnabled.value = savedGlass
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading glass effect preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[BLUR_STRENGTH_KEY] ?: 20f }
                    .collect { savedBlur ->
                        _blurStrength.value = savedBlur
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading blur strength preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[CORNER_RADIUS_KEY] ?: 16f }
                    .collect { savedRadius ->
                        _cornerRadius.value = savedRadius
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading corner radius preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[BACKGROUND_TRANSPARENCY_KEY] ?: 50f }
                    .collect { savedTransparency ->
                        _backgroundTransparency.value = savedTransparency
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading background transparency preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[REDUCE_MOTION_KEY] ?: false }
                    .collect { savedReduceMotion ->
                        _reduceMotionEnabled.value = savedReduceMotion
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reduce motion preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[AUTO_SEARCH_ARTIST_DATA_KEY] ?: true }
                    .collect { saved ->
                        _autoSearchArtistData.value = saved
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading auto search artist data preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[AUTO_SEARCH_WIFI_ONLY_KEY] ?: true }
                    .collect { saved ->
                        _autoSearchWifiOnly.value = saved
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading auto search wifi only preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[AUTO_DOWNLOAD_LYRICS_KEY] ?: true }
                    .collect { saved ->
                        _isAutoDownloadEnabled.value = saved
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading auto download lyrics preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[PLAYBACK_DELAY_KEY] ?: 0 }
                    .collect { savedDelay ->
                        _nextSongDelaySeconds.value = savedDelay
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading playback delay preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[CROSSFADE_ENABLED_KEY] ?: false }
                    .collect { enabled ->
                        _crossfadeEnabled.value = enabled
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading crossfade enabled preference: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data
                    .map { preferences -> preferences[CROSSFADE_DURATION_KEY] ?: 5 }
                    .collect { duration ->
                        _crossfadeDurationSeconds.value = duration
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading crossfade duration preference: ${e.message}")
            }
        }
    }

    private fun scanAndLoad() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.scanAndSyncMusic()
            _isScanning.value = false

            val initialSongs = repository.allSongs.first()
            if (initialSongs.isNotEmpty()) {
                if (audioEngine.currentPlaylist.value.isEmpty()) {
                    _currentPlaylist.value = initialSongs
                    
                    val lastPlayedSong = initialSongs.filter { it.lastPlayedTimestamp > 0 }
                        .maxByOrNull { it.lastPlayedTimestamp } ?: initialSongs.first()
                    
                    audioEngine.setSongWithoutPlaying(lastPlayedSong)
                }

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val uniqueArtists = initialSongs.map { it.displayArtist.ifBlank { "Unknown Artist" } }.distinct()
                        uniqueArtists.forEach { artistName ->
                            if (artistName != "Unknown Artist" && artistName.isNotBlank()) {
                                val existing = repository.getArtistSummary(artistName)
                                if (existing == null && _autoSearchArtistData.value) {
                                    kotlinx.coroutines.delay(1200)
                                    try {
                                        val summary = GeminiMusicService.fetchArtistSummaryFromAudioDB(artistName)
                                        var savedArtworkUri: String? = null
                                        val images = GeminiMusicService.fetchArtistImagesFromAudioDB(artistName)
                                        if (images.isNotEmpty()) {
                                            savedArtworkUri = images.firstOrNull { !it.isNullOrBlank() }
                                        }
                                        repository.saveArtistData(artistName, summary, savedArtworkUri)
                                    } catch (e: Exception) {
                                        Log.e("MusicPlayerViewModel", "Background artist fetch failed for $artistName: ${e.message}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicPlayerViewModel", "Error in background artist details scan", e)
                    }
                }
            }
        }
    }

    fun rescanLibrary() {
        scanAndLoad()
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun goToLibraryDashboard() {
        _currentTab.value = 0
        _activeCategoryIndex.value = null
        _searchQuery.value = ""
    }

    fun selectCategory(index: Int) {
        _activeCategoryIndex.value = index
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTheme(theme: OniTheme) {
        _currentTheme.value = theme
    }

    fun updateThemeFromOption(option: String, isSystemDark: Boolean) {
        val theme = when (option) {
            "Light" -> OniTheme.AERO_LIGHT
            "Dark" -> OniTheme.CLASSIC_DARK
            "AMOLED" -> OniTheme.COSMIC_OBSIDIAN
            "Follow System" -> if (isSystemDark) OniTheme.CLASSIC_DARK else OniTheme.AERO_LIGHT
            else -> OniTheme.CLASSIC_DARK
        }
        _currentTheme.value = theme
    }

    fun setThemeOption(option: String, isSystemDark: Boolean) {
        _selectedThemeOption.value = option
        updateThemeFromOption(option, isSystemDark)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[THEME_OPTION_KEY] = option
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving theme option: ${e.message}")
            }
        }
    }

    fun setCustomAccentColor(hex: String) {
        _customAccentColor.value = hex
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[ACCENT_COLOR_KEY] = hex
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving custom accent color: ${e.message}")
            }
        }
    }

    fun setMaterialYouEnabled(enabled: Boolean) {
        _materialYouEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[MATERIAL_YOU_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving material you preference: ${e.message}")
            }
        }
    }

    fun setGlassEffectEnabled(enabled: Boolean) {
        _glassEffectEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[GLASS_EFFECT_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving glass effect preference: ${e.message}")
            }
        }
    }

    fun setBlurStrength(strength: Float) {
        _blurStrength.value = strength
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[BLUR_STRENGTH_KEY] = strength
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving blur strength preference: ${e.message}")
            }
        }
    }

    fun setCornerRadius(radius: Float) {
        _cornerRadius.value = radius
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[CORNER_RADIUS_KEY] = radius
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving corner radius preference: ${e.message}")
            }
        }
    }

    fun setBackgroundTransparency(transparency: Float) {
        _backgroundTransparency.value = transparency
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[BACKGROUND_TRANSPARENCY_KEY] = transparency
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving background transparency preference: ${e.message}")
            }
        }
    }

    fun setReduceMotionEnabled(enabled: Boolean) {
        _reduceMotionEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[REDUCE_MOTION_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving reduce motion preference: ${e.message}")
            }
        }
    }

    fun resetAppearancePreferences() {
        setThemeOption("Dark", false)
        setCustomAccentColor("#3B73E3")
        setMaterialYouEnabled(false)
        setGlassEffectEnabled(true)
        setBlurStrength(20f)
        setCornerRadius(16f)
        setBackgroundTransparency(50f)
        setReduceMotionEnabled(false)
    }

    fun playSong(song: SongEntity, playlist: List<SongEntity>) {
        cancelDelay()
        _currentTab.value = 1

        _playedCategoryIndex.value = _activeCategoryIndex.value
        _playedSelectedGroup.value = _selectedGroup.value
        _playedActivePlaylist.value = _activePlaylist.value
        _playedActiveSmartPlaylistType.value = _activeSmartPlaylistType.value

        viewModelScope.launch {
            val dbSong = database.songDao().getSongById(song.id) ?: song
            
            val updated = dbSong.copy(
                playCount = dbSong.playCount + 1,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
            repository.updateSong(updated)
            
            if (audioEngine.currentSong.value?.id == updated.id) {
                audioEngine.resume()
            } else {
                val currentQueueIds = audioEngine.currentPlaylist.value.map { it.id }
                val isSamePlaylist = currentQueueIds == playlist.map { it.id }
                if (isSamePlaylist && currentQueueIds.contains(updated.id)) {
                    audioEngine.play(updated)
                } else {
                    val idx = playlist.indexOfFirst { it.id == updated.id }.coerceAtLeast(0)
                    audioEngine.setPlaylist(playlist, idx, true)
                }
            }
            
            if (updated.lyrics.isNullOrBlank()) {
                val localLrc = com.example.ui.lyrics.LyricsHelper.findLocalLrcFile(updated.filePath)
                if (localLrc != null) {
                    database.songDao().updateLyrics(updated.id, localLrc)
                    audioEngine.updateCurrentSongMetadata(updated.copy(lyrics = localLrc))
                } else if (_isAutoDownloadEnabled.value) {
                    _isFetchingLyrics.value = true
                    try {
                        val fetched = repository.fetchAndCacheLyrics(
                            songId = updated.id,
                            title = updated.customTitle ?: updated.title,
                            artist = updated.customArtist ?: updated.artist
                        )
                        if (fetched != null && audioEngine.currentSong.value?.id == updated.id) {
                            val refreshed = database.songDao().getSongById(updated.id)
                            if (refreshed != null) {
                                audioEngine.updateCurrentSongMetadata(refreshed)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Auto lyrics fetch failed: ${e.message}")
                    } finally {
                        _isFetchingLyrics.value = false
                    }
                }
            }
        }
    }

    fun playSongById(songId: String) {
        viewModelScope.launch {
            val song = database.songDao().getSongById(songId)
            if (song != null) {
                val playlist = _currentPlaylist.value.ifEmpty {
                    database.songDao().getAllSongs().firstOrNull() ?: emptyList()
                }
                playSong(song, playlist)
            }
        }
    }

    fun playNext(song: SongEntity) {
        audioEngine.playNext(song)
    }

    fun addToQueue(song: SongEntity) {
        audioEngine.addToQueue(song)
    }

    fun togglePlayPause() {
        cancelDelay()
        if (audioEngine.isPlaying.value) {
            audioEngine.pause()
        } else {
            if (audioEngine.currentSong.value == null && _currentPlaylist.value.isNotEmpty()) {
                val firstRaw = _currentPlaylist.value.first()
                viewModelScope.launch {
                    val dbSong = database.songDao().getSongById(firstRaw.id) ?: firstRaw
                    audioEngine.play(dbSong)
                }
            } else {
                audioEngine.resume()
            }
        }
    }

    fun pickShuffleStartSong(playlist: List<SongEntity>): SongEntity? {
        if (playlist.isEmpty()) return null
        val index = pickShuffleIndex(playlist, excludeIndex = -1)
        return playlist.getOrNull(index)
    }

    private fun pickShuffleIndex(playlist: List<SongEntity>, excludeIndex: Int): Int {
        if (playlist.size <= 1) return 0

        val weights: List<Double> = when (_shuffleMode.value) {
            ShuffleMode.DISCOVER -> playlist.map { 1.0 / (1.0 + it.playCount) }
            ShuffleMode.FAVORITES_BOOST -> playlist.map { if (it.isFavorite) 4.0 else 1.0 }
            ShuffleMode.RANDOM -> return playlist.indices.filter { it != excludeIndex }.random()
        }

        val total = weights.withIndex().filter { it.index != excludeIndex }.sumOf { it.value }
        if (total <= 0.0) return playlist.indices.filter { it != excludeIndex }.random()

        var roll = Math.random() * total
        for ((index, weight) in weights.withIndex()) {
            if (index == excludeIndex) continue
            if (roll < weight) return index
            roll -= weight
        }
        return playlist.indices.filter { it != excludeIndex }.last()
    }

    fun skipNext() {
        cancelDelay()
        val playlist = _currentPlaylist.value
        val current = audioEngine.currentSong.value ?: return
        if (playlist.isEmpty()) return

        val currentIndex = playlist.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return

        if (_isRepeat.value) {
            viewModelScope.launch {
                val dbSong = database.songDao().getSongById(current.id) ?: current
                audioEngine.play(dbSong)
            }
            return
        }

        val nextIndex = if (_isShuffle.value) {
            pickShuffleIndex(playlist, currentIndex)
        } else {
            (currentIndex + 1) % playlist.size
        }

        viewModelScope.launch {
            val nextRaw = playlist[nextIndex]
            val dbSong = database.songDao().getSongById(nextRaw.id) ?: nextRaw
            audioEngine.play(dbSong)
        }
    }

    fun skipPrevious() {
        cancelDelay()
        val playlist = _currentPlaylist.value
        val current = audioEngine.currentSong.value ?: return
        if (playlist.isEmpty()) return

        val currentIndex = playlist.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return

        val prevIndex = if (_isShuffle.value) {
            pickShuffleIndex(playlist, currentIndex)
        } else {
            if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        }

        viewModelScope.launch {
            val prevRaw = playlist[prevIndex]
            val dbSong = database.songDao().getSongById(prevRaw.id) ?: prevRaw
            audioEngine.play(dbSong)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setShuffleMode(mode: ShuffleMode) {
        _shuffleMode.value = mode
        _isShuffle.value = true
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        _isAutoDownloadEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[AUTO_DOWNLOAD_LYRICS_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving auto download lyrics preference: ${e.message}")
            }
        }
    }

    fun setFloatingLyricsEnabled(enabled: Boolean) {
        _floatingLyricsEnabled.value = enabled
    }

    fun updateLyrics(songId: String, text: String?) {
        viewModelScope.launch {
            database.songDao().updateLyrics(songId, text)
            val current = audioEngine.currentSong.value
            if (current != null && current.id == songId) {
                val updated = database.songDao().getSongById(songId)
                if (updated != null) {
                    audioEngine.updateCurrentSongMetadata(updated)
                }
            }
        }
    }

    fun searchLyricsManual(songId: String, title: String, artist: String) {
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            val fetched = repository.fetchAndCacheLyrics(
                songId = songId,
                title = title,
                artist = artist
            )
            if (fetched != null) {
                val updatedSong = database.songDao().getSongById(songId)
                if (updatedSong != null) {
                    val current = audioEngine.currentSong.value
                    if (current != null && current.id == songId) {
                        audioEngine.updateCurrentSongMetadata(updatedSong)
                    }
                }
            }
            _isFetchingLyrics.value = false
        }
    }

    fun searchLyricsOnline() {
        val current = audioEngine.currentSong.value ?: return
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            val fetched = repository.fetchAndCacheLyrics(
                songId = current.id,
                title = current.customTitle ?: current.title,
                artist = current.customArtist ?: current.artist
            )
            if (fetched != null) {
                val updatedSong = database.songDao().getSongById(current.id)
                if (updatedSong != null) {
                    audioEngine.updateCurrentSongMetadata(updatedSong)
                }
            }
            _isFetchingLyrics.value = false
        }
    }

    /**
     * Translates or romanizes lyrics for a song using Gemini.
     * Caches in memory for instant switching.
     */
    fun translateSongLyrics(
        songId: String,
        lyricsText: String,
        targetLanguage: String,
        onResult: (Result<String>) -> Unit
    ) {
        val cacheKey = "${songId}_$targetLanguage"
        val cached = lyricsTranslationCache[cacheKey]
        if (cached != null) {
            onResult(Result.success(cached))
            return
        }

        viewModelScope.launch {
            val result = repository.translateLyrics(lyricsText, targetLanguage)
            result.onSuccess { translated ->
                lyricsTranslationCache[cacheKey] = translated
            }
            onResult(result)
        }
    }

    /**
     * Shifts the timestamps of an LRC string by offsetMs and saves to the database.
     */
    fun shiftSongLyricsTiming(songId: String, currentLyrics: String, offsetMs: Long) {
        val shifted = com.example.ui.lyrics.LyricsHelper.shiftLrcTimestamps(currentLyrics, offsetMs)
        updateLyrics(songId, shifted)
    }

    fun optimizeMetadataWithGemini(song: SongEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isOptimizingTags.value = true
            val optimized = repository.optimizeAndEditTagsOnline(
                songId = song.id,
                currentFileName = File(song.filePath).name,
                currentTitle = song.title,
                currentArtist = song.artist,
                currentAlbum = song.album
            )
            _isOptimizingTags.value = false
            onComplete(optimized != null)
        }
    }

    fun updateTagsManual(songId: String, title: String, artist: String, album: String, genre: String) {
        viewModelScope.launch {
            repository.updateSongTagsManual(songId, title, artist, album, genre)
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            if (audioEngine.currentSong.value?.id == songId) {
                skipNext()
                if (audioEngine.currentSong.value?.id == songId) {
                    audioEngine.stop()
                }
            }
        }
    }

    fun deleteSongPhysically(songId: String) {
        viewModelScope.launch {
            val song = withContext(Dispatchers.IO) {
                database.songDao().getSongById(songId)
            }
            if (song != null) {
                val success = withContext(Dispatchers.IO) {
                    var fileDeleted = false
                    try {
                        val file = File(song.filePath)
                        if (file.exists()) {
                            fileDeleted = file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e("MusicPlayerViewModel", "Failed to delete file directly: ${e.message}")
                    }
                    
                    try {
                        val contentResolver = getApplication<Application>().contentResolver
                        val songIdLong = songId.toLongOrNull()
                        if (songIdLong != null) {
                            val songUri = android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                songIdLong
                            )
                            val deletedRows = contentResolver.delete(songUri, null, null)
                            if (deletedRows > 0) {
                                fileDeleted = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicPlayerViewModel", "Failed to delete from MediaStore: ${e.message}")
                    }
                    fileDeleted
                }

                if (!success && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    try {
                        val songIdLong = songId.toLongOrNull()
                        if (songIdLong != null) {
                            val songUri = android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                songIdLong
                            )
                            val deleteRequest = android.provider.MediaStore.createDeleteRequest(
                                getApplication<Application>().contentResolver,
                                listOf(songUri)
                            )
                            _pendingDeleteRequest.tryEmit(deleteRequest.intentSender)
                        }
                    } catch (e: Exception) {
                        Log.e("MusicPlayerViewModel", "Failed to build delete consent request: ${e.message}")
                    }
                }
            }

            deleteSong(songId)
        }
    }

    fun updateBand(bandIndex: Int, gainDb: Float) {
        audioEngine.setBandGain(bandIndex, gainDb)
        when (bandIndex) {
            0 -> _eqBand60Hz.value = gainDb
            1 -> _eqBand230Hz.value = gainDb
            2 -> _eqBand910Hz.value = gainDb
            3 -> _eqBand4kHz.value = gainDb
            4 -> _eqBand14kHz.value = gainDb
        }
        _currentPresetName.value = "Custom"
    }

    fun updateBassBoost(boost: Float) {
        audioEngine.setBassBoost(boost)
        _eqBassBoost.value = boost
        _currentPresetName.value = "Custom"
    }

    fun updateVirtualizer(virt: Float) {
        audioEngine.setVirtualizer(virt)
        _eqVirtualizer.value = virt
        _currentPresetName.value = "Custom"
    }

    /**
     * Toggles karaoke lead-vocal reduction (instrumental backing mode).
     * Dips the vocal frequency bands while preserving bass and highs.
     */
    fun toggleVocalReduction() {
        setVocalReduction(!_isVocalReductionActive.value)
    }

    fun setVocalReduction(enabled: Boolean) {
        if (_isVocalReductionActive.value == enabled) return
        _isVocalReductionActive.value = enabled
        if (enabled) {
            preVocalReductionGains = floatArrayOf(
                _eqBand60Hz.value,
                _eqBand230Hz.value,
                _eqBand910Hz.value,
                _eqBand4kHz.value,
                _eqBand14kHz.value
            )
            // Attenuate mid frequencies (lead vocals) while maintaining rhythm and air
            audioEngine.setBandGain(0, (_eqBand60Hz.value + 1.5f).coerceAtMost(12f))
            audioEngine.setBandGain(1, (_eqBand230Hz.value - 3.0f).coerceAtLeast(-12f))
            audioEngine.setBandGain(2, -12.0f) // Cut primary vocal formant band
            audioEngine.setBandGain(3, -9.0f)  // Cut vocal presence band
            audioEngine.setBandGain(4, _eqBand14kHz.value)
        } else {
            preVocalReductionGains?.let { gains ->
                audioEngine.setBandGain(0, gains[0])
                audioEngine.setBandGain(1, gains[1])
                audioEngine.setBandGain(2, gains[2])
                audioEngine.setBandGain(3, gains[3])
                audioEngine.setBandGain(4, gains[4])
            }
        }
    }

    fun selectPreset(preset: EqualizerPresetEntity) {
        _currentPresetName.value = preset.name
        _eqBand60Hz.value = preset.band60Hz
        _eqBand230Hz.value = preset.band230Hz
        _eqBand910Hz.value = preset.band910Hz
        _eqBand4kHz.value = preset.band4kHz
        _eqBand14kHz.value = preset.band14kHz
        _eqBassBoost.value = preset.bassBoost
        _eqVirtualizer.value = preset.virtualizer

        audioEngine.applyPreset(preset)
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val preset = EqualizerPresetEntity(
                name = name,
                isCustom = true,
                band60Hz = _eqBand60Hz.value,
                band230Hz = _eqBand230Hz.value,
                band910Hz = _eqBand910Hz.value,
                band4kHz = _eqBand4kHz.value,
                band14kHz = _eqBand14kHz.value,
                bassBoost = _eqBassBoost.value,
                virtualizer = _eqVirtualizer.value
            )
            repository.savePreset(preset)
            _currentPresetName.value = name
        }
    }

    fun deletePreset(preset: EqualizerPresetEntity) {
        viewModelScope.launch {
            repository.deletePreset(preset)
            if (_currentPresetName.value == preset.name) {
                _currentPresetName.value = "Flat"
            }
        }
    }

    fun createPlaylist(name: String, initialSongIds: List<String> = emptyList()) {
        viewModelScope.launch {
            val id = "playlist_" + System.currentTimeMillis()
            val distinctIds = initialSongIds.distinct()
            val initialJson = JSONArray(distinctIds).toString()
            val playlist = PlaylistEntity(id = id, name = name, songIdsJson = initialJson)
            repository.insertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylistById(playlistId)
            if (_activePlaylist.value?.id == playlistId) {
                _activePlaylist.value = null
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            val playlist = database.songDao().getPlaylistById(playlistId) ?: return@launch
            val updated = playlist.copy(name = newName.trim())
            repository.insertPlaylist(updated)
            if (_activePlaylist.value?.id == playlistId) {
                _activePlaylist.value = updated
            }
        }
    }

    fun addSongToPlaylist(songId: String, playlistId: String) {
        viewModelScope.launch {
            val playlist = database.songDao().getPlaylistById(playlistId) ?: return@launch
            val ids = mutableListOf<String>()
            try {
                val array = JSONArray(playlist.songIdsJson)
                for (i in 0 until array.length()) {
                    ids.add(array.getString(i))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing playlist song IDs", e)
            }
            if (!ids.contains(songId)) {
                ids.add(songId)
                val updatedJson = JSONArray(ids).toString()
                repository.insertPlaylist(playlist.copy(songIdsJson = updatedJson))
            }
        }
    }

    fun removeSongFromPlaylist(songId: String, playlistId: String) {
        viewModelScope.launch {
            val playlist = database.songDao().getPlaylistById(playlistId) ?: return@launch
            val ids = mutableListOf<String>()
            try {
                val array = JSONArray(playlist.songIdsJson)
                for (i in 0 until array.length()) {
                    ids.add(array.getString(i))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing playlist song IDs", e)
            }
            if (ids.remove(songId)) {
                val updatedJson = JSONArray(ids).toString()
                repository.insertPlaylist(playlist.copy(songIdsJson = updatedJson))
            }
        }
    }

    fun updateSongRating(songId: String, rating: Int) {
        viewModelScope.launch {
            val song = database.songDao().getSongById(songId) ?: return@launch
            val updated = song.copy(rating = rating)
            repository.updateSong(updated)
            val current = audioEngine.currentSong.value
            if (current != null && current.id == songId) {
                audioEngine.updateCurrentSongMetadata(updated)
            }
        }
    }

    fun updateSongFullTags(
        songId: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        composer: String,
        disc: String,
        track: String,
        year: String,
        comment: String,
        bpm: String,
        albumArtUri: String?
    ) {
        viewModelScope.launch {
            val song = database.songDao().getSongById(songId) ?: return@launch
            val updated = song.copy(
                customTitle = title,
                customArtist = artist,
                customAlbum = album,
                customGenre = genre,
                customAlbumArtist = albumArtist,
                customComposer = composer,
                customDisc = disc,
                customTrack = track,
                customYear = year,
                customComment = comment,
                customBpm = bpm,
                albumArtUri = albumArtUri
            )
            repository.updateSong(updated)
            
            val current = audioEngine.currentSong.value
            if (current != null && current.id == songId) {
                audioEngine.updateCurrentSongMetadata(updated)
            }
        }
    }

    fun renameSongFile(
        songId: String,
        newFileName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var wasPlaying = false
            var currentPosition = 0L
            var isCurrent = false
            var activeSong: SongEntity? = null
            
            try {
                val song = database.songDao().getSongById(songId) ?: throw Exception("Song not found")
                val oldFile = File(song.filePath)
                if (!oldFile.exists()) {
                    throw Exception("Physical file does not exist")
                }
                val parentDir = oldFile.parentFile ?: throw Exception("Parent directory not found")
                val extension = oldFile.extension
                
                var sanitizedName = newFileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                if (sanitizedName.isEmpty()) {
                    throw Exception("Filename cannot be empty")
                }
                
                if (!sanitizedName.endsWith(".$extension", ignoreCase = true)) {
                    sanitizedName = "$sanitizedName.$extension"
                }
                
                val newFile = File(parentDir, sanitizedName)
                if (newFile.exists() && newFile.absolutePath != oldFile.absolutePath) {
                    throw Exception("File with this name already exists")
                }

                activeSong = audioEngine.currentSong.value
                isCurrent = activeSong != null && activeSong.id == songId
                if (isCurrent) {
                    wasPlaying = audioEngine.isPlaying.value
                    currentPosition = audioEngine.position.value
                }
                
                audioEngine.stop()
                audioEngine.clearCurrentSource()
                
                System.gc()
                System.runFinalization()
                kotlinx.coroutines.delay(400)

                var success = false
                for (i in 1..5) {
                    success = oldFile.renameTo(newFile)
                    if (success) {
                        Log.d("MusicPlayerViewModel", "renameTo succeeded on attempt $i")
                        break
                    }
                    System.gc()
                    System.runFinalization()
                    kotlinx.coroutines.delay(200)
                }

                if (!success) {
                    try {
                        val contentResolver = getApplication<android.app.Application>().contentResolver
                        val songUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            songId.toLong()
                        )
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, sanitizedName)
                            put(android.provider.MediaStore.Audio.Media.DATA, newFile.absolutePath)
                        }
                        val updatedRows = contentResolver.update(songUri, values, null, null)
                        if (updatedRows > 0) {
                            if (newFile.exists()) {
                                success = true
                                if (oldFile.exists() && oldFile.absolutePath != newFile.absolutePath) {
                                    oldFile.delete()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicPlayerViewModel", "Failed to rename via ContentResolver update: ${e.message}")
                    }
                }

                if (!success) {
                    try {
                        oldFile.copyTo(newFile, overwrite = true)

                        var deleteSuccess = oldFile.delete()
                        if (!deleteSuccess) {
                            try {
                                val contentResolver = getApplication<android.app.Application>().contentResolver
                                val songUri = android.content.ContentUris.withAppendedId(
                                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    songId.toLong()
                                )
                                val deletedRows = contentResolver.delete(songUri, null, null)
                                if (deletedRows > 0) {
                                    deleteSuccess = true
                                }
                            } catch (ex: Exception) {
                                Log.e("MusicPlayerViewModel", "ContentResolver delete failed: ${ex.message}")
                            }
                        }

                        if (deleteSuccess) {
                            success = true
                        } else {
                            success = true
                            repository.markPendingCleanup(oldFile.absolutePath)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                try {
                                    val songUri = android.content.ContentUris.withAppendedId(
                                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        songId.toLong()
                                    )
                                    val deleteRequest = android.provider.MediaStore.createDeleteRequest(
                                        getApplication<android.app.Application>().contentResolver,
                                        listOf(songUri)
                                    )
                                    _pendingDeleteRequest.tryEmit(deleteRequest.intentSender)
                                } catch (e: Exception) {
                                    Log.e("MusicPlayerViewModel", "Failed to build delete consent request: ${e.message}")
                                }
                            } else {
                                try {
                                    java.io.FileOutputStream(oldFile).use { fos ->
                                        fos.write(ByteArray(0))
                                    }
                                } catch (e: Exception) {
                                    Log.w("MusicPlayerViewModel", "Could not truncate locked old file: ${e.message}")
                                }

                                viewModelScope.launch(Dispatchers.IO) {
                                    for (attempt in 1..15) {
                                        kotlinx.coroutines.delay(2000)
                                        System.gc()
                                        System.runFinalization()
                                        if (oldFile.delete()) {
                                            repository.clearPendingCleanup(oldFile.absolutePath)
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        throw Exception("Failed to copy file to new location: ${e.message}")
                    }
                }

                try {
                    val contentResolver = getApplication<android.app.Application>().contentResolver
                    contentResolver.delete(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        "${android.provider.MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(oldFile.absolutePath)
                    )
                } catch (e: Exception) {
                    Log.e("MusicPlayerViewModel", "Failed to delete old MediaStore row: ${e.message}")
                }

                try {
                    android.media.MediaScannerConnection.scanFile(
                        getApplication(),
                        arrayOf(oldFile.absolutePath, newFile.absolutePath),
                        null
                    ) { path, uri ->
                        viewModelScope.launch {
                            repository.scanAndSyncMusic()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicPlayerViewModel", "MediaScanner error: ${e.message}")
                }

                val updatedSong = song.copy(filePath = newFile.absolutePath)
                database.songDao().updateSong(updatedSong)

                if (isCurrent) {
                    audioEngine.updateCurrentSongMetadata(updatedSong)
                    if (wasPlaying) {
                        audioEngine.play(updatedSong)
                        if (currentPosition > 0) {
                            kotlinx.coroutines.delay(400)
                            audioEngine.seekTo(currentPosition)
                        }
                    } else {
                        audioEngine.setSongWithoutPlaying(updatedSong)
                    }
                } else if (activeSong != null) {
                    if (wasPlaying) {
                        audioEngine.play(activeSong)
                        if (currentPosition > 0) {
                            kotlinx.coroutines.delay(400)
                            audioEngine.seekTo(currentPosition)
                        }
                    } else {
                        audioEngine.setSongWithoutPlaying(activeSong)
                    }
                }

                withContext(Dispatchers.Main) {
                    onSuccess(newFile.name)
                }
            } catch (e: java.lang.Exception) {
                try {
                    val fallbackSong = database.songDao().getSongById(songId)
                    if (fallbackSong != null) {
                        if (wasPlaying) {
                            audioEngine.play(fallbackSong)
                            if (currentPosition > 0) {
                                kotlinx.coroutines.delay(400)
                                audioEngine.seekTo(currentPosition)
                            }
                        } else {
                            audioEngine.setSongWithoutPlaying(fallbackSong)
                        }
                    }
                } catch (ignore: Exception) {}
                
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun onDeleteConsentResult() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.scanAndSyncMusic()
        }
    }

    fun batchUpdateTags(
        songIds: List<String>,
        artist: String?,
        album: String?,
        albumArtist: String?,
        genre: String?,
        composer: String?,
        disc: String?,
        track: String?,
        year: String?,
        comment: String?,
        bpm: String?,
        rating: Int?,
        albumArtUri: String?,
        removeArt: Boolean
    ) {
        viewModelScope.launch {
            for (id in songIds) {
                val song = database.songDao().getSongById(id) ?: continue
                val updated = song.copy(
                    customArtist = artist?.ifEmpty { null } ?: song.customArtist,
                    customAlbum = album?.ifEmpty { null } ?: song.customAlbum,
                    customAlbumArtist = albumArtist?.ifEmpty { null } ?: song.customAlbumArtist,
                    customGenre = genre?.ifEmpty { null } ?: song.customGenre,
                    customComposer = composer?.ifEmpty { null } ?: song.customComposer,
                    customDisc = disc?.ifEmpty { null } ?: song.customDisc,
                    customTrack = track?.ifEmpty { null } ?: song.customTrack,
                    customYear = year?.ifEmpty { null } ?: song.customYear,
                    customComment = comment?.ifEmpty { null } ?: song.customComment,
                    customBpm = bpm?.ifEmpty { null } ?: song.customBpm,
                    rating = rating ?: song.rating,
                    albumArtUri = if (removeArt) null else (albumArtUri ?: song.albumArtUri)
                )
                repository.updateSong(updated)
                
                val current = audioEngine.currentSong.value
                if (current != null && current.id == id) {
                    audioEngine.updateCurrentSongMetadata(updated)
                }
            }
        }
    }

    private val _autoSearchArtistData = MutableStateFlow(true)
    val autoSearchArtistData: StateFlow<Boolean> = _autoSearchArtistData.asStateFlow()

    private val _autoSearchWifiOnly = MutableStateFlow(true)
    val autoSearchWifiOnly: StateFlow<Boolean> = _autoSearchWifiOnly.asStateFlow()

    private val _artistSummary = MutableStateFlow<String?>(null)
    val artistSummary: StateFlow<String?> = _artistSummary.asStateFlow()

    private val _artistArtworkUri = MutableStateFlow<String?>(null)
    val artistArtworkUri: StateFlow<String?> = _artistArtworkUri.asStateFlow()

    private val _onlineArtistImages = MutableStateFlow<List<String>>(emptyList())
    val onlineArtistImages: StateFlow<List<String>> = _onlineArtistImages.asStateFlow()

    private val _isFetchingArtistImages = MutableStateFlow(false)
    val isFetchingArtistImages: StateFlow<Boolean> = _isFetchingArtistImages.asStateFlow()

    private val _isSearchingArtistSummary = MutableStateFlow(false)
    val isSearchingArtistSummary: StateFlow<Boolean> = _isSearchingArtistSummary.asStateFlow()

    fun setAutoSearchArtistData(enabled: Boolean) {
        _autoSearchArtistData.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[AUTO_SEARCH_ARTIST_DATA_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving auto search artist data preference: ${e.message}")
            }
        }
    }

    fun setAutoSearchWifiOnly(enabled: Boolean) {
        _autoSearchWifiOnly.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[AUTO_SEARCH_WIFI_ONLY_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving auto search wifi only preference: ${e.message}")
            }
        }
    }

    fun setNextSongDelaySeconds(seconds: Int) {
        _nextSongDelaySeconds.value = seconds
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[PLAYBACK_DELAY_KEY] = seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving next song delay preference: ${e.message}")
            }
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[CROSSFADE_ENABLED_KEY] = enabled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving crossfade enabled preference: ${e.message}")
            }
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        _crossfadeDurationSeconds.value = seconds
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[CROSSFADE_DURATION_KEY] = seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving crossfade duration preference: ${e.message}")
            }
        }
    }

    fun cancelDelay() {
        delayJob?.cancel()
        delayJob = null
        _playbackDelayCountdown.value = null
    }

    fun triggerAutoNextWithDelay() {
        cancelDelay()
        val delaySecs = _nextSongDelaySeconds.value
        if (delaySecs <= 0) {
            skipNext()
            return
        }

        delayJob = viewModelScope.launch {
            for (remaining in delaySecs downTo 1) {
                _playbackDelayCountdown.value = remaining
                kotlinx.coroutines.delay(1000)
            }
            _playbackDelayCountdown.value = null
            delayJob = null
            skipNext()
        }
    }

    fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                } else false
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun loadArtistSummary(artistName: String) {
        viewModelScope.launch {
            val saved = repository.getArtistSummary(artistName)
            if (saved != null) {
                _artistSummary.value = saved.summary
                _artistArtworkUri.value = saved.artworkUri
            } else {
                _artistSummary.value = null
                _artistArtworkUri.value = null
                searchArtistSummaryOnline(artistName)
            }
        }
    }

    fun searchArtistSummaryOnline(artistName: String) {
        viewModelScope.launch {
            if (!_autoSearchArtistData.value) {
                _artistSummary.value = "Automatic search is disabled. Enable in Settings to fetch biography and images."
                return@launch
            }

            if (_autoSearchWifiOnly.value && !isWifiConnected()) {
                _artistSummary.value = "Automatic search is paused on mobile networks. Connect to Wi-Fi to fetch biography and images."
                return@launch
            }

            _isSearchingArtistSummary.value = true
            try {
                val summary = GeminiMusicService.fetchArtistSummaryFromAudioDB(artistName)
                
                var savedArtworkUri: String? = null
                try {
                    val images = GeminiMusicService.fetchArtistImagesFromAudioDB(artistName)
                    if (images.isNotEmpty()) {
                        savedArtworkUri = images.firstOrNull { !it.isNullOrBlank() }
                    }
                } catch (imgEx: Exception) {
                    Log.e("MusicPlayerViewModel", "Error fetching artist images automatically: ${imgEx.message}")
                }

                repository.saveArtistData(artistName, summary, savedArtworkUri)
                _artistSummary.value = summary
                _artistArtworkUri.value = savedArtworkUri
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error searching artist summary", e)
                _artistSummary.value = "Error: Failed to fetch biography from TheAudioDB. Please check your network connection."
            } finally {
                _isSearchingArtistSummary.value = false
            }
        }
    }

    fun fetchOnlineArtistImages(artistName: String) {
        viewModelScope.launch {
            _isFetchingArtistImages.value = true
            try {
                val images = GeminiMusicService.fetchArtistImagesFromAudioDB(artistName)
                _onlineArtistImages.value = images
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error fetching online artist images", e)
                _onlineArtistImages.value = emptyList()
            } finally {
                _isFetchingArtistImages.value = false
            }
        }
    }

    fun saveArtistArtworkUri(artistName: String, artworkUri: String?) {
        viewModelScope.launch {
            repository.saveArtistArtworkUri(artistName, artworkUri)
            _artistArtworkUri.value = artworkUri
        }
    }

    fun saveManualArtistSummary(artistName: String, summary: String) {
        viewModelScope.launch {
            repository.saveArtistSummary(artistName, summary)
            _artistSummary.value = summary
        }
    }

    override fun onCleared() {
        super.onCleared()
        karaokeMicEngine.stopMic()
    }
}
