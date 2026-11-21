package com.opencode.sereno.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opencode.sereno.audio.VoiceNarrator
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.ui.theme.SerenoColors

@Composable
fun VoicePickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val ready by VoiceNarrator.ready.collectAsState()
    val voices by VoiceNarrator.voices.collectAsState()
    val status by VoiceNarrator.statusMessage.collectAsState()
    val selected by Prefs.ttsVoice.collectAsState()

    LaunchedEffect(Unit) {
        VoiceNarrator.ensure(context)
        VoiceNarrator.setVoice(selected)
    }

    LaunchedEffect(voices, selected, ready) {
        if (ready && voices.isNotEmpty() && selected.isNotBlank() &&
            voices.none { it.name == selected }
        ) {
            Prefs.setTtsVoice("")
            VoiceNarrator.setVoice("")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voz da narração") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 390.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Apenas vozes offline do Brasil. As vozes online e de Portugal foram removidas.",
                    color = SerenoColors.Muted
                )
                Spacer(Modifier.height(12.dp))
                VoiceRow(
                    label = "Voz offline recomendada",
                    selected = selected.isBlank(),
                    onSelect = {
                        Prefs.setTtsVoice("")
                        VoiceNarrator.setVoice("")
                    },
                    onTest = {
                        VoiceNarrator.setVoice("")
                        VoiceNarrator.play(context, "Olá. Esta é a voz que vai guiar sua prática.", 0.9f)
                    }
                )
                if (!ready) {
                    Spacer(Modifier.height(10.dp))
                    Text(status, color = SerenoColors.Muted)
                }
                voices.forEach { voice ->
                    VoiceRow(
                        label = voice.label,
                        selected = selected == voice.name,
                        onSelect = {
                            Prefs.setTtsVoice(voice.name)
                            VoiceNarrator.setVoice(voice.name)
                        },
                        onTest = {
                            VoiceNarrator.setVoice(voice.name)
                            VoiceNarrator.play(context, "Olá. Esta é a voz que vai guiar sua prática.", 0.9f)
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        val intent = Intent("com.android.settings.TTS_SETTINGS")
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                    }
                ) {
                    Text("Abrir ajustes de voz do Android")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Pronto") }
        }
    )
}

@Composable
private fun VoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (selected) SerenoColors.Mint else SerenoColors.Text
        )
        TextButton(onClick = onTest) { Text("Testar") }
    }
}
