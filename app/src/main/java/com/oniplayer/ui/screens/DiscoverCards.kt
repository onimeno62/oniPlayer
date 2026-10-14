package com.oniplayer.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.oniplayer.data.online.RemoteAlbum
import com.oniplayer.data.online.RemoteArtist
import com.oniplayer.data.online.RemoteMusicItem
import com.oniplayer.data.online.RemoteMusicType
import com.oniplayer.data.online.SearchFilter
import com.oniplayer.ui.components.surface.OniSurface
import com.oniplayer.ui.theme.OniSkin
import com.oniplayer.ui.viewmodel.DiscoverDetailState
import com.oniplayer.ui.viewmodel.DiscoverLoadState
import com.oniplayer.ui.viewmodel.DiscoverViewModel
import com.oniplayer.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

@Composable
internal fun OnlineTrackCard(
    item: RemoteMusicItem,
    canStream: Boolean,
    badgeText: String? = null,
    onPlay: () -> Unit,
    onArtistClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(260.dp)
            .clip(OniSkin.shapes.card)
            .clickable(enabled = canStream, onClick = onPlay),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Row(
            modifier = Modifier.padding(OniSkin.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(68.dp)) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = "${item.title} artwork",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(OniSkin.shapes.card),
                    contentScale = ContentScale.Crop
                )
                if (canStream) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(OniSkin.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = OniSkin.colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (badgeText != null) {
                    Text(
                        badgeText.uppercase(),
                        style = OniSkin.typography.caption,
                        color = OniSkin.colors.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    item.title,
                    style = OniSkin.typography.bodyLarge,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.artistName ?: "Unknown",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onArtistClick)
                )
                Text(
                    item.identity.providerId.uppercase(),
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = OniSkin.colors.textSecondary)
            }
        }
    }
}

@Composable
internal fun ReleaseCard(
    item: RemoteMusicItem,
    onClick: () -> Unit,
    onArtistClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(160.dp)
            .clip(OniSkin.shapes.card)
            .clickable(onClick = onClick),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Column(modifier = Modifier.padding(OniSkin.spacing.sm)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = "${item.title} artwork",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(OniSkin.shapes.card),
                    contentScale = ContentScale.Crop
                )
                if (onPlayClick != null) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(OniSkin.colors.primary)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play release",
                            tint = OniSkin.colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(OniSkin.spacing.xs))
            Text(
                item.title,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.artistName ?: "Unknown",
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onArtistClick)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val releaseDate = item.metadata["release_date"]?.take(4) ?: ""
                Text(
                    releaseDate.ifBlank { item.identity.providerId },
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary.copy(alpha = 0.7f)
                )
                IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = OniSkin.colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun ArtistDiscoveryCard(
    item: RemoteMusicItem,
    isFollowed: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    OniSurface(
        modifier = Modifier
            .width(130.dp)
            .clip(OniSkin.shapes.card)
            .clickable(onClick = onClick),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Column(
            modifier = Modifier.padding(OniSkin.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            AsyncImage(
                model = item.artworkUrl,
                contentDescription = "${item.title} photo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                item.artistName ?: item.title,
                style = OniSkin.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FilledTonalButton(
                onClick = onToggleFollow,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = OniSkin.shapes.full,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isFollowed) OniSkin.colors.surfaceVariant else OniSkin.colors.primary.copy(alpha = 0.15f),
                    contentColor = if (isFollowed) OniSkin.colors.textSecondary else OniSkin.colors.primary
                )
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isFollowed) "Following" else "Follow",
                    style = OniSkin.typography.caption,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun DiscoverTrackSection(
    title: String,
    subtitle: String,
    items: List<RemoteMusicItem>,
    hasPartialFailures: Boolean,
    canStream: (RemoteMusicItem) -> Boolean,
    onTrackClick: (RemoteMusicItem) -> Unit,
    onOpenArtist: (RemoteMusicItem) -> Unit,
    onShowMenu: (RemoteMusicItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
        SectionHeader(title = title, subtitle = subtitle)
        if (hasPartialFailures) {
            Text(
                "Some online providers are temporarily unreachable.",
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            items(items, key = { it.identity.providerId + ":" + it.identity.itemId }) { item ->
                OnlineTrackCard(
                    item = item,
                    canStream = canStream(item),
                    onPlay = { onTrackClick(item) },
                    onArtistClick = { onOpenArtist(item) },
                    onMenuClick = { onShowMenu(item) }
                )
            }
        }
    }
}

// =================================================================================================
// Remote Detail Views (Album & Artist)
// =================================================================================================
