package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.music.OniArtwork
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.library.components.LibraryCategoryHero
import com.example.ui.library.components.LibraryEmptyState
import com.example.ui.library.model.FolderUiModel
import com.example.ui.theme.OniSkin

@Composable
fun FoldersScreen(
    folders: List<FolderUiModel>,
    layoutMode: String,
    onFolderClick: (FolderUiModel) -> Unit,
    modifier: Modifier = Modifier,
    gridIndex: Int = 0,
    gridOffset: Int = 0,
    onGridScroll: (Int, Int) -> Unit = { _, _ -> },
    listIndex: Int = 0,
    listOffset: Int = 0,
    onListScroll: (Int, Int) -> Unit = { _, _ -> }
) {
    if (folders.isEmpty()) {
        LibraryEmptyState(
            title = "No Folders",
            message = "No folders found in your library",
            icon = Icons.Default.Folder,
            modifier = modifier.fillMaxSize()
        )
    } else {
        val heroArtwork = folders.firstNotNullOfOrNull { it.artworkUri }
        val heroSubtitle = if (folders.size == 1) "1 folder" else "${folders.size} folders"

        if (layoutMode == "grid") {
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = gridIndex,
                initialFirstVisibleItemScrollOffset = gridOffset
            )
            LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
                onGridScroll(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = OniSkin.spacing.sm,
                    end = OniSkin.spacing.sm,
                    top = OniSkin.spacing.sm,
                    bottom = OniSkin.spacing.lg
                ),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryCategoryHero(
                        title = "Folders",
                        subtitle = heroSubtitle,
                        artworkUri = heroArtwork,
                        icon = Icons.Default.Folder,
                        modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
                    )
                }
                items(folders, key = { it.folderPath }) { folder ->
                    val songText = if (folder.songCount == 1) "1 song" else "${folder.songCount} songs"

                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.9f)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${folder.displayName}, $songText"
                            }
                            .clickable { onFolderClick(folder) },
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            OniArtwork(
                                artworkUri = folder.artworkUri,
                                shape = OniSkin.artwork.shape,
                                contentDescription = null,
                                placeholder = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = OniSkin.colors.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(OniSkin.spacing.sm)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                                    Text(
                                        text = folder.displayName,
                                        style = OniSkin.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                                Text(
                                    text = songText,
                                    style = OniSkin.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = listIndex,
                initialFirstVisibleItemScrollOffset = listOffset
            )
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                onListScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = OniSkin.spacing.screenHorizontal,
                    vertical = OniSkin.spacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
                modifier = modifier.fillMaxSize()
            ) {
                item(key = "folders_hero") {
                    LibraryCategoryHero(
                        title = "Folders",
                        subtitle = heroSubtitle,
                        artworkUri = heroArtwork,
                        icon = Icons.Default.Folder,
                        modifier = Modifier.padding(bottom = OniSkin.spacing.xs)
                    )
                }
                items(folders, key = { it.folderPath }) { folder ->
                    val songText = if (folder.songCount == 1) "1 song" else "${folder.songCount} songs"

                    OniSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 72.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${folder.displayName}, $songText"
                            }
                            .clickable { onFolderClick(folder) },
                        variant = OniSurfaceVariant.Soft,
                        shape = OniSkin.shapes.card
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OniArtwork(
                                artworkUri = folder.artworkUri,
                                size = 64.dp,
                                shape = OniSkin.artwork.shape,
                                contentDescription = null,
                                placeholder = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = OniSkin.colors.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(OniSkin.spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = OniSkin.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                                Text(
                                    text = songText,
                                    style = OniSkin.typography.bodySmall,
                                    color = OniSkin.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OniSkin.colors.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
