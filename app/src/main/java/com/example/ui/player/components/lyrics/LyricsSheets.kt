package com.example.ui.player.components.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.button.OniPrimaryButton
import com.example.ui.components.button.OniSecondaryButton
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin
import java.util.Locale
import kotlin.math.abs

const val TRANSLATION_ORIGINAL = "Original"

/** Same options as the previous globe sheet; translation logic itself is unchanged. */
val lyricsTranslationLanguages = listOf(
    TRANSLATION_ORIGINAL,
    "English",
    "Romanized / Romaji",
    "Japanese (日本語)",
    "Spanish (Español)",
    "French (Français)",
    "German (Deutsch)",
    "Korean (한국어)",
    "Chinese (中文)"
)

@Composable
private fun sheetShape(): CornerBasedShape =
    OniSkin.shapes.bottomSheet.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

/**
 * Contextual Lyrics tools. Only applicable tools are listed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsToolsSheet(
    hasLyrics: Boolean,
    isSynced: Boolean,
    isSingAlongOn: Boolean,
    isVocalCutOn: Boolean,
    translationLabel: String?,
    onDismiss: () -> Unit,
    onSearchOnline: () -> Unit,
    onEditLyrics: () -> Unit,
    onOpenSyncEditor: () -> Unit,
    onAdjustTiming: () -> Unit,
    onToggleSingAlong: () -> Unit,
    onToggleVocalCut: () -> Unit,
    onTranslate: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surfaceElevated,
        contentColor = OniSkin.colors.textPrimary,
        shape = sheetShape()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = OniSkin.spacing.md)
        ) {
            SheetTitle("Lyrics tools")

            ToolRow(Icons.Default.Search, "Search online", "Find lyrics for this song", onSearchOnline)
            ToolRow(Icons.Default.Edit, if (hasLyrics) "Edit lyrics" else "Paste lyrics", null, onEditLyrics)

            if (hasLyrics) {
                SectionLabel("Sync")
                ToolRow(Icons.Default.Sync, "Sync editor", "Tap along to set line timings", onOpenSyncEditor)
                if (isSynced) {
                    ToolRow(Icons.Default.Timer, "Adjust timing", "Shift all lines earlier or later", onAdjustTiming)
                }
            }

            SectionLabel("Sing")
            ToggleToolRow(Icons.Default.Mic, "Sing along", "Mic with live pitch feedback", isSingAlongOn, onToggleSingAlong)
            ToggleToolRow(Icons.Default.HearingDisabled, "Vocal cut", "Lower the lead vocal", isVocalCutOn, onToggleVocalCut)

            if (hasLyrics) {
                SectionLabel("Language")
                ToolRow(
                    Icons.Default.Translate,
                    "Translate",
                    translationLabel?.let { "Showing $it" } ?: "Translate or romanize with AI",
                    onTranslate
                )
            }
        }
    }
}

/**
 * Timing adjustment. Uses the existing offset model: effective position = playback + offset,
 * applied permanently via shiftSongLyricsTiming. No scrim, so the lyrics stay visible
 * and the preview is live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsTimingSheet(
    offsetMs: Long,
    onNudge: (Long) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surfaceElevated,
        contentColor = OniSkin.colors.textPrimary,
        scrimColor = Color.Transparent,
        shape = sheetShape()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OniSkin.spacing.xl)
                .padding(bottom = OniSkin.spacing.md)
        ) {
            Text(
                text = "Adjust timing",
                style = OniSkin.typography.titleMedium,
                color = OniSkin.colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Positive values show lyrics earlier. The preview is live.",
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(OniSkin.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
            ) {
                NudgeStep("-0.5s", Modifier.weight(1f)) { onNudge(-500L) }
                NudgeStep("-0.1s", Modifier.weight(1f)) { onNudge(-100L) }
                Text(
                    text = formatOffset(offsetMs),
                    style = OniSkin.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OniSkin.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1.3f)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
                NudgeStep("+0.1s", Modifier.weight(1f)) { onNudge(100L) }
                NudgeStep("+0.5s", Modifier.weight(1f)) { onNudge(500L) }
            }
            Spacer(modifier = Modifier.height(OniSkin.spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm, Alignment.End)
            ) {
                OniSecondaryButton(text = "Reset", onClick = onReset, enabled = offsetMs != 0L)
                OniPrimaryButton(text = "Apply to lyrics", onClick = onApply, enabled = offsetMs != 0L)
            }
        }
    }
}

/**
 * AI translation / romanization picker (moved from the header globe; behaviour unchanged).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsTranslateSheet(
    selectedLanguage: String,
    canSaveTranslation: Boolean,
    onSelect: (String) -> Unit,
    onSaveTranslation: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OniSkin.colors.surfaceElevated,
        contentColor = OniSkin.colors.textPrimary,
        shape = sheetShape()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = OniSkin.spacing.md)
        ) {
            SheetTitle("Translate lyrics")
            Text(
                text = "AI translation or romanization. Timestamps are preserved.",
                style = OniSkin.typography.bodySmall,
                color = OniSkin.colors.textSecondary,
                modifier = Modifier.padding(horizontal = OniSkin.spacing.xl)
            )
            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
            Column(modifier = Modifier.selectableGroup()) {
                lyricsTranslationLanguages.forEach { lang ->
                    val isSelected = lang == selectedLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(lang) })
                            .padding(horizontal = OniSkin.spacing.xl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang,
                            style = OniSkin.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OniSkin.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            if (canSaveTranslation) {
                Spacer(modifier = Modifier.height(OniSkin.spacing.sm))
                OniSecondaryButton(
                    text = "Save translation as lyrics",
                    onClick = onSaveTranslation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OniSkin.spacing.xl)
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = OniSkin.typography.titleMedium,
        color = OniSkin.colors.textPrimary,
        modifier = Modifier
            .padding(horizontal = OniSkin.spacing.xl, vertical = OniSkin.spacing.xs)
            .semantics { heading() }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = OniSkin.typography.labelMedium,
        color = OniSkin.colors.textSecondary,
        modifier = Modifier
            .padding(
                start = OniSkin.spacing.xl,
                end = OniSkin.spacing.xl,
                top = OniSkin.spacing.md,
                bottom = OniSkin.spacing.xxs
            )
            .semantics { heading() }
    )
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = OniSkin.spacing.xl, vertical = OniSkin.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OniSkin.colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(OniSkin.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ToggleToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = OniSkin.spacing.xl, vertical = OniSkin.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) OniSkin.colors.primary else OniSkin.colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(OniSkin.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = OniSkin.typography.bodyLarge, color = OniSkin.colors.textPrimary)
            Text(text = subtitle, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NudgeStep(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OniSurface(
        modifier = modifier,
        variant = OniSurfaceVariant.Soft,
        shape = OniSkin.shapes.button,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = OniSkin.typography.labelLarge, color = OniSkin.colors.textPrimary)
        }
    }
}

internal fun formatOffset(ms: Long): String {
    val sign = when {
        ms > 0 -> "+"
        ms < 0 -> "-"
        else -> ""
    }
    return sign + String.format(Locale.US, "%.2fs", abs(ms) / 1000.0)
}
