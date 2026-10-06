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
        subtitle = "Choose how songs transition and how playback behaves.",
        onBack = onBack,
        backButtonTestTag = "playback_back_button"
    ) {
        item {
            SettingSection(
                title = "Transitions",
                description = "Song transitions"
            ) {
                SwitchSettingRow(
                    title = "Crossfade",
                    description = "Gradually fades between tracks.",
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
                SliderSettingRow(
                    title = "Pause between songs",
                    value = nextSongDelaySeconds.toFloat().coerceIn(0f, 600f),
                    onValueChange = {
                        val seconds = (it / 10f).roundToInt() * 10
                        viewModel.setNextSongDelaySeconds(seconds)
                    },
                    valueRange = 0f..600f,
                    steps = 59,
                    valueFormatter = { seconds ->
                        if (seconds <= 0f) "Off"
                        else if (seconds < 60f) "${seconds.toInt()} s"
                        else "${seconds.toInt() / 60}m ${seconds.toInt() % 60}s"
                    },
                    testTag = "setting_playback_delay"
                )
            }
        }

        item {
            SettingSection(
                title = "Speed & pitch",
                description = "Playback rate and pitch controls."
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
                description = "Controls for previous, resume, and silence skipping."
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
                description = "Audio focus and output-device behavior."
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
