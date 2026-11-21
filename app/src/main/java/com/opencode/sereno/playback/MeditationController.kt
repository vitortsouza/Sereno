package com.opencode.sereno.playback

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.opencode.sereno.audio.AmbientBed
import com.opencode.sereno.audio.AmbientOptions
import com.opencode.sereno.audio.BreathCue
import com.opencode.sereno.audio.BreathCuePlayer
import com.opencode.sereno.audio.BreathHaptics
import com.opencode.sereno.audio.MeditationSession
import com.opencode.sereno.audio.offlinePtBr
import com.opencode.sereno.data.Prefs
import java.util.Locale
import kotlin.coroutines.resume
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
import kotlinx.coroutines.suspendCancellableCoroutine

enum class MeditationVoiceStatus { PREPARING, READY, UNAVAILABLE }

data class MeditationState(
    val sessionId: String = "",
    val sessionName: String = "",
    val running: Boolean = false,
    val finished: Boolean = false,
    val stepIndex: Int = 0,
    val elapsedMs: Long = 0L,
    val totalMs: Long = 0L,
    val voiceStatus: MeditationVoiceStatus = MeditationVoiceStatus.PREPARING,
    val voiceMessage: String = "Preparando voz…"
)

object MeditationController {
    private val _state = MutableStateFlow(MeditationState())
    val state: StateFlow<MeditationState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val focusToken = Any()
    private lateinit var appContext: Context
    private var ambientBed: AmbientBed? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var initialized = false
    private var session: MeditationSession? = null
    private var spokenStepIndex = -1
    private var spokenChunkCount = 0
    private var ambientId = "none"
    private var ambientVolume = 0.55f
    private var speechRate = 0.70f
    private var voiceName = ""
    private var elapsedBase = 0L
    private var elapsedStart = 0L
    private var timerJob: Job? = null
    private var stepJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        ambientBed = AmbientBed()
        PracticeFocus.register(focusToken) { stop() }
        initTts()
    }

    fun start(
        nextSession: MeditationSession,
        nextAmbientId: String,
        nextAmbientVolume: Float,
        nextRate: Float,
        nextVoice: String
    ) {
        if (!initialized) return
        val previous = _state.value
        val resume = previous.sessionId == nextSession.id && previous.elapsedMs > 0L && !previous.finished
        session = nextSession
        ambientId = nextAmbientId
        ambientVolume = nextAmbientVolume
        speechRate = nextRate
        voiceName = nextVoice
        tts?.setSpeechRate(nextRate)
        applyVoice()
        if (!resume) {
            elapsedBase = 0L
            spokenStepIndex = -1
            spokenChunkCount = 0
            _state.update {
                it.copy(
                    sessionId = nextSession.id,
                    sessionName = nextSession.name,
                    running = true,
                    finished = false,
                    stepIndex = 0,
                    elapsedMs = 0L,
                    totalMs = nextSession.totalMs
                )
            }
        } else {
            elapsedBase = previous.elapsedMs
            _state.update { it.copy(running = true, finished = false) }
        }
        Player.stopPlayback()
        startAmbient()
        acquireWakeLock()
        startService()
        startElapsed()
        startSteps()
    }

    fun pause() {
        if (!_state.value.running) return
        val elapsed = elapsedBase + (SystemClock.elapsedRealtime() - elapsedStart)
            .coerceAtLeast(0L)
        stopJobs()
        elapsedBase = elapsed.coerceAtMost(_state.value.totalMs)
        runCatching { tts?.stop() }
        ambientBed?.stop()
        releaseWakeLock()
        _state.update { it.copy(running = false, elapsedMs = elapsedBase) }
    }

    fun stop() {
        stopJobs()
        runCatching { tts?.stop() }
        ambientBed?.stop()
        releaseWakeLock()
        elapsedBase = 0L
        spokenStepIndex = -1
        spokenChunkCount = 0
        _state.update {
            it.copy(
                running = false,
                finished = false,
                stepIndex = 0,
                elapsedMs = 0L,
                sessionId = ""
            )
        }
        stopService()
    }

    fun toggle() {
        if (_state.value.running) {
            pause()
            return
        }
        val current = session ?: return
        if (_state.value.finished || _state.value.elapsedMs <= 0L) {
            start(current, ambientId, ambientVolume, speechRate, voiceName)
        } else {
            start(current, ambientId, ambientVolume, speechRate, voiceName)
        }
    }

    fun setRate(value: Float) {
        speechRate = value
        tts?.setSpeechRate(value)
    }

    fun setVoice(name: String) {
        voiceName = name
        applyVoice()
    }

    private fun startElapsed() {
        timerJob?.cancel()
        elapsedStart = SystemClock.elapsedRealtime()
        timerJob = scope.launch {
            while (_state.value.running) {
                val elapsed = elapsedBase + (SystemClock.elapsedRealtime() - elapsedStart)
                _state.update { it.copy(elapsedMs = elapsed.coerceAtMost(it.totalMs)) }
                delay(200)
            }
        }
    }

    private fun startSteps() {
        stepJob?.cancel()
        val currentSession = session ?: return
        val initialIndex = _state.value.stepIndex.coerceIn(0, currentSession.steps.lastIndex)
        val initialStepStart = currentSession.steps.take(initialIndex).sumOf { it.durationMs }
        val initialOffset = (elapsedBase - initialStepStart)
            .coerceIn(0L, currentSession.steps[initialIndex].durationMs)
        stepJob = scope.launch {
            var index = initialIndex
            while (_state.value.running) {
                _state.update { it.copy(stepIndex = index) }
                if (spokenStepIndex != index) {
                    spokenStepIndex = index
                    spokenChunkCount = 0
                }
                val step = currentSession.steps[index]
                val offset = if (index == initialIndex) initialOffset else 0L
                val stepStarted = SystemClock.elapsedRealtime() - offset
                if (ttsReady && tts != null) {
                    ambientBed?.setDucked(true)
                    try {
                        speakText(
                            tts!!,
                            step.text,
                            "med-${currentSession.id}-$index",
                            spokenChunkCount
                        ) { next -> spokenChunkCount = next }
                    } finally {
                        ambientBed?.setDucked(false)
                    }
                } else {
                    if (Prefs.breathHaptics.value) BreathHaptics.play(appContext, BreathCue.HOLD)
                    if (Prefs.breathCues.value) {
                        ambientBed?.setDucked(true)
                        try {
                            BreathCuePlayer.play(BreathCue.HOLD, Prefs.breathCueVolume.value)
                            delay(600)
                        } finally {
                            ambientBed?.setDucked(false)
                        }
                    }
                }
                if (!_state.value.running) break
                val remaining = step.durationMs - (SystemClock.elapsedRealtime() - stepStarted)
                if (remaining > 0L) delay(remaining)
                if (!_state.value.running) break
                if (index >= currentSession.steps.lastIndex) {
                    finish()
                    break
                }
                index += 1
            }
        }
    }

    private fun finish() {
        val total = _state.value.totalMs
        stopJobs()
        runCatching { tts?.stop() }
        ambientBed?.stop()
        releaseWakeLock()
        if (Prefs.breathHaptics.value) BreathHaptics.play(appContext, BreathCue.COMPLETE)
        if (Prefs.breathCues.value) {
            BreathCuePlayer.play(BreathCue.COMPLETE, Prefs.breathCueVolume.value)
        }
        _state.update {
            it.copy(
                running = false,
                finished = true,
                stepIndex = it.stepIndex,
                elapsedMs = total
            )
        }
        stopService()
    }

    private fun stopJobs() {
        timerJob?.cancel()
        timerJob = null
        stepJob?.cancel()
        stepJob = null
    }

    private fun startAmbient() {
        val option = AmbientOptions.firstOrNull { it.id == ambientId }
        if (option != null && option.id != "none") {
            ambientBed?.start(option.id, gain = option.level * ambientVolume)
        } else {
            ambientBed?.stop()
        }
    }

    private fun initTts() {
        val holder = arrayOfNulls<TextToSpeech>(1)
        val engine = TextToSpeech(appContext) { code ->
            val current = holder[0] ?: return@TextToSpeech
            if (code != TextToSpeech.SUCCESS) {
                ttsReady = false
                _state.update {
                    it.copy(
                        voiceStatus = MeditationVoiceStatus.UNAVAILABLE,
                        voiceMessage = "Voz indisponível. Modo silencioso."
                    )
                }
                return@TextToSpeech
            }
            val language = current.setLanguage(Locale.forLanguageTag("pt-BR"))
            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
                _state.update {
                    it.copy(
                        voiceStatus = MeditationVoiceStatus.UNAVAILABLE,
                        voiceMessage = "Voz offline do Brasil não instalada."
                    )
                }
                return@TextToSpeech
            }
            val available = runCatching { current.voices.orEmpty().offlinePtBr() }
                .getOrDefault(emptyList())
            if (available.isEmpty()) {
                ttsReady = false
                _state.update {
                    it.copy(
                        voiceStatus = MeditationVoiceStatus.UNAVAILABLE,
                        voiceMessage = "Instale uma voz offline do Brasil."
                    )
                }
                return@TextToSpeech
            }
            current.setSpeechRate(speechRate)
            current.setPitch(0.94f)
            ttsReady = true
            applyVoice()
            _state.update {
                it.copy(
                    voiceStatus = MeditationVoiceStatus.READY,
                    voiceMessage = "Voz offline do Brasil pronta."
                )
            }
        }
        holder[0] = engine
        tts = engine
    }

    private fun applyVoice() {
        val engine = tts ?: return
        val voice = runCatching {
            val available = engine.voices.orEmpty().offlinePtBr()
            available.firstOrNull { it.name == voiceName } ?: available.firstOrNull()
        }.getOrNull() ?: return
        runCatching { engine.voice = voice }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sereno:Meditation").apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startService() {
        val intent = Intent(appContext, MeditationService::class.java)
            .setAction(MeditationService.ACTION_START)
        runCatching { ContextCompat.startForegroundService(appContext, intent) }
    }

    private fun stopService() {
        runCatching { appContext.stopService(Intent(appContext, MeditationService::class.java)) }
    }
}

