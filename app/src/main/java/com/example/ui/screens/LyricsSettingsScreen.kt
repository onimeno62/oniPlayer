package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.LyricsAlignment
import com.example.data.preferences.LyricsAppearance
import com.example.data.preferences.LyricsSettingsStore
import com.example.data.preferences.PlayerSettingsStore
import com.example.ui.player.components.lyrics.LyricsAppearancePreview
import com.example.ui.player.components.lyrics.parseHexColor
import com.example.ui.player.components.lyrics.rememberLyricsAppearance
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val HighlightPalette = listOf(
    "#3B73E3" to "Blue",
    "#7C4DFF" to "Purple",
    "#00E5FF" to "Cyan",
    "#00E676" to "Green",
    "#FFD600" to "Yellow",
    "#FF9800" to "Orange",
    "#FF1744" to "Red",
    "#E91E63" to "Pink",
    "#FFFFFF" to "White"
)

private val TextPalette = listOf(
    "#FFFFFF" to "White",
    "#E0E0E0" to "Silver",
    "#B0BEC5" to "Slate",
    "#FFF3C4" to "Cream",
    "#C8E6FF" to "Ice",
    "#FFD1DC" to "Blush",
    "#212121" to "Ink"
)

/** Lyrics settings: appearance (with live preview), floating lyrics and online lyrics fetching. */
@Composable
fun LyricsSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val floatingLyricsEnabled by viewModel.floatingLyricsEnabled.collectAsStateWithLifecycle()
    val isAutoDownloadLyrics by viewModel.isAutoDownloadEnabled.collectAsStateWithLifecycle()
    val isAutoDownloadWifiOnly by viewModel.isAutoDownloadWifiOnly.collectAsStateWithLifecycle()
    val appearance by rememberLyricsAppearance()
    val canDrawOverlays = Settings.canDrawOverlays(context)

    fun updateAppearance(transform: (LyricsAppearance) -> LyricsAppearance) {
        scope.launch { LyricsSettingsStore.update(appContext, transform) }
    }

    SettingsPage(
        title = "Lyrics",
        subtitle = "Look & feel, floating lyrics and online lyrics lookup",
        onBack = onBack,
        backButtonTestTag = "lyrics_settings_back_button"
    ) {
        item {
            SettingSection(
                title = "Appearance",
                description = "How synced lyrics look in the lyrics screen. The current line is highlighted, the others are drawn a bit smaller."
            ) {
                LyricsAppearancePreview(
                    appearance = appearance,
                    modifier = Modifier.padding(OniSkin.spacing.md)
                )
                SettingDivider()
                SliderSettingRow(
                    title = "Text size",
                    value = appearance.fontSizeSp,
                    onValueChange = { v -> updateAppearance { it.copy(fontSizeSp = v.roundToInt().toFloat()) } },
                    valueRange = LyricsAppearance.MIN_FONT_SIZE_SP..LyricsAppearance.MAX_FONT_SIZE_SP,
                    steps = 19,
                    valueFormatter = { "${it.roundToInt()} sp" },
                    testTag = "setting_lyrics_font_size"
                )
                SettingDivider()
                SliderSettingRow(
                    title = "Other lines size",
                    value = appearance.inactiveScale,
                    onValueChange = { v -> updateAppearance { it.copy(inactiveScale = (v * 20f).roundToInt() / 20f) } },
                    valueRange = LyricsAppearance.MIN_INACTIVE_SCALE..1f,
                    steps = 7,
                    valueFormatter = { if (it >= 0.995f) "Same size" else "${(it * 100).roundToInt()}%" },
                    testTag = "setting_lyrics_inactive_scale"
                )
                SettingDivider()
                LyricsColorPickerRow(
                    title = "Highlight color",
                    description = "Current line. Theme uses your app accent color.",
                    selectedHex = appearance.highlightColorHex,
                    themeColor = OniSkin.colors.primary,
                    palette = HighlightPalette,
                    onSelect = { hex -> updateAppearance { it.copy(highlightColorHex = hex) } },
                    testTag = "setting_lyrics_highlight_color"
                )
                SettingDivider()
                LyricsColorPickerRow(
                    title = "Text color",
                    description = "Other lines. They fade with distance from the current line.",
                    selectedHex = appearance.textColorHex,
                    themeColor = OniSkin.colors.textSecondary,
                    palette = TextPalette,
                    onSelect = { hex -> updateAppearance { it.copy(textColorHex = hex) } },
                    testTag = "setting_lyrics_text_color"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Bold current line",
                    description = "Extra weight on the highlighted line.",
                    checked = appearance.boldActiveLine,
                    onCheckedChange = { checked -> updateAppearance { it.copy(boldActiveLine = checked) } },
                    testTag = "setting_lyrics_bold_active"
                )
                SettingDivider()
                SettingsChoiceRow(
                    title = "Alignment",
                    options = listOf("Center", "Start"),
                    selectedIndex = if (appearance.alignment == LyricsAlignment.CENTER) 0 else 1,
                    onSelect = { index ->
                        updateAppearance { it.copy(alignment = if (index == 0) LyricsAlignment.CENTER else LyricsAlignment.START) }
                    },
                    testTag = "setting_lyrics_alignment"
                )
                SettingDivider()
                SettingsActionRow(
                    title = "Reset appearance",
                    description = "Back to the default size and theme colors.",
                    icon = Icons.Default.RestartAlt,
                    showChevron = false,
                    onClick = { scope.launch { LyricsSettingsStore.reset(appContext) } },
                    testTag = "setting_lyrics_reset_appearance"
                )
            }
        }

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
                    description = "Fetch synced lyrics automatically whenever a song without lyrics starts playing.",
                    checked = isAutoDownloadLyrics,
                    onCheckedChange = { viewModel.setAutoDownloadEnabled(it) },
                    testTag = "setting_auto_download_lyrics"
                )
                SettingDivider()
                SwitchSettingRow(
                    title = "Only over Wi-Fi",
                    description = "Skip automatic downloads on mobile data. Manual search still works.",
                    checked = isAutoDownloadWifiOnly,
                    enabled = isAutoDownloadLyrics,
                    onCheckedChange = { viewModel.setAutoDownloadWifiOnly(it) },
                    testTag = "setting_auto_download_lyrics_wifi_only"
                )
                SettingsNote("While lyrics are downloading, a small animated cloud appears in the player's lyrics card.")
                SettingsNote("Tip: lyrics you edit or import from the player are saved per song and take priority over downloaded ones.")
            }
        }
    }
}

@Composable
private fun LyricsColorPickerRow(
    title: String,
    description: String,
    selectedHex: String?,
    themeColor: Color,
    palette: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm)
    ) {
        Text(title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary)
        Spacer(Modifier.height(OniSkin.spacing.xxs))
        Text(description, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
        Spacer(Modifier.height(OniSkin.spacing.sm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            LyricsColorSwatch(
                color = themeColor,
                label = "Theme",
                selected = selectedHex == null,
                onClick = { onSelect(null) }
            )
            palette.forEach { (hex, name) ->
                val color = parseHexColor(hex)
                if (color != null) {
                    LyricsColorSwatch(
                        color = color,
                        label = name,
                        selected = selectedHex.equals(hex, ignoreCase = true),
                        onClick = { onSelect(hex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .semantics {
                    this.selected = selected
                    contentDescription = "$label${if (selected) ", selected" else ""}"
                },
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(2.dp, OniSkin.colors.primary, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, OniSkin.colors.outline.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            style = OniSkin.typography.caption,
            color = if (selected) OniSkin.colors.primary else OniSkin.colors.textSecondary
        )
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
