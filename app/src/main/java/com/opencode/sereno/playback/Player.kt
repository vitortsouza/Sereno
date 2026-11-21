package com.opencode.sereno.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.opencode.sereno.audio.AudioEngine
import com.opencode.sereno.audio.LayerSpec
import com.opencode.sereno.audio.QuickMix
import com.opencode.sereno.audio.SampleBank
import com.opencode.sereno.audio.SoundCatalog
import com.opencode.sereno.data.MixStore
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.data.SavedLayer
import com.opencode.sereno.data.SavedMix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LayerUi(
    val uid: Long,
    val soundId: String,
    val gain: Float
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val layers: List<LayerUi> = emptyList(),
    val totalMs: Long = 15 * 60_000L,
    val remainingMs: Long = 15 * 60_000L,
    val fadeMs: Long = 20_000L,
    val warning: String? = null
)

object Player {

    private const val MAX_LAYERS = 3

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var engine: AudioEngine? = null
    private var session = 0
    private var nextUid = 1L
    private var deadline = 0L
    private var ticker: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    private var noisyRegistered = false
    private var initialized = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val audioManager: AudioManager
        get() = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> scope.launch { pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> engine?.setDucked(true)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> engine?.setDucked(true)
            AudioManager.AUDIOFOCUS_GAIN -> engine?.setDucked(false)
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        app = context.applicationContext
        Prefs.init(app)
        MixStore.init(app)
        _state.update {
            it.copy(
                fadeMs = Prefs.fadeSeconds.value * 1000L,
                totalMs = 15 * 60_000L,
                remainingMs = 15 * 60_000L
            )
        }
    }

    fun addSound(soundId: String) {
        val def = SoundCatalog.find(soundId) ?: return
        val st = _state.value
        val single = !Prefs.layersEnabled.value
        if (st.layers.any { it.soundId == soundId }) {
            warn("${def.name} já está na mistura")
            return
        }
        if (!single && st.layers.size >= MAX_LAYERS) {
            warn("Máximo de 3 camadas. Remova uma para adicionar outra.")
            return
        }
        val wasPlaying = st.isPlaying
        val wasPaused = st.isPaused
        val newLayer = LayerUi(nextUid++, soundId, 0.62f)
        val layers = if (single) listOf(newLayer) else st.layers + newLayer
        val needsWarning = def.headphone && Prefs.headphonesWarning.value && !hasHeadphones()

        if (wasPlaying) {
            SampleBank.ensure(soundId)
            _state.update { it.copy(layers = layers, isPaused = false) }
            engine?.update(specs(layers))
        } else {
            _state.update {
                it.copy(
                    layers = layers,
                    isPaused = wasPaused,
                    remainingMs = when {
                        wasPaused -> it.remainingMs
                        it.totalMs > 0 -> it.totalMs
                        else -> 0L
                    }
                )
            }
            if (Prefs.autoPlay.value) {
                startPlayback(resume = wasPaused)
            }
        }

        if (needsWarning) {
            warn("Use fones de ouvido para perceber as batidas binaurais")
        } else if (!wasPlaying && !Prefs.autoPlay.value) {
            warn("Som adicionado. Toque em reproduzir para ouvir.")
        }
    }

    fun onLayersEnabledChanged(enabled: Boolean) {
        Prefs.setLayersEnabled(enabled)
        if (!enabled) {
            val st = _state.value
            if (st.layers.size > 1) {
                val keep = st.layers.last()
                val layers = listOf(keep)
                _state.update { it.copy(layers = layers) }
                engine?.update(specs(layers))
                warn("Modo som único: mantivemos ${SoundCatalog.name(keep.soundId)}")
            }
        }
    }

    fun removeLayer(uid: Long) {
        val st = _state.value
        val layers = st.layers.filterNot { it.uid == uid }
        if (layers.isEmpty()) {
            stopPlayback()
            return
        }
        _state.update { it.copy(layers = layers) }
        engine?.update(specs(layers))
    }