private val phraseBreak = Regex("(?<=[.!?;:])\\s+|(?<=,)\\s+")

private suspend fun speakText(
    engine: TextToSpeech,
    text: String,
    utteranceId: String,
    startChunk: Int,
    onChunkDone: (Int) -> Unit
) {
    val parts = text.split(phraseBreak).filter { it.isNotBlank() }
    for (index in startChunk.coerceAtLeast(0) until parts.size) {
        val part = parts[index]
        speakChunk(engine, part.trim(), "$utteranceId-$index")
        onChunkDone(index + 1)
        if (index != parts.lastIndex) {
            val pause = when {
                part.trimEnd().endsWith(",") -> 320L
                part.trimEnd().endsWith(";") || part.trimEnd().endsWith(":") -> 430L
                else -> 700L
            }
            delay(pause)
        }
    }
}

private suspend fun speakChunk(engine: TextToSpeech, text: String, utteranceId: String) {
    suspendCancellableCoroutine { continuation ->
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit

            override fun onDone(id: String?) {
                if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
            }
        })
        val result = runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }.getOrDefault(TextToSpeech.ERROR)
        if (result != TextToSpeech.SUCCESS && continuation.isActive) continuation.resume(Unit)
        continuation.invokeOnCancellation { runCatching { engine.stop() } }
    }
}
