package com.example.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OniSkin
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        Icons.Default.MusicNote,
        "Welcome to oniPlayer",
        "A calm, beautiful home for the music you already own. No accounts, no ads, no noise."
    ),
    OnboardingPage(
        Icons.Default.Album,
        "Your library, your way",
        "Browse by songs, albums, artists, genres and folders. Pick a grid or a list once and it stays everywhere."
    ),
    OnboardingPage(
        Icons.AutoMirrored.Filled.QueueMusic,
        "Play, shuffle, find",
        "Every category has Play All, Shuffle and instant search right at your thumb."
    ),
    OnboardingPage(
        Icons.Default.LibraryMusic,
        "Let's find your music",
        "We'll scan your device for audio files. Everything stays on your phone."
    )
)

/**
 * Welcome slider shown once on first launch. The last page carries the primary CTA that
 * triggers a library scan. Host is responsible for permission handling inside [onScanLibrary]
 * and for persisting completion (LibraryPreferencesStore.setOnboardingDone).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onScanLibrary: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    reduceMotion: Boolean = false
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        OniSkin.colors.primary.copy(alpha = 0.20f),
                        OniSkin.colors.primaryContainer.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = OniSkin.spacing.md)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isLast) {
                    TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Skip", color = OniSkin.colors.textSecondary, style = OniSkin.typography.labelLarge)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { index ->
                val pageOffset = ((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction).absoluteValue
                PageContent(
                    page = pages[index],
                    parallax = if (reduceMotion) 0f else pageOffset,
                    reduceMotion = reduceMotion
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = OniSkin.spacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    val selected = pagerState.currentPage == i
                    val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (selected) OniSkin.colors.primary else OniSkin.colors.outline.copy(alpha = 0.4f))
                    )
                }
            }

            if (isLast) {
                Button(
                    onClick = onScanLibrary,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = OniSkin.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OniSkin.colors.primary,
                        contentColor = OniSkin.colors.onPrimary
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = OniSkin.colors.onPrimary
                        )
                        Spacer(Modifier.width(OniSkin.spacing.sm))
                        Text("Scanning...", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(OniSkin.spacing.sm))
                        Text("Scan my library", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = OniSkin.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OniSkin.colors.primary,
                        contentColor = OniSkin.colors.onPrimary
                    )
                ) {
                    Text("Next", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, parallax: Float, reduceMotion: Boolean) {
    val float by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floatY"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - (parallax * 0.6f).coerceIn(0f, 1f) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer { translationY = if (reduceMotion) 0f else float }
                .scale(1f - (parallax * 0.15f).coerceIn(0f, 0.3f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            OniSkin.colors.primary.copy(alpha = 0.30f),
                            OniSkin.colors.primaryContainer.copy(alpha = 0.55f)
                        )
                    )
                )
                .semantics { contentDescription = page.title },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = OniSkin.colors.primary,
                modifier = Modifier.size(84.dp)
            )
        }
        Spacer(Modifier.height(OniSkin.spacing.lg))
        Text(
            text = page.title,
            style = OniSkin.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = OniSkin.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(OniSkin.spacing.sm))
        Text(
            text = page.body,
            style = OniSkin.typography.bodyMedium,
            color = OniSkin.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = OniSkin.spacing.md)
        )
    }
}
