package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.surface.OniSurface
import com.example.ui.components.surface.OniSurfaceVariant
import com.example.ui.theme.OniSkin
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun EqualizerScreen(viewModel: MusicPlayerViewModel) {
    val presets by viewModel.allPresets.collectAsState()
    val activePresetName by viewModel.currentPresetName.collectAsState()
    val band60Hz by viewModel.eqBand60Hz.collectAsState()
    val band230Hz by viewModel.eqBand230Hz.collectAsState()
    val band910Hz by viewModel.eqBand910Hz.collectAsState()
    val band4kHz by viewModel.eqBand4kHz.collectAsState()
    val band14kHz by viewModel.eqBand14kHz.collectAsState()
    val bassBoost by viewModel.eqBassBoost.collectAsState()
    val virtualizer by viewModel.eqVirtualizer.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameToSave by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OniSkin.spacing.screenHorizontal, vertical = OniSkin.spacing.sm)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Acoustic Tuning", style = OniSkin.typography.displayMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary)
                Text("Studio master studio parametric custom filter", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
            }
            IconButton(onClick = { showSaveDialog = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Save, contentDescription = "Save Custom Preset", tint = OniSkin.colors.primary)
            }
        }

        EqualizerSectionLabel("PRESET SOUNDSTAGES")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.xs)) {
            items(presets) { preset ->
                val isActive = activePresetName == preset.name
                OniSurface(
                    variant = if (isActive) OniSurfaceVariant.Elevated else OniSurfaceVariant.Outlined,
                    shape = OniSkin.shapes.chip,
                    containerColor = if (isActive) OniSkin.colors.primary.copy(alpha = 0.14f) else null,
                    onClick = { viewModel.selectPreset(preset) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = OniSkin.spacing.md, vertical = OniSkin.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Text(preset.name, style = OniSkin.typography.labelLarge, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, color = if (isActive) OniSkin.colors.primary else OniSkin.colors.textPrimary)
                        if (preset.isCustom) {
                            Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                            Icon(Icons.Default.Close, contentDescription = "Delete custom preset", tint = OniSkin.colors.textSecondary, modifier = Modifier.size(24.dp).clickable {
                                viewModel.deletePreset(preset)
                                Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }
            }
        }

        EqualizerSectionLabel("FREQUENCY RESPONSE BANDS (dB)")
        OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.card) {
            Column(modifier = Modifier.padding(OniSkin.spacing.md)) {
                EqualizerSliderRow("60 Hz (Sub Bass)", band60Hz, OniSkin.colors.primary) { viewModel.updateBand(0, it) }
                EqualizerSliderRow("230 Hz (Punchy Bass)", band230Hz, OniSkin.colors.primary) { viewModel.updateBand(1, it) }
                EqualizerSliderRow("910 Hz (Acoustic Vocals)", band910Hz, OniSkin.colors.primary) { viewModel.updateBand(2, it) }
                EqualizerSliderRow("4 kHz (Detail / Presence)", band4kHz, OniSkin.colors.primary) { viewModel.updateBand(3, it) }
                EqualizerSliderRow("14 kHz (Air / Sparkle)", band14kHz, OniSkin.colors.primary) { viewModel.updateBand(4, it) }
            }
        }

        EqualizerSectionLabel("SPATIAL AUDIO EXPANSION")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 420.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                    EffectCard("Sub-Bass", Icons.Default.Hearing, bassBoost, OniSkin.colors.primary, "% boost", Modifier.fillMaxWidth()) { viewModel.updateBassBoost(it) }
                    EffectCard("Spatializer", Icons.Default.SurroundSound, virtualizer, OniSkin.colors.accentSecondary, "% width", Modifier.fillMaxWidth()) { viewModel.updateVirtualizer(it) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(OniSkin.spacing.sm)) {
                    EffectCard("Sub-Bass", Icons.Default.Hearing, bassBoost, OniSkin.colors.primary, "% boost", Modifier.weight(1f)) { viewModel.updateBassBoost(it) }
                    EffectCard("Spatializer", Icons.Default.SurroundSound, virtualizer, OniSkin.colors.accentSecondary, "% width", Modifier.weight(1f)) { viewModel.updateVirtualizer(it) }
                }
            }
        }
        Spacer(modifier = Modifier.height(OniSkin.spacing.xxl))
    }

    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            OniSurface(variant = OniSurfaceVariant.Elevated, shape = OniSkin.shapes.dialog, modifier = Modifier.fillMaxWidth().padding(OniSkin.spacing.md)) {
                Column(modifier = Modifier.padding(OniSkin.spacing.xl)) {
                    Text("Save Personal Preset", style = OniSkin.typography.titleMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary)
                    Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
                    Text("Save current frequency filter bands configuration to custom preset library.", style = OniSkin.typography.bodySmall, color = OniSkin.colors.textSecondary)
                    Spacer(modifier = Modifier.height(OniSkin.spacing.md))
                    OutlinedTextField(value = presetNameToSave, onValueChange = { presetNameToSave = it }, placeholder = { Text("My Preset Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = OniSkin.shapes.button, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OniSkin.colors.textPrimary, unfocusedTextColor = OniSkin.colors.textPrimary, focusedBorderColor = OniSkin.colors.primary, unfocusedBorderColor = OniSkin.colors.outline))
                    Spacer(modifier = Modifier.height(OniSkin.spacing.lg))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", style = OniSkin.typography.labelLarge) }
                        Spacer(modifier = Modifier.width(OniSkin.spacing.xs))
                        Button(onClick = { if (presetNameToSave.isNotBlank()) { viewModel.saveCustomPreset(presetNameToSave.trim()); Toast.makeText(context, "Preset saved!", Toast.LENGTH_SHORT).show(); showSaveDialog = false; presetNameToSave = "" } }, shape = OniSkin.shapes.button, colors = ButtonDefaults.buttonColors(containerColor = OniSkin.colors.primary, contentColor = OniSkin.colors.onPrimary)) { Text("Save", style = OniSkin.typography.labelLarge, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerSectionLabel(text: String) {
    Text(text, style = OniSkin.typography.labelMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.primary)
}

@Composable
private fun EffectCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, value: Float, tint: Color, suffix: String, modifier: Modifier, onValueChange: (Float) -> Unit) {
    OniSurface(variant = OniSurfaceVariant.Soft, shape = OniSkin.shapes.card, modifier = modifier) {
        Column(modifier = Modifier.padding(OniSkin.spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(OniSkin.spacing.xs))
            Text(title, style = OniSkin.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OniSkin.colors.textPrimary)
            Slider(value = value, onValueChange = onValueChange, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint, inactiveTrackColor = OniSkin.colors.outline.copy(alpha = 0.35f)))
            Text("${value.toInt()}$suffix", style = OniSkin.typography.caption, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
fun EqualizerSliderRow(label: String, value: Float, accentColor: Color, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = OniSkin.spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = OniSkin.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = OniSkin.colors.textPrimary)
            Text("${if (value >= 0) "+" else ""}${String.format("%.1f", value)} dB", style = OniSkin.typography.caption, fontWeight = FontWeight.Bold, color = accentColor)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = -15f..15f, colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = OniSkin.colors.outline.copy(alpha = 0.35f)))
    }
}
