package com.example.ui.library.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.example.ui.theme.OniSkin

/**
 * Full-width header pinned to the top of every library category, list and detail screen.
 *
 * Layout:
 *  - top row: back icon (start), and the title when [collapsed]
 *  - middle (expanded only): category icon + name, and a static stats line under it
 *  - bottom row: Play All, Shuffle, Search, Library menu
 *
 * Background = blurred cover art + dark gradient, same treatment as the previous hero.
 * Render it OUTSIDE the scrolling list (Column: header, then Box(weight(1f)) { list }) so it stays
 * pinned. Drive [collapsed] from list scroll state (e.g. firstVisibleItemIndex > 0).
 *
 * Search is contextual: it filters the current category. Input is debounced 300ms.
 */
@Composable
fun LibraryStickyHeader(
    title: String,
    icon: ImageVector,
    statsText: String,
    artworkUri: String?,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    collapsed: Boolean = false,
    actionsEnabled: Boolean = true,
    onArtworkAction: (() -> Unit)? = null,
    playlistActions: Boolean = false,
    onNewPlaylist: (() -> Unit)? = null,
    onImportPlaylist: (() -> Unit)? = null
) {
    val height by animateDpAsState(if (collapsed) 160.dp else 244.dp, label = "headerHeight")
    var searchOpen by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }
    val onDark = Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Background: same cover art + gradient treatment as before, now edge to edge
        if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(24.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.42f
            )
        } else {
            Box(Modifier.fillMaxSize().background(OniSkin.colors.primaryContainer))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        OniSkin.colors.primary.copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.46f),
                        Color.Black.copy(alpha = 0.90f)
                    )
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = OniSkin.spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: back (+ title when collapsed)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Back",
                    onClick = onBack
                )
                if (collapsed) {
                    Spacer(Modifier.width(OniSkin.spacing.sm))
                    Text(
                        text = title,
                        style = OniSkin.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (onArtworkAction != null) {
                    Spacer(Modifier.width(OniSkin.spacing.xs))
                    HeaderIconButton(Icons.Default.CloudDownload, "Artwork", onArtworkAction)
                }
            }

            // Middle: icon + name + static data (expanded only)
            if (!collapsed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = onDark, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(OniSkin.spacing.md))
                    Column(Modifier.weight(1f)) {
                        if (overline != null) {
                            Text(
                                text = overline.uppercase(),
                                style = OniSkin.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = onDark.copy(alpha = 0.72f),
                                maxLines = 1
                            )
                        }
                        Text(
                            text = title,
                            style = OniSkin.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = onDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = statsText,
                            style = OniSkin.typography.bodyMedium,
                            color = onDark.copy(alpha = 0.84f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom actions are contextual to the current library destination.
            Row(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
            ) {
                if (playlistActions) {
                    Button(
                        onClick = { onNewPlaylist?.invoke() },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OniSkin.colors.primary,
                            contentColor = OniSkin.colors.onPrimary
                        )
                    ) {
                        Text("New Playlist", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onImportPlaylist?.invoke() },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button
                    ) {
                        Text("Import", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else if (searchOpen) {
                    HeaderSearchField(
                        initialQuery = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f)
                    )
                    HeaderIconButton(Icons.Default.Close, "Close search", {
                        searchOpen = false
                        onSearchQueryChange("")
                    })
                } else {
                    Button(
                        onClick = onPlayAll,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OniSkin.colors.primary,
                            contentColor = OniSkin.colors.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(OniSkin.spacing.xs))
                        Text("Play All", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    HeaderIconButton(Icons.Default.Shuffle, "Shuffle", onShuffle, enabled = actionsEnabled)
                    HeaderIconButton(Icons.Default.Search, "Search this list", { searchOpen = true })
                }
                HeaderIconButton(Icons.Default.SettingsSuggest, "Library menu", onOpenMenu)
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.08f), CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f))
    }
}

/** Inline search field. Keeps local text and pushes it out debounced (300ms, distinct). */
@Composable
private fun HeaderSearchField(
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf(initialQuery) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var lastSent by remember { mutableStateOf(initialQuery) }

    LaunchedEffect(focus) { focus.requestFocus() }
    LaunchedEffect(initialQuery) {
        if (text != initialQuery) {
            text = initialQuery
            lastSent = initialQuery
        }
    }
    LaunchedEffect(text) {
        delay(300)
        if (text != lastSent) {
            lastSent = text
            onQueryChange(text)
        }
    }

    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier.heightIn(min = 48.dp).focusRequester(focus),
        singleLine = true,
        placeholder = { Text("Search this list", color = Color.White.copy(alpha = 0.6f)) },
        shape = OniSkin.shapes.full,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.18f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.14f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/** Static stats line, e.g. "128 tracks  ·  8 h 12 min". [extra] is appended (e.g. "14 albums"). */
fun libraryStatsLine(trackCount: Int, totalDurationMs: Long, extra: String? = null): String {
    val totalMin = totalDurationMs / 60_000L
    val time = if (totalMin >= 60) "${totalMin / 60} h ${totalMin % 60} min" else "$totalMin min"
    val tracks = if (trackCount == 1) "1 track" else "%,d tracks".format(trackCount)
    return listOfNotNull(tracks, time, extra).joinToString("  ·  ")
}
