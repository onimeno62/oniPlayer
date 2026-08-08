package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import com.example.ui.theme.OniTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

enum class ShuffleMode {
    RANDOM,          // pure random pick
    DISCOVER,        // weighted toward songs with low play counts
    FAVORITES_BOOST   // weighted toward favorited songs
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

    // User selected theme state
    private val _currentTheme = MutableStateFlow(OniTheme.HIGH_DENSITY)
    val currentTheme: StateFlow<OniTheme> = _currentTheme.asStateFlow()

    // User selected theme option name ("Light", "Dark", "AMOLED", "Follow System")
    private val _selectedThemeOption = MutableStateFlow("Dark")
    val selectedThemeOption: StateFlow<String> = _selectedThemeOption.asStateFlow()

    // Custom Accent Color hex string (defaulting to Purple: #7C4DFF)
    private val _customAccentColor = MutableStateFlow("#7C4DFF")
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
            // Fallback: infer context from the current song
            inferAndSetLibraryContextForCurrentSong()
        }
        _currentTab.value = 0
    }

    fun inferAndSetLibraryContextForCurrentSong() {
        val current = audioEngine.currentSong.value ?: return
        
        // If activeCategoryIndex is already set, don't override it (preserve user context)
        if (_activeCategoryIndex.value != null) return

        // Otherwise, try to infer it.
        // Let's default to setting the Artist or Folder context based on the current song!
        val artist = current.customArtist ?: current.artist
        if (artist.isNotBlank() && artist != "Unknown Artist" && artist != "<unknown>") {
            _activeCategoryIndex.value = 3 // Artists category index is 3
            _selectedGroup.value = artist
            return
        }

        // Fallback to Folder if artist is not set
        val filePath = current.filePath
        val file = File(filePath)
        val folderName = file.parentFile?.name ?: "Internal Storage"
        if (folderName.isNotBlank()) {
            _activeCategoryIndex.value = 1 // Folders category index is 1
            _selectedGroup.value = folderName
            return
        }

        // Ultimate fallback: All Songs (category 0)
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

    private val _floatingLyricsEnabled = MutableStateFlow(false)
    val floatingLyricsEnabled: StateFlow<Boolean> = _floatingLyricsEnabled.asStateFlow()

    // Online Tag Edit state
    private val _isOptimizingTags = MutableStateFlow(false)
    val isOptimizingTags: StateFlow<Boolean> = _isOptimizingTags.asStateFlow()

    // Active playlist being played
    private val _currentPlaylist = MutableStateFlow<List<SongEntity>>(emptyList())
    val currentPlaylist: StateFlow<List<SongEntity>> = _currentPlaylist.asStateFlow()

    // Emits an IntentSender whenever the OS needs one-time user consent to delete a
    // MediaStore-owned file we don't have direct write access to (rename cleanup).
    private val _pendingDeleteRequest = MutableSharedFlow<android.content.IntentSender>(extraBufferCapacity = 1)
    val pendingDeleteRequest: SharedFlow<android.content.IntentSender> = _pendingDeleteRequest.asSharedFlow()

    // Shuffle and Repeat modes
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    // Which algorithm _isShuffle uses when it's on. Independent of the on/off toggle so the
    // existing Player screen shuffle button keeps working unchanged — it only flips _isShuffle,
    // while this remembers which algorithm to use whenever shuffle is active.
    private val _shuffleMode = MutableStateFlow(ShuffleMode.RANDOM)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    // Search query for library
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Expose flows from Repository
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPresets: StateFlow<List<EqualizerPresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        activeInstance = this
        scanAndLoad()
        
        // Load saved theme option from DataStore
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
                    .map { preferences -> preferences[ACCENT_COLOR_KEY] ?: "#7C4DFF" }
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
        
        // Listen to completion of song to auto-skip
        audioEngine.onPlaybackCompleted = {
            skipNext()
        }
    }

    private fun scanAndLoad() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.scanAndSyncMusic()
            _isScanning.value = false

            // Set initial playlist as all songs if none loaded
            val initialSongs = repository.allSongs.first()
            if (initialSongs.isNotEmpty()) {
                _currentPlaylist.value = initialSongs
                
                // Find last played song (highest lastPlayedTimestamp > 0)
                val lastPlayedSong = initialSongs.filter { it.lastPlayedTimestamp > 0 }
                    .maxByOrNull { it.lastPlayedTimestamp } ?: initialSongs.first()
                
                // Pre-load metadata into audioEngine without playing
                audioEngine.setSongWithoutPlaying(lastPlayedSong)
            }
        }
    }

    fun rescanLibrary() {
        scanAndLoad()
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    // Always resets Library back to its dashboard, regardless of what category or
    // search state was left active — used specifically by the bottom nav's Library tab.
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

    // --- Media Playback Controls ---

    fun playSong(song: SongEntity, playlist: List<SongEntity>) {
        _currentPlaylist.value = playlist
        _currentTab.value = 1 // Switch to Player tab immediately

        // Capture current library context when a song starts playing
        _playedCategoryIndex.value = _activeCategoryIndex.value
        _playedSelectedGroup.value = _selectedGroup.value
        _playedActivePlaylist.value = _activePlaylist.value
        _playedActiveSmartPlaylistType.value = _activeSmartPlaylistType.value

        viewModelScope.launch {
            // 1. Fetch freshest database state to preserve manual tag/lyrics updates
            val dbSong = database.songDao().getSongById(song.id) ?: song
            
            // 2. Update play tracking info
            val updated = dbSong.copy(
                playCount = dbSong.playCount + 1,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
            repository.updateSong(updated)
            
            // 3. Play the song immediately so playback starts instantly
            audioEngine.play(updated)
            
            // 4. Handle lyrics loading sequentially
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
        val currentList = _currentPlaylist.value.toMutableList()
        val currentSong = audioEngine.currentSong.value
        if (currentSong == null) {
            playSong(song, listOf(song))
            return
        }
        val currentIndex = currentList.indexOfFirst { it.id == currentSong.id }
        // Remove duplicate if already present in the future part of playlist to make queue clean
        currentList.remove(song)
        val insertIndex = if (currentIndex == -1) 0 else currentIndex + 1
        currentList.add(insertIndex, song)
        _currentPlaylist.value = currentList
    }

    fun addToQueue(song: SongEntity) {
        val currentList = _currentPlaylist.value.toMutableList()
        if (!currentList.any { it.id == song.id }) {
            currentList.add(song)
            _currentPlaylist.value = currentList
        }
    }

    fun togglePlayPause() {
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

    // Public entry point for "Shuffle All"-style quick actions that don't have a "current song"
    // to exclude — picks a starting song using the same weighting as pickShuffleIndex.
    fun pickShuffleStartSong(playlist: List<SongEntity>): SongEntity? {
        if (playlist.isEmpty()) return null
        val index = pickShuffleIndex(playlist, excludeIndex = -1)
        return playlist.getOrNull(index)
    }

    // Picks a shuffle target according to the active ShuffleMode. Falls back to pure random
    // whenever weights can't produce a meaningful bias (e.g. everyone has the same play count).
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

    // Picking a mode also turns shuffle on, since choosing an algorithm implies wanting it active.
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

    // --- Online Lyrics Search (Real-world Public Databases) ---

    fun setAutoDownloadEnabled(enabled: Boolean) {
        _isAutoDownloadEnabled.value = enabled
    }

    fun setFloatingLyricsEnabled(enabled: Boolean) {
        _floatingLyricsEnabled.value = enabled
    }

    fun updateLyrics(songId: String, text: String?) {
        viewModelScope.launch {
            database.songDao().updateLyrics(songId, text)
            // Refresh song state in audio engine if it is the active song
            val current = audioEngine.currentSong.value
            if (current != null && current.id == songId) {
                val updated = database.songDao().getSongById(songId)
                if (updated != null) {
                    audioEngine.updateCurrentSongMetadata(updated) // reload updated lyrics without restarting playback
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
                // Refresh the current song entity in player state
                val updatedSong = database.songDao().getSongById(current.id)
                if (updatedSong != null) {
                    audioEngine.updateCurrentSongMetadata(updatedSong) // reload so lyrics is in state without restarting playback
                }
            }
            _isFetchingLyrics.value = false
        }
    }

    // --- Online Metadata Tag Editor (AI powered by Gemini) ---

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
            // If the deleted song is currently playing, skip to next or stop playback
            if (audioEngine.currentSong.value?.id == songId) {
                skipNext()
                // If it is still the same (e.g. only 1 song was in playlist), stop it
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

            // Always delete from local DB library
            deleteSong(songId)
        }
    }

    // --- Equalizer Customization ---

    fun updateBand(bandIndex: Int, gainDb: Float) {
        audioEngine.setBandGain(bandIndex, gainDb)
        // Update states
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

    // --- Playlist CRUD ---
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val id = "playlist_" + System.currentTimeMillis()
            val playlist = PlaylistEntity(id = id, name = name, songIdsJson = "[]")
            repository.insertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylistById(playlistId)
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
            // also refresh in audioEngine if needed
            val current = audioEngine.currentSong.value
            if (current != null && current.id == songId) {
                audioEngine.updateCurrentSongMetadata(updated)
            }
        }
    }

    // --- Standard Tag Editor Manual Update ---
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
            
            // Sync with current playing song if edited without restarting audio
            val current = audioEngine.currentSong.value
            if (current != null && current.id == songId) {
                audioEngine.updateCurrentSongMetadata(updated)
            }
        }
    }

    // --- Rename File Feature ---
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
                
                // Sanitize to prevent path traversal/invalid chars
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

                // 1. Fully release player source & active audio handles just in case
                activeSong = audioEngine.currentSong.value
                isCurrent = activeSong != null && activeSong.id == songId
                if (isCurrent) {
                    wasPlaying = audioEngine.isPlaying.value
                    currentPosition = audioEngine.position.value
                }
                
                // Always pause, stop, and clear current source to release file descriptors held by MediaPlayer
                audioEngine.stop()
                audioEngine.clearCurrentSource()
                
                // Trigger garbage collection to release any unreferenced file descriptors in retriever or player
                System.gc()
                System.runFinalization()
                kotlinx.coroutines.delay(400)

                var success = false
                // Attempt renameTo first
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
                    Log.d("MusicPlayerViewModel", "renameTo failed, trying to update MediaStore DisplayName via ContentResolver")
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
                            Log.d("MusicPlayerViewModel", "MediaStore update display_name & data succeeded")
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
                    Log.d("MusicPlayerViewModel", "ContentResolver update failed, falling back to copy & delete")
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
                            Log.d("MusicPlayerViewModel", "Successfully deleted original file after copying.")
                        } else {
                            // The old file couldn't be removed — almost always because it's a
                            // MediaStore-owned file on scoped storage and we lack write access to it.
                            // The rename itself still succeeds (the song now points at newFile), but
                            // we must never let the leftover old file silently reappear as a duplicate.
                            success = true
                            repository.markPendingCleanup(oldFile.absolutePath)
                            Log.w(
                                "MusicPlayerViewModel",
                                "Could not delete original file (likely missing storage permission). " +
                                "Tracking as pending cleanup: ${oldFile.absolutePath}"
                            )

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                // Ask the user for one-time consent to delete this specific file.
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
                                // Pre-Android 11: no consent API available. Truncate to reclaim disk
                                // space if possible; the pending-cleanup filter in scanAndSyncMusic()
                                // is what actually prevents the duplicate, whether or not this works.
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
                                            Log.d("MusicPlayerViewModel", "Deleted locked original file on background attempt $attempt")
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

                // Explicitly delete the old file entry from MediaStore database using ContentResolver to prevent duplicates
                try {
                    val contentResolver = getApplication<android.app.Application>().contentResolver
                    val deletedRows = contentResolver.delete(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        "${android.provider.MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(oldFile.absolutePath)
                    )
                    Log.d("MusicPlayerViewModel", "ContentResolver deleted old MediaStore row after successful rename: $deletedRows rows")
                } catch (e: Exception) {
                    Log.e("MusicPlayerViewModel", "Failed to delete old MediaStore row: ${e.message}")
                }

                // Notify media scanner about both the old path (to remove it) and new path (to index it)
                try {
                    android.media.MediaScannerConnection.scanFile(
                        getApplication(),
                        arrayOf(oldFile.absolutePath, newFile.absolutePath),
                        null
                    ) { path, uri ->
                        Log.d("MusicPlayerViewModel", "Scanned path: $path, uri: $uri")
                        viewModelScope.launch {
                            repository.scanAndSyncMusic()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicPlayerViewModel", "MediaScanner error: ${e.message}")
                }

                val updatedSong = song.copy(filePath = newFile.absolutePath)
                database.songDao().updateSong(updatedSong)

                // Restore player state with updated file path
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
                    // Reload the active song if it was some other song that we stopped
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
                // Restore active player state if we stopped it
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

    /**
     * Called by MainActivity after the user approves or denies the MediaStore delete-consent
     * prompt triggered from renameSongFile(). Re-syncs so the pending-cleanup filter re-checks
     * whether the leftover file is actually gone now.
     */
    fun onDeleteConsentResult() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.scanAndSyncMusic()
        }
    }

    // --- Batch Tag Editor Update ---
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
                
                // Sync with current playing song if edited without restarting audio
                val current = audioEngine.currentSong.value
                if (current != null && current.id == id) {
                    audioEngine.updateCurrentSongMetadata(updated)
                }
            }
        }
    }

    // --- Artist Summary ---
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

    fun loadArtistSummary(artistName: String) {
        viewModelScope.launch {
            val saved = repository.getArtistSummary(artistName)
            if (saved != null) {
                _artistSummary.value = saved.summary
                _artistArtworkUri.value = saved.artworkUri
            } else {
                _artistSummary.value = null
                _artistArtworkUri.value = null
                // Trigger auto-search if not exists
                searchArtistSummaryOnline(artistName)
            }
        }
    }

    fun searchArtistSummaryOnline(artistName: String) {
        viewModelScope.launch {
            _isSearchingArtistSummary.value = true
            try {
                val summary = GeminiMusicService.fetchArtistSummaryFromAudioDB(artistName)
                repository.saveArtistSummary(artistName, summary)
                _artistSummary.value = summary
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
        if (activeInstance == this) {
            activeInstance = null
        }
        karaokeMicEngine.stopMic()
        // Note: We don't call audioEngine.release() here because the player is a shared singleton
        // and background playback is managed by the MusicPlaybackService.
    }

    companion object {
        var activeInstance: MusicPlayerViewModel? = null
    }
}
