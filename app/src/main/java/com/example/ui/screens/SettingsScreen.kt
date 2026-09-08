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
import androidx.compose.ui.unit.dp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

data class SettingCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun SettingsScreen(viewModel: MusicPlayerViewModel) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    val categories = remember {
        listOf(
            SettingCategory("appearance", "Appearance", "Custom themes, animations, & visual layouts", Icons.Default.Palette),
            SettingCategory("playback", "Playback", "Audio engine, equalizer, & crossfade", Icons.Default.PlayCircle),
            SettingCategory("lyrics", "Lyrics", "Floating lyrics, sync settings, & alignment", Icons.Default.Description),
            SettingCategory("library_metadata", "Library & Metadata", "Scan folders, edit tags, & clean duplicates", Icons.Default.Folder),
            SettingCategory("widgets", "Widgets", "Homescreen widget styles & configuration", Icons.Default.Widgets),
            SettingCategory("notifications", "Notifications", "Status bar media controls & alerts", Icons.Default.Notifications),
            SettingCategory("backup", "Backup", "Export & import library database & preferences", Icons.Default.CloudUpload),
            SettingCategory("advanced", "Advanced", "Hardware acceleration, cache, & expert settings", Icons.Default.Tune),
            SettingCategory("help", "Help", "User manual, FAQs, & community support", Icons.Default.Help),
            SettingCategory("about", "About", "Version info, license agreement, & developer", Icons.Default.Info)
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    val transitionDuration = OniSkin.motion.screenTransitionDurationMs

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            val forwardSign = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) 1 else -1
            if (targetState != null) {
                (slideInHorizontally(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing)) { it * forwardSign } +
                        fadeIn(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing)) { -it * forwardSign } +
                        fadeOut(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing)))
            } else {
                (slideInHorizontally(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing)) { -it * forwardSign } +
                        fadeIn(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing)) { it * forwardSign } +
                        fadeOut(animationSpec = tween(transitionDuration, easing = OniSkin.motion.standardEasing)))
            }
        },
        label = "Settings Navigation"
    ) { subScreen ->
        if (subScreen == null) {
            SettingsList(categories = categories, onCategoryClick = { activeSubScreen = it })
        } else {
            val category = categories.find { it.id == subScreen }
            if (category != null) {
                when (category.id) {
                    "appearance" -> AppearanceSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                    "library_metadata" -> LibraryMetadataSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                    "playback" -> PlaybackSettingsScreen(viewModel, onBack = { activeSubScreen = null })
                    else -> SettingsDetailPlaceholder(category, onBack = { activeSubScreen = null })
                }
            } else {
                activeSubScreen = null
            }
        }
    }
}

@Composable
fun SettingsList(
    categories: List<SettingCategory>,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(OniSkin.spacing.screenVertical))
        Text(
            text = "Settings",
            style = OniSkin.typography.displayMedium,
            color = OniSkin.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
        Text(
            text = "Configure Oni Player to match your musical lifestyle",
            style = OniSkin.typography.bodySmall,
            color = OniSkin.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.section))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "${category.title}. ${category.subtitle}"
                        }
                        .clickable { onCategoryClick(category.id) }
                        .testTag("settings_card_${category.id}")
                        .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OniSurface(
                                variant = OniSurfaceVariant.Flat,
                                shape = OniSkin.shapes.button,
                                containerColor = OniSkin.colors.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = OniSkin.colors.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.title,
                                    style = OniSkin.typography.bodyLarge,
                                    color = OniSkin.colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                                Text(
                                    text = category.subtitle,
                                    style = OniSkin.typography.caption,
                                    color = OniSkin.colors.textSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OniSkin.colors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDetailPlaceholder(
    category: SettingCategory,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = category.title,
            subtitle = "Settings category",
            onBack = onBack,
            backButtonTestTag = "settings_back_button"
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.section))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.screenHorizontal)
        ) {
            OniSurface(
                variant = OniSurfaceVariant.Soft,
                shape = OniSkin.shapes.dialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OniSkin.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OniSurface(
                        variant = OniSurfaceVariant.Flat,
                        shape = OniSkin.shapes.card,
                        containerColor = OniSkin.colors.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = OniSkin.colors.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(OniSkin.spacing.lg))
                    Text(category.title, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                    Text(
                        text = category.subtitle,
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(OniSkin.spacing.lg))
                    Text(
                        text = "${category.title} configurations are structured and ready for implementation.",
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.textTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
