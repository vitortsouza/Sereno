package com.opencode.sereno.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sereno.audio.SoundCategory
import com.opencode.sereno.audio.SoundDef
import com.opencode.sereno.playback.LayerUi
import com.opencode.sereno.playback.Player
import com.opencode.sereno.playback.PlayerState
import com.opencode.sereno.ui.theme.SerenoColors
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun categoryAccent(category: SoundCategory): Color = when (category) {
    SoundCategory.NATURE -> SerenoColors.Nature
    SoundCategory.AMBIENCE -> SerenoColors.Warm
    SoundCategory.NOISE -> SerenoColors.Noise
    SoundCategory.BINAURAL -> SerenoColors.Binaural
    SoundCategory.FREQUENCY -> SerenoColors.Frequency
    SoundCategory.MUSIC -> SerenoColors.Music
    SoundCategory.FAVORITES -> SerenoColors.Warm
    SoundCategory.MIXES -> SerenoColors.Mix
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

fun timerLabel(ms: Long): String = when {
    ms <= 0 -> "Livre"
    ms % 60_000L == 0L && ms < 3_600_000L -> "${ms / 60_000L} min"
    else -> formatDuration(ms)
}

@Composable
fun WaveGlyph(
    seed: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    lineCount: Int = 3
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val radius = max(w, h) * 0.56f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w / 2f, h / 2f),
                radius = radius
            ),
            radius = radius,
            center = Offset(w / 2f, h / 2f)
        )
        val rnd = Random(seed)
        for (line in 0 until lineCount) {
            val amp = h * (0.17f - line * 0.038f)
            val period = w / (1.9f + rnd.nextFloat() * 1.6f)
            val phase = rnd.nextFloat() * 6.283f
            val centerY = h / 2f + (line - (lineCount - 1) / 2f) * h * 0.11f
            val path = Path()
            var x = w * 0.08f
            path.moveTo(x, centerY + sin((phase).toDouble()).toFloat() * amp)
            val step = w / 30f
            while (x <= w * 0.93f) {
                val y = centerY + sin((phase + x / period * 6.283f).toDouble()).toFloat() * amp
                path.lineTo(x, y)
                x += step
            }
            drawPath(
                path = path,
                color = accent.copy(alpha = 0.88f - line * 0.24f),
                style = Stroke(width = (1.9f - line * 0.4f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.14f) else SerenoColors.Surface,
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else SerenoColors.Muted
        )
    }
}

@Composable
fun VolumeSlider(
    label: String,
    value: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = SerenoColors.Muted
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = SerenoColors.Muted
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
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

@Composable
fun SoundCard(
    def: SoundDef,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    onFavorite: (() -> Unit)? = null
) {
    val accent = categoryAccent(def.category)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 152.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (active) accent.copy(alpha = 0.09f) else SerenoColors.Surface,
        border = if (active) BorderStroke(1.dp, accent.copy(alpha = 0.45f)) else null
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    WaveGlyph(def.id.hashCode(), accent, Modifier.fillMaxSize())
                }
                Spacer(Modifier.weight(1f))
                if (onFavorite != null) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(44.dp)) {
                        Icon(
                            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = if (favorite) "Remover dos favoritos" else "Favoritar",
                            tint = if (favorite) accent else SerenoColors.Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (active) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Na mistura",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                def.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                def.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MiniPlayer(
    state: PlayerState,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val first = state.layers.firstOrNull()
    val def = first?.let { com.opencode.sereno.audio.SoundCatalog.find(it.soundId) }
    val accent = def?.let { categoryAccent(it.category) } ?: SerenoColors.Mint
    val title = when {
        state.layers.isEmpty() -> "Nenhum som"
        state.layers.size == 1 -> com.opencode.sereno.audio.SoundCatalog.name(state.layers[0].soundId)
        else -> "${state.layers.size} camadas"
    }
    val subtitle = when {
        state.isPlaying && state.totalMs > 0 -> "restam ${formatDuration(state.remainingMs)}"
        state.isPlaying -> "tocando sem limite"
        state.isPaused -> "pausado · ${formatDuration(state.remainingMs)}"
        else -> "pronto para tocar"
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = SerenoColors.SurfaceHi
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaveGlyph(first?.soundId?.hashCode() ?: 3, accent, Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted,
                    maxLines = 1
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pausar" else "Retomar",
                    tint = SerenoColors.Mint
                )
            }
            Icon(
                Icons.Outlined.KeyboardArrowUp,
                contentDescription = "Abrir mistura",
                tint = SerenoColors.Muted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun SerenoBottomBar(
    tabs: List<Tab>,
    current: Tab,
    onSelect: (Tab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (tab in tabs) {
            val selected = tab == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) SerenoColors.Mint.copy(alpha = 0.13f) else Color.Transparent,
                            CircleShape
                        )
                        .padding(horizontal = 15.dp, vertical = 4.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) SerenoColors.Mint else SerenoColors.Muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    tab.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) SerenoColors.Mint else SerenoColors.Muted
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = SerenoColors.Muted,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun BrandRow(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "SERENO",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 5.sp),
            color = SerenoColors.Mint
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Ajustes",
                tint = SerenoColors.Muted
            )
        }
    }
}

fun layerSummary(layers: List<LayerUi>): String =
    layers.joinToString(" · ") { com.opencode.sereno.audio.SoundCatalog.name(it.soundId) }

fun currentLayerNames(layers: List<LayerUi>): List<String> =
    layers.map { com.opencode.sereno.audio.SoundCatalog.name(it.soundId) }

fun headphonesOk(): Boolean = Player.hasHeadphones()

enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Hoje", Icons.Outlined.Home),
    BREATH("Respirar", Icons.Outlined.Air),
    MEDITATE("Meditar", Icons.Outlined.SelfImprovement),
    SOUNDS("Sons", Icons.Outlined.GraphicEq),
    JOURNAL("Diário", Icons.Outlined.Book)
}
