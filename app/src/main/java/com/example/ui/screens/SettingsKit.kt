package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.PlayerSettings
import com.example.data.preferences.PlayerSettingsStore
import com.example.ui.theme.OniSkin
import java.util.Locale

/* Shared building blocks for every settings sub-screen. Existing components in
 * SettingsComponents.kt (SettingSection, SwitchSettingRow, SliderSettingRow...) stay as-is. */

@Composable
fun SettingsPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backButtonTestTag: String,
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SettingsSubscreenHeader(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
            backButtonTestTag = backButtonTestTag
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OniSkin.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.section),
            contentPadding = PaddingValues(top = OniSkin.spacing.xs, bottom = 96.dp),
            content = content
        )
    }
}

@Composable
fun SettingsIconTile(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    iconSize: Dp = 36.dp,
    trailingText: String? = null,
    showChevron: Boolean = true,
    destructive: Boolean = false,
    enabled: Boolean = true,
    singleLineDescription: Boolean = false,
    testTag: String? = null
) {
    val titleColor = when {
        !enabled -> OniSkin.colors.disabled
        destructive -> OniSkin.colors.error
        else -> OniSkin.colors.textPrimary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            SettingsIconTile(
                icon = icon,
                tint = iconTint ?: if (destructive) OniSkin.colors.error else OniSkin.colors.primary,
                size = iconSize
            )
            Spacer(Modifier.width(OniSkin.spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = OniSkin.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary,
                    maxLines = if (singleLineDescription) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingText != null) {
            Spacer(Modifier.width(OniSkin.spacing.sm))
            Text(
                text = trailingText,
                style = OniSkin.typography.labelMedium,
                color = OniSkin.colors.textSecondary,
                maxLines = 1
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(OniSkin.spacing.xs))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OniSkin.colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Segmented pill selector for small enumerations (speed presets, start screen...). */
@Composable
fun SettingsChoiceRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm)
    ) {
        Text(title, style = OniSkin.typography.bodyLarge, fontWeight = FontWeight.Medium, color = OniSkin.colors.textPrimary)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(description, style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
        }
        Spacer(Modifier.height(OniSkin.spacing.sm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clip(OniSkin.shapes.full)
                        .background(if (selected) OniSkin.colors.primary else OniSkin.colors.surfaceVariant)
                        .clickable(role = Role.RadioButton) { onSelect(index) }
                        .heightIn(min = 36.dp)
                        .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = OniSkin.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) OniSkin.colors.onPrimary else OniSkin.colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = OniSkin.typography.bodyMedium, color = OniSkin.colors.textPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(OniSkin.spacing.sm))
        Text(value, style = OniSkin.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = OniSkin.colors.primary)
    }
}

@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = OniSkin.typography.caption,
        color = OniSkin.colors.textTertiary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.sm)
    )
}

@Composable
fun rememberPlayerSettings(): State<PlayerSettings> {
    val context = LocalContext.current.applicationContext
    val flow = remember(context) { PlayerSettingsStore.settings(context) }
    return flow.collectAsStateWithLifecycle(initialValue = PlayerSettings())
}

internal fun formatPlaybackRate(value: Float): String {
    val text = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return "${text}x"
}

internal fun Context.launchFirstAvailable(vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: Exception) {
        }
    }
    Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show()
    return false
}

internal fun Context.settingsToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
