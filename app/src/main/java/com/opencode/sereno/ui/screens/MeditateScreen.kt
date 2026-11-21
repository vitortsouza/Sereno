package com.opencode.sereno.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sereno.audio.AmbientOptions
import com.opencode.sereno.audio.MeditationSession
import com.opencode.sereno.audio.meditations
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.playback.MeditationController
import com.opencode.sereno.playback.MeditationVoiceStatus
import com.opencode.sereno.ui.BrandRow
import com.opencode.sereno.ui.CategoryChip
import com.opencode.sereno.ui.SectionLabel
import com.opencode.sereno.ui.VoicePickerDialog
import com.opencode.sereno.ui.VolumeSlider
import com.opencode.sereno.ui.formatDuration
import com.opencode.sereno.ui.theme.SerenoColors

private enum class VoiceState { PREPARING, READY, UNAVAILABLE }

private data class SpeechRateOption(val value: Float, val label: String)

private val speechRates = listOf(
    SpeechRateOption(0.62f, "Muito lenta"),
    SpeechRateOption(0.70f, "Lenta"),
    SpeechRateOption(0.82f, "Natural")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeditateScreen(onSettings: () -> Unit) {
    var sessionId by rememberSaveable { mutableStateOf(meditations.first().id) }
    var showVoicePicker by rememberSaveable { mutableStateOf(false) }
    val ambientId by Prefs.meditationAmbient.collectAsState()
    val ambientVolume by Prefs.meditationAmbientVolume.collectAsState()
    val speechRate by Prefs.meditationSpeechRate.collectAsState()
    val selectedVoice by Prefs.ttsVoice.collectAsState()
    val mState by MeditationController.state.collectAsState()
    val view = LocalView.current

    val controlling = mState.sessionId.isNotBlank()
    val session = if (controlling) {
        meditations.firstOrNull { it.id == mState.sessionId }
    } else {
        meditations.firstOrNull { it.id == sessionId }
    } ?: meditations.first()
    val safeIndex = if (controlling) {
        mState.stepIndex.coerceIn(0, session.steps.lastIndex)
    } else {
        0
    }
    val step = session.steps[safeIndex]
    val finished = controlling && mState.finished
    val running = controlling && mState.running
    val elapsedMs = if (controlling) mState.elapsedMs else 0L
    val active = running || elapsedMs > 0L || finished
    val progress = when {
        finished -> 1f
        session.totalMs > 0L -> (elapsedMs.toFloat() / session.totalMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val voiceState = when (mState.voiceStatus) {
        MeditationVoiceStatus.PREPARING -> VoiceState.PREPARING
        MeditationVoiceStatus.READY -> VoiceState.READY
        MeditationVoiceStatus.UNAVAILABLE -> VoiceState.UNAVAILABLE
    }
    val accent = SerenoColors.Music
    val canStart = voiceState != VoiceState.PREPARING

    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(speechRate) {
        MeditationController.setRate(speechRate)
    }

    LaunchedEffect(selectedVoice, mState.voiceStatus) {
        if (mState.voiceStatus == MeditationVoiceStatus.READY) {
            MeditationController.setVoice(selectedVoice)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp)
    ) {
        BrandRow(onSettings)
        Spacer(Modifier.height(8.dp))
        Text(
            "Meditar",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 30.sp
        )
        Text(
            "Voz suave para pausar o dia.",
            style = MaterialTheme.typography.bodyMedium,
            color = SerenoColors.Muted
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Sessões")
        meditations.forEachIndexed { index, option ->
            SessionCard(
                session = option,
                selected = option.id == session.id,
                enabled = !running,
                onSelect = {
                    MeditationController.stop()
                    sessionId = option.id
                }
            )
            if (index != meditations.lastIndex) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Som ambiente")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmbientOptions.forEach { option ->
                CategoryChip(
                    label = option.label,
                    selected = ambientId == option.id,
                    accent = SerenoColors.Music
                ) {
                    Prefs.setMeditationAmbient(option.id)
                }
            }
        }
        if (ambientId != "none") {
            Spacer(Modifier.height(4.dp))
            VolumeSlider(
                label = "Volume do ambiente",
                value = ambientVolume,
                accent = SerenoColors.Music
            ) {
                Prefs.setMeditationAmbientVolume(it)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Ritmo da voz")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speechRates.forEach { option ->
                CategoryChip(
                    label = option.label,
                    selected = speechRate == option.value,
                    accent = SerenoColors.Music
                ) {
                    Prefs.setMeditationSpeechRate(option.value)
                }
            }
        }
        TextButton(onClick = { showVoicePicker = true }) {
            Text("Escolher voz", color = SerenoColors.Music)
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Prática")
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = SerenoColors.Surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            finished -> "Sessão concluída"
                            active -> step.title
                            else -> "Pronto para começar"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        when {
                            finished -> "concluída"
                            active -> "passo ${safeIndex + 1} de ${session.steps.size}"
                            else -> session.subtitle.substringBefore(" ·")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SerenoColors.Muted
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        finished -> "Quando estiver pronto, siga o dia com calma."
                        active -> step.text
                        else -> session.description
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = if (active && !finished) SerenoColors.Text else SerenoColors.Muted
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(SerenoColors.SurfaceSoft, CircleShape)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(accent, CircleShape)
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDuration(elapsedMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = SerenoColors.Muted
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "de ${formatDuration(session.totalMs)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SerenoColors.Muted
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = {
                    when {
                        running -> MeditationController.pause()
                        canStart -> MeditationController.start(
                            nextSession = session,
                            nextAmbientId = ambientId,
                            nextAmbientVolume = ambientVolume,
                            nextRate = speechRate,
                            nextVoice = selectedVoice
                        )
                    }
                },
                enabled = canStart || running,
                shape = CircleShape,
                color = if (canStart) accent else SerenoColors.SurfaceHi,
                modifier = Modifier.size(62.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when {
                            finished -> Icons.Rounded.Refresh
                            running -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = when {
                            finished -> "Começar de novo"
                            running -> "Pausar"
                            active -> "Continuar meditação"
                            else -> "Iniciar meditação"
                        },
                        tint = if (canStart) SerenoColors.Bg else SerenoColors.Muted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            if (active && !finished) {
                Surface(
                    onClick = { MeditationController.stop() },
                    shape = RoundedCornerShape(16.dp),
                    color = SerenoColors.SurfaceHi
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
                        Spacer(Modifier.size(8.dp))
                        Text("Encerrar", color = SerenoColors.Text)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            VoiceStatus(voiceState)
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Use fones ou deixe o volume confortável. A voz é gerada pelo sistema do aparelho. " +
                "Pare se sentir desconforto. Esta prática não substitui acompanhamento de saúde.",
            style = MaterialTheme.typography.bodyMedium,
            color = SerenoColors.Muted.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showVoicePicker) {
        VoicePickerDialog(onDismiss = { showVoicePicker = false })
    }
}

@Composable
private fun SessionCard(
    session: MeditationSession,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val accent = SerenoColors.Music
    Surface(
        onClick = onSelect,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accent.copy(alpha = 0.09f) else SerenoColors.Surface,
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.45f)) else null
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) SerenoColors.Text else SerenoColors.Muted
                )
                Text(
                    session.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Selecionada",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    "≈ ${session.totalMs / 60_000} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = SerenoColors.Muted
                )
            }
        }
    }
}

@Composable
private fun VoiceStatus(state: VoiceState) {
    val color: Color = when (state) {
        VoiceState.PREPARING -> SerenoColors.Muted
        VoiceState.READY -> SerenoColors.Music
        VoiceState.UNAVAILABLE -> SerenoColors.Warm
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.size(7.dp))
        Text(
            when (state) {
                VoiceState.PREPARING -> "Preparando voz"
                VoiceState.READY -> "Voz offline do Brasil"
                VoiceState.UNAVAILABLE -> "Modo silencioso"
            },
            style = MaterialTheme.typography.labelSmall,
            color = SerenoColors.Muted
        )
    }
}

