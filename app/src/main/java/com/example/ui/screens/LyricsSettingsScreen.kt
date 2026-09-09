package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * Real lyrics settings screen wired to floating lyrics service and synchronization engine.
 */
@Composable
fun LyricsSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val floatingLyricsEnabled by viewModel.floatingLyricsEnabled.collectAsStateWithLifecycle()
    val isAutoDownloadLyrics by viewModel.isAutoDownloadEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Lyrics Settings",
            subtitle = "Floating lyrics, auto-fetching, and synchronization",
            onBack = onBack,
            backButtonTestTag = "lyrics_settings_back_button"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            item {
                SettingSection(
                    title = "Floating Lyrics Window",
                    description = "Show synchronized floating lyrics overlay on top of other applications."
                ) {
                    SwitchSettingRow(
                        title = "Enable Floating Lyrics",
                        description = "Displays compact or expanded karaoke window during music playback.",
                        checked = floatingLyricsEnabled,
                        onCheckedChange = { viewModel.setFloatingLyricsEnabled(it) },
                        testTag = "setting_floating_lyrics"
                    )
                }
            }

            item {
                SettingSection(
                    title = "Online Lyrics Database",
                    description = "Automatically search LRCLIB, Lyrist, and open lyric repositories."
                ) {
                    SwitchSettingRow(
                        title = "Auto-Download Lyrics",
                        description = "Fetch LRC synchronized lyrics automatically when playing a song without lyrics.",
                        checked = isAutoDownloadLyrics,
                        onCheckedChange = { viewModel.setAutoDownloadEnabled(it) },
                        testTag = "setting_auto_download_lyrics"
                    )
                }
            }
        }
    }
}

@Composable
fun AudioEqualizerSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Audio & Equalizer",
            subtitle = "Equalizer, DSP, and sound enhancement",
            onBack = onBack,
            backButtonTestTag = "audio_eq_back_button"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
        ) {
            EqualizerScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun StorageSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Storage & Cache",
            subtitle = "Library indexing and media cache",
            onBack = onBack,
            backButtonTestTag = "storage_back_button"
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            item {
                SettingSection(
                    title = "Media Storage Scanner",
                    description = "Manage local files and scan directories."
                ) {
                    SwitchSettingRow(
                        title = "Fast MediaStore Scan",
                        description = "Query Android media provider directly for quick library indexing.",
                        checked = true,
                        onCheckedChange = {},
                        enabled = false
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "About oniPlayer",
            subtitle = "Version, design system, and license",
            onBack = onBack,
            backButtonTestTag = "about_back_button"
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            item {
                SettingSection(
                    title = "Application Information",
                    description = "Build and identity specifications."
                ) {
                    Column(modifier = Modifier.padding(OniSkin.spacing.md)) {
                        Text("oniPlayer Android", style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                        Text("Version 1.0 (Production Build)", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Default Skin Design System v1.0", style = OniSkin.typography.labelMedium, color = OniSkin.colors.primary)
                        Text("Soft surfaces + strong hierarchy + restrained accent + beautiful artwork", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSettingsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Help & Guidance",
            subtitle = "Navigation and gestures reference",
            onBack = onBack,
            backButtonTestTag = "help_back_button"
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            item {
                SettingSection(
                    title = "Player Gestures",
                    description = "Convenient interaction shortcuts."
                ) {
                    Column(modifier = Modifier.padding(OniSkin.spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Swipe Artwork Left / Right: Skip to next or previous track", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Tap Artwork: Toggle Play / Pause", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Long Press Track: Open contextual track options", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                    }
                }
            }
        }
    }
}
