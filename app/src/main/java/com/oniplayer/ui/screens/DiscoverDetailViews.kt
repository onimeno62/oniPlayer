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
internal fun RemoteAlbumDetailView(
    detail: DiscoverDetailState.RemoteAlbumDetail,
    followedArtistIds: Set<String>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onTrackPlay: (RemoteMusicItem) -> Unit,
    onOpenArtist: (String) -> Unit,
    onToggleFollow: (String, String, String?) -> Unit,
    onShowMenu: (RemoteMusicItem) -> Unit
) {
    val album = detail.album
    val artistName = album.artistName ?: "Unknown Artist"
    val artistId = album.musicIdentity.canonicalArtistId ?: album.identity.itemId
    val isArtistFollowed = followedArtistIds.contains(artistId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Release Details", style = OniSkin.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = album.artworkUrl,
                        contentDescription = "${album.title} artwork",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(OniSkin.shapes.card),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            album.title,
                            style = OniSkin.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary
                        )
                        Text(
                            artistName,
                            style = OniSkin.typography.bodyLarge,
                            color = OniSkin.colors.primary,
                            modifier = Modifier.clickable { onOpenArtist(artistName) }
                        )
                        album.releaseDate?.let { date ->
                            Text("Released $date", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                        }
                        Text("Source: ${album.identity.providerId.uppercase()}", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)
                ) {
                    Button(
                        onClick = onPlayAll,
                        modifier = Modifier.weight(1f),
                        shape = OniSkin.shapes.full
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play All")
                    }
                    FilledTonalButton(
                        onClick = { onToggleFollow(artistId, artistName, album.artworkUrl) },
                        shape = OniSkin.shapes.full
                    ) {
                        Icon(
                            if (isArtistFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isArtistFollowed) "Following" else "Follow Artist")
                    }
                }
            }

            if (detail.isLoading) {
                item { LoadingSection("Loading album tracklist...") }
            } else if (album.tracks.isEmpty()) {
                item {
                    Text(
                        "No individual streaming tracks cataloged for this release.",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary
                    )
                }
            } else {
                item {
                    Text("Tracks (${album.tracks.size})", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                itemsIndexed(album.tracks, key = { _, track -> track.identity.itemId }) { index, track ->
                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OniSkin.shapes.card)
                            .clickable { onTrackPlay(track) },
                        containerColor = OniSkin.surfaces.soft.containerColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
                        ) {
                            Text(
                                "${index + 1}",
                                style = OniSkin.typography.bodyMedium,
                                color = OniSkin.colors.textSecondary,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = OniSkin.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OniSkin.colors.textPrimary)
                                Text(track.artistName ?: artistName, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                            }
                            IconButton(onClick = { onShowMenu(track) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RemoteArtistDetailView(
    detail: DiscoverDetailState.RemoteArtistDetail,
    isFollowed: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenAlbum: (RemoteAlbum) -> Unit,
    onTrackPlay: (RemoteMusicItem) -> Unit,
    onShowMenu: (RemoteMusicItem) -> Unit
) {
    val artist = detail.artist

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Artist Details", style = OniSkin.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = artist.artworkUrl,
                        contentDescription = "${artist.name} photo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            artist.name,
                            style = OniSkin.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OniSkin.colors.textPrimary
                        )
                        Text("Source: ${artist.identity.providerId.uppercase()}", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        FilledTonalButton(
                            onClick = onToggleFollow,
                            shape = OniSkin.shapes.full
                        ) {
                            Icon(
                                if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFollowed) "Following" else "Follow Artist")
                        }
                    }
                }
            }

            if (detail.isLoading) {
                item { LoadingSection("Loading artist catalog...") }
            }

            if (detail.releases.isNotEmpty()) {
                item {
                    Text("Releases (${detail.releases.size})", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        items(detail.releases, key = { it.identity.itemId }) { album ->
                            ReleaseCard(
                                item = RemoteMusicItem(
                                    identity = album.identity,
                                    title = album.title,
                                    artistName = album.artistName ?: artist.name,
                                    artworkUrl = album.artworkUrl ?: artist.artworkUrl,
                                    musicIdentity = album.musicIdentity,
                                    metadata = mapOf("release_date" to (album.releaseDate ?: ""))
                                ),
                                onClick = { onOpenAlbum(album) },
                                onArtistClick = {},
                                onMenuClick = {
                                    onShowMenu(
                                        RemoteMusicItem(
                                            identity = album.identity,
                                            title = album.title,
                                            artistName = album.artistName ?: artist.name,
                                            artworkUrl = album.artworkUrl ?: artist.artworkUrl
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (detail.tracks.isNotEmpty()) {
                item {
                    Text("Popular Tracks", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(detail.tracks, key = { it.identity.itemId }) { track ->
                    OnlineTrackCard(
                        item = track,
                        canStream = true,
                        onPlay = { onTrackPlay(track) },
                        onArtistClick = {},
                        onMenuClick = { onShowMenu(track) }
                    )
                }
            }
        }
    }
}

// =================================================================================================
// Track Actions Modal
// =================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackActionsModal(
    track: RemoteMusicItem,
    isFavorite: Boolean,
    isArtistFollowed: Boolean,
    canStream: Boolean,
    canDownload: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFollowArtist: () -> Unit,
    onOpenArtist: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(OniSkin.shapes.card),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = OniSkin.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artistName ?: "Unknown", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary, maxLines = 1)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = OniSkin.spacing.xs))

            if (canStream) {
                ActionMenuItem(icon = Icons.Default.PlayArrow, title = "Play now", onClick = onPlay)
                ActionMenuItem(icon = Icons.Default.SkipNext, title = "Play next", onClick = onPlayNext)
                ActionMenuItem(icon = Icons.AutoMirrored.Filled.QueueMusic, title = "Add to queue", onClick = onAddToQueue)
            }

            ActionMenuItem(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = onToggleFavorite
            )

            ActionMenuItem(
                icon = if (isArtistFollowed) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                title = if (isArtistFollowed) "Unfollow ${track.artistName ?: "artist"}" else "Follow ${track.artistName ?: "artist"}",
                onClick = onToggleFollowArtist
            )

            ActionMenuItem(
                icon = Icons.Default.Person,
                title = "View artist details",
                onClick = onOpenArtist
            )

            if (canDownload) {
                ActionMenuItem(
                    icon = Icons.Default.Download,
                    title = "Download track",
                    onClick = { /* handled if download URL present */ }
                )
            }
        }
    }
}

@Composable
internal fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = OniSkin.spacing.lg, vertical = OniSkin.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = OniSkin.colors.textPrimary)
        Text(title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary)
    }
}
