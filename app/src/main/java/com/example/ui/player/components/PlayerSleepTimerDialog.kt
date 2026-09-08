package com.example.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

@Composable
fun PlayerSleepTimerDialog(isTimerRunning: Boolean, minutesLeft: Int, onSelectMinutes: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(5, 15, 30, 45, 60)
    Dialog(onDismissRequest = onDismiss) {
        OniSurface(variant = OniSurfaceVariant.Elevated, shape = OniSkin.shapes.large, modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp).padding(OniSkin.spacing.sm).testTag("player_sleep_timer_dialog")) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OniSkin.spacing.lg, vertical = OniSkin.spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = OniSkin.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                Text("Sleep Timer", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary)
                Spacer(modifier = Modifier.height(OniSkin.spacing.xxs))
                Text(if (isTimerRunning) "Playback stops in $minutesLeft min" else "Select duration to stop playback", style = OniSkin.typography.bodySmall, color = if (isTimerRunning) OniSkin.colors.primary else OniSkin.colors.textSecondary)
                Spacer(modifier = Modifier.height(OniSkin.spacing.sm))

                options.chunked(2).forEach { rowOptions ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
                        rowOptions.forEach { mins ->
                            val isSelected = isTimerRunning && minutesLeft == mins
                            OniSurface(
                                variant = if (isSelected) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft,
                                shape = OniSkin.shapes.medium,
                                onClick = { onSelectMinutes(mins); onDismiss() },
                                modifier = Modifier.weight(1f).height(56.dp).semantics { selected = isSelected }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                                    Text("${mins}m", style = OniSkin.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textPrimary)
                                }
                            }
                        }
                        if (rowOptions.size == 1) Spacer(modifier = Modifier.weight(1f).height(56.dp))
                    }
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                }

                if (isTimerRunning) {
                    OutlinedButton(onClick = { onSelectMinutes(0); onDismiss() }, shape = OniSkin.shapes.medium, colors = ButtonDefaults.outlinedButtonColors(contentColor = OniSkin.colors.error), modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                        Icon(Icons.Default.TimerOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Text("Turn Off Timer", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).heightIn(min = 44.dp)) { Text("Close", style = OniSkin.typography.labelLarge, color = OniSkin.colors.textSecondary) }
            }
        }
    }
}
