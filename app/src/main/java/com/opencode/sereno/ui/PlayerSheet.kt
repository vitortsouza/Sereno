package com.opencode.sereno.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opencode.sereno.audio.SoundCatalog
import com.opencode.sereno.playback.LayerUi
import com.opencode.sereno.playback.Player
import com.opencode.sereno.playback.PlayerState
import com.opencode.sereno.ui.screens.SaveMixDialog
import com.opencode.sereno.ui.theme.SerenoColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerSheet(
    state: PlayerState,
    layersEnabled: Boolean,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSetTimer: (Long) -> Unit,
    onLayerGain: (Long, Float) -> Unit,
    onRemoveLayer: (Long) -> Unit,
    onSave: (String) -> Unit,
    onAddLayer: () -> Unit
) {
    var showCustomTimer by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val hasBinaural = state.layers.any {
        SoundCatalog.find(it.soundId)?.headphone == true
    }
    val headphones = remember(state.layers) { Player.hasHeadphones() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SerenoColors.Surface,
        contentColor = SerenoColors.Text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SerenoColors.Outline) },
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (layersEnabled) "Sua mistura" else "Seu som",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        timerStatus(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = SerenoColors.Muted)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "DURAÇÃO",
                style = MaterialTheme.typography.labelSmall,
                color = SerenoColors.Muted
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    0L to "Livre",
                    5 * 60_000L to "5 min",
                    15 * 60_000L to "15 min",
                    30 * 60_000L to "30 min"
                )
                presets.forEach { (ms, label) ->
                    CategoryChip(
                        label = label,
                        selected = state.totalMs == ms,
                        accent = SerenoColors.Mint
                    ) { onSetTimer(ms) }
                }
                CategoryChip(
                    label = if (isCustom(state.totalMs)) timerLabel(state.totalMs) else "Personalizado",
                    selected = isCustom(state.totalMs),
                    accent = SerenoColors.Mint
                ) { showCustomTimer = true }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                if (layersEnabled) "CAMADAS" else "SOM",
                style = MaterialTheme.typography.labelSmall,
                color = SerenoColors.Muted
            )
            Spacer(Modifier.height(4.dp))
            if (state.layers.isEmpty()) {
                Text(
                    "Nenhum som na mistura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                state.layers.forEach { layer ->
                    LayerRow(
                        layer = layer,
                        onGain = { onLayerGain(layer.uid, it) },
                        onRemove = { onRemoveLayer(layer.uid) }
                    )
                }
            }

            if (layersEnabled && state.layers.size < 3) {
                TextButton(onClick = onAddLayer) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = SerenoColors.Mint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar som", color = SerenoColors.Mint)
                }
            }

            if (hasBinaural && !headphones) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SerenoColors.Binaural.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Headphones,
                            contentDescription = null,
                            tint = SerenoColors.Binaural,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Batidas binaurais precisam de fones de ouvido.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SerenoColors.Binaural
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onToggle,
                    shape = CircleShape,
                    color = SerenoColors.Mint,
                    modifier = Modifier.size(62.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pausar" else "Reproduzir",
                            tint = SerenoColors.Bg,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    onClick = onStop,
                    shape = RoundedCornerShape(16.dp),
                    color = SerenoColors.SurfaceHi,
                    border = BorderStroke(1.dp, SerenoColors.Outline)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = null,
                            tint = SerenoColors.Muted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Parar", color = SerenoColors.Text)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { showSave = true },
                    enabled = state.layers.isNotEmpty()
                ) {
                    Icon(
                        Icons.Outlined.BookmarkAdd,
                        contentDescription = null,
                        tint = SerenoColors.Mint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar", color = SerenoColors.Mint)
                }
            }
        }
    }

    if (showCustomTimer) {
        CustomTimerDialog(
            currentMinutes = if (state.totalMs > 0) (state.totalMs / 60_000L).toInt() else 20,
            onDismiss = { showCustomTimer = false },
            onConfirm = { minutes ->
                onSetTimer(minutes * 60_000L)
                showCustomTimer = false
            }
        )
    }

    if (showSave) {
        SaveMixDialog(
            onDismiss = { showSave = false },
            onConfirm = { name ->
                onSave(name)
                showSave = false
            }
        )
    }
}

@Composable
private fun LayerRow(
    layer: LayerUi,
    onGain: (Float) -> Unit,
    onRemove: () -> Unit
) {
    val def = SoundCatalog.find(layer.soundId)
    val accent = def?.let { categoryAccent(it.category) } ?: SerenoColors.Mint
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaveGlyph(layer.soundId.hashCode(), accent, Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    def?.name ?: layer.soundId,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(layer.gain * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = SerenoColors.Muted
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remover ${def?.name ?: "som"}",
                        tint = SerenoColors.Muted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Slider(
                value = layer.gain,
                onValueChange = onGain,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.75f),
                    inactiveTrackColor = SerenoColors.Outline,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CustomTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var value by remember { mutableStateOf(currentMinutes.toString()) }
    val minutes = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SerenoColors.SurfaceHi,
        title = { Text("Duração personalizada") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(3) },
                    label = { Text("Minutos") },
                    supportingText = { Text("Entre 1 e 180 minutos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { minutes?.let { onConfirm(it.coerceIn(1, 180)) } },
                enabled = minutes != null && minutes in 1..180
            ) {
                Text("Definir", color = SerenoColors.Mint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SerenoColors.Muted)
            }
        }
    )
}

private fun isCustom(ms: Long): Boolean =
    ms > 0 && ms != 5 * 60_000L && ms != 15 * 60_000L && ms != 30 * 60_000L

private fun timerStatus(state: PlayerState): String = when {
    state.totalMs <= 0 -> "sem limite de tempo"
    state.isPlaying -> "restam ${formatDuration(state.remainingMs)}"
    state.isPaused -> "pausado em ${formatDuration(state.remainingMs)}"
    else -> "duração ${timerLabel(state.totalMs)}"
}
