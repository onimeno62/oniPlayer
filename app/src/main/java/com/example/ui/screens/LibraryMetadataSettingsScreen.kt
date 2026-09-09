package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/** Library controls, including the single search entry point for dashboard and category results. */
@Composable
fun LibraryMetadataSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val autoSearchArtistData by viewModel.autoSearchArtistData.collectAsStateWithLifecycle()
    val autoSearchWifiOnly by viewModel.autoSearchWifiOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Library Settings",
            subtitle = "Search, scan, and metadata behavior",
            onBack = onBack,
            backButtonTestTag = "library_metadata_back_button"
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
                    title = "Search your library",
                    description = "This search follows your current location: the dashboard or the active library category."
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
                                TextButton(onClick = { viewModel.updateSearchQuery("") }) { Text("Clear") }
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
                    description = "Refresh local files and keep the library current."
                ) {
                    TextButton(
                        onClick = viewModel::rescanLibrary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs)
                            .heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(OniSkin.spacing.xs))
                        Text("Scan local music")
                    }
                }
            }

            item {
                SettingSection(
                    title = "Metadata synchronization",
                    description = "Manage online artist profiles and artwork lookup."
                ) {
                    SwitchSettingRow(
                        title = "Search Artist Data",
                        description = "Automatically query artist images and biography summaries.",
                        checked = autoSearchArtistData,
                        onCheckedChange = viewModel::setAutoSearchArtistData,
                        testTag = "setting_auto_search_artist"
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Wi-Fi Only Sync",
                        description = "Avoid metadata searches and artwork downloads over cellular data.",
                        checked = autoSearchWifiOnly,
                        onCheckedChange = viewModel::setAutoSearchWifiOnly,
                        enabled = autoSearchArtistData,
                        testTag = "setting_auto_search_wifi_only"
                    )
                }
            }
        }
    }
}
