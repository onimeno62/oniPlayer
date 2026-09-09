package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.navigation.OniFloatingNavigation
import com.example.ui.components.navigation.OniNavigationDestination
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniPlayerTheme
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun MainAppContainer(viewModel: MusicPlayerViewModel) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val selectedThemeOption by viewModel.selectedThemeOption.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()
    val glassEffectEnabled by viewModel.glassEffectEnabled.collectAsState()
    val blurStrength by viewModel.blurStrength.collectAsState()
    val cornerRadius by viewModel.cornerRadius.collectAsState()
    val backgroundTransparency by viewModel.backgroundTransparency.collectAsState()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    LaunchedEffect(selectedThemeOption, isSystemDark) {
        viewModel.updateThemeFromOption(selectedThemeOption, isSystemDark)
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()

    val destinations = remember {
        listOf(
            OniNavigationDestination(0, "Library", Icons.Default.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
            OniNavigationDestination(1, "Player", Icons.Default.PlayCircle, Icons.Outlined.PlayCircle, "nav_player"),
            OniNavigationDestination(2, "Equalizer", Icons.Default.Tune, Icons.Outlined.Tune, "nav_equalizer"),
            OniNavigationDestination(3, "Settings", Icons.Default.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    OniPlayerTheme(
        theme = currentTheme,
        currentSong = currentSong,
        customAccentColor = customAccentColor,
        materialYouEnabled = materialYouEnabled,
        glassEffectEnabled = glassEffectEnabled,
        blurStrength = blurStrength,
        cornerRadius = cornerRadius,
        backgroundTransparency = backgroundTransparency,
        reduceMotion = reduceMotion
    ) {
        val motion = OniSkin.motion
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            ),
            bottomBar = {
                if (currentTab != 1) {
                    Column(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                            )
                        )
                    ) {
                        OniFloatingNavigation(
                            destinations = destinations,
                            selectedId = currentTab,
                            onDestinationSelected = { destination ->
                                if (destination == 0) viewModel.goToLibraryDashboard()
                                else viewModel.selectTab(destination)
                            },
                            surfaceVariant = if (glassEffectEnabled) OniSurfaceVariant.Frosted else OniSurfaceVariant.Soft,
                            shape = if (glassEffectEnabled) RoundedCornerShape(cornerRadius.dp) else OniSkin.navigation.shape,
                            containerColor = if (glassEffectEnabled) {
                                val frostedColor = OniSkin.surfaces.frosted.containerColor
                                frostedColor.copy(
                                    alpha = (frostedColor.alpha * (backgroundTransparency / 50f)).coerceIn(0f, 1f)
                                )
                            } else null
                        )
                    }
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        if (reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            fadeIn(animationSpec = tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) togetherWith
                                fadeOut(animationSpec = tween(motion.screenTransitionDurationMs, easing = motion.standardEasing))
                        }
                    },
                    label = "Screen transition"
                ) { tab ->
                    when (tab) {
                        0 -> LibraryScreen(viewModel = viewModel)
                        1 -> PlayerScreen(viewModel = viewModel)
                        2 -> EqualizerScreen(viewModel = viewModel)
                        3 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
