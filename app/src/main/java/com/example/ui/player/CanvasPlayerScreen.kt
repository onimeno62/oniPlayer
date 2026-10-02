package com.example.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

enum class LyricLineMode { UNDERNEATH, OVER_BOTTOM, HIDDEN }
enum class SeekBarStyle { SIMPLE, WAVY, DOT_LINE, AUDIO_SPECTRUM }

/**
 * Canvas Player Screen - Apple Music inspired design
 * Features: Edge-to-edge artwork, serif typography, glass panel controls,
 * live lyric line with multiple display modes, and configurable seek bar styles.
 */
@Composable
fun CanvasPlayerScreen(
    title: String = "Again",
    artist: String = "Sheldon Riley",
    artwork: String = "",
    quality: String = "Hi-Res 320",
    isPlaying: Boolean = false,
    currentPosition: Long = 102000,
    duration: Long = 210000,
    currentLyric: String = "I keep falling again...",
    lyricLineMode: LyricLineMode = LyricLineMode.OVER_BOTTOM,
    lyricLineCount: Int = 2,
    seekBarStyle: SeekBarStyle = SeekBarStyle.SIMPLE,
    audioAnalyzerEnabled: Boolean = false,
    artworkEffectsEnabled: Boolean = true,
    beatEnergy: Float = 0.5f,
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var dragOffset by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen artwork with swipe-to-skip gesture
        AsyncImage(
            model = artwork,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        dragOffset += dragAmount
                        if (dragOffset > 100) { onNextClick(); dragOffset = 0f }
                        if (dragOffset < -100) { onPreviousClick(); dragOffset = 0f }
                    }
                },
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                    )
                )
        )

        // Top collapse button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Collapse", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        // Menu button
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Tune, "Menu", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        // Glass panel with controls - positioned at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(16.dp)
        ) {
            // Track info with serif title font
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = artist,
                fontSize = 16.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Quality/format badge
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(quality, fontSize = 12.sp, color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live lyric line - OVER_BOTTOM mode (over bottom edge of artwork)
            if (lyricLineMode == LyricLineMode.OVER_BOTTOM && currentLyric.isNotEmpty()) {
                Text(
                    text = currentLyric,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Progress bar - currently simple slider (SIMPLE style)
            // TODO: Add support for WAVY, DOT_LINE, AUDIO_SPECTRUM variants
            if (seekBarStyle == SeekBarStyle.SIMPLE) {
                Slider(
                    value = currentPosition.toFloat() / duration.toFloat().coerceAtLeast(1f),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }

            // Time labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${currentPosition / 60000}:${(currentPosition % 60000) / 1000}", fontSize = 12.sp, color = Color.LightGray)
                Text("${duration / 60000}:${(duration % 60000) / 1000}", fontSize = 12.sp, color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /*shuffle*/ }) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = Color.White)
                }
                
                IconButton(onClick = onPreviousClick) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                // Play/Pause - large center button
                Button(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                IconButton(onClick = onNextClick) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                IconButton(onClick = { /*repeat*/ }) {
                    Icon(Icons.Default.Repeat, "Repeat", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Favorite and menu buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(Icons.Default.FavoriteBorder, "Favorite", tint = Color.White)
                }
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                }
            }
        }
    }
}
