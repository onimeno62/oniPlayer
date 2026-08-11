from pathlib import Path
import re

p = Path("app/src/main/java/com/example/ui/viewmodel/MusicPlayerViewModel.kt")
s = p.read_text()

s = s.replace("import com.example.playback.OniAudioEngine\n", "import com.example.playback.OniAudioEngine\nimport com.example.playback.ShuffleMode\n", 1)
s = re.sub(r"\nenum class ShuffleMode \{.*?\n\}\n\nclass MusicPlayerViewModel", "\nclass MusicPlayerViewModel", s, count=1, flags=re.S)
s = s.replace("        activeInstance = this\n", "", 1)
s = re.sub(r"\n        // Listen to completion of song to auto-skip\n        audioEngine\.onPlaybackCompleted = \{\n            triggerAutoNextWithDelay\(\)\n        \}\n", "\n", s, count=1)
s = re.sub(r"\n    override fun onCleared\(\) \{\n        super\.onCleared\(\)\n        if \(activeInstance == this\) \{\n            activeInstance = null\n        \}\n        karaokeMicEngine\.stopMic\(\)\n        // Note: We don't call audioEngine\.release\(\) here because the player is a shared singleton\n        // and background playback is managed by the MusicPlaybackService\.\n    \}\n\n    companion object \{\n        var activeInstance: MusicPlayerViewModel\? = null\n    \}\n", "\n    override fun onCleared() {\n        super.onCleared()\n        karaokeMicEngine.stopMic()\n        // Playback is owned by MusicPlaybackService and survives ViewModel destruction.\n    }\n", s, count=1, flags=re.S)

s = re.sub(r"\n    // Active playlist being played\n    private val _currentPlaylist = MutableStateFlow<List<SongEntity>>\(emptyList\(\)\)\n    val currentPlaylist: StateFlow<List<SongEntity>> = _currentPlaylist\.asStateFlow\(\)\n", "\n    // Playback queue is owned by the service/controller.\n    val currentPlaylist: StateFlow<List<SongEntity>> = audioEngine.queue\n", s, count=1)
s = re.sub(r"\n    // Shuffle and Repeat modes\n    private val _isShuffle = MutableStateFlow\(false\)\n    val isShuffle: StateFlow<Boolean> = _isShuffle\.asStateFlow\(\)\n\n    private val _isRepeat = MutableStateFlow\(false\)\n    val isRepeat: StateFlow<Boolean> = _isRepeat\.asStateFlow\(\)\n\n    // Which algorithm _isShuffle uses when it's on\. Independent of the on/off toggle so the\n    // existing Player screen shuffle button keeps working unchanged — it only flips _isShuffle,\n    // while this remembers which algorithm to use whenever shuffle is active\.\n    private val _shuffleMode = MutableStateFlow\(ShuffleMode\.RANDOM\)\n    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode\.asStateFlow\(\)\n", "\n    // Shuffle/repeat state is owned by the service/controller.\n    val isShuffle: StateFlow<Boolean> = audioEngine.isShuffle\n    val isRepeat: StateFlow<Boolean> = audioEngine.isRepeat\n    val shuffleMode: StateFlow<ShuffleMode> = audioEngine.shuffleMode\n", s, count=1, flags=re.S)
s = re.sub(r"\n    private val _playbackDelayCountdown = MutableStateFlow<Int\?>\(null\)\n    val playbackDelayCountdown: StateFlow<Int\?> = _playbackDelayCountdown\.asStateFlow\(\)\n\n    private var delayJob: Job\? = null\n", "\n    val playbackDelayCountdown: StateFlow<Int?> = audioEngine.autoNextCountdown\n", s, count=1)

s = s.replace("                _currentPlaylist.value = initialSongs\n                \n", "", 1)
s = re.sub(r"\n                // Find last played song \(highest lastPlayedTimestamp > 0\).*?                audioEngine\.setSongWithoutPlaying\(lastPlayedSong\)\n", "\n", s, count=1, flags=re.S)
s = s.replace("        _currentPlaylist.value = playlist\n", "", 1)
s = s.replace("            audioEngine.play(updated)\n", "            val startIndex = playlist.indexOfFirst { it.id == updated.id }.coerceAtLeast(0)\n            val queue = playlist.map { if (it.id == updated.id) updated else it }\n            audioEngine.setQueue(queue, startIndex, true)\n", 1)
s = s.replace("val playlist = _currentPlaylist.value.ifEmpty {", "val playlist = currentPlaylist.value.ifEmpty {", 1)

s = re.sub(r"    fun playNext\(song: SongEntity\) \{.*?    fun togglePlayPause\(\) \{.*?    \}\n\n    // Public entry point", """    fun playNext(song: SongEntity) {
        audioEngine.playNext(song)
    }

    fun addToQueue(song: SongEntity) {
        audioEngine.addToQueue(song)
    }

    fun togglePlayPause() {
        cancelDelay()
        if (audioEngine.isPlaying.value) {
            audioEngine.pause()
        } else if (audioEngine.currentSong.value == null && currentPlaylist.value.isNotEmpty()) {
            audioEngine.setQueue(currentPlaylist.value, 0, true)
        } else {
            audioEngine.resume()
        }
    }

    // Public entry point""", s, count=1, flags=re.S)
s = s.replace("when (_shuffleMode.value) {", "when (shuffleMode.value) {")
s = re.sub(r"    fun skipNext\(\) \{.*?    fun toggleRepeat\(\) \{.*?    \}\n", """    fun skipNext() {
        cancelDelay()
        audioEngine.next()
    }

    fun skipPrevious() {
        cancelDelay()
        audioEngine.previous()
    }

    fun toggleShuffle() {
        audioEngine.setShuffle(!isShuffle.value, shuffleMode.value)
    }

    // Picking a mode also turns shuffle on, since choosing an algorithm implies wanting it active.
    fun setShuffleMode(mode: ShuffleMode) {
        audioEngine.setShuffle(true, mode)
    }

    fun toggleRepeat() {
        audioEngine.setRepeat(!isRepeat.value)
    }
""", s, count=1, flags=re.S)
s = s.replace("        _nextSongDelaySeconds.value = seconds\n", "        _nextSongDelaySeconds.value = seconds\n        audioEngine.setAutoNextDelay(seconds)\n", 1)
s = re.sub(r"\n    fun cancelDelay\(\) \{.*?    fun triggerAutoNextWithDelay\(\) \{.*?    \}\n", """\n    fun cancelDelay() {
        audioEngine.cancelPendingNext()
    }

    fun triggerAutoNextWithDelay() {
        audioEngine.triggerAutoNextWithDelay()
    }
""", s, count=1, flags=re.S)

for marker in ("activeInstance", "private val _currentPlaylist", "private val _isShuffle", "private val _isRepeat", "private val _shuffleMode", "audioEngine.onPlaybackCompleted"):
    if marker in s:
        raise SystemExit("cleanup incomplete: " + marker)
p.write_text(s)

e = Path("app/src/main/java/com/example/playback/OniAudioEngine.kt")
es = e.read_text()
marker = "    val bufferedPosition: StateFlow<Long> = client.bufferedPosition\n"
if "val autoNextCountdown: StateFlow<Int?>" not in es:
    es = es.replace(marker, marker + "    val autoNextCountdown: StateFlow<Int?> = client.autoNextCountdown\n", 1)
    e.write_text(es)
