package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val totalDurationMs = remember(songs) { songs.sumOf { it.duration } }
    val formattedDuration = remember(totalDurationMs) { formatDuration(totalDurationMs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Storage & Cache",
            subtitle = "Library storage, database, and local media index",
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
                    title = "Library Storage Index",
                    description = "Current cached database records from local MediaStore."
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Indexed Songs", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary)
                            Text("${songs.size} tracks", style = OniSkin.typography.labelMedium, color = OniSkin.colors.primary)
                        }
                        SettingDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Audio Duration", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary)
                            Text(formattedDuration, style = OniSkin.typography.labelMedium, color = OniSkin.colors.primary)
                        }
                        SettingDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Custom Playlists", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary)
                            Text("${playlists.size} playlists", style = OniSkin.typography.labelMedium, color = OniSkin.colors.primary)
                        }
                        SettingDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saved Equalizer Presets", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary)
                            Text("${presets.size} presets", style = OniSkin.typography.labelMedium, color = OniSkin.colors.primary)
                        }
                    }
                }
            }

            item {
                SettingSection(
                    title = "Storage Diagnostics",
                    description = "Android platform scoped storage information."
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Scoped Storage • Scoped Media Access",
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textPrimary
                        )
                        Text(
                            text = "Audio files are referenced via system URIs and local file descriptors without invasive storage modifications.",
                            style = OniSkin.typography.bodySmall,
                            color = OniSkin.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Interaction & Accessibility",
            subtitle = "Gestures, animation reduction, and touch preferences",
            onBack = onBack,
            backButtonTestTag = "interaction_back_button"
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
                    title = "Motion Accessibility",
                    description = "Controls animation intensity across the app."
                ) {
                    SwitchSettingRow(
                        title = "Reduce Motion",
                        description = "Disables tab sliding, ambient animations, and eliminates decorative movement.",
                        checked = reduceMotion,
                        onCheckedChange = { viewModel.setReduceMotionEnabled(it) },
                        testTag = "setting_reduce_motion"
                    )
                }
            }

            item {
                SettingSection(
                    title = "Player Gestures Reference",
                    description = "Active interactive gesture shortcuts."
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("• Swipe Artwork Left: Skip to next song (RTL-aware)", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Swipe Artwork Right: Skip to previous song (RTL-aware)", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Tap Artwork: Toggle Play / Pause", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Long Press Library Track: Context menu for tags, delete, and playlist", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                    }
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
            subtitle = "Navigation, audio tuning, and usage guide",
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
                    title = "Audio Tuning Guidance",
                    description = "Parametric equalizer and bass expansion tips."
                ) {
                    Column(modifier = Modifier.padding(OniSkin.spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Use presets like 'Classical', 'Rock', or 'Bass Booster' for quick soundstage adjustments.", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Adjust Spatializer to widen soundstage imaging for headphone listening.", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                        Text("• Save personal presets using the save button in Acoustic Tuning.", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textPrimary)
                    }
                }
            }
        }
    }
}
