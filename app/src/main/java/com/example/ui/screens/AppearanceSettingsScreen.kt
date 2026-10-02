package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.PlayerSettingsStore
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.components.button.OniSecondaryButton
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.player.LyricLineMode
import com.example.ui.player.SeekBarStyle
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * Complete Appearance settings screen for oniPlayer Default Skin.
 * Features Theme, Active Skin architecture readiness, Accent Color, Surfaces & Effects,
 * Seek Bar customization styles, Canvas player controls, Typography, and Reset Appearance.
 */
@Composable
fun AppearanceSettingsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit
) {
    val selectedThemeOption by viewModel.selectedThemeOption.collectAsStateWithLifecycle()
    val materialYouEnabled by viewModel.materialYouEnabled.collectAsStateWithLifecycle()
    val customAccentColor by viewModel.customAccentColor.collectAsStateWithLifecycle()
    val glassEffectEnabled by viewModel.glassEffectEnabled.collectAsStateWithLifecycle()
    val blurStrength by viewModel.blurStrength.collectAsStateWithLifecycle()
    val cornerRadius by viewModel.cornerRadius.collectAsStateWithLifecycle()
    val backgroundTransparency by viewModel.backgroundTransparency.collectAsStateWithLifecycle()
    val nowPlayingEffect by viewModel.nowPlayingEffect.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()

    var showResetDialog by remember { mutableStateOf(false) }

    // Player customization preferences
    val context = LocalContext.current.applicationContext
    val settings by rememberPlayerSettings()
    var selectedSeekBarStyle by remember { mutableStateOf(SeekBarStyle.SIMPLE) }
    var selectedLyricMode by remember { mutableStateOf(LyricLineMode.UNDERNEATH) }
    var selectedLineCount by remember { mutableIntStateOf(2) }
    var audioAnalyzerEffectOn by remember { mutableStateOf(false) }
    var artworkEffectsOn by remember { mutableStateOf(true) }
    var showPlayerBottomNav by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = "Appearance",
            subtitle = "Custom themes, colors, and visual styling",
            onBack = onBack,
            backButtonTestTag = "appearance_back_button"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            // Live Preview Section
            item {
                SettingSection(
                    title = "Live Token Preview",
                    description = "Demonstrates active theme, surface, and typography tokens."
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OniArtwork(
                                artworkUri = null,
                                contentDescription = "Preview artwork",
                                shape = OniSkin.artwork.shape,
                                elevation = OniSkin.artwork.shadowElevation,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "oniPlayer Default Skin",
                                    style = OniSkin.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.textPrimary
                                )
                                Text(
                                    text = "$selectedThemeOption • Accent $customAccentColor",
                                    style = OniSkin.typography.bodySmall,
                                    color = OniSkin.colors.textSecondary
                                )
                            }
                            OniPrimaryButton(
                                text = "Preview",
                                onClick = {},
                                modifier = Modifier.heightIn(min = 40.dp)
                            )
                        }
                    }
                }
            }

            // Seek Bar Customization Section
            item {
                SettingSection(
                    title = "Player Seek Bar Style",
                    description = "Choose your playback scrubber aesthetic across all player screens."
                ) {
                    val seekStyles = listOf(
                        SeekBarStyle.SIMPLE to ("Simple Slider" to "Minimal clean slider track"),
                        SeekBarStyle.WAVY to ("M3 Wavy Waveform" to "Android 13+ animated wavy squiggly pattern"),
                        SeekBarStyle.DOT_LINE to ("Dotted to Solid" to "Dotted inactive track morphing to solid active progress"),
                        SeekBarStyle.AUDIO_SPECTRUM to ("Audio Spectrum Bar" to "Real-time 16-band reactive frequency equalizer bar")
                    )

                    seekStyles.forEachIndexed { index, (style, labelDesc) ->
                        val isSelected = selectedSeekBarStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable { selectedSeekBarStyle = style }
                                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { selectedSeekBarStyle = style })
                            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(labelDesc.first, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary)
                                Text(labelDesc.second, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                            }
                        }
                        if (index < seekStyles.lastIndex) SettingDivider()
                    }
                }
            }

            // Canvas & Live Lyrics Display Section
            item {
                SettingSection(
                    title = "Live Lyrics Display",
                    description = "Choose placement and multi-line density for player lyrics."
                ) {
                    SettingsChoiceRow(
                        title = "Lyric Placement",
                        options = listOf("Underneath Art", "Over Artwork Bottom", "Hidden"),
                        selectedIndex = when (selectedLyricMode) {
                            LyricLineMode.UNDERNEATH -> 0
                            LyricLineMode.OVER_BOTTOM -> 1
                            LyricLineMode.HIDDEN -> 2
                        },
                        onSelect = {
                            selectedLyricMode = when (it) {
                                0 -> LyricLineMode.UNDERNEATH
                                1 -> LyricLineMode.OVER_BOTTOM
                                else -> LyricLineMode.HIDDEN
                            }
                        }
                    )
                    SettingDivider()
                    SettingsChoiceRow(
                        title = "Visible Lyric Lines",
                        options = listOf("1 Line", "2 Lines", "3 Lines"),
                        selectedIndex = (selectedLineCount - 1).coerceIn(0, 2),
                        onSelect = { selectedLineCount = it + 1 }
                    )
                }
            }

            // Audio & Artwork Effects Section
            item {
                SettingSection(
                    title = "Player Visual & Audio Effects",
                    description = "Real-time beat responsiveness and motion enhancements."
                ) {
                    SwitchSettingRow(
                        title = "Audio Analyzer Pulse",
                        description = "Glow and amplitude reactive to track rhythm & beat energy.",
                        checked = audioAnalyzerEffectOn,
                        onCheckedChange = { audioAnalyzerEffectOn = it }
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Living Artwork Effects",
                        description = "Smooth Ken Burns pan/zoom and dynamic ambient artwork radiance.",
                        checked = artworkEffectsOn,
                        onCheckedChange = { artworkEffectsOn = it }
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Bottom Navigation Bar on Player",
                        description = "Keep bottom nav bar visible while full player is active.",
                        checked = showPlayerBottomNav,
                        onCheckedChange = { showPlayerBottomNav = it }
                    )
                }
            }

            // Theme Section
            item {
                SettingSection(
                    title = "Theme Option",
                    description = "Choose between light, dark, AMOLED, or system appearance.",
                    cardModifier = Modifier.testTag("appearance_card_theme")
                ) {
                    val themeOptions = listOf("Light", "Dark", "AMOLED", "Follow System")
                    themeOptions.forEachIndexed { index, option ->
                        val isSelected = selectedThemeOption.equals(option, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("theme_option_$option")
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = { viewModel.setThemeOption(option, isSystemDark) }
                                )
                                .padding(
                                    horizontal = OniSkin.spacing.md,
                                    vertical = OniSkin.spacing.sm
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OniSkin.colors.primary,
                                    unselectedColor = OniSkin.colors.outline
                                )
                            )
                            Spacer(modifier = Modifier.width(OniSkin.spacing.sm))
                            Text(
                                text = option,
                                style = OniSkin.typography.bodyLarge,
                                color = OniSkin.colors.textPrimary
                            )
                        }
                        if (index < themeOptions.lastIndex) {
                            SettingDivider()
                        }
                    }
                }
            }

            // Accent Color Section
            item {
                SettingSection(
                    title = "Accent Color",
                    description = "Personalize buttons, sliders, and highlights across oniPlayer.",
                    cardModifier = Modifier.testTag("appearance_card_accent_colors")
                ) {
                    SwitchSettingRow(
                        title = "Dynamic Material You",
                        description = "Extract accent colors from your system wallpaper.",
                        checked = materialYouEnabled,
                        onCheckedChange = { viewModel.setMaterialYouEnabled(it) },
                        testTag = "setting_material_you"
                    )

                    if (!materialYouEnabled) {
                        SettingDivider()
                        AccentColorPickerRow(
                            selectedHex = customAccentColor,
                            onColorSelect = { viewModel.setCustomAccentColor(it) },
                            testTag = "setting_accent_color"
                        )
                    }
                }
            }

            // Surfaces & Effects Section
            item {
                SettingSection(
                    title = "Surfaces & Glass",
                    description = "Configure translucent glass styling, radius, and transparency.",
                    cardModifier = Modifier.testTag("appearance_card_visual_effects")
                ) {
                    SwitchSettingRow(
                        title = "Glass Effect",
                        description = "Enable frosted glass depth on navigation capsules and floating cards.",
                        checked = glassEffectEnabled,
                        onCheckedChange = { viewModel.setGlassEffectEnabled(it) },
                        testTag = "setting_glass_effect"
                    )

                    if (glassEffectEnabled) {
                        SettingDivider()
                        SliderSettingRow(
                            title = "Blur Strength",
                            value = blurStrength,
                            onValueChange = { viewModel.setBlurStrength(it) },
                            valueRange = 0f..50f,
                            valueFormatter = { "${it.toInt()} dp" },
                            testTag = "setting_blur_strength"
                        )
                    }

                    SettingDivider()
                    SliderSettingRow(
                        title = "Corner Radius",
                        value = cornerRadius,
                        onValueChange = { viewModel.setCornerRadius(it) },
                        valueRange = 4f..32f,
                        valueFormatter = { "${it.toInt()} dp" },
                        testTag = "setting_corner_radius"
                    )

                    SettingDivider()
                    SliderSettingRow(
                        title = "Background Transparency",
                        value = backgroundTransparency,
                        onValueChange = { viewModel.setBackgroundTransparency(it) },
                        valueRange = 0f..100f,
                        valueFormatter = { "${it.toInt()}%" },
                        testTag = "setting_background_transparency"
                    )
                }
            }

            // Typography Scale
            item {
                SettingSection(
                    title = "Typography Hierarchy",
                    description = "Default Skin typography scale adhering to Section 10 tokens."
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Display • 30sp SemiBold",
                            style = OniSkin.typography.displayMedium,
                            color = OniSkin.colors.textPrimary
                        )
                        Text(
                            text = "Title • 18sp Medium",
                            style = OniSkin.typography.titleMedium,
                            color = OniSkin.colors.primary
                        )
                        Text(
                            text = "Body • 14sp Regular proportional scale",
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textSecondary
                        )
                    }
                }
            }

            // Reset Appearance Action
            item {
                SettingSection(
                    title = "Reset Appearance",
                    description = "Revert all skin and appearance settings back to factory defaults."
                ) {
                    Box(modifier = Modifier.padding(OniSkin.spacing.md)) {
                        OniSecondaryButton(
                            text = "Reset Appearance to Defaults",
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Appearance?", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset your theme, accent colors, surface effects, and motion preferences back to default. Your music library and playback history will not be affected.", style = OniSkin.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAppearancePreferences()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = OniSkin.colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = OniSkin.colors.textPrimary)
                }
            },
            containerColor = OniSkin.colors.surface,
            shape = OniSkin.shapes.dialog
        )
    }
}
