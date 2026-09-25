package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.example.data.entity.SongEntity
import com.example.ui.components.button.OniCircularActionButton
import com.example.ui.components.button.OniIconButton
import com.example.ui.components.button.OniIconButtonStyle
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.components.button.OniSecondaryButton
import com.example.ui.components.state.OniLoadingState
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.lyrics.LyricsLanguage
import com.example.ui.lyrics.LyricsSearchUiState
import com.example.ui.lyrics.LyricsSource
import com.example.ui.lyrics.LrcLine
import com.example.ui.lyrics.OnlineLyricsResult
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.LyricsSearchViewModel
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Online Lyrics Search (Phase 9.4)
// ---------------------------------------------------------------------------------------------

/** Service source identifier for automatic selection (passed through to the service unchanged). */
private const val SOURCE_AUTO = "All (Auto)"

/** Service source identifiers mapped to user-facing labels. */
private val lyricsSources = listOf(
    SOURCE_AUTO to "Automatic",
    "LRCLIB Database" to "LRCLIB",
    "Lyrist API" to "Lyrist",
    "Lyrics.ovh" to "Lyrics.ovh"
)

private data class LyricsPreviewTarget(
    val result: OnlineLyricsResult,
    val editing: Boolean
)

/**
 * "Find the lyrics for this song."
 *
 * Query (title + artist) -> Search lyrics -> Results -> Preview -> Use these lyrics.
 * No language picker: language is detected per result and shown as metadata.
 * No AI translation options in this dialog.
 */
