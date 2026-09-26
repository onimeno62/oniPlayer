package com.example.ui.player.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.components.state.OniLoadingState
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.player.components.lyrics.CompactLyricsPlayer
import com.example.ui.player.components.lyrics.LyricsEmptyState
import com.example.ui.player.components.lyrics.LyricsHeader
import com.example.ui.player.components.lyrics.LyricsModeRow
import com.example.ui.player.components.lyrics.LyricsStatus
import com.example.ui.player.components.lyrics.LyricsTimingSheet
import com.example.ui.player.components.lyrics.LyricsToolsSheet
import com.example.ui.player.components.lyrics.LyricsTranslateSheet
import com.example.ui.player.components.lyrics.PlainLyricsList
import com.example.ui.player.components.lyrics.SingAlongStrip
import com.example.ui.player.components.lyrics.SyncedLyricsList
import com.example.ui.player.components.lyrics.TRANSLATION_ORIGINAL
import com.example.ui.player.components.lyrics.formatOffset
import com.example.ui.player.components.lyrics.rememberLyricsFollowState
import com.example.ui.screens.LyricsEditorDialog
import com.example.ui.screens.ManualSearchDialog
import com.example.ui.screens.SyncEditorDialog
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/** Mutually exclusive secondary surfaces of the lyrics screen (null = none open). */
private enum class LyricsSurface { Tools, Search, Editor, SyncEditor, AdjustTiming, Translate }

/**
 * Full-screen lyrics experience (Default Skin), Phase 9.
 *
 * Lyrics are the primary content. Everything else is contextual:
 * - Header: close, title/artist, one Lyrics tools action
 * - Synced | Plain only when synced lyrics exist
 * - Viewport: active line at the upper-middle focus position; drag pauses follow; Resume chip
 * - Compact player: progress + previous / play-pause / next
 * - Tools sheet: search, edit, sync editor, adjust timing, sing along, vocal cut, translate
 *
 * State ownership is unchanged: playback, lyrics persistence, vocal cut and translation stay in
 * [MusicPlayerViewModel]; the mic stays in its engine; follow/scroll state is UI-only.
 */
