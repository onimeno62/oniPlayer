package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * Playback options settings screen migrated to oniPlayer Default Skin.
 * Preserves all delay and crossfade preference controls.
 */
@Composable
fun PlaybackSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val nextSongDelaySeconds by viewModel.nextSongDelaySeconds.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Playback Options",
            subtitle = "Delay, crossfade, and queue behavior",
            onBack = onBack,
            backButtonTestTag = "playback_back_button"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            // Playback Delay Section
            item {
                SettingSection(
                    title = "Auto-Play Delay Settings",
                    description = "Set a custom minutes/seconds delay before automatically playing the next song."
                ) {
                    DurationPickerSettingRow(
                        title = "Next Song Delay",
                        totalSeconds = nextSongDelaySeconds,
                        onValueChange = { viewModel.setNextSongDelaySeconds(it) },
                        testTag = "setting_playback_delay"
                    )
                }
            }

            // Crossfade Section
            item {
                SettingSection(
                    title = "Playback Crossfade Settings",
                    description = "Smoothly fade out the current song and fade in the next song near the end of playback."
                ) {
                    SwitchSettingRow(
                        title = "Enable Crossfade",
                        description = "Fade tracks into each other.",
                        checked = crossfadeEnabled,
                        onCheckedChange = { viewModel.setCrossfadeEnabled(it) },
                        testTag = "setting_crossfade_enabled"
                    )

                    if (crossfadeEnabled) {
                        SettingDivider()
                        SliderSettingRow(
                            title = "Crossfade Duration",
                            value = crossfadeDurationSeconds.toFloat(),
                            onValueChange = { viewModel.setCrossfadeDurationSeconds(it.toInt()) },
                            valueRange = 1f..20f,
                            valueFormatter = { value -> "${value.toInt()} seconds" },
                            testTag = "setting_crossfade_duration"
                        )
                    }
                }
            }
        }
    }
}
