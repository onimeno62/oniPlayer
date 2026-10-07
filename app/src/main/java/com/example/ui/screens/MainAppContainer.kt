package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.LibraryPreferencesStore
import com.example.ui.components.navigation.OniFloatingNavigation
import com.example.ui.components.navigation.OniNavigationDestination
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.onboarding.OnboardingScreen
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
    val context = LocalContext.current

    LaunchedEffect(selectedThemeOption, isSystemDark) {
        viewModel.updateThemeFromOption(selectedThemeOption, isSystemDark)
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()

    // Mini-player visibility: every tab except Player. On the Library dashboard it only shows
    // once the Continue Listening card has scrolled off screen (both show the same track).
    var dashboardResumeVisible by remember { mutableStateOf(true) }
    val showMiniPlayer = currentSong != null && currentTab != 1 && !(currentTab == 0 && dashboardResumeVisible)

    // ---- first-run onboarding ----------------------------------------------------------------
    val onboardingDone by LibraryPreferencesStore.onboardingDone(context)
        .collectAsStateWithLifecycle<Boolean?>(initialValue = null)
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.rescanLibrary()
            LibraryPreferencesStore.setOnboardingDone(context)
        }
    }
    val startScanFromOnboarding = {
        val has = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (has) {
            viewModel.rescanLibrary()
            LibraryPreferencesStore.setOnboardingDone(context)
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    val destinations = remember {
        listOf(
            OniNavigationDestination(0, "Library", Icons.Default.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
            OniNavigationDestination(1, "Player", Icons.Default.PlayCircle, Icons.Outlined.PlayCircle, "nav_player"),
            OniNavigationDestination(2, "Discover", Icons.Default.Explore, Icons.Outlined.Explore, "nav_discover"),
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

        if (onboardingDone == false) {
            OnboardingScreen(
                onScanLibrary = startScanFromOnboarding,
                onSkip = { LibraryPreferencesStore.setOnboardingDone(context) },
                isScanning = isScanning,
                reduceMotion = reduceMotion
            )
            return@OniPlayerTheme
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            ),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(if (currentTab == 1) Color.Black else OniSkin.colors.background)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                            )
                        )
                ) {
                    // The Player keeps the same primary app navigation as every other
                    // top-level screen. Only the contextual mini-player is suppressed
                    // while Player is active.
                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = if (reduceMotion) EnterTransition.None else expandVertically() + fadeIn(),
                        exit = if (reduceMotion) ExitTransition.None else shrinkVertically() + fadeOut()
                    ) {
                        MiniPlayerBar(
                            viewModel = viewModel,
                            surfaceVariant = if (glassEffectEnabled) OniSurfaceVariant.Frosted else OniSurfaceVariant.Soft
                        )
                    }
                    OniFloatingNavigation(
                        destinations = destinations,
                        selectedId = currentTab,
                        onDestinationSelected = { destination ->
                            if (destination == 0) viewModel.goToLibraryDashboard()
                            else viewModel.selectTab(destination)
                        },
                        surfaceVariant = OniSurfaceVariant.Soft,
                        shape = OniSkin.navigation.shape,
                        containerColor = OniSkin.surfaces.soft.containerColor
                    )
                }
            },
            // Keep Scaffold/content and the navigation component on their existing surfaces.
            // The bottom-bar host itself paints the area behind the navigation separately.
            containerColor = OniSkin.colors.background,
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
                        0 -> LibraryHostScreen(
                            viewModel = viewModel,
                            onDashboardResumeVisibleChange = { dashboardResumeVisible = it }
                        )
                        1 -> PlayerScreen(viewModel = viewModel)
                        2 -> DiscoverScreen()
                        3 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