@Composable
fun PlayerKaraokeDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val micEngine = viewModel.karaokeMicEngine

    val currentPosition by viewModel.position.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isVocalReductionActive by viewModel.isVocalReductionActive.collectAsStateWithLifecycle()
    val isMicEnabled by micEngine.isMicEnabled.collectAsStateWithLifecycle()
    val micAmplitude by micEngine.amplitude.collectAsStateWithLifecycle()
    val micGain by micEngine.micGain.collectAsStateWithLifecycle()
    val isAudioPassThroughEnabled by micEngine.isAudioPassThroughEnabled.collectAsStateWithLifecycle()
    val pitchStatus by micEngine.pitchStatus.collectAsStateWithLifecycle()

    var activeSurface by remember { mutableStateOf<LyricsSurface?>(null) }

    // Translation (unchanged behaviour, moved from the header globe into Tools)
    var selectedLanguage by remember(song.id) { mutableStateOf(TRANSLATION_ORIGINAL) }
    var isTranslatingLyrics by remember(song.id) { mutableStateOf(false) }
    var translatedLyricsText by remember(song.id) { mutableStateOf<String?>(null) }

    // Unsaved timing preview (same model as the old Sync Nudge)
    var manualOffsetMs by remember(song.id) { mutableStateOf(0L) }

    val rawLyrics = song.lyrics ?: ""
    val activeLyricsText = remember(selectedLanguage, translatedLyricsText, rawLyrics) {
        val translated = translatedLyricsText
        if (selectedLanguage == TRANSLATION_ORIGINAL || translated.isNullOrBlank()) rawLyrics else translated
    }
    val isSyncedText = remember(activeLyricsText) { LyricsHelper.isSynced(activeLyricsText) }
    val parsedLrc = remember(activeLyricsText) {
        LyricsHelper.parseLrc(activeLyricsText).filter { it.text.isNotBlank() }
    }
    val effectivePosition = maxOf(0L, currentPosition + manualOffsetMs)
    val activeLrcIndex = remember(effectivePosition, parsedLrc) {
        LyricsHelper.getActiveLineIndex(parsedLrc, effectivePosition)
    }
    var showPlainTextMode by remember(activeLyricsText) { mutableStateOf(!isSyncedText) }

    // Resets to Following on track change, lyrics change, or mode switch.
    val followState = rememberLyricsFollowState(key = listOf(song.id, activeLyricsText, showPlainTextMode))

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micEngine.startMic()
        } else {
            Toast.makeText(context, "Microphone permission is required to sing along", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleSingAlong: () -> Unit = {
        when {
            isMicEnabled -> micEngine.stopMic()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> micEngine.startMic()
            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val toggleVocalCut: () -> Unit = {
        viewModel.toggleVocalReduction()
        val msg = if (!isVocalReductionActive) "Vocal cut on" else "Vocal cut off"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    val dismiss: () -> Unit = {
        micEngine.stopMic()
        onDismiss()
    }

    // Ensure the microphone is stopped when the screen leaves composition
    DisposableEffect(Unit) {
        onDispose { micEngine.stopMic() }
    }

    val hasTranslation = selectedLanguage != TRANSLATION_ORIGINAL && translatedLyricsText != null
    val statuses = buildList {
        if (isVocalReductionActive) add(LyricsStatus(Icons.Default.HearingDisabled, "Vocal cut"))
        if (hasTranslation) add(LyricsStatus(Icons.Default.Translate, selectedLanguage))
        if (manualOffsetMs != 0L) add(LyricsStatus(Icons.Default.Timer, "${formatOffset(manualOffsetMs)} not saved"))
    }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OniSkin.colors.background,
            contentColor = OniSkin.colors.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                LyricsHeader(
                    title = song.customTitle ?: song.title,
                    artist = song.customArtist ?: song.artist,
                    onClose = dismiss,
                    onOpenTools = { activeSurface = LyricsSurface.Tools }
                )

                LyricsModeRow(
                    showModeSelector = isSyncedText && parsedLrc.isNotEmpty() && !isTranslatingLyrics,
                    plainSelected = showPlainTextMode,
                    onSelectPlain = { showPlainTextMode = it },
                    statuses = statuses
                )

                if (isMicEnabled) {
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                    SingAlongStrip(
                        pitchStatus = pitchStatus,
                        amplitudeFraction = micAmplitude.toFloat() / 100f,
                        micGain = micGain,
                        passThroughEnabled = isAudioPassThroughEnabled,
                        onGainChange = { micEngine.setMicGain(it) },
                        onPassThroughChange = { micEngine.setAudioPassThrough(it) },
                        onStop = { micEngine.stopMic() }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isTranslatingLyrics -> OniLoadingState(
                            message = "Translating to $selectedLanguage…",
                            modifier = Modifier.align(Alignment.Center)
                        )
                        activeLyricsText.isBlank() -> LyricsEmptyState(
                            onSearchOnline = { activeSurface = LyricsSurface.Search },
                            onPasteManually = { activeSurface = LyricsSurface.Editor }
                        )
                        showPlainTextMode || parsedLrc.isEmpty() -> PlainLyricsList(text = activeLyricsText)
                        else -> SyncedLyricsList(
                            lines = parsedLrc,
                            activeIndex = activeLrcIndex,
                            followState = followState,
                            accentActiveLine = isMicEnabled,
                            onLineClick = { line ->
                                // Seek so the tapped line is active under the current timing preview.
                                viewModel.seekTo((line.timestampMs - manualOffsetMs).coerceAtLeast(0L))
                                followState.resume()
                            }
                        )
                    }
                }

                CompactLyricsPlayer(
                    positionMs = currentPosition,
                    durationMs = duration,
                    isPlaying = isPlaying,
                    onSeek = { viewModel.seekTo(it) },
                    onPrevious = { viewModel.skipPrevious() },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() }
                )
            }
        }
    }

    when (activeSurface) {
        LyricsSurface.Tools -> LyricsToolsSheet(
            hasLyrics = rawLyrics.isNotBlank(),
            isSynced = LyricsHelper.isSynced(rawLyrics),
            isSingAlongOn = isMicEnabled,
            isVocalCutOn = isVocalReductionActive,
            translationLabel = selectedLanguage.takeIf { hasTranslation },
            onDismiss = { activeSurface = null },
            onSearchOnline = { activeSurface = LyricsSurface.Search },
            onEditLyrics = { activeSurface = LyricsSurface.Editor },
            onOpenSyncEditor = { activeSurface = LyricsSurface.SyncEditor },
            onAdjustTiming = { activeSurface = LyricsSurface.AdjustTiming },
            onToggleSingAlong = toggleSingAlong,
            onToggleVocalCut = toggleVocalCut,
            onTranslate = { activeSurface = LyricsSurface.Translate }
        )

        LyricsSurface.Search -> ManualSearchDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { activeSurface = null }
        )

        LyricsSurface.Editor -> LyricsEditorDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { activeSurface = null }
        )

        LyricsSurface.SyncEditor -> SyncEditorDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { activeSurface = null }
        )

        LyricsSurface.AdjustTiming -> LyricsTimingSheet(
            offsetMs = manualOffsetMs,
            onNudge = { delta -> manualOffsetMs += delta },
            onReset = { manualOffsetMs = 0L },
            onApply = {
                viewModel.shiftSongLyricsTiming(song.id, rawLyrics, manualOffsetMs)
                manualOffsetMs = 0L
                Toast.makeText(context, "Timing saved", Toast.LENGTH_SHORT).show()
                activeSurface = null
            },
            onDismiss = { activeSurface = null }
        )

        LyricsSurface.Translate -> LyricsTranslateSheet(
            selectedLanguage = selectedLanguage,
            canSaveTranslation = hasTranslation,
            onSelect = { lang ->
                activeSurface = null
                if (lang == TRANSLATION_ORIGINAL) {
                    selectedLanguage = TRANSLATION_ORIGINAL
                    translatedLyricsText = null
                } else {
                    selectedLanguage = lang
                    isTranslatingLyrics = true
                    viewModel.translateSongLyrics(song.id, rawLyrics, lang) { result ->
                        isTranslatingLyrics = false
                        result.onSuccess { translated ->
                            translatedLyricsText = translated
                        }.onFailure { err ->
                            selectedLanguage = TRANSLATION_ORIGINAL
                            translatedLyricsText = null
                            Toast.makeText(context, "Translation failed: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onSaveTranslation = {
                viewModel.updateLyrics(song.id, translatedLyricsText)
                Toast.makeText(context, "Saved $selectedLanguage as song lyrics", Toast.LENGTH_SHORT).show()
                activeSurface = null
            },
            onDismiss = { activeSurface = null }
        )

        null -> Unit
    }
}
