package com.opencode.sereno.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.opencode.sereno.audio.SoundCatalog
import com.opencode.sereno.audio.VoiceNarrator
import com.opencode.sereno.data.ThemeMode
import com.opencode.sereno.ui.CategoryChip
import com.opencode.sereno.ui.SectionLabel
import com.opencode.sereno.ui.theme.SerenoColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    fadeSeconds: Int,
    onFadeChange: (Int) -> Unit,
    headphonesWarning: Boolean,
    onHeadphonesChange: (Boolean) -> Unit,
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    layersEnabled: Boolean,
    onLayersEnabledChange: (Boolean) -> Unit,
    sleepMode: Boolean,
    onSleepModeChange: (Boolean) -> Unit,
    journalReminder: Boolean,
    onJournalReminderChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    mixCount: Int,
    onDeleteAllMixes: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceReady by VoiceNarrator.ready.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = SerenoColors.Muted
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontSize = 26.sp)
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Aparência")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Column(Modifier.padding(16.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Siga o sistema ou fixe claro ou escuro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "Sistema",
                        ThemeMode.LIGHT to "Claro",
                        ThemeMode.DARK to "Escuro"
                    ).forEach { (mode, label) ->
                        CategoryChip(
                            label = label,
                            selected = themeMode == mode,
                            accent = SerenoColors.Mint
                        ) { onThemeChange(mode) }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Áudio")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Column(Modifier.padding(16.dp)) {
                Text("Fade ao encerrar", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Os últimos segundos caem suavemente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30).forEach { seconds ->
                        CategoryChip(
                            label = "${seconds}s",
                            selected = fadeSeconds == seconds,
                            accent = SerenoColors.Mint
                        ) { onFadeChange(seconds) }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SwitchRow(
                    title = "Aviso de fones",
                    subtitle = "Lembrar que batidas binaurais pedem fones",
                    checked = headphonesWarning,
                    onChange = onHeadphonesChange
                )
                SwitchRow(
                    title = "Reproduzir ao adicionar",
                    subtitle = "Começar o som assim que tocar nele",
                    checked = autoPlay,
                    onChange = onAutoPlayChange
                )
                SwitchRow(
                    title = "Permitir misturar sons",
                    subtitle = if (layersEnabled) {
                        "Combine até 3 sons ao mesmo tempo"
                    } else {
                        "Um som por vez; tocar em outro substitui"
                    },
                    checked = layersEnabled,
                    onChange = onLayersEnabledChange
                )
                SwitchRow(
                    title = "Modo dormir",
                    subtitle = "Ao fim do timer, encerrar e limpar os sons",
                    checked = sleepMode,
                    onChange = onSleepModeChange
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Diário")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SwitchRow(
                    title = "Lembrete gentil",
                    subtitle = "Um convite para escrever, todos os dias às 20h",
                    checked = journalReminder,
                    onChange = onJournalReminderChange
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Notificações")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (notificationsEnabled) Icons.Outlined.NotificationsActive
                    else Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = if (notificationsEnabled) SerenoColors.Mint else SerenoColors.Muted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Controles na tela de bloqueio", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (notificationsEnabled) {
                            "Ativado. Toque em Ativado para ajustar a permissão."
                        } else {
                            "Permissão negada. Ative nas configurações do Android para ver os controles."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                if (notificationsEnabled) {
                    TextButton(onClick = { openNotificationSettings(context) }) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = SerenoColors.Mint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ativado", color = SerenoColors.Mint)
                    }
                } else {
                    TextButton(onClick = { openNotificationSettings(context) }) {
                        Text("Ajustes", color = SerenoColors.Mint)
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Misturas")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = SerenoColors.Muted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Apagar todas as misturas", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (mixCount == 0) "Nenhuma mistura salva"
                        else "$mixCount mistura${if (mixCount > 1) "s" else ""} salva${if (mixCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = mixCount > 0
                ) {
                    Text("Apagar", color = SerenoColors.Muted)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Diagnóstico")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Áudio e voz", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (voiceReady) "Voz pronta · saída em português"
                        else "Voz ainda não inicializada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SerenoColors.Muted
                    )
                }
                TextButton(onClick = { showDiagnostics = true }) {
                    Text("Ver", color = SerenoColors.Mint)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Sobre")

        Surface(shape = RoundedCornerShape(20.dp), color = SerenoColors.Surface) {
            Column(Modifier.padding(18.dp)) {
                Text("Sereno 0.6.0", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sons da natureza e práticas guiadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Mint.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sons, respiração, diário e meditações com voz funcionam no aparelho, " +
                        "sem downloads e sem contas. A voz usa o sintetizador do sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SerenoColors.Muted
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SerenoColors.SurfaceHi,
            title = { Text("Apagar misturas?") },
            text = { Text("Todas as combinações salvas serão removidas.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllMixes()
                    confirmDelete = false
                }) {
                    Text("Apagar", color = SerenoColors.Mint)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar", color = SerenoColors.Muted)
                }
            }
        )
    }

    if (showDiagnostics) {
        val outputs = audioOutputs(context)
        AlertDialog(
            onDismissRequest = { showDiagnostics = false },
            containerColor = SerenoColors.SurfaceHi,
            title = { Text("Diagnóstico de áudio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saída atual: $outputs")
                    Text("Voz do sistema: ${if (voiceReady) "pronta" else "não inicializada"}")
                    Text("Formato dos sons reais: 22050 Hz · estéreo · PCM 16 bits")
                    Text("Sons disponíveis: ${SoundCatalog.sounds.size}")
                    Text(
                        "Se a voz falhar, instale uma voz em português nos ajustes de texto do Android.",
                        color = SerenoColors.Muted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnostics = false }) {
                    Text("Fechar", color = SerenoColors.Mint)
                }
            }
        )
    }
}

private fun audioOutputs(context: Context): String {
    val manager = context.getSystemService(AudioManager::class.java) ?: return "desconhecida"
    val names = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { device ->
        when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "alto-falante"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "fone do aparelho"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "fone com fio"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
            else -> null
        }
    }.distinct()
    return names.ifEmpty { listOf("desconhecida") }.joinToString(", ")
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SerenoColors.Muted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SerenoColors.Bg,
                checkedTrackColor = SerenoColors.Mint,
                checkedBorderColor = SerenoColors.Mint,
                uncheckedThumbColor = SerenoColors.Muted,
                uncheckedTrackColor = SerenoColors.SurfaceHi,
                uncheckedBorderColor = SerenoColors.Outline
            )
        )
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
