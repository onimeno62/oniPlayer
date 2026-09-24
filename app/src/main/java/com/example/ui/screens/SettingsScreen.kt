package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.ui.widgets.manager.WidgetsSettingsScreen

data class SettingCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val section: String,
    val tint: Color = Color(0xFF7E57C2),
    val keywords: List<String> = emptyList()
)

private object SettingsTints {
    val Pink = Color(0xFFEC407A)
    val Violet = Color(0xFF7E57C2)
    val Blue = Color(0xFF42A5F5)
    val Teal = Color(0xFF26A69A)
    val Orange = Color(0xFFFF8A3D)
    val Green = Color(0xFF66BB6A)
    val Indigo = Color(0xFF5C6BC0)
    val Amber = Color(0xFFFFB300)
    val Cyan = Color(0xFF26C6DA)
    val Slate = Color(0xFF78909C)
    val Red = Color(0xFFEF5350)
}

@Composable
fun SettingsScreen(viewModel: MusicPlayerViewModel) {
    var activeSubScreen by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedThemeOption by viewModel.selectedThemeOption.collectAsStateWithLifecycle()
    val nextSongDelaySeconds by viewModel.nextSongDelaySeconds.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val autoSearchArtistData by viewModel.autoSearchArtistData.collectAsStateWithLifecycle()
    val floatingLyricsEnabled by viewModel.floatingLyricsEnabled.collectAsStateWithLifecycle()
    val isAutoDownloadLyrics by viewModel.isAutoDownloadEnabled.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsStateWithLifecycle()
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playerSettings by rememberPlayerSettings()

    val appearanceValue = "$selectedThemeOption theme"
    val transitionValue = when {
        crossfadeEnabled -> "Crossfade ${crossfadeDurationSeconds}s"
        nextSongDelaySeconds > 0 -> "Delay ${nextSongDelaySeconds}s"
        else -> "Gapless"
    }
    val playbackValue = if (playerSettings.playbackSpeed != 1f) {
        "$transitionValue • ${formatPlaybackRate(playerSettings.playbackSpeed)} speed"
    } else transitionValue
    val audioValue = if (playerSettings.loudnessBoostDb > 0f) {
        "Equalizer • +${playerSettings.loudnessBoostDb.toInt()} dB boost"
    } else "Equalizer, bass, spatializer, volume boost"
    val lyricsValue = when {
        floatingLyricsEnabled -> "Floating lyrics on"
        isAutoDownloadLyrics -> "Auto-download on"
        else -> "Manual lyrics"
    }
    val libraryValue = "${songs.size} songs • " + if (autoSearchArtistData) "online metadata" else "local only"
    val interactionValue = listOfNotNull(
        if (reduceMotion) "Reduced motion" else null,
        if (playerSettings.keepScreenOn) "Screen stays on" else null
    ).joinToString(" • ").ifEmpty { "Start screen, motion, gestures" }

    val categories = remember(appearanceValue, playbackValue, audioValue, lyricsValue, libraryValue, interactionValue) {
        listOf(
            SettingCategory("appearance", "Appearance", appearanceValue, Icons.Default.Palette, "Look & feel", SettingsTints.Pink,
                listOf("theme", "dark", "light", "accent", "color", "material you", "glass", "blur", "corner", "transparency")),
            SettingCategory("widgets", "Widgets", "Home screen players and lyrics", Icons.Default.Widgets, "Look & feel", SettingsTints.Violet,
                listOf("home screen", "mini player", "now playing", "album", "skin")),
            SettingCategory("playback", "Playback", playbackValue, Icons.Default.PlayCircle, "Sound", SettingsTints.Blue,
                listOf("speed", "pitch", "gapless", "crossfade", "delay", "silence", "audio focus", "headphones", "bluetooth", "previous", "resume", "position")),
            SettingCategory("audio_eq", "Audio & Equalizer", audioValue, Icons.Default.Tune, "Sound", SettingsTints.Teal,
                listOf("equalizer", "eq", "bass", "virtualizer", "spatializer", "preamp", "loudness", "volume", "boost", "preset")),
            SettingCategory("lyrics", "Lyrics", lyricsValue, Icons.Default.Description, "Content", SettingsTints.Orange,
                listOf("floating", "overlay", "karaoke", "lrc", "synced", "download")),
            SettingCategory("library_metadata", "Library & Metadata", libraryValue, Icons.Default.LibraryMusic, "Content", SettingsTints.Green,
                listOf("scan", "rescan", "search", "artist", "artwork", "metadata", "wifi", "permissions", "folders")),
            SettingCategory("storage", "Storage & Cache", "Library stats and cache cleanup", Icons.Default.Storage, "System", SettingsTints.Indigo,
                listOf("cache", "clear", "space", "database", "stats")),
            SettingCategory("backup", "Backup & Reset", "Export, restore or reset settings", Icons.Default.SettingsBackupRestore, "System", SettingsTints.Amber,
                listOf("backup", "restore", "export", "import", "reset", "playlists")),
            SettingCategory("interaction", "Interaction & Accessibility", interactionValue, Icons.Default.TouchApp, "System", SettingsTints.Cyan,
                listOf("motion", "animation", "gestures", "start screen", "keep screen on", "accessibility")),
            SettingCategory("about", "About", "Version, battery, notifications, licenses", Icons.Default.Info, "About", SettingsTints.Slate,
                listOf("version", "github", "battery", "notification", "license", "source", "bug")),
            SettingCategory("help", "Help & FAQ", "Fixes for common problems", Icons.AutoMirrored.Filled.Help, "About", SettingsTints.Red,
                listOf("faq", "help", "problem", "support", "guide"))
        )
    }

    BackHandler(enabled = activeSubScreen != null) { activeSubScreen = null }

    val layoutDirection = LocalLayoutDirection.current
    val motion = OniSkin.motion
    val localReduceMotion = LocalReduceMotion.current

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            if (localReduceMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val sign = if (layoutDirection == LayoutDirection.Ltr) 1 else -1
                if (targetState != null) {
                    (slideInHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { it * sign } + fadeIn(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing))) togetherWith
                        (slideOutHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { -it * sign } + fadeOut(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)))
                } else {
                    (slideInHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { -it * sign } + fadeIn(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing))) togetherWith
                        (slideOutHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { it * sign } + fadeOut(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)))
                }
            }
        },
        label = "Settings Navigation"
    ) { subScreen ->
        val back: () -> Unit = { activeSubScreen = null }
        if (subScreen == null) {
            SettingsList(categories) { activeSubScreen = it }
        } else {
            when (subScreen) {
                "appearance" -> AppearanceSettingsScreen(viewModel, onBack = back)
                "widgets" -> WidgetsSettingsScreen(onBack = back)
                "library_metadata" -> LibraryMetadataSettingsScreen(viewModel, onBack = back)
                "playback" -> PlaybackSettingsScreen(viewModel, onBack = back)
                "lyrics" -> LyricsSettingsScreen(viewModel, onBack = back)
                "audio_eq" -> AudioEqualizerSettingsScreen(viewModel, onBack = back)
                "storage" -> StorageSettingsScreen(viewModel, onBack = back)
                "backup" -> BackupSettingsScreen(viewModel, onBack = back)
                "interaction" -> InteractionSettingsScreen(viewModel, onBack = back)
                "about" -> AboutSettingsScreen(onBack = back)
                "help" -> HelpSettingsScreen(onBack = back)
                else -> SettingsDetailPlaceholder(
                    category = categories.find { it.id == subScreen } ?: SettingCategory("unknown", "Settings", "", Icons.Default.Settings, "General"),
                    onBack = back
                )
            }
        }
    }
}