@Composable
fun ManualSearchDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val searchViewModel: LyricsSearchViewModel = composeViewModel(key = "lyrics_search_${song.id}")
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val motion = OniSkin.motion

    var title by rememberSaveable(song.id) { mutableStateOf(song.customTitle ?: song.title) }
    var artist by rememberSaveable(song.id) {
        mutableStateOf(
            (song.customArtist ?: song.artist).let {
                if (it.equals("Unknown", true) || it.equals("Unknown Artist", true)) "" else it
            }
        )
    }
    var selectedSource by rememberSaveable { mutableStateOf(SOURCE_AUTO) }
    var preview by remember { mutableStateOf<LyricsPreviewTarget?>(null) }
    var editedLyrics by remember { mutableStateOf("") }

    DisposableEffect(searchViewModel) {
        onDispose { searchViewModel.reset() }
    }

    val submitSearch: () -> Unit = {
        focusManager.clearFocus()
        if (title.isNotBlank()) searchViewModel.search(title, artist, selectedSource)
    }
    val openManualPaste: () -> Unit = {
        editedLyrics = ""
        preview = LyricsPreviewTarget(
            result = OnlineLyricsResult(
                title = title,
                artist = artist,
                score = 0.0,
                lyrics = "",
                language = LyricsLanguage.UNKNOWN,
                source = LyricsSource.MANUAL,
                isSynchronized = false
            ),
            editing = true
        )
    }
    val hasExistingLyrics = !song.lyrics.isNullOrBlank()
    val textFieldColors = lyricsTextFieldColors()

    Dialog(
        // Back from Preview returns to results instead of closing everything.
        onDismissRequest = { if (preview != null) preview = null else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        OniSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.md),
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.dialog
        ) {
            AnimatedContent(
                targetState = preview,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OniSkin.spacing.lg),
                transitionSpec = {
                    fadeIn(tween(motion.screenTransitionDurationMs)) togetherWith
                        fadeOut(tween(motion.quickDurationMs))
                },
                contentKey = { it == null },
                label = "lyrics_search_step"
            ) { target ->
                if (target == null) {
                    LyricsSearchStep(
                        title = title,
                        onTitleChange = { title = it },
                        artist = artist,
                        onArtistChange = { artist = it },
                        selectedSource = selectedSource,
                        onSourceChange = { selectedSource = it },
                        state = searchState,
                        textFieldColors = textFieldColors,
                        onSearch = submitSearch,
                        onSelectResult = { result -> preview = LyricsPreviewTarget(result, editing = false) },
                        onPasteManually = openManualPaste,
                        onClose = onDismiss
                    )
                } else {
                    LyricsPreviewStep(
                        target = target,
                        editedLyrics = editedLyrics,
                        onEditedLyricsChange = { editedLyrics = it },
                        hasExistingLyrics = hasExistingLyrics,
                        textFieldColors = textFieldColors,
                        onBack = { preview = null },
                        onStartEdit = {
                            editedLyrics = target.result.lyrics
                            preview = target.copy(editing = true)
                        },
                        onApply = {
                            val text = (if (target.editing) editedLyrics else target.result.lyrics).trim()
                            if (text.isNotEmpty()) {
                                viewModel.updateLyrics(song.id, text)
                                Toast.makeText(context, "Lyrics updated", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsSearchStep(
    title: String,
    onTitleChange: (String) -> Unit,
    artist: String,
    onArtistChange: (String) -> Unit,
    selectedSource: String,
    onSourceChange: (String) -> Unit,
    state: LyricsSearchUiState,
    textFieldColors: TextFieldColors,
    onSearch: () -> Unit,
    onSelectResult: (OnlineLyricsResult) -> Unit,
    onPasteManually: () -> Unit,
    onClose: () -> Unit
) {
    val isSearching = state is LyricsSearchUiState.Searching

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Online lyrics",
                style = OniSkin.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OniSkin.colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            OniIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onClose,
                style = OniIconButtonStyle.Ghost
            )
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Song title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = OniSkin.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            shape = OniSkin.shapes.button,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

        OutlinedTextField(
            value = artist,
            onValueChange = onArtistChange,
            label = { Text("Artist") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = OniSkin.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            shape = OniSkin.shapes.button,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OniPrimaryButton(
                text = "Search lyrics",
                onClick = onSearch,
                enabled = title.isNotBlank(),
                loading = isSearching,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            LyricsSourceMenu(selectedSource = selectedSource, onSelect = onSourceChange)
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = OniSkin.spacing.sm),
            color = OniSkin.colors.divider
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state) {
                LyricsSearchUiState.Idle -> Column {
                    Text(
                        text = "Results show their language and whether they're synced.",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                    PasteManuallyAction(onPasteManually)
                }

                LyricsSearchUiState.Searching -> OniLoadingState(
                    message = "Searching for lyrics…",
                    indicatorSize = 32.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                LyricsSearchUiState.Empty -> Column {
                    Text(
                        text = "No lyrics found",
                        style = OniSkin.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OniSkin.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                    Text(
                        text = "Try checking the title or artist.",
                        style = OniSkin.typography.bodyMedium,
                        color = OniSkin.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(OniSkin.spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                        OniPrimaryButton(
                            text = "Search again",
                            onClick = onSearch,
                            enabled = title.isNotBlank()
                        )
                        OniSecondaryButton(
                            text = "Paste manually",
                            onClick = onPasteManually
                        )
                    }
                }

                is LyricsSearchUiState.Results -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    item {
                        Text(
                            text = if (state.items.size == 1) "1 result" else "${state.items.size} results",
                            style = OniSkin.typography.labelMedium,
                            color = OniSkin.colors.textSecondary
                        )
                    }
                    items(state.items) { result ->
                        LyricsSearchResultRow(result = result, onClick = { onSelectResult(result) })
                    }
                    item { PasteManuallyAction(onPasteManually) }
                }
            }
        }
    }
}

@Composable
private fun LyricsSearchResultRow(
    result: OnlineLyricsResult,
    onClick: () -> Unit
) {
    OniSurface(
        onClick = onClick,
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.listItem,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.card, vertical = OniSkin.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs)
        ) {
            Text(
                text = result.title,
                style = OniSkin.typography.titleSmall.copy(textDirection = TextDirection.Content),
                color = OniSkin.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (result.artist.isNotBlank()) {
                Text(
                    text = result.artist,
                    style = OniSkin.typography.bodySmall.copy(textDirection = TextDirection.Content),
                    color = OniSkin.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LyricsResultMeta(result)
            if (result.cleanSnippet.isNotBlank()) {
                Text(
                    text = result.cleanSnippet,
                    style = OniSkin.typography.bodySmall.copy(textDirection = TextDirection.Content),
                    color = OniSkin.colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** "Synced · English" + source. Icon + text, so status never depends on color alone. */
@Composable
private fun LyricsResultMeta(result: OnlineLyricsResult) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (result.isSynchronized) Icons.Default.Sync else Icons.AutoMirrored.Filled.Subject,
                contentDescription = null,
                tint = OniSkin.colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xxs))
            Text(
                text = "${if (result.isSynchronized) "Synced" else "Plain"} · ${languageLabel(result.language)}",
                style = OniSkin.typography.labelMedium,
                color = OniSkin.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = sourceLabel(result.source),
            style = OniSkin.typography.caption,
            color = OniSkin.colors.textTertiary
        )
    }
}

@Composable
private fun LyricsPreviewStep(
    target: LyricsPreviewTarget,
    editedLyrics: String,
    onEditedLyricsChange: (String) -> Unit,
    hasExistingLyrics: Boolean,
    textFieldColors: TextFieldColors,
    onBack: () -> Unit,
    onStartEdit: () -> Unit,
    onApply: () -> Unit
) {
    val result = target.result
    val isManual = result.source == LyricsSource.MANUAL
    val applyText = if (target.editing) editedLyrics else result.lyrics

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OniIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isManual) "Back to search" else "Back to results",
                onClick = onBack,
                style = OniIconButtonStyle.Ghost
            )
            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isManual) "Paste lyrics" else result.title,
                    style = OniSkin.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    fontWeight = FontWeight.SemiBold,
                    color = OniSkin.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                val subtitle = if (isManual) listOf(result.title, result.artist).filter { it.isNotBlank() }.joinToString(" · ") else result.artist
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = OniSkin.typography.bodySmall.copy(textDirection = TextDirection.Content),
                        color = OniSkin.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!isManual) {
            Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
            LyricsResultMeta(result)
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (target.editing) {
                OutlinedTextField(
                    value = editedLyrics,
                    onValueChange = onEditedLyricsChange,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = {
                        Text(
                            "Paste plain lyrics, or LRC with [mm:ss.xx] timestamps",
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textSecondary
                        )
                    },
                    textStyle = OniSkin.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                    shape = OniSkin.shapes.button,
                    colors = textFieldColors
                )
            } else {
                val lines = remember(result.lyrics) {
                    LyricsHelper.stripLrcTags(result.lyrics).lines().map { it.trim() }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(lines) { line ->
                        if (line.isBlank()) {
                            Spacer(modifier = Modifier.height(OniSkin.spacing.sm))
                        } else {
                            Text(
                                text = line,
                                style = OniSkin.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                                color = OniSkin.colors.textPrimary,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = OniSkin.spacing.xxs)
                            )
                        }
                    }
                }
            }
        }

        if (hasExistingLyrics) {
            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
            Text(
                text = "This replaces the current lyrics for this song.",
                style = OniSkin.typography.caption,
                color = OniSkin.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End)
        ) {
            if (!target.editing) {
                OniSecondaryButton(text = "Edit first", onClick = onStartEdit)
            }
            OniPrimaryButton(
                text = "Use these lyrics",
                onClick = onApply,
                enabled = applyText.isNotBlank()
            )
        }
    }
}

@Composable
private fun LyricsSourceMenu(
    selectedSource: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = lyricsSources.firstOrNull { it.first == selectedSource }?.second ?: "Automatic"

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.heightIn(min = 48.dp),
            shape = OniSkin.shapes.button,
            colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.textSecondary)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
            Text("Source: $selectedLabel", style = OniSkin.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            lyricsSources.forEach { (id, label) ->
                val isSelected = id == selectedSource
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = OniSkin.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = OniSkin.colors.primary)
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    },
                    modifier = Modifier.semantics { this.selected = isSelected }
                )
            }
        }
    }
}

