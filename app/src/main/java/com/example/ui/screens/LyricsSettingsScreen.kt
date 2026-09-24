package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.PlayerSettingsStore
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlin.math.roundToInt

/** Lyrics settings wired to the floating lyrics service and the online lyrics fetcher. */
@Composable
fun LyricsSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val floatingLyricsEnabled by viewModel.floatingLyricsEnabled.collectAsStateWithLifecycle()
    val isAutoDownloadLyrics by viewModel.isAutoDownloadEnabled.collectAsStateWithLifecycle()
    val canDrawOverlays = Settings.canDrawOverlays(context)

    SettingsPage(
        title = "Lyrics",
        subtitle = "Floating lyrics and online lyrics lookup",
        onBack = onBack,
        backButtonTestTag = "lyrics_settings_back_button"
    ) {
        item {
            SettingSection(
                title = "Floating lyrics",
                description = "A synced lyrics window on top of other apps while music plays."
            ) {
                SwitchSettingRow(
                    title = "Show floating lyrics",
                    description = "Compact or expanded karaoke window during playback.",
                    checked = floatingLyricsEnabled,
                    onCheckedChange = { viewModel.setFloatingLyricsEnabled(it) },
                    testTag = "setting_floating_lyrics"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Display over other apps",
                    description = "Android permission required for the floating window.",
                    icon = Icons.Default.PictureInPicture,
                    trailingText = if (canDrawOverlays) "Allowed" else "Not allowed",
                    onClick = {
                        context.launchFirstAvailable(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        )
                    },
                    testTag = "setting_overlay_permission"
                )
            }
        }

        item {
            SettingSection(
                title = "Online lyrics",
                description = "Searches LRCLIB and open lyric sources for synced (LRC) lyrics."
            ) {
                SwitchSettingRow(
                    title = "Auto-download lyrics",
                    description = "Fetch synced lyrics automatically when a song has none.",
                    checked = isAutoDownloadLyrics,
                    onCheckedChange = { viewModel.setAutoDownloadEnabled(it) },
                    testTag = "setting_auto_download_lyrics"
                )
                SettingsNote("Tip: lyrics you edit or import from the player are saved per song and take priority over downloaded ones.")
            }
        }
    }
}

@Composable
fun AudioEqualizerSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val settings by rememberPlayerSettings()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Audio & Equalizer",
            subtitle = "Equalizer, bass, spatializer and volume boost",
            onBack = onBack,
            backButtonTestTag = "audio_eq_back_button"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
                .padding(bottom = OniSkin.spacing.md)
        ) {
            SettingSection(
                title = "Volume boost",
                description = "Pre-amp gain for quiet recordings. High values can distort loud tracks."
            ) {
                SliderSettingRow(
                    title = "Loudness boost",
                    value = settings.loudnessBoostDb.coerceIn(0f, 10f),
                    onValueChange = { PlayerSettingsStore.update(context, PlayerSettingsStore.LOUDNESS_BOOST_DB, (it * 2f).roundToInt() / 2f) },
                    valueRange = 0f..10f,
                    steps = 19,
                    valueFormatter = { if (it <= 0f) "Off" else "+${"%.1f".format(it)} dB" },
                    testTag = "setting_loudness_boost"
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
        ) {
            EqualizerScreen(viewModel = viewModel)
        }
    }
}
