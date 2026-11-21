package com.opencode.sereno.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sereno.audio.QuickMix
import com.opencode.sereno.audio.SoundCatalog
import com.opencode.sereno.audio.SoundCategory
import com.opencode.sereno.data.JournalEntry
import com.opencode.sereno.playback.PlayerState
import com.opencode.sereno.ui.BrandRow
import com.opencode.sereno.ui.SectionLabel
import com.opencode.sereno.ui.formatDuration
import com.opencode.sereno.ui.layerSummary
import com.opencode.sereno.ui.theme.SerenoColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TodayScreen(
    state: PlayerState,
    layersEnabled: Boolean,
    journalEntries: List<JournalEntry>,
    onSettings: () -> Unit,
    onOpenBreath: () -> Unit,
    onOpenJournal: () -> Unit,
    onQuickMix: (QuickMix) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSounds: (SoundCategory?) -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    val hour = remember { LocalTime.now().hour }
    val greeting = when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val todayEntries = remember(journalEntries) {
        journalEntries.count {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BrandRow(onSettings) }

        item {
            Column {
                Text(
                    greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light
                )
                Text(
                    "Respire. Ouça. Desacelere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted
                )
            }
        }

        item {
            HeroCard(layersEnabled = layersEnabled, onQuickMix = onQuickMix)
        }

        if (state.layers.isNotEmpty()) {
            item {
                NowPlayingCard(
                    state = state,
                    layersEnabled = layersEnabled,
                    onOpen = onOpenPlayer,
                    onToggle = onToggle,
                    onStop = onStop
                )
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            SectionLabel("Cuidar de você")
            Spacer(Modifier.height(10.dp))
            CareRow(
                todayEntries = todayEntries,
                onBreath = onOpenBreath,
                onJournal = onOpenJournal
            )
        }

        item {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Explorar")
        }

        item {
            ExploreRow(
                icon = Icons.Outlined.Cloud,
                title = "Sons da natureza",
                subtitle = "chuva, mar, floresta, fogo e mais",
                accent = SerenoColors.Nature
            ) { onOpenSounds(SoundCategory.NATURE) }
        }
        item {
            ExploreRow(
                icon = Icons.Outlined.GraphicEq,
                title = "Ruído e frequências",
                subtitle = "branco, rosa, marrom e tons",
                accent = SerenoColors.Noise
            ) { onOpenSounds(SoundCategory.NOISE) }
        }
        item {
            ExploreRow(
                icon = Icons.Outlined.MusicNote,
                title = "Instrumentos e pads",
                subtitle = "taças, gongo, kalimba e harpa",
                accent = SerenoColors.Music
            ) { onOpenSounds(SoundCategory.MUSIC) }
        }
        item {
            ExploreRow(
                icon = Icons.Outlined.Spa,
                title = "Minhas misturas",
                subtitle = "combinações salvas por você",
                accent = SerenoColors.Mix
            ) { onOpenSounds(SoundCategory.MIXES) }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text(
                "Sons sintetizados no aparelho, sem downloads.",
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroCard(layersEnabled: Boolean, onQuickMix: (QuickMix) -> Unit) {
    val ringColor = SerenoColors.Mint.copy(alpha = 0.07f)
    val heroTop = SerenoColors.SurfaceHi
    val heroBottom = SerenoColors.Surface
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(heroTop, heroBottom)
                )
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.92f, size.height * 0.05f)
            for (i in 0..3) {
                drawCircle(
                    color = ringColor,
                    radius = size.width * (0.18f + i * 0.12f),
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(
                "Escolha um momento",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (layersEnabled) "Uma mistura pronta para agora."
                else "O som principal de cada momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val icons = listOf(
                    Icons.Outlined.Bedtime,
                    Icons.Outlined.CenterFocusStrong,
                    Icons.Outlined.Waves
                )
                SoundCatalog.quickMixes.forEachIndexed { index, mix ->
                    QuickPill(
                        mix = mix,
                        icon = icons[index % icons.size],
                        modifier = Modifier.weight(1f)
                    ) { onQuickMix(mix) }
                }
            }
        }
    }
}

@Composable
private fun QuickPill(
    mix: QuickMix,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SerenoColors.Surface.copy(alpha = 0.72f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Icon(icon, contentDescription = null, tint = SerenoColors.Mint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(8.dp))
            Text(mix.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                "${mix.minutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    state: PlayerState,
    layersEnabled: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    val status = when {
        state.isPlaying && state.totalMs > 0 -> "restam ${formatDuration(state.remainingMs)}"
        state.isPlaying -> "tocando sem limite"
        state.isPaused -> "pausado"
        else -> "pronto"
    }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        color = SerenoColors.Surface
    ) {
        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (layersEnabled) "Sua mistura" else "Seu som",
                        style = MaterialTheme.typography.labelSmall,
                        color = SerenoColors.Mint
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        layerSummary(state.layers),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Retomar",
                        tint = SerenoColors.Mint
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Rounded.Stop,
                        contentDescription = "Parar",
                        tint = SerenoColors.Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = SerenoColors.Surface
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = SerenoColors.Muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CareRow(
    todayEntries: Int,
    onBreath: () -> Unit,
    onJournal: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CareCard(
            icon = Icons.Outlined.Air,
            title = "Respirar",
            subtitle = "Guiada · 4-7-8",
            accent = SerenoColors.Binaural,
            onClick = onBreath,
            modifier = Modifier.weight(1f)
        )
        CareCard(
            icon = Icons.Outlined.Book,
            title = "Gratidão",
            subtitle = when (todayEntries) {
                0 -> "O que fez bem hoje?"
                1 -> "1 entrada hoje"
                else -> "$todayEntries entradas hoje"
            },
            accent = SerenoColors.Warm,
            onClick = onJournal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CareCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 122.dp),
        shape = RoundedCornerShape(22.dp),
        color = SerenoColors.Surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