@Composable
private fun PasteManuallyAction(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = OniSkin.shapes.button,
        colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
        Text("Paste manually", style = OniSkin.typography.labelLarge)
    }
}

@Composable
private fun lyricsTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OniSkin.colors.textPrimary,
    unfocusedTextColor = OniSkin.colors.textPrimary,
    focusedContainerColor = OniSkin.colors.surfaceVariant,
    unfocusedContainerColor = OniSkin.colors.surfaceVariant,
    focusedBorderColor = OniSkin.colors.primary,
    unfocusedBorderColor = OniSkin.colors.outline,
    focusedLabelColor = OniSkin.colors.primary,
    unfocusedLabelColor = OniSkin.colors.textSecondary,
    cursorColor = OniSkin.colors.primary,
    selectionColors = TextSelectionColors(OniSkin.colors.primary, OniSkin.colors.primaryContainer)
)

private fun languageLabel(language: LyricsLanguage): String = when (language) {
    LyricsLanguage.UNKNOWN -> "Unknown language"
    LyricsLanguage.ROMAJI -> "Romaji"
    else -> language.displayName
}

private fun sourceLabel(source: LyricsSource): String = when (source) {
    LyricsSource.LRCLIB -> "LRCLIB"
    LyricsSource.LYRIST -> "Lyrist"
    LyricsSource.LYRICS_OVH -> "Lyrics.ovh"
    LyricsSource.LOCAL -> "Local file"
    LyricsSource.MANUAL -> "Manual entry"
}

