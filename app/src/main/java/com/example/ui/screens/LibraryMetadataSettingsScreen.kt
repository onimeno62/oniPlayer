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
 * Library & metadata settings screen migrated to oniPlayer Default Skin.
 * Preserves artist data search and Wi-Fi only preferences.
 */
@Composable
fun LibraryMetadataSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val autoSearchArtistData by viewModel.autoSearchArtistData.collectAsStateWithLifecycle()
    val autoSearchWifiOnly by viewModel.autoSearchWifiOnly.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Library & Metadata",
            subtitle = "Online art, metadata sync, and scanning",
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
                    title = "Metadata Synchronization",
                    description = "Manage online data fetching for artist profiles and artwork."
                ) {
                    SwitchSettingRow(
                        title = "Search Artist Data",
                        description = "Automatically query high-resolution artist images and biography summaries from online repositories.",
                        checked = autoSearchArtistData,
                        onCheckedChange = { viewModel.setAutoSearchArtistData(it) },
                        testTag = "setting_auto_search_artist"
                    )

                    SettingDivider()

                    SwitchSettingRow(
                        title = "Wi-Fi Only Sync",
                        description = "Prevent metadata searches and artwork downloads over cellular mobile networks to conserve data.",
                        checked = autoSearchWifiOnly,
                        onCheckedChange = { viewModel.setAutoSearchWifiOnly(it) },
                        enabled = autoSearchArtistData,
                        testTag = "setting_auto_search_wifi_only"
                    )
                }
            }
        }
    }
}
