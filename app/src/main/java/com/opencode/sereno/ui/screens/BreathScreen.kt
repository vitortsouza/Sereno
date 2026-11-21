package com.opencode.sereno.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sereno.audio.AmbientBed
import com.opencode.sereno.audio.AmbientOptions
import com.opencode.sereno.audio.BreathCue
import com.opencode.sereno.audio.BreathCuePlayer
import com.opencode.sereno.audio.BreathHaptics
import com.opencode.sereno.audio.VoiceNarrator
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.playback.MeditationController
import com.opencode.sereno.playback.Player
import com.opencode.sereno.playback.PracticeFocus
import com.opencode.sereno.ui.BrandRow
import com.opencode.sereno.ui.CategoryChip
import com.opencode.sereno.ui.SectionLabel
import com.opencode.sereno.ui.VoicePickerDialog
import com.opencode.sereno.ui.VolumeSlider
import com.opencode.sereno.ui.formatDuration
import com.opencode.sereno.ui.theme.SerenoColors
import kotlinx.coroutines.delay

private enum class PhaseKind { INHALE, HOLD, EXHALE }

private data class BreathPhase(val kind: PhaseKind, val seconds: Int)

private data class BreathPattern(
    val id: String,
    val name: String,
    val tempo: String,
    val phases: List<BreathPhase>
)

private val breathPatterns = listOf(
    BreathPattern(
        id = "relax",
        name = "Relaxar",
        tempo = "4-7-8",
        phases = listOf(
            BreathPhase(PhaseKind.INHALE, 4),
            BreathPhase(PhaseKind.HOLD, 7),
            BreathPhase(PhaseKind.EXHALE, 8)
        )
    ),
    BreathPattern(
        id = "box",
        name = "Equilíbrio",
        tempo = "4-4-4-4",
        phases = listOf(
            BreathPhase(PhaseKind.INHALE, 4),
            BreathPhase(PhaseKind.HOLD, 4),
            BreathPhase(PhaseKind.EXHALE, 4),
            BreathPhase(PhaseKind.HOLD, 4)
        )
    ),
    BreathPattern(
        id = "coherent",
        name = "Ritmo",
        tempo = "5-5",
        phases = listOf(
            BreathPhase(PhaseKind.INHALE, 5),
            BreathPhase(PhaseKind.EXHALE, 5)
        )
    ),
    BreathPattern(
        id = "calm",
        name = "Calma",
        tempo = "4-6",
        phases = listOf(
            BreathPhase(PhaseKind.INHALE, 4),
            BreathPhase(PhaseKind.EXHALE, 6)
        )
    )
)

