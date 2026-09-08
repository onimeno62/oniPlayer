package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
    val isSystemDark = isSystemInDarkTheme()

    LaunchedEffect(selectedThemeOption, isSystemDark) {
        viewModel.updateThemeFromOption(selectedThemeOption, isSystemDark)
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()

    val position by viewModel.audioEngine.position.collectAsState()
    val duration by viewModel.audioEngine.duration.collectAsState()
    val progressFraction = remember(position, duration) {
        if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    }
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
        backgroundTransparency = backgroundTransparency
    ) {
        // Capture composition-local tokens outside the non-composable transition callback.
        val motion = OniSkin.motion
        Scaffold(
            // The shell owns top/side protection. Player still owns its bottom system inset
            // when the bottom chrome is absent. Applied padding is consumed below.
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
                        currentSong?.let { song ->
                            MiniPlayerBar(
                                title = song.customTitle ?: song.title,
                                artist = song.customArtist ?: song.artist,
                                artworkUri = song.albumArtUri,
                                isPlaying = isPlaying,
                                progressFraction = progressFraction,
                                onPlayPauseToggle = { viewModel.togglePlayPause() },
                                onBarClick = { viewModel.selectTab(1) }
                            )
                        }

                        // Keep the supported appearance overrides, but render them through
                        // the shared skin surface. No new blur or background effect is added.
                        val frostedColor = OniSkin.surfaces.frosted.containerColor
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
                                // Preserve the existing slider response: 50 is the baseline.
                                frostedColor.copy(
                                    alpha = (frostedColor.alpha * (backgroundTransparency / 50f)).coerceIn(0f, 1f)
                                )
                            } else null
                        )
                    }
                }
            },
            // ThemeProvider remains the background owner, including AMOLED compatibility.
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                // A direction-neutral crossfade preserves the existing navigation behavior.
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(motion.screenTransitionDurationMs, easing = motion.standardEasing)) togetherWith
                            fadeOut(animationSpec = tween(motion.screenTransitionDurationMs, easing = motion.standardEasing))
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

@Composable
fun MiniPlayerBar(
    title: String,
    artist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    progressFraction: Float,
    onPlayPauseToggle: () -> Unit,
    onBarClick: () -> Unit
) {
    com.example.ui.components.playback.OniMiniPlayer(
        title = title,
        artist = artist,
        artworkUri = artworkUri,
        isPlaying = isPlaying,
        progressFraction = progressFraction,
        onPlayPauseClick = onPlayPauseToggle,
        onPlayerClick = onBarClick
    )
}
