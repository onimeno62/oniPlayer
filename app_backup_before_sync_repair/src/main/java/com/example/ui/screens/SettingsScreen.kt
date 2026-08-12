package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAccentColor
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
                if (category.id == "appearance") {
                    AppearanceSettingsScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
                } else if (category.id == "library_metadata") {
                    LibraryMetadataSettingsScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
                } else if (category.id == "playback") {
                    PlaybackSettingsScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
                } else {
                    SettingsDetailPlaceholder(
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
            .background(Color.Transparent)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Text(
            text = "Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure Oni Player to match your musical lifestyle",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Cards list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp) // extra padding to clear mini player & bottom bar
        ) {
            items(categories, key = { it.id }) { category ->
                val accentColor = LocalAccentColor.current
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onCategoryClick(category.id) }
                        .testTag("settings_card_${category.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Beautiful container for Icon
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.title,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = category.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Navigation Arrow
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Navigate to ${category.title}",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
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
    val accentColor = LocalAccentColor.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        // Custom Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                    .size(40.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = category.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Settings category",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Modern Glass Card to show Detail Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = category.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = category.subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "${category.title} configurations are structured and ready for implementation. No functionality has been activated yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(viewModel: MusicPlayerViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appearance",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("appearance_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val accentColor = LocalAccentColor.current
        val selectedTheme by viewModel.selectedThemeOption.collectAsState()
        val selectedAccentColor by viewModel.customAccentColor.collectAsState()
        val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()
        val glassEffectEnabled by viewModel.glassEffectEnabled.collectAsState()
        val blurStrength by viewModel.blurStrength.collectAsState()
        val cornerRadius by viewModel.cornerRadius.collectAsState()
        val backgroundTransparency by viewModel.backgroundTransparency.collectAsState()
        val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
        val themeOptions = listOf("Light", "Dark", "AMOLED", "Follow System")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Theme Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Theme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose the application's appearance.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("appearance_card_theme"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            themeOptions.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setThemeOption(option, isSystemDark) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("theme_option_$option"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (selectedTheme == option),
                                        onClick = { viewModel.setThemeOption(option, isSystemDark) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = accentColor,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = option,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selectedTheme == option) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Accent Colors Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Accent Colors",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("appearance_card_accent_colors"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Material You Setting
                            SwitchSettingRow(
                                title = "Material You",
                                description = "Use colors extracted from your wallpaper.",
                                checked = materialYouEnabled,
                                onCheckedChange = { viewModel.setMaterialYouEnabled(it) },
                                testTag = "setting_material_you"
                            )
                            
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            // Accent Color Setting
                            AccentColorPickerRow(
                                title = "Accent Color",
                                selectedColorHex = selectedAccentColor,
                                onColorSelected = { viewModel.setCustomAccentColor(it) },
                                enabled = !materialYouEnabled,
                                testTag = "setting_accent_color"
                            )
                        }
                    }
                }
            }

            // Visual Effects Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Visual Effects",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("appearance_card_visual_effects"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Glass Effect Switch
                            SwitchSettingRow(
                                title = "Glass Effect",
                                description = null,
                                checked = glassEffectEnabled,
                                onCheckedChange = { viewModel.setGlassEffectEnabled(it) },
                                testTag = "setting_glass_effect"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Blur Strength Slider
                            SliderSettingRow(
                                title = "Blur Strength",
                                value = blurStrength,
                                onValueChange = { viewModel.setBlurStrength(it) },
                                valueRange = 0f..100f,
                                valueFormatter = { "${it.toInt()}" },
                                enabled = glassEffectEnabled,
                                testTag = "setting_blur_strength"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Corner Radius Slider
                            SliderSettingRow(
                                title = "Corner Radius",
                                value = cornerRadius,
                                onValueChange = { viewModel.setCornerRadius(it) },
                                valueRange = 8f..32f,
                                valueFormatter = { "${it.toInt()}dp" },
                                enabled = glassEffectEnabled,
                                testTag = "setting_corner_radius"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Transparency Slider
                            SliderSettingRow(
                                title = "Background Transparency",
                                value = backgroundTransparency,
                                onValueChange = { viewModel.setBackgroundTransparency(it) },
                                valueRange = 0f..100f,
                                valueFormatter = { "${it.toInt()}%" },
                                enabled = glassEffectEnabled,
                                testTag = "setting_background_transparency"
                            )
                        }
                    }
                }
            }

            // Player Style Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Player Style",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("appearance_card_player_style"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    }
                }
            }

            // Typography Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Typography",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("appearance_card_typography"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchSettingRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = LocalAccentColor.current,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("${testTag ?: "switch"}_control")
        )
    }
}

