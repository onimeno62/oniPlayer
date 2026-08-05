package com.example.ui.library.hero

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SongEntity
import com.example.ui.theme.LocalAccentColor

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun ContinueListeningHeroV2(
    song: SongEntity,
    isPlaying: Boolean,
    viewModel: com.example.ui.viewmodel.MusicPlayerViewModel,
    onPlayPauseClick: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()
    
    val heroColors = rememberHeroColors(
        artworkUri = song.albumArtUri,
        fallbackAccent = LocalAccentColor.current
    )
    val accentColor = heroColors.dominant

    val position by viewModel.audioEngine.position.collectAsStateWithLifecycle()
    val duration by viewModel.audioEngine.duration.collectAsStateWithLifecycle()

    val auraBleed = 10.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp + auraBleed * 2)
    ) {
        // Traveling Glow Perimeter Aura (Placed behind Card, sizing takes bleed into account)
        OniAura(
            color = accentColor,
            isPlaying = isPlaying,
            reduceMotion = reduceMotion,
            cornerRadius = 24.dp,
            bleed = auraBleed,
            modifier = Modifier.fillMaxSize()
        )

        // The Glass Hero Card itself, padded to leave room for the outer aura bleed
        Card(
            modifier = Modifier
                .padding(auraBleed)
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenNowPlaying),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background layers (includes the blurred art + organic Oni Flow animation)
                HeroBackground(
                    artworkUri = song.albumArtUri,
                    colors = heroColors,
                    reduceMotion = reduceMotion
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Main Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkPanel(
                            artworkUri = song.albumArtUri,
                            isPlaying = isPlaying,
                            reduceMotion = reduceMotion
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        MetadataPanel(
                            song = song,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        HeroPlayButton(
                            isPlaying = isPlaying,
                            tintColor = accentColor,
                            reduceMotion = reduceMotion,
                            onClick = onPlayPauseClick
                        )
                    }

                    // Progress Indicator
                    HeroProgressBar(
                        position = position,
                        duration = duration,
                        tintColor = accentColor,
                        onOpenNowPlaying = onOpenNowPlaying
                    )
                }
            }
        }
    }
}
