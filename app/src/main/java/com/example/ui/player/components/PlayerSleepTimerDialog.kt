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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin

/**
 * Clean Sleep Timer selection dialog for the Player screen in Default Skin.
 *
 * Replaces the legacy Aurora Glass timer dialog with Default Skin tokens.
 */
@Composable
fun PlayerSleepTimerDialog(
    isTimerRunning: Boolean,
    minutesLeft: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 15, 30, 45, 60)

    Dialog(onDismissRequest = onDismiss) {
        OniSurface(
            variant = OniSurfaceVariant.Elevated,
            shape = OniSkin.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("player_sleep_timer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = OniSkin.colors.primary,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sleep Timer",
                    style = OniSkin.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OniSkin.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isTimerRunning) "Playback stops in $minutesLeft min" else "Select duration to stop playback",
                    style = OniSkin.typography.bodySmall,
                    color = if (isTimerRunning) OniSkin.colors.primary else OniSkin.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Options grid/column
                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { mins ->
                            val isSelected = isTimerRunning && minutesLeft == mins
                            val variant = if (isSelected) OniSurfaceVariant.Elevated else OniSurfaceVariant.Soft

                            OniSurface(
                                variant = variant,
                                shape = OniSkin.shapes.medium,
                                onClick = {
                                    onSelectMinutes(mins)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = OniSkin.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OniSkin.colors.primary else OniSkin.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isTimerRunning) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            onSelectMinutes(0)
                            onDismiss()
                        },
                        shape = OniSkin.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OniSkin.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.TimerOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Turn Off Timer", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = OniSkin.colors.textSecondary)
                }
            }
        }
    }
}
