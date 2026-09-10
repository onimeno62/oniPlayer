package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

data class SettingCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val section: String
)

@Composable
fun SettingsScreen(viewModel: MusicPlayerViewModel) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    
    val selectedThemeOption by viewModel.selectedThemeOption.collectAsStateWithLifecycle()
    val nextSongDelaySeconds by viewModel.nextSongDelaySeconds.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val autoSearchArtistData by viewModel.autoSearchArtistData.collectAsStateWithLifecycle()
    val floatingLyricsEnabled by viewModel.floatingLyricsEnabled.collectAsStateWithLifecycle()
    val isAutoDownloadLyrics by viewModel.isAutoDownloadEnabled.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsStateWithLifecycle()

    val appearanceValue = "$selectedThemeOption • Default Skin"
    val playbackValue = if (crossfadeEnabled) "Crossfade • ${crossfadeDurationSeconds}s" else if (nextSongDelaySeconds > 0) "Delay • ${nextSongDelaySeconds}s" else "Gapless playback"
    val lyricsValue = if (floatingLyricsEnabled) "Floating lyrics active" else if (isAutoDownloadLyrics) "Auto-download enabled" else "Manual lyrics"
    val libraryValue = if (autoSearchArtistData) "Automatic metadata" else "Local library only"
    val interactionValue = if (reduceMotion) "Reduced motion active" else "Gesture controls enabled"

    val categories = remember(appearanceValue, playbackValue, lyricsValue, libraryValue, interactionValue) {
        listOf(
            SettingCategory("appearance", "Appearance", appearanceValue, Icons.Default.Palette, "Appearance"),
            SettingCategory("playback", "Playback", playbackValue, Icons.Default.PlayCircle, "Playback"),
            SettingCategory("audio_eq", "Audio & Equalizer", "5-band parametric EQ & spatializer", Icons.Default.Tune, "Playback"),
            SettingCategory("lyrics", "Lyrics", lyricsValue, Icons.Default.Description, "Lyrics"),
            SettingCategory("library_metadata", "Library & Metadata", libraryValue, Icons.Default.LibraryMusic, "Library"),
            SettingCategory("storage", "Storage & Cache", "Library storage & database stats", Icons.Default.Storage, "Library"),
            SettingCategory("interaction", "Interaction & Accessibility", interactionValue, Icons.Default.TouchApp, "Interaction & Accessibility"),
            SettingCategory("about", "About & Legal", "oniPlayer v1.0 • Default Skin", Icons.Default.Info, "Application"),
            SettingCategory("help", "Help & Guidance", "FAQ, gestures, & audio guide", Icons.Default.Help, "Application")
        )
    }

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
        if (subScreen == null) {
            SettingsList(categories) { activeSubScreen = it }
        } else {
            when (subScreen) {
                "appearance" -> AppearanceSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "library_metadata" -> LibraryMetadataSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "playback" -> PlaybackSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "lyrics" -> LyricsSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "audio_eq" -> AudioEqualizerSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "storage" -> StorageSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "interaction" -> InteractionSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "about" -> AboutSettingsScreen(onBack = { activeSubScreen = null })
                "help" -> HelpSettingsScreen(onBack = { activeSubScreen = null })
                else -> SettingsDetailPlaceholder(
                    category = categories.find { it.id == subScreen } ?: SettingCategory("unknown", "Settings", "", Icons.Default.Settings, "General"),
                    onBack = { activeSubScreen = null }
                )
            }
        }
    }
}

@Composable
fun SettingsList(categories: List<SettingCategory>, onCategoryClick: (String) -> Unit) {
    val grouped = remember(categories) { categories.groupBy { it.section } }

    Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = OniSkin.spacing.screenHorizontal)) {
        Spacer(Modifier.height(OniSkin.spacing.screenVertical))
        Text("Settings", style = OniSkin.typography.displayMedium, color = OniSkin.colors.textPrimary)
        Spacer(Modifier.height(OniSkin.spacing.xxs))
        Text("Configure oniPlayer Default Skin, audio engine, and playback", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
        Spacer(Modifier.height(OniSkin.spacing.section))

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            grouped.forEach { (sectionName, itemsList) ->
                item {
                    Text(
                        text = sectionName.uppercase(),
                        style = OniSkin.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OniSkin.colors.primary,
                        modifier = Modifier.padding(horizontal = OniSkin.spacing.xs, vertical = OniSkin.spacing.xxs)
                    )
                }
                items(itemsList, key = { it.id }) { category ->
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${category.title}, ${category.subtitle}"
                            }
                            .clickable { onCategoryClick(category.id) }
                            .testTag("settings_card_${category.id}")
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OniSurface(
                                variant = OniSurfaceVariant.Flat,
                                shape = OniSkin.shapes.button,
                                containerColor = OniSkin.colors.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(category.icon, null, tint = OniSkin.colors.primary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(OniSkin.spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    category.title,
                                    style = OniSkin.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OniSkin.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(OniSkin.spacing.xxs))
                                Text(
                                    category.subtitle,
                                    style = OniSkin.typography.caption,
                                    color = OniSkin.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(OniSkin.spacing.xs))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = OniSkin.colors.textTertiary, modifier = Modifier.size(20.dp))
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
            OniSurface(OniSurfaceVariant.Soft, OniSkin.shapes.dialog, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(OniSkin.spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                    OniSurface(OniSurfaceVariant.Flat, OniSkin.shapes.card, containerColor = OniSkin.colors.primary.copy(alpha = 0.12f), modifier = Modifier.size(64.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(category.icon, null, tint = OniSkin.colors.primary, modifier = Modifier.size(32.dp)) }
                    }
                    Spacer(Modifier.height(OniSkin.spacing.lg))
                    Text(category.title, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                    Spacer(Modifier.height(OniSkin.spacing.xs))
                    Text(category.subtitle, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
                }
            }
        }
    }
}
