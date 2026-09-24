package com.example.ui.widgets.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.screens.SettingDivider
import com.example.ui.screens.SettingSection
import com.example.ui.screens.SettingsSubscreenHeader
import com.example.ui.screens.SwitchSettingRow
import com.example.ui.theme.OniSkin
import com.example.ui.widgets.defaultpack.DefaultWidgetPack
import com.example.ui.widgets.settings.WidgetSettings
import com.example.ui.widgets.settings.WidgetSettingsStore
import kotlinx.coroutines.launch

/** Widget visibility and refresh settings for the oniPlayer widget system. */
@Composable
fun WidgetsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by WidgetSettingsStore.settings(context).collectAsStateWithLifecycle(
        initialValue = WidgetSettings()
    )

    fun update(transform: (WidgetSettings) -> WidgetSettings) {
        scope.launch { WidgetSettingsStore.update(context, transform) }
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        SettingsSubscreenHeader(
            title = "Home Screen Widgets",
            subtitle = "Control widget visibility and refresh behavior",
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
                    description = "Enable the widget families you want to keep updated on your launcher."
                ) {
                    SwitchSettingRow(
                        title = "Enable oniPlayer widgets",
                        description = "Allow playback and lyrics updates for launcher widgets.",
                        checked = settings.widgetsEnabled,
                        onCheckedChange = { value -> update { it.copy(widgetsEnabled = value) } },
                        testTag = "setting_widgets_enabled"
                    )
                    SettingDivider()
                    WidgetSwitch("Mini Player", "Transport-first control strip.", settings.miniPlayerEnabled, settings.widgetsEnabled, "setting_widget_mini_player") { value -> update { it.copy(miniPlayerEnabled = value) } }
                    SettingDivider()
                    WidgetSwitch("Now Playing", "Flagship artwork-led player with progress and transport.", settings.nowPlayingEnabled, settings.widgetsEnabled, "setting_widget_now_playing") { value -> update { it.copy(nowPlayingEnabled = value) } }
                    SettingDivider()
                    WidgetSwitch("Dynamic Album", "Immersive artwork-first widget with minimal controls.", settings.dynamicAlbumEnabled, settings.widgetsEnabled, "setting_widget_dynamic_album") { value -> update { it.copy(dynamicAlbumEnabled = value) } }
                    SettingDivider()
                    WidgetSwitch("Lyrics", "Lyric-first widget with active-line emphasis.", settings.lyricsEnabled, settings.widgetsEnabled, "setting_widget_lyrics") { value -> update { it.copy(lyricsEnabled = value) } }
                }
            }

            item {
                SettingSection(
                    title = "Update behavior",
                    description = "Shorter intervals improve progress and lyric freshness but can increase launcher work."
                ) {
                    SwitchSettingRow(
                        title = "Live lyric updates",
                        description = "Refresh synchronized lyric context while a song is playing.",
                        checked = settings.liveLyricsUpdates,
                        enabled = settings.widgetsEnabled && settings.lyricsEnabled,
                        onCheckedChange = { value -> update { it.copy(liveLyricsUpdates = value) } },
                        testTag = "setting_widget_live_lyrics"
                    )
                    SettingDivider()
                    RefreshChoiceRow(
                        title = "Playback refresh interval",
                        values = listOf(1, 2, 3, 5, 10),
                        selected = settings.playerRefreshSeconds,
                        enabled = settings.widgetsEnabled,
                        onSelected = { value -> update { it.copy(playerRefreshSeconds = value) } },
                        testTag = "setting_widget_player_refresh"
                    )
                    SettingDivider()
                    RefreshChoiceRow(
                        title = "Lyrics refresh interval",
                        values = listOf(1, 2, 3, 5, 10),
                        selected = settings.lyricsRefreshSeconds,
                        enabled = settings.widgetsEnabled && settings.lyricsEnabled && settings.liveLyricsUpdates,
                        onSelected = { value -> update { it.copy(lyricsRefreshSeconds = value) } },
                        testTag = "setting_widget_lyrics_refresh"
                    )
                }
            }

            item {
                SettingSection(
                    title = "Default widget pack",
                    description = "The built-in widget layouts inherit the active oniPlayer skin."
                ) {
                    Column(Modifier.fillMaxWidth().padding(OniSkin.spacing.md), verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        Text(DefaultWidgetPack.name, style = OniSkin.typography.titleSmall, color = OniSkin.colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text("${DefaultWidgetPack.description} Version ${DefaultWidgetPack.version}", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        DefaultWidgetPack.widgets.forEach { widget ->
                            OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.listItem, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(OniSkin.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
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
        }
    }
}

@Composable
private fun WidgetSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchSettingRow(
        title = title,
        description = description,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        testTag = testTag
    )
}

@Composable
private fun RefreshChoiceRow(
    title: String,
    values: List<Int>,
    selected: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
    testTag: String
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm), verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = if (enabled) OniSkin.colors.primary else OniSkin.colors.disabled, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(OniSkin.spacing.xs))
                Text(title, style = OniSkin.typography.bodyLarge, color = if (enabled) OniSkin.colors.textPrimary else OniSkin.colors.disabled)
            }
            Text("${selected}s", style = OniSkin.typography.labelMedium, color = if (enabled) OniSkin.colors.primary else OniSkin.colors.disabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            values.forEach { value ->
                FilterChip(
                    selected = enabled && selected == value,
                    onClick = { if (enabled) onSelected(value) },
                    enabled = enabled,
                    label = { Text("${value}s", style = OniSkin.typography.labelMedium) },
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
