package com.example.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.library.model.DashboardSectionPref
import com.example.ui.library.model.DefaultDashboardSections
import com.example.ui.theme.OniSkin

/** "Customize home": show, hide and reorder dashboard sections. */
@Composable
fun DashboardCustomizeDialog(
    current: List<DashboardSectionPref>,
    onSave: (List<DashboardSectionPref>) -> Unit,
    onDismiss: () -> Unit
) {
    val items = remember(current) { mutableStateListOf<DashboardSectionPref>().apply { addAll(current) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize home") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.xxs)) {
                Text(
                    text = "Show, hide and reorder sections.",
                    style = OniSkin.typography.caption,
                    color = OniSkin.colors.textSecondary
                )
                items.forEachIndexed { index, pref ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pref.visible,
                            onCheckedChange = { checked -> items[index] = pref.copy(visible = checked) }
                        )
                        Text(
                            text = pref.section.label,
                            style = OniSkin.typography.bodyMedium,
                            color = OniSkin.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val above = items[index - 1]
                                items[index - 1] = pref
                                items[index] = above
                            },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${pref.section.label} up")
                        }
                        IconButton(
                            onClick = {
                                val below = items[index + 1]
                                items[index + 1] = pref
                                items[index] = below
                            },
                            enabled = index < items.lastIndex
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${pref.section.label} down")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(items.toList()) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    items.clear()
                    items.addAll(DefaultDashboardSections)
                }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
