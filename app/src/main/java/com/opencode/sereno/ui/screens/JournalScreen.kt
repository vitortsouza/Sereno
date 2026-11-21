package com.opencode.sereno.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sereno.data.JournalEntry
import com.opencode.sereno.data.JournalMoods
import com.opencode.sereno.ui.BrandRow
import com.opencode.sereno.ui.CategoryChip
import com.opencode.sereno.ui.SectionLabel
import com.opencode.sereno.ui.theme.SerenoColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptLocale = Locale.forLanguageTag("pt-BR")
private val dayFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", ptLocale)
private val dayYearFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ptLocale)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", ptLocale)

private val prompts = listOf(
    "Hoje eu sou grato por…",
    "Um pequeno momento que trouxe calma…",
    "Alguém que fez diferença no meu dia…",
    "O que eu quero lembrar deste dia?",
    "Um gesto simples que me fez bem…"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    entries: List<JournalEntry>,
    onAdd: (String, String?) -> Unit,
    onDelete: (Long) -> Unit,
    onSettings: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf<String?>(null) }
    var prompt by rememberSaveable { mutableStateOf(prompts.random()) }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val canSave = text.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandRow(onSettings) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Diário",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 30.sp
                    )
                    Text(
                        "Um espaço para notar o que fez bem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                if (entries.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, exportJournal(entries, zone))
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Exportar diário")
                            )
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar", color = SerenoColors.Mint)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SerenoColors.SurfaceHi
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Nova entrada",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { prompt = prompts.random() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Mudar sugestão",
                                tint = SerenoColors.Muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                    Spacer(Modifier.size(10.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(4000) },
                        placeholder = { Text("Escreva sem pressa…") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JournalMoods.forEach { option ->
                            CategoryChip(
                                label = option.label,
                                selected = mood == option.key,
                                accent = moodAccent(option.key)
                            ) {
                                mood = if (mood == option.key) null else option.key
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                onAdd(text, mood)
                                text = ""
                                mood = null
                                prompt = prompts.random()
                            },
                            enabled = canSave
                        ) {
                            Text(
                                "Salvar",
                                color = if (canSave) SerenoColors.Mint else SerenoColors.Muted
                            )
                        }
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item { EmptyJournal() }
        } else {
            val groups = entries.groupBy { dayLabel(it.createdAt, today, zone) }
            groups.forEach { (label, dayEntries) ->
                item(key = "header-$label") {
                    SectionLabel(label)
                }
                items(dayEntries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        zone = zone,
                        onDelete = { pendingDelete = entry.id }
                    )
                }
            }
            item {
                Text(
                    "Suas entradas ficam só neste aparelho.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    val deleteId = pendingDelete
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = SerenoColors.SurfaceHi,
            title = { Text("Apagar entrada?") },
            text = { Text("Esta lembrança será removida do diário.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deleteId)
                    pendingDelete = null
                }) {
                    Text("Apagar", color = SerenoColors.Mint)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = SerenoColors.Muted)
                }
            }
        )
    }
}

@Composable
private fun EmptyJournal() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SerenoColors.Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Suas gratidões aparecem aqui.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "Comece com uma frase simples.",
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: JournalEntry,
    zone: ZoneId,
    onDelete: () -> Unit
) {
    val accent = entry.mood?.let { moodAccent(it) }
    val moodLabel = entry.mood?.let { key ->
        JournalMoods.firstOrNull { it.key == key }?.label
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SerenoColors.Surface
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeFormatter.format(Instant.ofEpochMilli(entry.createdAt).atZone(zone)),
                    style = MaterialTheme.typography.labelSmall,
                    color = SerenoColors.Muted
                )
                if (accent != null && moodLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            moodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Apagar entrada",
                        tint = SerenoColors.Muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                entry.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp, end = 8.dp),
                maxLines = 12,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun moodAccent(key: String) = when (key) {
    "grato" -> SerenoColors.Nature
    "calmo" -> SerenoColors.Binaural
    "bem" -> SerenoColors.Warm
    else -> SerenoColors.Mix
}

private fun dayLabel(timestamp: Long, today: LocalDate, zone: ZoneId): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> {
            val formatter = if (date.year == today.year) dayFormatter else dayYearFormatter
            formatter.format(date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptLocale) else it.toString() }
        }
    }
}

private fun exportJournal(entries: List<JournalEntry>, zone: ZoneId): String {
    val header = "Meu diário no Sereno\n"
    val body = entries.joinToString("\n\n") { entry ->
        val date = dayYearFormatter.format(
            Instant.ofEpochMilli(entry.createdAt).atZone(zone)
        )
        val time = timeFormatter.format(
            Instant.ofEpochMilli(entry.createdAt).atZone(zone)
        )
        val mood = entry.mood?.let { key -> JournalMoods.firstOrNull { it.key == key }?.label }
        buildString {
            append("$date · $time")
            if (mood != null) append(" · $mood")
            append("\n")
            append(entry.text)
        }
    }
    return header + "\n" + body
}
