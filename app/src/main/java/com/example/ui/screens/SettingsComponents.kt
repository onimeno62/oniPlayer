package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Standard subscreen top header for settings detail pages.
 * Ensures consistent back button touch target (48dp), typography, and layout.
 */
@Composable
fun SettingsSubscreenHeader(
    title: String,
    onBack: () -> Unit,
    backButtonTestTag: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = OniSkin.spacing.screenHorizontal,
                vertical = OniSkin.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag(backButtonTestTag)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = OniSkin.colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OniSkin.typography.titleLarge,
                color = OniSkin.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                Text(
                    text = subtitle,
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Unified section wrapper for settings sub-screens.
 * Renders an optional section title, optional description, and an [OniSurface] card container.
 */
@Composable
fun SettingSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    cardModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = OniSkin.spacing.xxs)
        ) {
            Text(
                text = title,
                style = OniSkin.typography.titleSmall,
                color = OniSkin.colors.primary
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                Text(
                    text = description,
                    style = OniSkin.typography.bodySmall,
                    color = OniSkin.colors.textSecondary
                )
            }
        }
        OniSurface(
            variant = OniSurfaceVariant.Soft,
            shape = OniSkin.shapes.card,
            modifier = cardModifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Standard divider between setting rows within an [OniSurface] card.
 */
@Composable
fun SettingDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = OniSkin.colors.divider,
        thickness = 1.dp,
        modifier = modifier.padding(horizontal = OniSkin.spacing.md)
    )
}

/**
 * Accessible switch row conforming to OniSkin tokens.
 * The entire row acts as a 48dp+ interactive touch target with [Role.Switch].
 */
@Composable
fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .semantics(mergeDescendants = true) { }
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(
                horizontal = OniSkin.spacing.md,
                vertical = OniSkin.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = OniSkin.spacing.md)
        ) {
            Text(
                text = title,
                style = OniSkin.typography.bodyLarge,
                color = if (enabled) OniSkin.colors.textPrimary else OniSkin.colors.disabled
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                Text(
                    text = description,
                    style = OniSkin.typography.bodySmall,
                    color = if (enabled) OniSkin.colors.textSecondary else OniSkin.colors.disabled
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OniSkin.colors.onPrimary,
                checkedTrackColor = OniSkin.colors.primary,
                uncheckedThumbColor = OniSkin.colors.outline,
                uncheckedTrackColor = OniSkin.colors.surfaceVariant,
                disabledCheckedThumbColor = OniSkin.colors.disabled,
                disabledUncheckedThumbColor = OniSkin.colors.disabled
            ),
            modifier = Modifier.testTag("${testTag ?: "switch"}_control")
        )
    }
}

/**
 * Accessible slider row conforming to OniSkin tokens.
 */
@Composable
fun SliderSettingRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(
                horizontal = OniSkin.spacing.md,
                vertical = OniSkin.spacing.sm
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = OniSkin.typography.bodyLarge,
                color = OniSkin.colors.textPrimary
            )
            Text(
                text = valueFormatter(value),
                style = OniSkin.typography.labelMedium,
                color = OniSkin.colors.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = OniSkin.colors.primary,
                activeTrackColor = OniSkin.colors.primary,
                inactiveTrackColor = OniSkin.colors.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTag ?: "slider"}_control")
                .semantics {
                    contentDescription = "$title, currently ${valueFormatter(value)}"
                }
        )
    }
}

/**
 * Accent color palette picker row with 48dp touch targets and clear selection semantics.
 */
@Composable
fun AccentColorPickerRow(
    selectedHex: String,
    onColorSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val colors = listOf(
        "#3B73E3" to "Default Blue",
        "#7C4DFF" to "Purple",
        "#00E5FF" to "Cyan",
        "#00E676" to "Green",
        "#FFD600" to "Yellow",
        "#FF9800" to "Orange",
        "#FF1744" to "Red",
        "#E91E63" to "Pink"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(
                horizontal = OniSkin.spacing.md,
                vertical = OniSkin.spacing.sm
            )
    ) {
        Text(
            text = "Custom Accent Palette",
            style = OniSkin.typography.bodyLarge,
            color = OniSkin.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm),
            contentPadding = PaddingValues(vertical = OniSkin.spacing.xxs)
        ) {
            items(colors) { (hex, name) ->
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                val color = Color(android.graphics.Color.parseColor(hex))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("color_circle_$name")
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onColorSelect(hex) }
                        )
                        .semantics {
                            this.selected = isSelected
                            this.contentDescription = "$name accent color${if (isSelected) ", selected" else ""}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .border(2.dp, OniSkin.colors.primary, CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard Duration picker setting row for delay / timing preferences.
 */
@Composable
fun DurationPickerSettingRow(
    title: String,
    totalSeconds: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val currentMinutes = totalSeconds / 60
    val currentSeconds = totalSeconds % 60

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(
                horizontal = OniSkin.spacing.md,
                vertical = OniSkin.spacing.sm
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = OniSkin.typography.bodyLarge,
                color = OniSkin.colors.textPrimary
            )
            Text(
                text = if (totalSeconds == 0) "No Delay" else "${currentMinutes}m ${currentSeconds}s",
                style = OniSkin.typography.labelMedium,
                color = OniSkin.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                range = 0..10,
                selectedValue = currentMinutes,
                onValueChange = { mins -> onValueChange(mins * 60 + currentSeconds) },
                label = "min",
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = OniSkin.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OniSkin.colors.textSecondary,
                modifier = Modifier.padding(horizontal = OniSkin.spacing.xs)
            )

            WheelPicker(
                range = 0..59,
                selectedValue = currentSeconds,
                onValueChange = { secs -> onValueChange(currentMinutes * 60 + secs) },
                label = "sec",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

        val presets = listOf(
            0 to "Instant",
            5 to "5s",
            10 to "10s",
            30 to "30s",
            60 to "1m",
            120 to "2m"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs)
        ) {
            presets.forEach { (secs, label) ->
                val isSelected = totalSeconds == secs
                SuggestionChip(
                    onClick = { onValueChange(secs) },
                    label = {
                        Text(
                            text = label,
                            style = OniSkin.typography.caption,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isSelected) OniSkin.colors.primary.copy(alpha = 0.15f) else Color.Transparent,
                        labelColor = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textSecondary
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = if (isSelected) OniSkin.colors.primary else OniSkin.colors.outline.copy(alpha = 0.3f),
                        enabled = true
                    ),
                    shape = OniSkin.shapes.chip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Scrollable wheel picker for numbers conforming to OniSkin tokens.
 */
@Composable
fun WheelPicker(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val items = remember(range) { range.toList() }
    val itemHeight = 36.dp
    val visibleItemsCount = 3
    val middleIndex = visibleItemsCount / 2

    val initialIndex = remember(selectedValue, items) {
        val idx = items.indexOf(selectedValue)
        if (idx >= 0) idx else 0
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .filter { !listState.isScrollInProgress }
            .collect { index ->
                if (index in items.indices && items[index] != selectedValue) {
                    onValueChange(items[index])
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(itemHeight)
                .clip(OniSkin.shapes.small)
                .background(OniSkin.colors.primary.copy(alpha = 0.1f))
                .border(1.dp, OniSkin.colors.primary.copy(alpha = 0.25f), OniSkin.shapes.small)
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * middleIndex),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedValue
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable { onValueChange(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$item $label",
                        style = if (isSelected) OniSkin.typography.titleMedium else OniSkin.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
