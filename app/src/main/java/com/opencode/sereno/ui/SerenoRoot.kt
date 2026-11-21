package com.opencode.sereno.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.sereno.audio.SoundCategory
import com.opencode.sereno.data.MixStore
import com.opencode.sereno.data.JournalStore
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.playback.Player
import com.opencode.sereno.reminder.ReminderScheduler
import com.opencode.sereno.ui.screens.BreathScreen
import com.opencode.sereno.ui.screens.JournalScreen
import com.opencode.sereno.ui.screens.MeditateScreen
import com.opencode.sereno.ui.screens.SettingsScreen
import com.opencode.sereno.ui.screens.SoundsScreen
import com.opencode.sereno.ui.screens.TodayScreen
import com.opencode.sereno.ui.theme.SerenoColors
import kotlinx.coroutines.launch

@Composable
fun SerenoRoot() {
    val playerState by Player.state.collectAsStateWithLifecycle()
    val mixes by MixStore.mixes.collectAsStateWithLifecycle()
    val journalEntries by JournalStore.entries.collectAsStateWithLifecycle()
    val fadeSeconds by Prefs.fadeSeconds.collectAsStateWithLifecycle()
    val headphonesWarning by Prefs.headphonesWarning.collectAsStateWithLifecycle()
    val autoPlay by Prefs.autoPlay.collectAsStateWithLifecycle()
    val layersEnabled by Prefs.layersEnabled.collectAsStateWithLifecycle()
    val sleepMode by Prefs.sleepMode.collectAsStateWithLifecycle()
    val journalReminder by Prefs.journalReminder.collectAsStateWithLifecycle()
    val themeMode by Prefs.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    var categoryKey by rememberSaveable { mutableStateOf(SoundCategory.NATURE.key) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedCategory = SoundCategory.entries.firstOrNull { it.key == categoryKey }
        ?: SoundCategory.NATURE

    LaunchedEffect(playerState.warning) {
        val message = playerState.warning
        if (message != null) {
            snackbar.showSnackbar(message)
            Player.consumeWarning()
        }
    }

    BackHandler(enabled = showPlayer || showSettings) {
        if (showPlayer) showPlayer = false else showSettings = false
    }

    Scaffold(
        containerColor = SerenoColors.Bg,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SerenoColors.SurfaceHi,
                    contentColor = SerenoColors.Text,
                    actionColor = SerenoColors.Mint
                )
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .background(SerenoColors.Bg)
                    .navigationBarsPadding()
            ) {
                if (playerState.layers.isNotEmpty()) {
                    MiniPlayer(
                        state = playerState,
                        onToggle = { Player.togglePlayback() },
                        onClick = { showPlayer = true }
                    )
                }
                SerenoBottomBar(
                    tabs = Tab.entries,
                    current = tab,
                    onSelect = {
                        tab = it
                        showSettings = false
                    }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                showSettings -> SettingsScreen(
                    onBack = { showSettings = false },
                    fadeSeconds = fadeSeconds,
                    onFadeChange = { Player.setFadeSeconds(it) },
                    headphonesWarning = headphonesWarning,
                    onHeadphonesChange = { Prefs.setHeadphonesWarning(it) },
                    autoPlay = autoPlay,
                    onAutoPlayChange = { Prefs.setAutoPlay(it) },
                    layersEnabled = layersEnabled,
                    onLayersEnabledChange = { Player.onLayersEnabledChanged(it) },
                    sleepMode = sleepMode,
                    onSleepModeChange = { Prefs.setSleepMode(it) },
                    journalReminder = journalReminder,
                    onJournalReminderChange = { enabled ->
                        Prefs.setJournalReminder(enabled)
                        if (enabled) ReminderScheduler.schedule(context)
                        else ReminderScheduler.cancel(context)
                    },
                    themeMode = themeMode,
                    onThemeChange = { Prefs.setThemeMode(it) },
                    mixCount = mixes.size,
                    onDeleteAllMixes = {
                        MixStore.deleteAll()
                        Player.warn("Misturas apagadas")
                    }
                )

                tab == Tab.TODAY -> TodayScreen(
                    state = playerState,
                    layersEnabled = layersEnabled,
                    journalEntries = journalEntries,
                    onSettings = { showSettings = true },
                    onOpenBreath = { tab = Tab.BREATH },
                    onOpenJournal = { tab = Tab.JOURNAL },
                    onQuickMix = {
                        Player.playQuickMix(it)
                        showPlayer = true
                    },
                    onOpenPlayer = { showPlayer = true },
                    onOpenSounds = { category ->
                        if (category != null) categoryKey = category.key
                        tab = Tab.SOUNDS
                    },
                    onToggle = { Player.togglePlayback() },
                    onStop = { Player.stopPlayback() }
                )

                tab == Tab.SOUNDS -> SoundsScreen(
                    state = playerState,
                    layersEnabled = layersEnabled,
                    selected = selectedCategory,
                    onSelectCategory = { categoryKey = it.key },
                    onAddSound = { Player.addSound(it) },
                    onSettings = { showSettings = true },
                    onOpenPlayer = { showPlayer = true },
                    mixes = mixes,
                    onSaveMix = { Player.saveCurrentMix(it) },
                    onDeleteMix = { name ->
                        val removed = Player.deleteMix(name)
                        if (removed != null) {
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    message = "Mistura \"${removed.name}\" apagada",
                                    actionLabel = "Desfazer"
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    MixStore.restoreLast()
                                }
                            }
                        }
                    },
                    onRenameMix = { oldName, newName -> Player.renameMix(oldName, newName) },
                    onLoadMix = { Player.loadMix(it) }
                )

                tab == Tab.BREATH -> BreathScreen(
                    onSettings = { showSettings = true }
                )

                tab == Tab.MEDITATE -> MeditateScreen(
                    onSettings = { showSettings = true }
                )

                else -> JournalScreen(
                    entries = journalEntries,
                    onAdd = { text, mood -> JournalStore.add(text, mood) },
                    onDelete = { JournalStore.delete(it) },
                    onSettings = { showSettings = true }
                )
            }
        }
    }

    if (showPlayer) {
        PlayerSheet(
            state = playerState,
            layersEnabled = layersEnabled,
            onDismiss = { showPlayer = false },
            onToggle = { Player.togglePlayback() },
            onStop = {
                Player.stopPlayback()
                showPlayer = false
            },
            onSetTimer = { Player.setTimer(it) },
            onLayerGain = { uid, gain -> Player.setLayerGain(uid, gain) },
            onRemoveLayer = { Player.removeLayer(it) },
            onSave = {
                Player.saveCurrentMix(it)
                showPlayer = false
            },
            onAddLayer = {
                showPlayer = false
                tab = Tab.SOUNDS
            }
        )
    }
}
