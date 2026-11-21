package com.opencode.sereno.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.sereno.audio.SoundCatalog
import com.opencode.sereno.audio.SoundCategory
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.data.SavedMix
import com.opencode.sereno.playback.PlayerState
import com.opencode.sereno.ui.BrandRow
import com.opencode.sereno.ui.CategoryChip
import com.opencode.sereno.ui.SoundCard
import com.opencode.sereno.ui.categoryAccent
import com.opencode.sereno.ui.layerSummary
import com.opencode.sereno.ui.theme.SerenoColors
import com.opencode.sereno.ui.timerLabel

@Composable
fun SoundsScreen(
    state: PlayerState,
    layersEnabled: Boolean,
    selected: SoundCategory,
    onSelectCategory: (SoundCategory) -> Unit,
    onAddSound: (String) -> Unit,
    onSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    mixes: List<SavedMix>,
    onSaveMix: (String) -> Unit,
    onDeleteMix: (String) -> Unit,
    onRenameMix: (String, String) -> Unit,
    onLoadMix: (SavedMix) -> Unit
) {
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SavedMix?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val favorites by Prefs.favoriteSounds.collectAsState()
    val categorySounds = when (selected) {
        SoundCategory.MIXES -> emptyList()
        SoundCategory.FAVORITES -> SoundCatalog.sounds.filter { it.id in favorites }
        else -> SoundCatalog.soundsOf(selected)
    }
    val shownSounds = categorySounds.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.subtitle.contains(query, ignoreCase = true)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            BrandRow(onSettings)
        }

        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    "Sons",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 30.sp
                )
                Text(
                    if (layersEnabled) {
                        "Toque para adicionar camadas. Até três ao mesmo tempo."
                    } else {
                        "Toque para ouvir. Um som por vez."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted
                )
            }
        }

        item(span = { GridItemSpan(2) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SoundCategory.entries.size) { index ->
                    val category = SoundCategory.entries[index]
                    CategoryChip(
                        label = category.label,
                        selected = category == selected,
                        accent = categoryAccent(category)
                    ) { onSelectCategory(category) }
                }
            }
        }

        if (selected != SoundCategory.MIXES) {
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar som") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = SerenoColors.Muted
                        )
                    },
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        if (state.layers.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Surface(
                    onClick = onOpenPlayer,
                    shape = RoundedCornerShape(18.dp),
                    color = SerenoColors.SurfaceHi
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (layersEnabled) "${state.layers.size} de 3 camadas"
                                else "Som único",
                                style = MaterialTheme.typography.labelSmall,
                                color = SerenoColors.Mint
                            )
                            Text(
                                layerSummary(state.layers),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            if (state.isPlaying) timerLabel(state.remainingMs) else "Abrir",
                            style = MaterialTheme.typography.labelLarge,
                            color = SerenoColors.Mint
                        )
                    }
                }
            }
        }

        if (selected == SoundCategory.MIXES) {
            item(span = { GridItemSpan(2) }) {
                SaveMixButton(
                    enabled = state.layers.isNotEmpty(),
                    layerText = layerSummary(state.layers),
                    single = !layersEnabled,
                    onClick = { showSaveDialog = true }
                )
            }
            if (mixes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    EmptyMixes()
                }
            } else {
                items(mixes, key = { it.name }) { mix ->
                    MixCard(
                        mix = mix,
                        onLoad = { onLoadMix(mix) },
                        onRename = { renameTarget = mix },
                        onDelete = { onDeleteMix(mix.name) }
                    )
                }
            }
        } else {
            if (shownSounds.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        when {
                            query.isNotBlank() -> "Nenhum som encontrado para “$query”."
                            selected == SoundCategory.FAVORITES -> "Toque na estrela de um som para guardar aqui."
                            else -> "Nenhum som nesta categoria."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                }
            } else {
                items(shownSounds, key = { it.id }) { def ->
                    SoundCard(
                        def = def,
                        active = state.layers.any { it.soundId == def.id },
                        onClick = { onAddSound(def.id) },
                        modifier = Modifier.fillMaxWidth(),
                        favorite = def.id in favorites,
                        onFavorite = { Prefs.toggleFavoriteSound(def.id) }
                    )
                }
            }
            item(span = { GridItemSpan(2) }) {
                Text(
                    if (layersEnabled) {
                        "O timer vale para a mistura inteira. Ajuste volumes na reprodução."
                    } else {
                        "O timer vale para o som atual. Toque em outro som para trocar."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveMixDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                onSaveMix(name)
                showSaveDialog = false
            }
        )
    }

    val renaming = renameTarget
    if (renaming != null) {
        RenameMixDialog(
            current = renaming.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                onRenameMix(renaming.name, name)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun SaveMixButton(
    enabled: Boolean,
    layerText: String,
    single: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) SerenoColors.Mint.copy(alpha = 0.1f) else SerenoColors.Surface,
        border = if (enabled) androidx.compose.foundation.BorderStroke(
            1.dp,
            SerenoColors.Mint.copy(alpha = 0.35f)
        ) else null
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.BookmarkAdd,
                contentDescription = null,
                tint = if (enabled) SerenoColors.Mint else SerenoColors.Muted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    if (single) "Salvar som atual" else "Salvar mistura atual",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) SerenoColors.Text else SerenoColors.Muted
                )
                Text(
                    when {
                        enabled -> layerText
                        single -> "Escolha um som para salvar"
                        else -> "Adicione sons para salvar"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyMixes() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(54.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Spa,
                contentDescription = null,
                tint = SerenoColors.Mix,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("Nenhuma mistura salva", style = MaterialTheme.typography.titleMedium)
        Text(
            "Monte uma combinação e toque em salvar.",
            style = MaterialTheme.typography.bodyMedium,
            color = SerenoColors.Muted
        )
    }
}

@Composable
private fun MixCard(
    mix: SavedMix,
    onLoad: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SerenoColors.Surface,
        onClick = onLoad
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = SerenoColors.Mint.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Ouvir",
                        tint = SerenoColors.Mint,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    mix.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    mix.layers.joinToString(" · ") { SoundCatalog.name(it.soundId) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    timerLabel(mix.durationMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Mint.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onRename) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Renomear mistura",
                    tint = SerenoColors.Muted,
                    modifier = Modifier.size(19.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Apagar mistura",
                    tint = SerenoColors.Muted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun SaveMixDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SerenoColors.SurfaceHi,
        title = { Text("Salvar mistura") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(32) },
                label = { Text("Nome") },
                placeholder = { Text("Ex.: Chuva para dormir") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.ifBlank { "Mistura" }) }) {
                Text("Salvar", color = SerenoColors.Mint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SerenoColors.Muted)
            }
        }
    )
}

@Composable
private fun RenameMixDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SerenoColors.SurfaceHi,
        title = { Text("Renomear mistura") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(32) },
                label = { Text("Nome") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("Salvar", color = SerenoColors.Mint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SerenoColors.Muted)
            }
        }
    )
}
