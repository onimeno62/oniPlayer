package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

/**
 * Appearance settings screen migrated to oniPlayer Default Skin.
 * Preserves all existing theme selection, accent color, and visual effect preferences.
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
    val isSystemDark = isSystemInDarkTheme()

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
            // Theme Section
            item {
                SettingSection(
                    title = "Theme Option",
                    description = "Choose between light, dark, or automatic system appearance.",
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
                                onClick = null, // Handled by outer row
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

            // Visual Effects Section
            item {
                SettingSection(
                    title = "Visual Effects",
                    description = "Configure translucent glass styling and layout parameters.",
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
            }

            // Player Style Section (Preserved with clean Default Skin placeholder status)
            item {
                SettingSection(
                    title = "Player Style",
                    description = "Choose layout and playback screen density variants.",
                    cardModifier = Modifier.testTag("appearance_card_player_style")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md)
                    ) {
                        Text(
                            text = "oniPlayer Default Skin",
                            style = OniSkin.typography.titleSmall,
                            color = OniSkin.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                        Text(
                            text = "Currently using the unified high-density Default Skin layout. Custom layout presets will be configurable in future skins.",
                            style = OniSkin.typography.bodySmall,
                            color = OniSkin.colors.textSecondary
                        )
                    }
                }
            }

            // Typography Section (Preserved with clean Default Skin placeholder status)
            item {
                SettingSection(
                    title = "Typography",
                    description = "Font scale and text rendering characteristics.",
                    cardModifier = Modifier.testTag("appearance_card_typography")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OniSkin.spacing.md)
                    ) {
                        Text(
                            text = "Default Skin Type Hierarchy",
                            style = OniSkin.typography.titleSmall,
                            color = OniSkin.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                        Text(
                            text = "Configured with modern high-contrast semi-bold titles and readable proportional body scales.",
                            style = OniSkin.typography.bodySmall,
                            color = OniSkin.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
