package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.OniDatabase
import com.example.data.entity.EqualizerPresetEntity
import com.example.data.entity.SongEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.repository.MusicRepository
import com.example.playback.OniAudioEngine
import com.example.ui.theme.OniTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MusicPlayerViewModel"

    private val database = OniDatabase.getDatabase(application)
    private val repository = MusicRepository(application, database.songDao())
    val audioEngine = OniAudioEngine(application)

    // Library scroll position persistence
    var libraryScrollIndex: Int = 0
    var libraryScrollOffset: Int = 0

    // User selected theme state
    private val _currentTheme = MutableStateFlow(OniTheme.HIGH_DENSITY)
    val currentTheme: StateFlow<OniTheme> = _currentTheme.asStateFlow()

    // Screen selection / Tab state
    private val _currentTab = MutableStateFlow(1) // 0: Songs/Library, 1: Player, 2: Equalizer, 3: Themes
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Library category selection: 0=All, 1=Folders, 2=Albums, 3=Artists, 4=Genres, 5=Favorites
    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex.asStateFlow()

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

    // Shuffle and Repeat modes
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

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

    fun selectCategory(index: Int) {
        _selectedCategoryIndex.value = index
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTheme(theme: OniTheme) {
        _currentTheme.value = theme
    }

    // --- Media Playback Controls ---

    fun playSong(song: SongEntity, playlist: List<SongEntity>) {
        _currentPlaylist.value = playlist
        _currentTab.value = 1 // Switch to Player tab immediately

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
            playlist.indices.random()
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
            playlist.indices.random()
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

    override fun onCleared() {
        super.onCleared()
        if (activeInstance == this) {
            activeInstance = null
        }
        audioEngine.release()
    }

    companion object {
        var activeInstance: MusicPlayerViewModel? = null
    }
}