@Composable
fun AccentColorPickerRow(
    title: String,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val colors = listOf(
        "#7C4DFF" to "Purple",
        "#00E5FF" to "Cyan",
        "#00E676" to "Green",
        "#FFD600" to "Yellow",
        "#FF9800" to "Orange",
        "#FF1744" to "Red",
        "#E91E63" to "Pink"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { (hex, name) ->
                val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (enabled) color else color.copy(alpha = 0.38f))
                        .clickable(enabled = enabled) { onColorSelected(hex) }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("color_circle_$name"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (hex == "#FFD600" || hex == "#00E5FF" || hex == "#00E676") Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSettingRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueFormatter(value),
                fontSize = 14.sp,
                color = if (enabled) LocalAccentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                activeTrackColor = if (enabled) LocalAccentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTag ?: "slider"}_control")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryMetadataSettingsScreen(viewModel: MusicPlayerViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library & Metadata",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("library_metadata_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val accentColor = LocalAccentColor.current
        val autoSearchArtistData by viewModel.autoSearchArtistData.collectAsState()
        val autoSearchWifiOnly by viewModel.autoSearchWifiOnly.collectAsState()
        val nextSongDelaySeconds by viewModel.nextSongDelaySeconds.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Artist Data Auto-Search Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Artist Data Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configure how biography and pictures are fetched from online music databases.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchSettingRow(
                                title = "Automatically search artist data",
                                description = "Automatically search and fetch the artist's biography and picture from web databases when loading an artist.",
                                checked = autoSearchArtistData,
                                onCheckedChange = { viewModel.setAutoSearchArtistData(it) },
                                testTag = "setting_auto_search_artist"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            SwitchSettingRow(
                                title = "Only search on Wi-Fi",
                                description = "To save mobile network usage, only search and download artist biography and artwork when connected to Wi-Fi.",
                                checked = autoSearchWifiOnly,
                                onCheckedChange = { viewModel.setAutoSearchWifiOnly(it) },
                                testTag = "setting_auto_search_wifi_only"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    value: Int,
    range: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelFormatter: (Int) -> String = { it.toString() }
) {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = range.indexOf(value).coerceAtLeast(0)
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState)
    val accentColor = LocalAccentColor.current

    // Observe changes to the scrolling position to update the value
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val centerIndex = lazyListState.firstVisibleItemIndex
            if (centerIndex in range.indices) {
                onValueChange(range[centerIndex])
            }
        }
    }

    // Keep state in sync with external value changes
    LaunchedEffect(value) {
        val targetIndex = range.indexOf(value)
        if (targetIndex >= 0 && targetIndex != lazyListState.firstVisibleItemIndex) {
            lazyListState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .height(110.dp)
            .width(70.dp),
        contentAlignment = Alignment.Center
    ) {
        // Highlighting bar behind
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = 37.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(range.size) { index ->
                val itemValue = range[index]
                val isSelected = itemValue == value
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onValueChange(itemValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFormatter(itemValue),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun DurationPickerSettingRow(
    title: String,
    totalSeconds: Int,
    onValueChange: (Int) -> Unit,
    testTag: String? = null
) {
    val accentColor = LocalAccentColor.current
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    totalSeconds == 0 -> "No Delay (Instant)"
                    totalSeconds < 60 -> "$totalSeconds seconds"
                    totalSeconds % 60 == 0 -> "${totalSeconds / 60} minute${if (totalSeconds / 60 > 1) "s" else ""}"
                    else -> "${totalSeconds / 60} min ${totalSeconds % 60} sec"
                },
                fontSize = 14.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Picker controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minutes Wheel Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(90.dp)
            ) {
                Text(
                    text = "MINUTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                WheelPicker(
                    value = minutes,
                    range = (0..30).toList(),
                    onValueChange = { newMin ->
                        onValueChange(newMin * 60 + seconds)
                    },
                    labelFormatter = { it.toString() }
                )
            }

            // Divider / Colon
            Text(
                text = ":",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)
            )

            // Seconds Wheel Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(90.dp)
            ) {
                Text(
                    text = "SECONDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                WheelPicker(
                    value = seconds,
                    range = (0..59).toList(),
                    onValueChange = { newSec ->
                        onValueChange(minutes * 60 + newSec)
                    },
                    labelFormatter = { String.format("%02d", it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick presets
        Text(
            text = "QUICK PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf(
                0 to "Instant",
                5 to "5s",
                10 to "10s",
                30 to "30s",
                60 to "1m",
                120 to "2m"
            )
            val chunks = presets.chunked(3)
            chunks.forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEach { (secs, label) ->
                        val isSelected = totalSeconds == secs
                        SuggestionChip(
                            onClick = { onValueChange(secs) },
                            label = { 
                                Text(
                                    text = label, 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) 
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                                labelColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                enabled = true
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(viewModel: MusicPlayerViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playback Options",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("playback_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val accentColor = LocalAccentColor.current
        val nextSongDelaySeconds by viewModel.nextSongDelaySeconds.collectAsState()
        val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsState()
        val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Playback Delay Settings Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Auto-Play Delay Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Set a custom minutes/seconds delay before automatically playing the next song.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        DurationPickerSettingRow(
                            title = "Next Song Delay",
                            totalSeconds = nextSongDelaySeconds,
                            onValueChange = { viewModel.setNextSongDelaySeconds(it) },
                            testTag = "setting_playback_delay"
                        )
                    }
                }
            }

            // Playback Crossfade Settings Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Playback Crossfade Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Smoothly fade out the current song and fade in the next song near the end of playback.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchSettingRow(
                                title = "Enable Crossfade",
                                description = "Fade tracks into each other.",
                                checked = crossfadeEnabled,
                                onCheckedChange = { viewModel.setCrossfadeEnabled(it) },
                                testTag = "setting_crossfade_enabled"
                            )

                            if (crossfadeEnabled) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                SliderSettingRow(
                                    title = "Crossfade Duration",
                                    value = crossfadeDurationSeconds.toFloat(),
                                    onValueChange = { viewModel.setCrossfadeDurationSeconds(it.toInt()) },
                                    valueRange = 1f..20f,
                                    valueFormatter = { value -> "${value.toInt()} seconds" },
                                    testTag = "setting_crossfade_duration"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

