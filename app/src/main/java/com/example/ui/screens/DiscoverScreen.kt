package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.entity.SongEntity
import com.example.data.online.RemoteMusicItem
import com.example.ui.components.surface.OniSurface
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.DiscoverLoadState
import com.example.ui.viewmodel.DiscoverViewModel

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OniSkin.spacing.screenHorizontal,
            end = OniSkin.spacing.screenHorizontal,
            top = OniSkin.spacing.md,
            bottom = OniSkin.spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.lg)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                Text(
                    text = "Discover",
                    style = OniSkin.typography.headlineMedium,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Find music beyond your local library.",
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary
                )
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                LocalSection("Continue Listening", recentlyPlayed)
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                LocalSection("Favorites", favorites)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search online music") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Online search")
                    },
                    shape = OniSkin.shapes.full,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OniSkin.colors.textPrimary,
                        unfocusedTextColor = OniSkin.colors.textPrimary,
                        focusedBorderColor = OniSkin.colors.primary,
                        unfocusedBorderColor = OniSkin.colors.outline.copy(alpha = 0.5f),
                        focusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = OniSkin.colors.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
                IconButton(onClick = viewModel::search) {
                    Icon(Icons.Default.Search, contentDescription = "Search online")
                }
            }
        }

        when (val search = state.search) {
            DiscoverLoadState.Idle -> Unit
            DiscoverLoadState.Loading -> item { LoadingSection() }
            is DiscoverLoadState.Success -> item {
                DiscoverSection("Search results", search.items)
            }
            is DiscoverLoadState.Error -> item { ErrorText(search.message) }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending",
                    style = OniSkin.typography.titleLarge,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::refreshTrending) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh trending")
                }
            }
        }

        when (val trending = state.trending) {
            DiscoverLoadState.Idle -> Unit
            DiscoverLoadState.Loading -> item { LoadingSection() }
            is DiscoverLoadState.Success -> item { DiscoverSection(null, trending.items) }
            is DiscoverLoadState.Error -> item { ErrorText(trending.message) }
        }
    }
}

@Composable
private fun LocalSection(title: String, songs: List<SongEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        Text(
            text = title,
            style = OniSkin.typography.titleLarge,
            color = OniSkin.colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            items(songs, key = { it.id }) { song ->
                OniSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(OniSkin.shapes.card),
                    containerColor = OniSkin.surfaces.soft.containerColor
                ) {
                    Column(
                        modifier = Modifier.padding(OniSkin.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = song.displayTitle,
                            style = OniSkin.typography.bodyLarge,
                            color = OniSkin.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.displayArtist,
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverSection(title: String?, items: List<RemoteMusicItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
        title?.let {
            Text(
                text = it,
                style = OniSkin.typography.titleLarge,
                color = OniSkin.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "No results found.",
                style = OniSkin.typography.bodyMedium,
                color = OniSkin.colors.textSecondary
            )
            return@Column
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
            items(items, key = { item ->
                item.identity.providerId + ":" + item.identity.itemId
            }) { item ->
                DiscoverItem(item)
            }
        }
    }
}

@Composable
private fun DiscoverItem(item: RemoteMusicItem) {
    OniSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OniSkin.shapes.card),
        containerColor = OniSkin.surfaces.soft.containerColor
    ) {
        Row(
            modifier = Modifier.padding(OniSkin.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.artworkUrl,
                contentDescription = item.title + " artwork",
                modifier = Modifier.size(64.dp).clip(OniSkin.shapes.card),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = OniSkin.typography.bodyLarge,
                    color = OniSkin.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artistName ?: item.identity.providerId,
                    style = OniSkin.typography.bodyMedium,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorText(message: String?) {
    Text(
        text = message ?: "Unable to load this section.",
        style = OniSkin.typography.bodyMedium,
        color = OniSkin.colors.textSecondary
    )
}
