package com.example.ui.widgets.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.screens.SettingDivider
import com.example.ui.screens.SettingSection
import com.example.ui.screens.SettingsSubscreenHeader
import com.example.ui.screens.SwitchSettingRow
import com.example.ui.theme.OniSkin
import com.example.ui.widgets.core.OniWidgetRegistry
import com.example.ui.widgets.defaultpack.DefaultWidgetPack
import com.example.ui.widgets.settings.WidgetSettingsStore
import kotlinx.coroutines.launch

/** Complete widget management and refresh settings, backed by persistent DataStore state. */
@Composable
fun WidgetsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by WidgetSettingsStore.settings(context).collectAsStateWithLifecycle(
        initialValue = com.example.ui.widgets.settings.WidgetSettings()
    )

    if (OniWidgetRegistry.getAllPacks().isEmpty()) {
        OniWidgetRegistry.registerPack(DefaultWidgetPack)
    }
    val pack = OniWidgetRegistry.getAllPacks().firstOrNull() ?: DefaultWidgetPack

    fun update(transform: (com.example.ui.widgets.settings.WidgetSettings) -> com.example.ui.widgets.settings.WidgetSettings) {
        scope.launch { WidgetSettingsStore.update(context, transform) }
    }

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        SettingsSubscreenHeader(
            title = "Home Screen Widgets",
            subtitle = "Choose widgets, controls, and update behavior",
            onBack = onBack,
            backButtonTestTag = "widgets_settings_back_button"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp)
        ) {
            item {
                SettingSection(
                    title = "Widget availability",
                    description = "These switches control which widget types oniPlayer refreshes. Existing launcher placements remain safe and can be re-enabled anytime."
                ) {
                    SwitchSettingRow(
                        title = "Enable oniPlayer widgets",
                        description = "Allow widget updates from the playback service.",
                        checked = settings.widgetsEnabled,
                        onCheckedChange = { value -> update { it.copy(widgetsEnabled = value) } },
                        testTag = "setting_widgets_enabled"
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Now Playing",
                        description = "Artwork, title, progress, and full playback controls.",
                        checked = settings.nowPlayingEnabled,
                        enabled = settings.widgetsEnabled,
                        onCheckedChange = { value -> update { it.copy(nowPlayingEnabled = value) } },
                        testTag = "setting_widget_now_playing"
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Mini Player",
                        description = "Compact transport controls for fast access.",
                        checked = settings.miniPlayerEnabled,
                        enabled = settings.widgetsEnabled,
                        onCheckedChange = { value -> update { it.copy(miniPlayerEnabled = value) } },
                        testTag = "setting_widget_mini_player"
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Dynamic Album",
                        description = "Artwork-first identity widget with minimal controls.",
                        checked = settings.dynamicAlbumEnabled,
                        enabled = settings.widgetsEnabled,
                        onCheckedChange = { value -> update { it.copy(dynamicAlbumEnabled = value) } },
                        testTag = "setting_widget_dynamic_album"
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        title = "Lyrics",
                        description = "Active lyric line with previous and next context.",
                        checked = settings.lyricsEnabled,
                        enabled = settings.widgetsEnabled,
                        onCheckedChange = { value -> update { it.copy(lyricsEnabled = value) } },
                        testTag = "setting_widget_lyrics"
                    )
                }
            }

            item {
                SettingSection(
                    title = "Live updates",
                    description = "Widgets are event-driven. Faster refresh improves progress accuracy but may use more launcher work."
                ) {
                    SwitchSettingRow(
                        title = "Live lyric updates",
                        description = "Keep synchronized lyric lines moving while playback is active.",
                        checked = settings.liveLyricsUpdates,
                        enabled = settings.widgetsEnabled && settings.lyricsEnabled,
                        onCheckedChange = { value -> update { it.copy(liveLyricsUpdates = value) } },
                        testTag = "setting_widget_live_lyrics"
                    )
                    SettingDivider()
                    RefreshChoiceRow(
                        title = "Player progress refresh",
                        values = listOf(1, 2, 5),
                        selected = settings.playerRefreshSeconds,
                        enabled = settings.widgetsEnabled,
                        suffix = "s",
                        onSelected = { value -> update { it.copy(playerRefreshSeconds = value) } },
                        testTag = "setting_widget_player_refresh"
                    )
                    SettingDivider()
                    RefreshChoiceRow(
                        title = "Lyrics refresh",
                        values = listOf(1, 2, 5),
                        selected = settings.lyricsRefreshSeconds,
                        enabled = settings.widgetsEnabled && settings.lyricsEnabled && settings.liveLyricsUpdates,
                        suffix = "s",
                        onSelected = { value -> update { it.copy(lyricsRefreshSeconds = value) } },
                        testTag = "setting_widget_lyrics_refresh"
                    )
                }
            }

            item {
                SettingSection(
                    title = "Installed widget pack",
                    description = "Built-in widgets inherit the active oniPlayer skin."
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(OniSkin.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                    ) {
                        Text(pack.name, style = OniSkin.typography.titleSmall, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text("${pack.description} Version ${pack.version}", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        pack.widgets.forEach { widget ->
                            OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.listItem, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(OniSkin.spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Widgets, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(OniSkin.spacing.sm))
                                    Column(Modifier.weight(1f)) {
                                        Text(widget.name, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(widget.description, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                                    }
                                    Text(widget.supportedSizes.joinToString(" · ") { it.displayName }, style = OniSkin.typography.caption, color = OniSkin.colors.primary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingSection(
                    title = "How to place a widget",
                    description = "Android controls widget placement and individual launcher instances."
                ) {
                    Text(
                        text = "Long-press an empty area on your home screen, choose Widgets, select oniPlayer, then drag the size you want. You can place multiple sizes and multiple instances.",
                        style = OniSkin.typography.bodySmall,
                        color = OniSkin.colors.textSecondary,
                        modifier = Modifier.padding(OniSkin.spacing.md)
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshChoiceRow(
    title: String,
    values: List<Int>,
    selected: Int,
    enabled: Boolean,
    suffix: String,
    onSelected: (Int) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = if (enabled) OniSkin.colors.primary else OniSkin.colors.disabled, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(OniSkin.spacing.xs))
                Text(title, style = OniSkin.typography.bodyLarge, color = if (enabled) OniSkin.colors.textPrimary else OniSkin.colors.disabled)
            }
            Text("${selected}${suffix}", style = OniSkin.typography.labelMedium, color = if (enabled) OniSkin.colors.primary else OniSkin.colors.disabled)
        }
        Spacer(Modifier.height(OniSkin.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            values.forEach { value ->
                FilterChip(
                    selected = enabled && selected == value,
                    onClick = { if (enabled) onSelected(value) },
                    enabled = enabled,
                    label = { Text("${value}${suffix}", style = OniSkin.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = OniSkin.colors.surfaceVariant,
                        labelColor = OniSkin.colors.textSecondary,
                        selectedContainerColor = OniSkin.colors.primaryContainer,
                        selectedLabelColor = OniSkin.colors.onPrimaryContainer,
                        disabledContainerColor = OniSkin.colors.surfaceVariant,
                        disabledLabelColor = OniSkin.colors.disabled
                    ),
                    shape = OniSkin.shapes.chip,
                    modifier = Modifier.testTag("${testTag}_$value")
                )
            }
        }
    }
}