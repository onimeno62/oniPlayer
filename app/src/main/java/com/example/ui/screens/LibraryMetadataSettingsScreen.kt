package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/** Library controls, including the single search entry point for dashboard and category results. */
@Composable
fun LibraryMetadataSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val autoSearchArtistData by viewModel.autoSearchArtistData.collectAsStateWithLifecycle()
    val autoSearchWifiOnly by viewModel.autoSearchWifiOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()

    SettingsPage(
        title = "Library & Metadata",
        subtitle = "Search, scanning and online metadata",
        onBack = onBack,
        backButtonTestTag = "library_metadata_back_button"
    ) {
        item {
            SettingSection(
                title = "Search your library",
                description = "Follows your current location: the dashboard or the active library category."
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OniSkin.spacing.md)
                        .testTag("library_settings_search"),
                    placeholder = { Text("Songs, artists, albums...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = OniSkin.shapes.full,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OniSkin.colors.textPrimary,
                        unfocusedTextColor = OniSkin.colors.textPrimary,
                        focusedBorderColor = OniSkin.colors.primary,
                        unfocusedBorderColor = OniSkin.colors.outline,
                        cursorColor = OniSkin.colors.primary
                    )
                )
            }
        }

        item {
            SettingSection(
                title = "Music library",
                description = "oniPlayer reads your music from Android's media index."
            ) {
                SettingsInfoRow("Indexed songs", "${songs.size}")
                SettingDivider()
                SettingsActionRow(
                    title = "Rescan music",
                    description = "Pick up new, moved or retagged files.",
                    icon = Icons.Default.Refresh,
                    showChevron = false,
                    onClick = {
                        viewModel.rescanLibrary()
                        context.settingsToast("Scanning your music…")
                    },
                    testTag = "setting_rescan_library"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Storage permission",
                    description = "Check that oniPlayer can read your music files.",
                    icon = Icons.Default.FolderOpen,
                    onClick = {
                        context.launchFirstAvailable(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        )
                    },
                    testTag = "setting_library_permission"
                )
            }
        }

        item {
            SettingSection(
                title = "Online metadata",
                description = "Artist photos and bios fetched from the web."
            ) {
                SwitchSettingRow(
                    title = "Fetch artist info",
                    description = "Automatically look up artist images and biographies.",
                    checked = autoSearchArtistData,
                    onCheckedChange = viewModel::setAutoSearchArtistData,
                    testTag = "setting_auto_search_artist"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Wi-Fi only",
                    description = "Never use mobile data for metadata and artwork downloads.",
                    checked = autoSearchWifiOnly,
                    onCheckedChange = viewModel::setAutoSearchWifiOnly,
                    enabled = autoSearchArtistData,
                    testTag = "setting_auto_search_wifi_only"
                )
            }
        }
    }
}
