package com.example.ui.screens

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.SongEntity
import com.example.ui.components.button.OniCircularActionButton
import com.example.ui.components.button.OniIconButton
import com.example.ui.components.button.OniIconButtonStyle
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.components.button.OniSecondaryButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.lyrics.LyricsHelper
import com.example.ui.lyrics.LyricsLanguage
import com.example.ui.lyrics.LrcLine
import com.example.ui.lyrics.OnlineLyricsResult
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualSearchDialog(
    song: SongEntity,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(song.customTitle ?: song.title) }
    var artist by remember { mutableStateOf((song.customArtist ?: song.artist).let { if (it.equals("Unknown", true) || it.equals("Unknown Artist", true)) "" else it }) }
    var selectedSource by remember { mutableStateOf("All (Auto)") }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineLyricsResult>>(emptyList()) }
    var selectedResultIndex by remember { mutableStateOf<Int?>(null) }
    var editedLyrics by remember { mutableStateOf("") }

    // Two-Language Search selections
    var primaryLanguage by remember { mutableStateOf(LyricsLanguage.UNKNOWN) }
    var secondaryLanguage by remember { mutableStateOf(LyricsLanguage.UNKNOWN) }
    
    val sources = listOf("All (Auto)", "LRCLIB Database", "Lyrist API", "Lyrics.ovh")
    val selectableLanguages = listOf(
        LyricsLanguage.ENGLISH,
        LyricsLanguage.JAPANESE,
        LyricsLanguage.ROMAJI,
        LyricsLanguage.KOREAN,
        LyricsLanguage.CHINESE,
        LyricsLanguage.SPANISH,
        LyricsLanguage.FRENCH,
        LyricsLanguage.GERMAN,
        LyricsLanguage.PERSIAN,
        LyricsLanguage.ARABIC,
        LyricsLanguage.RUSSIAN
    )

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textFieldColors = OutlinedTextFieldDefaults.colors(
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
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = OniSkin.colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Online Lyrics Search", fontWeight = FontWeight.Bold, style = OniSkin.typography.titleMedium, color = OniSkin.colors.textPrimary)
                            Text("Query verified live databases to fetch real lyrics", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
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

                // Search Controls Box
                OniSurface(
                    variant = OniSurfaceVariant.Soft,
                    shape = OniSkin.shapes.card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(OniSkin.spacing.sm), verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title", style = OniSkin.typography.caption) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                textStyle = OniSkin.typography.bodySmall,
                                shape = OniSkin.shapes.button,
                                colors = textFieldColors
                            )
                            OutlinedTextField(
                                value = artist,
                                onValueChange = { artist = it },
                                label = { Text("Artist", style = OniSkin.typography.caption) },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true,
                                textStyle = OniSkin.typography.bodySmall,
                                shape = OniSkin.shapes.button,
                                colors = textFieldColors
                            )
                        }

                        // Two-Language Selection Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "DESIRED LYRIC LANGUAGES (MAX 2):",
                                    style = OniSkin.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OniSkin.colors.primary
                                )
                                if (primaryLanguage != LyricsLanguage.UNKNOWN || secondaryLanguage != LyricsLanguage.UNKNOWN) {
                                    TextButton(
                                        onClick = {
                                            primaryLanguage = LyricsLanguage.UNKNOWN
                                            secondaryLanguage = LyricsLanguage.UNKNOWN
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text("Clear Filters", style = OniSkin.typography.caption, color = OniSkin.colors.primary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(selectableLanguages) { lang ->
                                    val isPrimary = primaryLanguage == lang
                                    val isSecondary = secondaryLanguage == lang
                                    val isSelected = isPrimary || isSecondary

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            when {
                                                isPrimary -> {
                                                    primaryLanguage = secondaryLanguage
                                                    secondaryLanguage = LyricsLanguage.UNKNOWN
                                                }
                                                isSecondary -> {
                                                    secondaryLanguage = LyricsLanguage.UNKNOWN
                                                }
                                                primaryLanguage == LyricsLanguage.UNKNOWN -> {
                                                    primaryLanguage = lang
                                                }
                                                secondaryLanguage == LyricsLanguage.UNKNOWN -> {
                                                    secondaryLanguage = lang
                                                }
                                                else -> {
                                                    // Replace secondary
                                                    secondaryLanguage = lang
                                                }
                                            }
                                        },
                                        label = {
                                            val badge = when {
                                                isPrimary -> " (Lang 1)"
                                                isSecondary -> " (Lang 2)"
                                                else -> ""
                                            }
                                            Text(
                                                "${lang.labelWithFlag}$badge",
                                                style = OniSkin.typography.caption,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = OniSkin.shapes.chip,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = OniSkin.colors.surface,
                                            labelColor = OniSkin.colors.textPrimary,
                                            selectedContainerColor = OniSkin.colors.primaryContainer,
                                            selectedLabelColor = OniSkin.colors.onPrimaryContainer
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = OniSkin.colors.outline,
                                            selectedBorderColor = OniSkin.colors.primary
                                        )
                                    )
                                }
                            }
                        }

                        // Source Selection Row
                        Column {
                            Text("TARGET DATABASE:", style = OniSkin.typography.labelMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.primary)
                            Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs)
                            ) {
                                sources.forEach { src ->
                                    val isSelected = selectedSource == src
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSource = src },
                                        label = { Text(src, style = OniSkin.typography.labelMedium) },
                                        modifier = Modifier.heightIn(min = 44.dp),
                                        shape = OniSkin.shapes.chip,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = OniSkin.colors.surface,
                                            labelColor = OniSkin.colors.textPrimary,
                                            selectedContainerColor = OniSkin.colors.primaryContainer,
                                            selectedLabelColor = OniSkin.colors.onPrimaryContainer
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = OniSkin.colors.outline,
                                            selectedBorderColor = OniSkin.colors.primary
                                        )
                                    )
                                }
                            }
                        }

                        OniPrimaryButton(
                            text = if (isSearching) "Searching Real Databases..." else "Search Live Databases",
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Please enter a song title", Toast.LENGTH_SHORT).show()
                                    return@OniPrimaryButton
                                }
                                coroutineScope.launch {
                                    isSearching = true
                                    selectedResultIndex = null
                                    editedLyrics = ""
                                    hasSearched = false
                                    
                                    try {
                                        val requestedLangs = listOfNotNull(
                                            primaryLanguage.takeIf { it != LyricsLanguage.UNKNOWN },
                                            secondaryLanguage.takeIf { it != LyricsLanguage.UNKNOWN }
                                        )
                                        val fetchedResults = com.example.data.api.GeminiMusicService.searchRealLyricsOnline(
                                            title = title,
                                            artist = artist,
                                            sourceSelection = selectedSource,
                                            desiredLanguages = requestedLangs
                                        )
                                        results = fetchedResults
                                        isSearching = false
                                        hasSearched = true
                                        if (results.isNotEmpty()) {
                                            selectedResultIndex = 0
                                            editedLyrics = results[0].lyrics
                                        }
                                    } catch (e: Exception) {
                                        Log.w("ManualSearchDialog", "Search returned error: ${e.message}")
                                        results = emptyList()
                                        isSearching = false
                                        hasSearched = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSearching,
                            loading = isSearching,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                            CircularProgressIndicator(color = OniSkin.colors.primary)
                            Text("Connecting to live lyric databases...", style = OniSkin.typography.bodySmall, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary)
                            Text("Querying real lyrics from LRCLIB, Lyrist, and Lyrics.ovh", style = OniSkin.typography.caption, color = OniSkin.colors.textSecondary)
                        }
                    }
                } else if (hasSearched) {
                    if (results.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REAL VERIFIED RESULTS (${results.size}):",
                                style = OniSkin.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.primary
                            )
                            Text(
                                text = "Select to preview",
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

                        // Results selection cards showing real metadata
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(results) { idx, item ->
                                val isSelected = selectedResultIndex == idx
                                OniSurface(
                                    onClick = {
                                        selectedResultIndex = idx
                                        editedLyrics = item.lyrics
                                    },
                                    variant = OniSurfaceVariant.Soft,
                                    shape = OniSkin.shapes.card,
                                    containerColor = if (isSelected) OniSkin.colors.primaryContainer else null,
                                    border = if (isSelected) BorderStroke(1.5.dp, OniSkin.colors.primary) else BorderStroke(0.5.dp, OniSkin.colors.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .heightIn(min = 80.dp)
                                        .semantics(mergeDescendants = true) { selected = isSelected }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(OniSkin.spacing.xs),
                                        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.language.labelWithFlag,
                                                style = OniSkin.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) OniSkin.colors.onPrimaryContainer else OniSkin.colors.primary
                                            )
                                            Text(
                                                text = item.source.displayName,
                                                style = OniSkin.typography.caption,
                                                color = OniSkin.colors.textTertiary
                                            )
                                        }

                                        Text(
                                            text = item.title + (if (item.artist.isNotBlank()) " • ${item.artist}" else ""),
                                            style = OniSkin.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isSelected) OniSkin.colors.onPrimaryContainer else OniSkin.colors.textPrimary
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (item.isSynchronized) "⏱ Synchronized" else "📄 Plain Text",
                                                style = OniSkin.typography.caption,
                                                fontWeight = if (item.isSynchronized) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textSecondary
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "%.1f".format(item.score),
                                                    style = OniSkin.typography.caption,
                                                    color = if (isSelected) OniSkin.colors.onPrimaryContainer else OniSkin.colors.textSecondary
                                                )
                                            }
                                        }

                                        if (item.cleanSnippet.isNotBlank()) {
                                            Text(
                                                text = "\"${item.cleanSnippet}\"",
                                                style = OniSkin.typography.caption,
                                                color = OniSkin.colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))

                        // Preview Area Header
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PREVIEW & VERIFY LYRICS:",
                                style = OniSkin.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OniSkin.colors.primary
                            )
                            Text(
                                text = "Editable before saving",
                                style = OniSkin.typography.caption,
                                color = OniSkin.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))

                        // Lyrics preview box (Editable)
                        OutlinedTextField(
                            value = editedLyrics,
                            onValueChange = { editedLyrics = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = OniSkin.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            shape = OniSkin.shapes.button,
                            colors = textFieldColors
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
                                text = "Save Lyrics",
                                onClick = {
                                    if (editedLyrics.isNotBlank()) {
                                        viewModel.updateLyrics(song.id, editedLyrics.trim())
                                        Toast.makeText(context, "Real lyrics saved successfully!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Lyrics cannot be blank", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    } else {
                        // Empty State / No Results Found
                        val hasLanguageFilter = primaryLanguage != LyricsLanguage.UNKNOWN || secondaryLanguage != LyricsLanguage.UNKNOWN
                        val langNames = listOfNotNull(
                            primaryLanguage.takeIf { it != LyricsLanguage.UNKNOWN }?.displayName,
                            secondaryLanguage.takeIf { it != LyricsLanguage.UNKNOWN }?.displayName
                        ).joinToString(" & ")

                        OniSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            variant = OniSurfaceVariant.Soft,
                            shape = OniSkin.shapes.card,
                            border = BorderStroke(1.dp, OniSkin.colors.outline.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.padding(OniSkin.spacing.xl),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SentimentDissatisfied,
                                        contentDescription = "Not found",
                                        modifier = Modifier.size(48.dp),
                                        tint = OniSkin.colors.primary
                                    )
                                    Text(
                                        text = if (hasLanguageFilter) "No Lyrics Found in $langNames" else "No Real Lyrics Found Online",
                                        fontWeight = FontWeight.Bold,
                                        style = OniSkin.typography.titleSmall,
                                        color = OniSkin.colors.textPrimary
                                    )
                                    Text(
                                        text = if (hasLanguageFilter) {
                                            "Online databases have no real lyrics for \"$title\" in $langNames. You can clear language filters or try other languages."
                                        } else {
                                            "Searched LRCLIB, Lyrist, and Lyrics.ovh databases for \"$title\"" +
                                                (if (artist.isNotEmpty()) " by \"$artist\"" else "") +
                                                ", but no matching lyrics were found."
                                        },
                                        textAlign = TextAlign.Center,
                                        style = OniSkin.typography.bodySmall,
                                        color = OniSkin.colors.textSecondary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                                    
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs),
                                        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
                                    ) {
                                        if (hasLanguageFilter) {
                                            OniPrimaryButton(
                                                text = "Search All Languages",
                                                onClick = {
                                                    primaryLanguage = LyricsLanguage.UNKNOWN
                                                    secondaryLanguage = LyricsLanguage.UNKNOWN
                                                    coroutineScope.launch {
                                                        isSearching = true
                                                        val fetchedResults = com.example.data.api.GeminiMusicService.searchRealLyricsOnline(
                                                            title = title,
                                                            artist = artist,
                                                            sourceSelection = selectedSource,
                                                            desiredLanguages = emptyList()
                                                        )
                                                        results = fetchedResults
                                                        isSearching = false
                                                        if (results.isNotEmpty()) {
                                                            selectedResultIndex = 0
                                                            editedLyrics = results[0].lyrics
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        OniSecondaryButton(
                                            text = "Paste Manually",
                                            onClick = {
                                                results = listOf(
                                                    OnlineLyricsResult(
                                                        title = title,
                                                        artist = artist,
                                                        score = 5.0,
                                                        lyrics = "",
                                                        language = LyricsLanguage.UNKNOWN,
                                                        source = com.example.ui.lyrics.LyricsSource.MANUAL,
                                                        isSynchronized = false
                                                    )
                                                )
                                                selectedResultIndex = 0
                                                editedLyrics = ""
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = OniSkin.shapes.button,
                                colors = ButtonDefaults.textButtonColors(contentColor = OniSkin.colors.primary)
                            ) {
                                Text("Cancel", style = OniSkin.typography.labelLarge)
                            }
                        }
                    }
                } else {
                    // Empty State or Search Prompt
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(48.dp), tint = OniSkin.colors.textTertiary)
                            Spacer(modifier = Modifier.height(OniSkin.spacing.sm))
                            Text("Enter terms and click Search above", style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textSecondary)
                            Text("Live queries to LRCLIB, Lyrist, and Lyrics.ovh", style = OniSkin.typography.caption, color = OniSkin.colors.textTertiary)
                        }
                    }
                }
            }
        }
    }
}

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