    fun setLayerGain(uid: Long, gain: Float) {
        val value = gain.coerceIn(0f, 1f)
        val layers = _state.value.layers.map {
            if (it.uid == uid) it.copy(gain = value) else it
        }
        _state.update { it.copy(layers = layers) }
        engine?.update(specs(layers))
    }

    fun setTimer(durationMs: Long) {
        val value = durationMs.coerceAtLeast(0)
        _state.update { it.copy(totalMs = value, remainingMs = value) }
        if (_state.value.isPlaying) {
            deadline = if (value > 0) SystemClock.elapsedRealtime() + value else 0L
            engine?.setTimer(value)
        }
    }

    fun setFadeSeconds(seconds: Int) {
        Prefs.setFadeSeconds(seconds)
        val ms = seconds * 1000L
        _state.update { it.copy(fadeMs = ms) }
        engine?.setFade(ms)
    }

    fun togglePlayback() {
        if (_state.value.isPlaying) pause() else resumePlayback()
    }

    fun resumePlayback() {
        val st = _state.value
        if (st.layers.isEmpty()) return
        startPlayback(resume = st.isPaused)
    }

    fun pause() {
        val st = _state.value
        if (!st.isPlaying) return
        val remaining = currentRemaining()
        session++
        engine?.requestStop(320)
        engine = null
        deadline = 0L
        ticker?.cancel()
        ticker = null
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = true,
                remainingMs = if (it.totalMs > 0) remaining else 0
            )
        }
        abandonFocus()
        unregisterNoisy()
        releaseWakeLock()
    }

    fun stopPlayback() {
        session++
        engine?.requestStop(400)
        engine = null
        deadline = 0L
        ticker?.cancel()
        ticker = null
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                layers = emptyList(),
                remainingMs = it.totalMs
            )
        }
        abandonFocus()
        unregisterNoisy()
        releaseWakeLock()
        stopService()
    }

    fun playQuickMix(quick: QuickMix) {
        val limit = if (Prefs.layersEnabled.value) MAX_LAYERS else 1
        val layers = quick.soundIds.take(limit)
            .filter { SoundCatalog.find(it) != null }
            .map { LayerUi(nextUid++, it, 0.62f) }
        if (layers.isEmpty()) {
            warn("Esta mistura não está mais disponível")
            return
        }
        session++
        engine?.requestStop(250)
        engine = null
        ticker?.cancel()
        ticker = null
        val ms = quick.minutes * 60_000L
        _state.update {
            it.copy(
                layers = layers,
                totalMs = ms,
                remainingMs = ms,
                isPlaying = false,
                isPaused = false,
                warning = null
            )
        }
        startPlayback(resume = false)
        if (layers.any { SoundCatalog.find(it.soundId)?.headphone == true } &&
            Prefs.headphonesWarning.value && !hasHeadphones()
        ) {
            warn("Use fones de ouvido para perceber as batidas binaurais")
        }
    }

    fun loadMix(mix: SavedMix) {
        val limit = if (Prefs.layersEnabled.value) MAX_LAYERS else 1
        val layers = mix.layers
            .filter { SoundCatalog.find(it.soundId) != null }
            .take(limit)
            .map { LayerUi(nextUid++, it.soundId, it.gain.coerceIn(0f, 1f)) }
        if (layers.isEmpty()) {
            warn("Esta mistura não está mais disponível")
            return
        }
        session++
        engine?.requestStop(250)
        engine = null
        ticker?.cancel()
        ticker = null
        val ms = mix.durationMs.coerceAtLeast(0L)
        _state.update {
            it.copy(
                layers = layers,
                totalMs = ms,
                remainingMs = ms,
                isPlaying = false,
                isPaused = false,
                warning = null
            )
        }
        startPlayback(resume = false)
    }

    fun saveCurrentMix(name: String) {
        val st = _state.value
        if (st.layers.isEmpty()) return
        MixStore.save(
            name = name,
            durationMs = st.totalMs,
            layers = st.layers.map { SavedLayer(it.soundId, it.gain) }
        )
        warn("Mistura \"${name.trim().ifEmpty { "Mistura" }}\" salva")
    }

    fun deleteMix(name: String): SavedMix? = MixStore.delete(name)

    fun renameMix(oldName: String, newName: String) {
        MixStore.rename(oldName, newName)
    }

    fun consumeWarning() {
        _state.update { it.copy(warning = null) }
    }

    fun warn(message: String) {
        _state.update { it.copy(warning = message) }
    }

    fun hasHeadphones(): Boolean {
        return runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> true
                    else -> false
                }
            }
        }.getOrDefault(false)
    }

    private fun startPlayback(resume: Boolean) {
        val st = _state.value
        if (st.layers.isEmpty()) return
        PracticeFocus.stopActive()
        st.layers.forEach { SampleBank.ensure(it.soundId) }
        if (!requestFocus()) {
            warn("Não foi possível iniciar o áudio agora")
            return
        }
        val duration = when {
            st.totalMs <= 0 -> 0L
            resume && st.remainingMs > 0 -> st.remainingMs
            else -> st.totalMs
        }
        val sid = ++session
        deadline = if (duration > 0) SystemClock.elapsedRealtime() + duration else 0L
        val eng = AudioEngine { timerFinished ->
            scope.launch { onEngineEnded(sid, timerFinished) }
        }
        engine = eng
        eng.start(specs(st.layers), duration, st.fadeMs)
        _state.update {
            it.copy(
                isPlaying = true,
                isPaused = false,
                remainingMs = if (it.totalMs > 0) duration else 0,
                warning = null
            )
        }
        ensureService()
        registerNoisy()
        acquireWakeLock()
        startTicker()
    }

    private fun onEngineEnded(sid: Int, timerFinished: Boolean) {
        if (sid != session) return
        engine = null
        deadline = 0L
        ticker?.cancel()
        ticker = null
        abandonFocus()
        unregisterNoisy()
        releaseWakeLock()
        val sleep = timerFinished && Prefs.sleepMode.value
        _state.update {
            if (sleep) {
                it.copy(
                    isPlaying = false,
                    isPaused = false,
                    layers = emptyList(),
                    remainingMs = it.totalMs
                )
            } else {
                it.copy(
                    isPlaying = false,
                    isPaused = it.layers.isNotEmpty(),
                    remainingMs = it.totalMs
                )
            }
        }
        if (timerFinished) {
            if (sleep) warn("Boa noite. O som foi encerrado.")
            stopService()
        } else {
            warn("A reprodução foi encerrada")
            stopService()
        }
    }

    private fun currentRemaining(): Long {
        val st = _state.value
        if (!st.isPlaying) return st.remainingMs
        if (st.totalMs <= 0) return 0L
        return (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(500)
                val st = _state.value
                if (!st.isPlaying) break
                if (st.totalMs <= 0) continue
                val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                _state.update { it.copy(remainingMs = remaining) }
            }
        }
    }

    private fun specs(layers: List<LayerUi>): List<LayerSpec> =
        layers.map { LayerSpec(it.uid, it.soundId, it.gain) }

    private fun requestFocus(): Boolean {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
    }

    private fun registerNoisy() {
        if (noisyRegistered) return
        noisyRegistered = true
        runCatching {
            ContextCompat.registerReceiver(
                app,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        noisyRegistered = false
        runCatching { app.unregisterReceiver(noisyReceiver) }
    }

    private fun ensureService() {
        runCatching {
            val intent = Intent(app, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_START)
            ContextCompat.startForegroundService(app, intent)
        }
    }

    private fun stopService() {
        runCatching { app.stopService(Intent(app, PlaybackService::class.java)) }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        runCatching {
            val power = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sereno:audio")
                .apply {
                    setReferenceCounted(false)
                    acquire(12 * 60 * 60 * 1000L)
                }
        }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }
}
