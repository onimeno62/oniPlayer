package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun MainAppContainer(viewModel: MusicPlayerViewModel) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    
    val position by viewModel.audioEngine.position.collectAsState()
    val duration by viewModel.audioEngine.duration.collectAsState()
    val progressFraction = remember(position, duration) {
        if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    }

    OniPlayerTheme(theme = currentTheme, currentSong = currentSong) {
        Scaffold(
            bottomBar = {
                if (currentTab != 1) { // Floating bottom navigation disappears while playing!
                    Column(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Mini Player Bar (Frosted glass floating capsule)
                        if (currentSong != null) {
                            MiniPlayerBar(
                                title = currentSong!!.customTitle ?: currentSong!!.title,
                                artist = currentSong!!.customArtist ?: currentSong!!.artist,
                                artworkUri = currentSong!!.albumArtUri,
                                isPlaying = isPlaying,
                                progressFraction = progressFraction,
                                onPlayPauseToggle = { viewModel.togglePlayPause() },
                                onBarClick = { viewModel.selectTab(1) } // jump to Full Player
                            )
                        }

                        // Floating Bottom Navigation Bar (Frosted Glass Capsule)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp, top = 2.dp),
                            shape = RoundedCornerShape(28.dp), // Premium 28dp radius
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f) // Glass Surface
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), // Glass Border 12%
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat floating glass look
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                modifier = Modifier.height(64.dp)
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { viewModel.selectTab(0) },
                                    icon = { Icon(if (currentTab == 0) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "Library") },
                                    label = { Text("Library", fontSize = 10.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LocalAccentColor.current,
                                        selectedTextColor = LocalAccentColor.current,
                                        indicatorColor = LocalAccentColor.current.copy(alpha = 0.14f),
                                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_library")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { viewModel.selectTab(1) },
                                    icon = { Icon(if (currentTab == 1) Icons.Default.PlayCircle else Icons.Outlined.PlayCircle, contentDescription = "Player") },
                                    label = { Text("Player", fontSize = 10.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LocalAccentColor.current,
                                        selectedTextColor = LocalAccentColor.current,
                                        indicatorColor = LocalAccentColor.current.copy(alpha = 0.14f),
                                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_player")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { viewModel.selectTab(2) },
                                    icon = { Icon(if (currentTab == 2) Icons.Default.Tune else Icons.Outlined.Tune, contentDescription = "Equalizer") },
                                    label = { Text("Equalizer", fontSize = 10.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LocalAccentColor.current,
                                        selectedTextColor = LocalAccentColor.current,
                                        indicatorColor = LocalAccentColor.current.copy(alpha = 0.14f),
                                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_equalizer")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { viewModel.selectTab(3) },
                                    icon = { Icon(if (currentTab == 3) Icons.Default.Palette else Icons.Outlined.Palette, contentDescription = "Themes") },
                                    label = { Text("Themes", fontSize = 10.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LocalAccentColor.current,
                                        selectedTextColor = LocalAccentColor.current,
                                        indicatorColor = LocalAccentColor.current.copy(alpha = 0.14f),
                                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_themes")
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent, // Let the background auras shine through!
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (currentTab == 1) 0.dp else innerPadding.calculateBottomPadding()
                    )
            ) {
                // Crossfade or slide animation when switching screens
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(260))
                    },
                    label = "Screen transition"
                ) { tab ->
                    when (tab) {
                        0 -> LibraryScreen(viewModel = viewModel)
                        1 -> PlayerScreen(viewModel = viewModel)
                        2 -> EqualizerScreen(viewModel = viewModel)
                        3 -> ThemesScreen(viewModel = viewModel)
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
    val accentColor = LocalAccentColor.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onBarClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161922).copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beautifully Rounded Tiny Album Art
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_media_play)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Play/Pause button
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.12f), CircleShape)
                        .size(38.dp)
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Elegant, high-craft 2dp Mini Progress Bar at bottom of card
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.06f),
            )
        }
    }
}
