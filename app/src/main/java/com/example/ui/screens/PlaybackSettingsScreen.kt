package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.PlayerSettingsStore
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

private val SpeedPresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Playback behaviour: transitions, speed/pitch, queue behaviour, audio focus and outputs. */
@Composable
fun PlaybackSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val nextSongDelaySeconds by viewModel.nextSongDelaySeconds.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val settings by rememberPlayerSettings()

    SettingsPage(
        title = "Playback",
        subtitle = "Transitions, speed, and playback behavior",
        onBack = onBack,
        backButtonTestTag = "playback_back_button"
    ) {
        item {
            SettingSection(
                title = "Transitions",
                description = "What happens between two songs. Gapless is used when both are off."
            ) {
                SwitchSettingRow(
                    title = "Crossfade",
                    description = "Fade the current song out while the next one fades in.",
                    checked = crossfadeEnabled,
                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) },
                    testTag = "setting_crossfade_enabled"
                )
                if (crossfadeEnabled) {
                    SettingDivider()
                    SliderSettingRow(
                        title = "Crossfade duration",
                        value = crossfadeDurationSeconds.toFloat(),
                        onValueChange = { viewModel.setCrossfadeDurationSeconds(it.toInt()) },
                        valueRange = 1f..20f,
                        valueFormatter = { value -> "${value.toInt()} s" },
                        testTag = "setting_crossfade_duration"
                    )
                }
                SettingDivider()
                DurationPickerSettingRow(
                    title = "Pause between songs",
                    totalSeconds = nextSongDelaySeconds,
                    onValueChange = { viewModel.setNextSongDelaySeconds(it) },
                    testTag = "setting_playback_delay"
                )
            }
        }

        item {
            SettingSection(
                title = "Speed & pitch",
                description = "Applied instantly to everything you play. Handy for audiobooks, podcasts and practice."
            ) {
                SettingsChoiceRow(
                    title = "Quick speed",
                    options = SpeedPresets.map { formatPlaybackRate(it) },
                    selectedIndex = SpeedPresets.indexOfFirst { abs(it - settings.playbackSpeed) < 0.001f },
                    onSelect = { PlayerSettingsStore.update(context, PlayerSettingsStore.PLAYBACK_SPEED, SpeedPresets[it]) },
                    testTag = "setting_speed_presets"
                )
                SettingDivider()
                SliderSettingRow(
                    title = "Playback speed",
                    value = settings.playbackSpeed.coerceIn(0.5f, 2f),
                    onValueChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.PLAYBACK_SPEED, (it * 20f).roundToInt() / 20f) },
                    valueRange = 0.5f..2f,
                    steps = 29,
                    valueFormatter = { formatPlaybackRate(it) },
                    testTag = "setting_playback_speed"
                )
                SettingDivider()
                SliderSettingRow(
                    title = "Pitch",
                    value = settings.playbackPitch.coerceIn(0.5f, 2f),
                    onValueChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.PLAYBACK_PITCH, (it * 20f).roundToInt() / 20f) },
                    valueRange = 0.5f..2f,
                    steps = 29,
                    valueFormatter = { formatPlaybackRate(it) },
                    testTag = "setting_playback_pitch"
                )
                if (settings.playbackSpeed != 1f || settings.playbackPitch != 1f) {
                    SettingDivider()
                    SettingsActionRow(
                        title = "Reset to normal",
                        description = "Back to 1x speed and original pitch.",
                        icon = Icons.Default.RestartAlt,
                        showChevron = false,
                        onClick = { PlayerSettingsStore.resetSpeedAndPitch(context) },
                        testTag = "setting_reset_speed_pitch"
                    )
                }
            }
        }

        item {
            SettingSection(
                title = "Behavior",
                description = "How the player reacts to your controls and restarts."
            ) {
                SwitchSettingRow(
                    title = "Restart song on previous",
                    description = "After 3 seconds, Previous restarts the current song instead of jumping back.",
                    checked = settings.rewindOnPrevious,
                    onCheckedChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.REWIND_ON_PREVIOUS, it) },
                    testTag = "setting_rewind_on_previous"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Remember position",
                    description = "Reopen the app exactly where you left off in the current song.",
                    checked = settings.rememberPosition,
                    onCheckedChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.REMEMBER_POSITION, it) },
                    testTag = "setting_remember_position"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Skip silence",
                    description = "Trim silent gaps inside tracks. Best for spoken word.",
                    checked = settings.skipSilence,
                    onCheckedChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.SKIP_SILENCE, it) },
                    testTag = "setting_skip_silence"
                )
            }
        }

        item {
            SettingSection(
                title = "Audio focus & outputs",
                description = "Play nicely with calls, navigation, other apps and headphones."
            ) {
                SwitchSettingRow(
                    title = "Pause for other audio",
                    description = "Pause for calls and other players, duck for short sounds, then resume automatically.",
                    checked = settings.handleAudioFocus,
                    onCheckedChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.HANDLE_AUDIO_FOCUS, it) },
                    testTag = "setting_audio_focus"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Pause on headphone disconnect",
                    description = "Stop playback when wired or Bluetooth headphones disconnect.",
                    checked = settings.pauseOnDisconnect,
                    onCheckedChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.PAUSE_ON_DISCONNECT, it) },
                    testTag = "setting_pause_on_disconnect"
                )
            }
        }
    }
}
