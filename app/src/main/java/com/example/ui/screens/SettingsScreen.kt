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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            SettingCategory(
                id = "appearance",
                title = "Appearance",
                subtitle = "Custom themes, animations, & visual layouts",
                icon = Icons.Default.Palette
            ),
            SettingCategory(
                id = "playback",
                title = "Playback",
                subtitle = "Audio engine, equalizer, & crossfade",
                icon = Icons.Default.PlayCircle
            ),
            SettingCategory(
                id = "lyrics",
                title = "Lyrics",
                subtitle = "Floating lyrics, sync settings, & alignment",
                icon = Icons.Default.Description
            ),
            SettingCategory(
                id = "library_metadata",
                title = "Library & Metadata",
                subtitle = "Scan folders, edit tags, & clean duplicates",
                icon = Icons.Default.Folder
            ),
            SettingCategory(
                id = "widgets",
                title = "Widgets",
                subtitle = "Homescreen widget styles & configuration",
                icon = Icons.Default.Widgets
            ),
            SettingCategory(
                id = "notifications",
                title = "Notifications",
                subtitle = "Status bar media controls & alerts",
                icon = Icons.Default.Notifications
            ),
            SettingCategory(
                id = "backup",
                title = "Backup",
                subtitle = "Export & import library database & preferences",
                icon = Icons.Default.CloudUpload
            ),
            SettingCategory(
                id = "advanced",
                title = "Advanced",
                subtitle = "Hardware acceleration, cache, & expert settings",
                icon = Icons.Default.Tune
            ),
            SettingCategory(
                id = "help",
                title = "Help",
                subtitle = "User manual, FAQs, & community support",
                icon = Icons.Default.Help
            ),
            SettingCategory(
                id = "about",
                title = "About",
                subtitle = "Version info, license agreement, & developer",
                icon = Icons.Default.Info
            )
        )
    }

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            if (targetState != null) {
                // Navigate forward
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(animationSpec = tween(300)))
            } else {
                // Navigate backward
                (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300)))
            }
        },
        label = "Settings Navigation"
    ) { subScreen ->
        if (subScreen == null) {
            SettingsList(
                categories = categories,
                onCategoryClick = { activeSubScreen = it }
            )
        } else {
            val category = categories.find { it.id == subScreen }
            if (category != null) {
                when (category.id) {
                    "appearance" -> AppearanceSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = null }
                    )
                    "library_metadata" -> LibraryMetadataSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = null }
                    )
                    "playback" -> PlaybackSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = null }
                    )
                    else -> SettingsDetailPlaceholder(
                        category = category,
                        onBack = { activeSubScreen = null }
                    )
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
        
        // Header
        Text(
            text = "Settings",
            style = OniSkin.typography.displayMedium,
            color = OniSkin.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Configure Oni Player to match your musical lifestyle",
            style = OniSkin.typography.bodySmall,
            color = OniSkin.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.section))

        // Cards list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_card_${category.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category.id) }
                            .padding(
                                horizontal = OniSkin.spacing.md,
                                vertical = OniSkin.spacing.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon container using OniSkin surface & primary tint
                        OniSurface(
                            variant = OniSurfaceVariant.Flat,
                            shape = OniSkin.shapes.button,
                            containerColor = OniSkin.colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.title,
                                    tint = OniSkin.colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(OniSkin.spacing.md))

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.title,
                                style = OniSkin.typography.bodyLarge,
                                color = OniSkin.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = category.subtitle,
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))

                        // Navigation Arrow
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate to ${category.title}",
                            tint = OniSkin.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = OniSkin.colors.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(OniSkin.spacing.lg))

                    Text(
                        text = category.title,
                        style = OniSkin.typography.titleMedium,
                        color = OniSkin.colors.textPrimary
                    )

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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
