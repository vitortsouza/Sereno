package com.opencode.sereno.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.Locale

data class VoiceOption(
    val name: String,
    val label: String,
    val quality: Int
)

object VoiceNarrator {
    val ready = MutableStateFlow(false)
    val voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val statusMessage = MutableStateFlow("Preparando voz…")

    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var initializing = false
    private var selectedVoiceName: String = ""
    private var rate: Float = 0.70f
    private var media: MediaPlayer? = null
    private var pendingId: String? = null
    private var pendingFile: File? = null
    private var pendingVolume: Float = 0.85f
    private val hiddenShortNames = setOf("PTE", "PT-BR-LANGUAGE")

    fun ensure(context: Context) {
        if (tts != null || initializing) return
        initializing = true
        statusMessage.value = "Preparando voz…"
        val holder = arrayOfNulls<TextToSpeech>(1)
        val engine = TextToSpeech(context.applicationContext) { code ->
            initializing = false
            val current = holder[0]
            if (code != TextToSpeech.SUCCESS || current == null) {
                ready.value = false
                statusMessage.value = "Voz indisponível neste aparelho."
                return@TextToSpeech
            }
            val language = current.setLanguage(Locale.forLanguageTag("pt-BR"))
            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                ready.value = false
                statusMessage.value = "Instale uma voz offline do Brasil nos ajustes do Android."
                return@TextToSpeech
            }
            current.setSpeechRate(rate)
            current.setPitch(0.94f)
            current.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    handler.post {
                        if (utteranceId == pendingId) {
                            val file = pendingFile
                            val volume = pendingVolume
                            pendingId = null
                            pendingFile = null
                            if (file != null && file.exists() && file.length() > 0L) {
                                playFile(file, volume)
                            }
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handler.post {
                        if (utteranceId == pendingId) {
                            pendingId = null
                            pendingFile = null
                        }
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handler.post {
                        if (utteranceId == pendingId) {
                            pendingId = null
                            pendingFile = null
                        }
                    }
                }
            })
            ready.value = true
            statusMessage.value = "Voz pronta."
            refreshVoices(current)
            applySelectedVoice(current)
        }
        holder[0] = engine
        tts = engine
    }

    fun setVoice(name: String) {
        selectedVoiceName = name
        tts?.let { applySelectedVoice(it) }
    }

    fun setRate(value: Float) {
        rate = value
        tts?.setSpeechRate(value)
    }

    fun play(context: Context, text: String, volume: Float) {
        ensure(context)
        val engine = tts ?: return
        if (!ready.value) return
        applySelectedVoice(engine)
        val dir = File(context.cacheDir, "voice-prompts").apply { mkdirs() }
        val voice = engine.voice?.name.orEmpty()
        val key = "$text|$voice|$rate".hashCode().toUInt().toString(16)
        val file = File(dir, "$key.wav")
        if (file.exists() && file.length() > 0L) {
            playFile(file, volume)
            return
        }
        runCatching {
            if (file.exists()) file.delete()
            val id = "sereno-${System.nanoTime()}"
            pendingId = id
            pendingFile = file
            pendingVolume = volume
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
            }
            val result = engine.synthesizeToFile(text, params, file, id)
            if (result != TextToSpeech.SUCCESS) {
                pendingId = null
                pendingFile = null
            }
        }.onFailure {
            pendingId = null
            pendingFile = null
        }
    }

    fun stop() {
        pendingId = null
        pendingFile = null
        runCatching { tts?.stop() }
        releaseMedia()
    }

    private fun applySelectedVoice(engine: TextToSpeech) {
        val voice = runCatching {
            val available = engine.voices.orEmpty().offlinePtBr()
            available.firstOrNull { it.name == selectedVoiceName }
                ?: available.firstOrNull { shortNameOf(it.name) == "PTE" }
                ?: available.firstOrNull()
        }.getOrNull() ?: return
        runCatching { engine.voice = voice }
    }

    private fun refreshVoices(engine: TextToSpeech) {
        val list = runCatching {
            engine.voices.orEmpty()
                .offlinePtBr()
                .map { voice ->
                    val tags = mutableListOf("Brasil")
                    if (voice.quality >= Voice.QUALITY_HIGH) tags += "neural"
                    tags += "offline"
                    val shortName = shortNameOf(voice.name)
                    VoiceOption(
                        name = voice.name,
                        label = "$shortName · ${tags.joinToString(" · ")}",
                        quality = voice.quality
                    )
                }
                .filter { it.label.substringBefore(" ·") !in hiddenShortNames }
                .sortedWith(
                    compareByDescending<VoiceOption> { it.quality }
                        .thenBy { it.label }
                )
        }.getOrDefault(emptyList())
        voices.value = list
        if (list.isEmpty()) {
            ready.value = false
            statusMessage.value = "Nenhuma voz offline do Brasil encontrada. Instale uma nos ajustes do Android."
        } else {
            ready.value = true
            statusMessage.value = "Voz offline do Brasil pronta."
        }
    }

    private fun playFile(file: File, volume: Float) {
        releaseMedia()
        val player = MediaPlayer()
        media = player
        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            player.setOnCompletionListener {
                releaseMedia()
            }
            player.prepare()
            player.start()
        }.onFailure {
            releaseMedia()
        }
    }

    private fun releaseMedia() {
        val current = media
        media = null
        if (current != null) {
            runCatching { current.stop() }
            current.release()
        }
    }
}

private fun shortNameOf(name: String): String = name
    .replace("pt-br-x-", "", ignoreCase = true)
    .replace("-local", "", ignoreCase = true)
    .uppercase(Locale.ROOT)