private val durationOptions = listOf(1, 3, 5, 10)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreathScreen(onSettings: () -> Unit) {
    var patternId by rememberSaveable { mutableStateOf("relax") }
    var minutes by rememberSaveable { mutableIntStateOf(3) }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var countdownLeft by rememberSaveable { mutableIntStateOf(0) }
    var showVoicePicker by rememberSaveable { mutableStateOf(false) }

    val pattern = breathPatterns.firstOrNull { it.id == patternId } ?: breathPatterns.first()
    val sessionMs = minutes * 60_000L
    val cycleMs = pattern.phases.sumOf { it.seconds * 1000L }.coerceAtLeast(1L)
    val cyclePos = if (cycleMs > 0) elapsedMs % cycleMs else 0L
    val phaseInfo = phaseAt(pattern, cyclePos)
    val phase = phaseInfo.first
    val phaseRemainingMs = phaseInfo.second
    val completedCycles = (elapsedMs / cycleMs).toInt()
    val active = running || elapsedMs > 0L
    val view = LocalView.current
    val context = LocalContext.current
    val hapticsOn by Prefs.breathHaptics.collectAsState()
    val cuesOn by Prefs.breathCues.collectAsState()
    val cueVolume by Prefs.breathCueVolume.collectAsState()
    val ambientId by Prefs.breathAmbient.collectAsState()
    val ambientVolume by Prefs.breathAmbientVolume.collectAsState()
    val narrationOn by Prefs.breathNarration.collectAsState()
    val narrationVolume by Prefs.breathNarrationVolume.collectAsState()
    val countdownOn by Prefs.breathCountdown.collectAsState()
    val selectedVoice by Prefs.ttsVoice.collectAsState()
    val ambientBed = remember { AmbientBed() }
    val focusToken = remember { Any() }

    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    DisposableEffect(focusToken) {
        PracticeFocus.register(focusToken) {
            running = false
            finished = false
            countdownLeft = 0
        }
        onDispose {
            PracticeFocus.unregister(focusToken)
            ambientBed.stop()
            BreathCuePlayer.release()
        }
    }

    LaunchedEffect(running) {
        if (running) {
            Player.stopPlayback()
            MeditationController.stop()
        } else {
            VoiceNarrator.stop()
        }
    }

    LaunchedEffect(selectedVoice) {
        VoiceNarrator.ensure(context)
        VoiceNarrator.setVoice(selectedVoice)
        VoiceNarrator.setRate(0.72f)
    }

    LaunchedEffect(narrationOn) {
        if (!narrationOn) VoiceNarrator.stop()
    }

    LaunchedEffect(running, ambientId, ambientVolume) {
        val option = AmbientOptions.firstOrNull { it.id == ambientId }
        if (running && option != null && option.id != "none") {
            ambientBed.start(option.id, gain = option.level * ambientVolume)
        } else {
            ambientBed.stop()
        }
    }

    LaunchedEffect(running, sessionMs, countdownLeft) {
        if (!running || countdownLeft > 0) return@LaunchedEffect
        val base = elapsedMs
        val start = SystemClock.elapsedRealtime()
        while (true) {
            val next = base + (SystemClock.elapsedRealtime() - start)
            elapsedMs = next
            if (next >= sessionMs) {
                elapsedMs = sessionMs
                running = false
                finished = true
                break
            }
            delay(80)
        }
    }

    LaunchedEffect(running, countdownLeft) {
        if (!running || countdownLeft <= 0) return@LaunchedEffect
        if (narrationOn) {
            ambientBed.setDucked(true)
            try {
                val word = when (countdownLeft) {
                    3 -> "Três"
                    2 -> "Dois"
                    else -> "Um"
                }
                VoiceNarrator.play(context, word, narrationVolume)
                delay(850)
            } finally {
                ambientBed.setDucked(false)
            }
        } else {
            delay(400)
        }
        countdownLeft -= 1
    }

    LaunchedEffect(phaseIndex(pattern, cyclePos), running, countdownLeft, narrationOn) {
        if (!running || countdownLeft > 0) return@LaunchedEffect
        if (narrationOn) {
            val index = phaseIndex(pattern, cyclePos)
            val prompt = when (phase.kind) {
                PhaseKind.INHALE -> "Inspire"
                PhaseKind.EXHALE -> "Solte"
                PhaseKind.HOLD -> if (index > 0 && pattern.phases[index - 1].kind == PhaseKind.EXHALE) {
                    "Solte o ar"
                } else {
                    "Segure"
                }
            }
            ambientBed.setDucked(true)
            try {
                VoiceNarrator.play(context, prompt, narrationVolume)
                delay(1100)
            } finally {
                ambientBed.setDucked(false)
            }
            return@LaunchedEffect
        }
        val cue = phase.kind.toCue()
        if (Prefs.breathHaptics.value) BreathHaptics.play(context, cue)
        if (Prefs.breathCues.value && !narrationOn) {
            ambientBed.setDucked(true)
            try {
                BreathCuePlayer.play(cue, Prefs.breathCueVolume.value)
                delay(650)
            } finally {
                ambientBed.setDucked(false)
            }
        }
    }

    LaunchedEffect(finished) {
        if (finished) {
            countdownLeft = 0
            if (Prefs.breathHaptics.value) BreathHaptics.play(context, BreathCue.COMPLETE)
            if (narrationOn) {
                VoiceNarrator.play(context, "Concluído", narrationVolume)
            } else if (Prefs.breathCues.value) {
                BreathCuePlayer.play(BreathCue.COMPLETE, Prefs.breathCueVolume.value)
            }
        }
    }

    val targetScale = when (phase.kind) {
        PhaseKind.INHALE -> 1f
        PhaseKind.EXHALE -> 0.56f
        PhaseKind.HOLD -> holdScale(pattern, phaseIndex(pattern, cyclePos))
    }
    val scaleTarget = if (!active) 0.8f else targetScale
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = if (running) {
            tween(durationMillis = phase.seconds * 1000, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 220, easing = FastOutSlowInEasing)
        },
        label = "breathScale"
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp)
    ) {
        BrandRow(onSettings)
        Spacer(Modifier.height(8.dp))
        Text(
            "Respirar",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 30.sp
        )
        Text(
            "Um ritmo simples para voltar ao corpo.",
            style = MaterialTheme.typography.bodyMedium,
            color = SerenoColors.Muted
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("Ritmo")
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            breathPatterns.forEach { option ->
                CategoryChip(
                    label = option.name,
                    selected = option.id == patternId,
                    accent = SerenoColors.Mint
                ) {
                    patternId = option.id
                    elapsedMs = 0L
                    running = false
                    finished = false
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${pattern.tempo} · ${patternHint(pattern)}",
            style = MaterialTheme.typography.bodyMedium,
            color = SerenoColors.Muted
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel("Duração")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durationOptions.forEach { option ->
                CategoryChip(
                    label = "$option min",
                    selected = option == minutes,
                    accent = SerenoColors.Mint
                ) {
                    minutes = option
                    elapsedMs = 0L
                    running = false
                    finished = false
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(
                label = "Vibração",
                selected = hapticsOn,
                accent = SerenoColors.Mint
            ) {
                Prefs.setBreathHaptics(!hapticsOn)
            }
            CategoryChip(
                label = "Som guia",
                selected = cuesOn,
                accent = SerenoColors.Mint
            ) {
                Prefs.setBreathCues(!cuesOn)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Sinais suaves a cada troca de fase.",
            style = MaterialTheme.typography.labelSmall,
            color = SerenoColors.Muted
        )
        if (cuesOn && !narrationOn) {
            Spacer(Modifier.height(4.dp))
            VolumeSlider(
                label = "Volume do beep",
                value = cueVolume,
                accent = SerenoColors.Mint
            ) {
                Prefs.setBreathCueVolume(it)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(
                label = "Narração",
                selected = narrationOn,
                accent = SerenoColors.Mint
            ) {
                Prefs.setBreathNarration(!narrationOn)
            }
            if (narrationOn) {
                CategoryChip(
                    label = "Contagem 3-2-1",
                    selected = countdownOn,
                    accent = SerenoColors.Mint
                ) {
                    Prefs.setBreathCountdown(!countdownOn)
                }
            }
        }
        if (narrationOn) {
            Spacer(Modifier.height(4.dp))
            VolumeSlider(
                label = "Volume da voz",
                value = narrationVolume,
                accent = SerenoColors.Mint
            ) {
                Prefs.setBreathNarrationVolume(it)
            }
            TextButton(onClick = { showVoicePicker = true }) {
                Text("Escolher voz", color = SerenoColors.Mint)
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Som de fundo")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmbientOptions.forEach { option ->
                CategoryChip(
                    label = option.label,
                    selected = ambientId == option.id,
                    accent = SerenoColors.Mint
                ) {
                    Prefs.setBreathAmbient(option.id)
                }
            }
        }
        if (ambientId != "none") {
            Spacer(Modifier.height(4.dp))
            VolumeSlider(
                label = "Volume do fundo",
                value = ambientVolume,
                accent = SerenoColors.Mint
            ) {
                Prefs.setBreathAmbientVolume(it)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Volume baixo, abaixo dos sinais da respiração.",
            style = MaterialTheme.typography.labelSmall,
            color = SerenoColors.Muted
        )

        Spacer(Modifier.height(26.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            val ringColor = SerenoColors.Mint
            val glowColor = SerenoColors.Mint
            val cycleProgress = (elapsedMs % cycleMs).toFloat() / cycleMs
            Canvas(Modifier.size(284.dp)) {
                val radius = size.minDimension / 2f
                drawCircle(
                    color = ringColor.copy(alpha = 0.10f),
                    radius = radius
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.34f),
                            glowColor.copy(alpha = 0.08f)
                        ),
                        center = center,
                        radius = radius * scale
                    ),
                    radius = radius * scale
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.35f),
                    radius = radius,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * cycleProgress,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                if (!active) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.18f),
                        radius = radius * 0.8f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        finished -> "Concluído"
                        !active -> "Pronto"
                        phase.kind == PhaseKind.INHALE -> "Inspire"
                        phase.kind == PhaseKind.EXHALE -> "Expire"
                        else -> "Segure"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                if (active && !finished) {
                    Text(
                        "${((phaseRemainingMs + 999) / 1000).coerceAtLeast(1)}",
                        style = MaterialTheme.typography.displayLarge,
                        color = SerenoColors.Mint
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (finished) {
                            "$completedCycles ${if (completedCycles == 1) "ciclo" else "ciclos"}"
                        } else {
                            "Escolha um ritmo"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                if (active) {
                    Text(
                        if (finished) "sessão encerrada" else "ciclo ${completedCycles + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SerenoColors.Muted
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = {
                    when {
                        finished -> {
                            elapsedMs = 0L
                            finished = false
                            countdownLeft = if (narrationOn && countdownOn) 3 else 0
                            running = true
                        }
                        running -> running = false
                        else -> {
                            countdownLeft = if (narrationOn && countdownOn) 3 else 0
                            running = true
                        }
                    }
                },
                shape = CircleShape,
                color = SerenoColors.Mint,
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
                            else -> "Iniciar"
                        },
                        tint = SerenoColors.Bg,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            if (active && !finished) {
                Surface(
                    onClick = {
                        running = false
                        elapsedMs = 0L
                        finished = false
                        countdownLeft = 0
                        VoiceNarrator.stop()
                    },
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
            if (active) {
                Text(
                    formatDuration((sessionMs - elapsedMs).coerceAtLeast(0L)),
                    style = MaterialTheme.typography.titleMedium,
                    color = SerenoColors.Muted
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Sente-se confortável e respire pelo nariz, sem forçar. Pare se sentir desconforto.",
            style = MaterialTheme.typography.bodyMedium,
            color = SerenoColors.Muted.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showVoicePicker) {
        VoicePickerDialog(onDismiss = { showVoicePicker = false })
    }
}

private fun phaseAt(pattern: BreathPattern, cyclePos: Long): Pair<BreathPhase, Long> {
    var acc = 0L
    for (phase in pattern.phases) {
        val duration = phase.seconds * 1000L
        if (cyclePos < acc + duration || phase === pattern.phases.last()) {
            return phase to (acc + duration - cyclePos).coerceAtLeast(0L)
        }
        acc += duration
    }
    val last = pattern.phases.last()
    return last to last.seconds * 1000L
}

private fun phaseIndex(pattern: BreathPattern, cyclePos: Long): Int {
    var acc = 0L
    pattern.phases.forEachIndexed { index, phase ->
        val duration = phase.seconds * 1000L
        if (cyclePos < acc + duration) return index
        acc += duration
    }
    return pattern.phases.lastIndex
}

private fun holdScale(pattern: BreathPattern, index: Int): Float {
    var i = index
    while (i > 0 && pattern.phases[i].kind == PhaseKind.HOLD) i--
    return if (pattern.phases[i].kind == PhaseKind.INHALE) 1f else 0.56f
}

private fun patternHint(pattern: BreathPattern): String = pattern.phases.joinToString(" · ") {
    when (it.kind) {
        PhaseKind.INHALE -> "inspire ${it.seconds}"
        PhaseKind.HOLD -> "segure ${it.seconds}"
        PhaseKind.EXHALE -> "expire ${it.seconds}"
    }
}

private fun PhaseKind.toCue(): BreathCue = when (this) {
    PhaseKind.INHALE -> BreathCue.INHALE
    PhaseKind.HOLD -> BreathCue.HOLD
    PhaseKind.EXHALE -> BreathCue.EXHALE
}