// ---------------------------------------------------------------------------------------------
// Lyrics Editor (unchanged)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsEditorDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(song.lyrics ?: "") }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        OniSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.md),
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.dialog
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OniSkin.spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lyrics Editor", fontWeight = FontWeight.Bold, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                            Text(song.customTitle ?: song.title, style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        }
                    }
                    OniIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        style = OniIconButtonStyle.Ghost
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = OniSkin.spacing.sm), color = OniSkin.colors.divider)

                // Guide
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    containerColor = OniSkin.colors.primaryContainer,
                    contentColor = OniSkin.colors.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(OniSkin.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Text(
                            "You can paste plain text or synchronized LRC text with [mm:ss.xx] timestamp tags.",
                            style = OniSkin.typography.caption,
                            color = OniSkin.colors.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

                // Text Area
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type or paste lyrics here...", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = OniSkin.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = OniSkin.shapes.button,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OniSkin.colors.textPrimary,
                        unfocusedTextColor = OniSkin.colors.textPrimary,
                        focusedContainerColor = OniSkin.colors.surfaceVariant,
                        unfocusedContainerColor = OniSkin.colors.surfaceVariant,
                        focusedBorderColor = OniSkin.colors.primary,
                        unfocusedBorderColor = OniSkin.colors.outline,
                        cursorColor = OniSkin.colors.primary,
                        selectionColors = TextSelectionColors(OniSkin.colors.primary, OniSkin.colors.primaryContainer)
                    )
                )

                Spacer(modifier = Modifier.height(OniSkin.spacing.md))

                // Actions
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
                    ) {
                        Text("Cancel", style = OniSkin.typography.labelLarge)
                    }
                    OniPrimaryButton(
                        text = "Save Changes",
                        onClick = {
                            viewModel.updateLyrics(song.id, text.trim().ifEmpty { null })
                            Toast.makeText(context, "Lyrics saved successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Sync Editor (unchanged)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyncEditorDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Playback state bindings
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val position by viewModel.audioEngine.position.collectAsState()
    val duration by viewModel.audioEngine.duration.collectAsState()

    val rawLyrics = song.lyrics ?: ""

    val initialLines = remember(rawLyrics) {
        val parsed = LyricsHelper.parseLrc(rawLyrics)
        if (parsed.isNotEmpty()) {
            parsed.map { it.text to it.timestampMs }.toMutableStateList()
        } else {
            rawLyrics.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it to -1L }
                .toMutableStateList()
        }
    }

    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        OniSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(OniSkin.spacing.md),
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.dialog
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OniSkin.spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interactive Sync Editor", fontWeight = FontWeight.Bold, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                            Text("Tap the sync icon on each line when it is sung!", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        }
                    }
                    OniIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        style = OniIconButtonStyle.Ghost
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = OniSkin.spacing.xs), color = OniSkin.colors.divider)

                // Progress Info Card
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(OniSkin.spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Position: ${LyricsHelper.formatLrcTime(position).replace("[", "").replace("]", "")}",
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Monospace,
                                style = OniSkin.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.primary
                            )

                            OniCircularActionButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                onClick = { viewModel.togglePlayPause() },
                                size = 48.dp,
                                iconSize = 20.dp,
                                elevation = OniSkin.elevation.flat
                            )
                        }

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                        ) {
                            OniSecondaryButton(
                                text = "Delay -0.5s",
                                onClick = {
                                    for (i in initialLines.indices) {
                                        val (text, time) = initialLines[i]
                                        if (time >= 0) {
                                            initialLines[i] = text to maxOf(0L, time - 500L)
                                        }
                                    }
                                    Toast.makeText(context, "Delayed all lines by -0.5s", Toast.LENGTH_SHORT).show()
                                }
                            )

                            OniSecondaryButton(
                                text = "Advance +0.5s",
                                onClick = {
                                    for (i in initialLines.indices) {
                                        val (text, time) = initialLines[i]
                                        if (time >= 0) {
                                            initialLines[i] = text to time + 500L
                                        }
                                    }
                                    Toast.makeText(context, "Advanced all lines by +0.5s", Toast.LENGTH_SHORT).show()
                                }
                            )

                            Button(
                                onClick = {
                                    for (i in initialLines.indices) {
                                        initialLines[i] = initialLines[i].first to -1L
                                    }
                                    Toast.makeText(context, "Cleared all timing tags", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = OniSkin.spacing.buttonHorizontal, vertical = OniSkin.spacing.buttonVertical),
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = OniSkin.shapes.button,
                                colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.error, contentColor = OniSkin.colors.onError)
                            ) {
                                Text("Reset All", style = OniSkin.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

                // List of Lyrics to Sync
                OniSurface(
                    modifier = Modifier.weight(1f),
                    variant = OniSurfaceVariant.Outlined,
                    shape = OniSkin.shapes.card
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(OniSkin.spacing.xxs)
                    ) {
                        itemsIndexed(initialLines) { index, linePair ->
                            val (lineText, timestamp) = linePair
                            val isSynced = timestamp >= 0L

                            OniSurface(
                                modifier = Modifier.fillMaxWidth(),
                                variant = OniSurfaceVariant.Flat,
                                shape = OniSkin.shapes.listItem,
                                containerColor = if (isSynced) OniSkin.colors.primaryContainer else OniSkin.colors.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = OniSkin.spacing.sm, vertical = OniSkin.spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OniIconButton(
                                        icon = if (isSynced) Icons.Default.CheckCircle else Icons.Default.Timer,
                                        contentDescription = "Sync line",
                                        onClick = {
                                            initialLines[index] = lineText to position
                                            if (index + 1 < initialLines.size) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(maxOf(0, index - 1))
                                                }
                                            }
                                        },
                                        selected = isSynced,
                                        style = OniIconButtonStyle.Neutral,
                                        iconSize = 18.dp
                                    )

                                    Spacer(modifier = Modifier.width(OniSkin.spacing.sm))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lineText,
                                            style = OniSkin.typography.bodyMedium,
                                            color = if (isSynced) OniSkin.colors.onPrimaryContainer else OniSkin.colors.textSecondary,
                                            fontWeight = if (isSynced) FontWeight.Medium else FontWeight.Normal
                                        )

                                        if (isSynced) {
                                            Text(
                                                text = LyricsHelper.formatLrcTime(timestamp).replace("[", "").replace("]", ""),
                                                style = OniSkin.typography.caption,
                                                fontFamily = FontFamily.Monospace,
                                                color = OniSkin.colors.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (isSynced) {
                                        OniIconButton(
                                            icon = Icons.Default.Clear,
                                            contentDescription = "Clear timestamp",
                                            onClick = { initialLines[index] = lineText to -1L },
                                            style = OniIconButtonStyle.Ghost,
                                            iconSize = 16.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.md))

                // Actions
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = OniSkin.shapes.button,
                        colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
                    ) {
                        Text("Cancel", style = OniSkin.typography.labelLarge)
                    }
                    OniPrimaryButton(
                        text = "Save & Apply",
                        onClick = {
                            val syncedLines = initialLines.filter { it.second >= 0L }.map { LrcLine(it.second, it.first) }
                            if (syncedLines.isEmpty()) {
                                val plainText = initialLines.joinToString("\n") { it.first }
                                viewModel.updateLyrics(song.id, plainText)
                                Toast.makeText(context, "Saved as plain lyrics", Toast.LENGTH_SHORT).show()
                            } else {
                                val lrcString = LyricsHelper.buildLrcString(syncedLines)
                                viewModel.updateLyrics(song.id, lrcString)
                                Toast.makeText(context, "Successfully saved synchronized LRC lyrics!", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}
