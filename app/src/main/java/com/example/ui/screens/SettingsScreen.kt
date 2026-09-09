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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

data class SettingCategory(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

@Composable
fun SettingsScreen(viewModel: MusicPlayerViewModel) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    val categories = remember {
        listOf(
            SettingCategory("appearance", "Appearance", "Custom themes, animations, & visual layouts", Icons.Default.Palette),
            SettingCategory("playback", "Playback", "Audio engine, equalizer, & crossfade", Icons.Default.PlayCircle),
            SettingCategory("lyrics", "Lyrics", "Floating lyrics, sync settings, & alignment", Icons.Default.Description),
            SettingCategory("library_metadata", "Library & Metadata", "Search, scan, metadata, and library organization", Icons.Default.LibraryMusic),
            SettingCategory("widgets", "Widgets", "Homescreen widget styles & configuration", Icons.Default.Widgets),
            SettingCategory("notifications", "Notifications", "Status bar media controls & alerts", Icons.Default.Notifications),
            SettingCategory("backup", "Backup", "Export & import library database & preferences", Icons.Default.CloudUpload),
            SettingCategory("advanced", "Advanced", "Hardware acceleration, cache, & expert settings", Icons.Default.Tune),
            SettingCategory("help", "Help", "User manual, FAQs, & community support", Icons.Default.Help),
            SettingCategory("about", "About", "Version info, license agreement, & developer", Icons.Default.Info)
        )
    }
    val layoutDirection = LocalLayoutDirection.current
    val motion = OniSkin.motion

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            val sign = if (layoutDirection == LayoutDirection.Ltr) 1 else -1
            if (targetState != null) {
                (slideInHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { it * sign } + fadeIn(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing))) togetherWith
                    (slideOutHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { -it * sign } + fadeOut(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)))
            } else {
                (slideInHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { -it * sign } + fadeIn(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing))) togetherWith
                    (slideOutHorizontally(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) { it * sign } + fadeOut(tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)))
            }
        },
        label = "Settings Navigation"
    ) { subScreen ->
        if (subScreen == null) {
            SettingsList(categories) { activeSubScreen = it }
        } else {
            val category = categories.find { it.id == subScreen }
            when (category?.id) {
                "appearance" -> AppearanceSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "library_metadata" -> LibraryMetadataSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                "playback" -> PlaybackSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                null -> if (category != null) SettingsDetailPlaceholder(category, onBack = { activeSubScreen = null }) else activeSubScreen = null
                else -> SettingsDetailPlaceholder(category, onBack = { activeSubScreen = null })
            }
        }
    }
}

@Composable
fun SettingsList(categories: List<SettingCategory>, onCategoryClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = OniSkin.spacing.screenHorizontal)) {
        Spacer(Modifier.height(OniSkin.spacing.screenVertical))
        Text("Settings", style = OniSkin.typography.displayMedium, color = OniSkin.colors.textPrimary)
        Spacer(Modifier.height(OniSkin.spacing.xxs))
        Text("Configure Oni Player to match your musical lifestyle", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
        Spacer(Modifier.height(OniSkin.spacing.section))
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(categories, key = { it.id }) { category ->
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "${category.title}. ${category.subtitle}" }.clickable { onCategoryClick(category.id) }.testTag("settings_card_${category.id}")
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        OniSurface(OniSurfaceVariant.Flat, OniSkin.shapes.button, containerColor = OniSkin.colors.primary.copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(category.icon, null, tint = OniSkin.colors.primary, modifier = Modifier.size(22.dp)) }
                        }
                        Spacer(Modifier.width(OniSkin.spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(category.title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(OniSkin.spacing.xxs))
                            Text(category.subtitle, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(OniSkin.spacing.xs))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = OniSkin.colors.textTertiary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