@Composable
fun SettingsList(categories: List<SettingCategory>, onCategoryClick: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    val filtered = remember(categories, trimmed) {
        if (trimmed.isEmpty()) categories else categories.filter { category ->
            (listOf(category.title, category.subtitle, category.section) + category.keywords)
                .any { it.contains(trimmed, ignoreCase = true) }
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.section } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = OniSkin.spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
        contentPadding = PaddingValues(top = OniSkin.spacing.screenVertical, bottom = 96.dp)
    ) {
        item(key = "settings_title") {
            Column(Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.xxs)) {
                Text("Settings", style = OniSkin.typography.displayMedium, color = OniSkin.colors.textPrimary)
                Spacer(Modifier.height(OniSkin.spacing.xxs))
                Text(
                    "Tune oniPlayer to the way you listen",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary
                )
                Spacer(Modifier.height(OniSkin.spacing.md))
            }
        }
        item(key = "settings_search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_search"),
                placeholder = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
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
                    cursorColor = OniSkin.colors.primary,
                    focusedLeadingIconColor = OniSkin.colors.primary,
                    unfocusedLeadingIconColor = OniSkin.colors.textSecondary
                )
            )
        }

        if (filtered.isEmpty()) {
            item(key = "settings_empty") {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = OniSkin.spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = OniSkin.colors.textTertiary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(OniSkin.spacing.sm))
                    Text("Nothing matches \"$trimmed\"", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
                }
            }
        } else {
            grouped.forEach { (sectionName, sectionItems) ->
                item(key = "header_$sectionName") {
                    Text(
                        text = sectionName.uppercase(),
                        style = OniSkin.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.textTertiary,
                        modifier = Modifier.padding(start = OniSkin.spacing.sm, top = OniSkin.spacing.md, bottom = OniSkin.spacing.xxs)
                    )
                }
                item(key = "card_$sectionName") {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = OniSkin.spacing.xxs)) {
                            sectionItems.forEachIndexed { index, category ->
                                SettingsActionRow(
                                    title = category.title,
                                    description = category.subtitle,
                                    icon = category.icon,
                                    iconTint = category.tint,
                                    iconSize = 40.dp,
                                    singleLineDescription = true,
                                    onClick = { onCategoryClick(category.id) },
                                    modifier = Modifier.semantics(mergeDescendants = true) {
                                        contentDescription = "${category.title}, ${category.subtitle}"
                                    },
                                    testTag = "settings_card_${category.id}"
                                )
                                if (index < sectionItems.lastIndex) SettingDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDetailPlaceholder(category: SettingCategory, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        SettingsSubscreenHeader(title = category.title, subtitle = "Settings category", onBack = onBack, backButtonTestTag = "settings_back_button")
        Spacer(Modifier.height(OniSkin.spacing.section))
        Box(Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.screenHorizontal)) {
            OniSurface(
                modifier = Modifier.fillMaxWidth(),
                variant = OniSurfaceVariant.Soft,
                shape = OniSkin.shapes.dialog
            ) {
                Column(Modifier.fillMaxWidth().padding(OniSkin.spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                    SettingsIconTile(icon = category.icon, tint = category.tint, size = 64.dp)
                    Spacer(Modifier.height(OniSkin.spacing.lg))
                    Text(category.title, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                    Spacer(Modifier.height(OniSkin.spacing.xs))
                    Text(category.subtitle, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
                }
            }
        }
    }
}
