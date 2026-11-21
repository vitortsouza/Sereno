package com.opencode.sereno.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock

data class LayerSpec(
    val uid: Long,
    val soundId: String,
    val gain: Float
)

class AudioEngine(private val onEnded: (timerFinished: Boolean) -> Unit) {

    private val blockSize = 1024
    private val voices = LinkedHashMap<Long, ActiveVoice>()
    private val scratchL = FloatArray(blockSize)
    private val scratchR = FloatArray(blockSize)
    private val mixL = FloatArray(blockSize)
    private val mixR = FloatArray(blockSize)
    private val interleaved = FloatArray(blockSize * 2)

    @Volatile private var pending: List<LayerSpec>? = null
    @Volatile private var endAt = 0L
    @Volatile private var stopAt = 0L
    @Volatile private var fadeMs = 20_000L
    @Volatile private var ducked = false
    @Volatile private var started = false

    private var masterEnv = 0f
    private var thread: Thread? = null

    private class ActiveVoice(
        val uid: Long,
        val voice: Voice,
        var gain: Float,
        var env: Float = 0f,
        var removing: Boolean = false
    )

    fun start(initial: List<LayerSpec>, durationMs: Long, fadeMs: Long) {
        if (started) return
        started = true
        this.fadeMs = fadeMs.coerceAtLeast(1_000L)
        pending = initial
        endAt = if (durationMs > 0) SystemClock.elapsedRealtime() + durationMs else 0L
        thread = Thread({ loop() }, "sereno-audio").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    fun update(specs: List<LayerSpec>) {
        pending = specs
    }

    fun setTimer(remainingMs: Long) {
        endAt = if (remainingMs > 0) SystemClock.elapsedRealtime() + remainingMs else 0L
    }

    fun setFade(ms: Long) {
        fadeMs = ms.coerceAtLeast(1_000L)
    }

    fun setDucked(value: Boolean) {
        ducked = value
    }

    fun requestStop(fade: Long = 320L) {
        stopAt = SystemClock.elapsedRealtime() + fade
    }

    fun stopImmediately() {
        stopAt = SystemClock.elapsedRealtime() + 40L
    }

    private fun loop() {
        var finishedByTimer = false
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferBytes = maxOf(minBuffer, blockSize * 2 * 4 * 3)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } catch (_: Throwable) {
            onEnded(false)
            return
        }

        try {
            track.play()
            while (true) {
                pending?.let {
                    pending = null
                    applySpecs(it)
                }

                val now = SystemClock.elapsedRealtime()
                var timerGain = 1f
                if (endAt > 0L) {
                    val remaining = endAt - now
                    if (remaining <= 0L) {
                        finishedByTimer = true
                        break
                    }
                    timerGain = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
                }

                var stopGain = 1f
                if (stopAt > 0L) {
                    val remaining = stopAt - now
                    if (remaining <= 0L) break
                    stopGain = (remaining / 320f).coerceIn(0f, 1f)
                }

                renderBlock(timerGain * stopGain)
                val written = track.write(interleaved, 0, blockSize * 2, AudioTrack.WRITE_BLOCKING)
                if (written < 0) break
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { track.pause() }
            runCatching { track.flush() }
            runCatching { track.release() }
            onEnded(finishedByTimer)
        }
    }

    private fun applySpecs(specs: List<LayerSpec>) {
        val wanted = HashSet<Long>(specs.size * 2)
        for (spec in specs) {
            wanted.add(spec.uid)
            val existing = voices[spec.uid]
            if (existing == null) {
                val voice = SoundCatalog.voiceFor(spec.soundId) ?: continue
                voices[spec.uid] = ActiveVoice(spec.uid, voice, spec.gain.coerceIn(0f, 1f))
            } else {
                existing.gain = spec.gain.coerceIn(0f, 1f)
                existing.removing = false
            }
        }
        for (voice in voices.values) {
            if (voice.uid !in wanted) voice.removing = true
        }
    }

    private fun renderBlock(scale: Float) {
        java.util.Arrays.fill(mixL, 0f)
        java.util.Arrays.fill(mixR, 0f)

        val iterator = voices.values.iterator()
        while (iterator.hasNext()) {
            val active = iterator.next()
            if (active.removing && active.env < 0.003f) {
                iterator.remove()
                continue
            }
            java.util.Arrays.fill(scratchL, 0f)
            java.util.Arrays.fill(scratchR, 0f)
            runCatching { active.voice.render(scratchL, scratchR, blockSize) }
            val target = if (active.removing) 0f else active.gain
            active.env += (target - active.env) * 0.12f
            val g = active.env
            if (g <= 0.0001f) continue
            for (i in 0 until blockSize) {
                mixL[i] += scratchL[i] * g
                mixR[i] += scratchR[i] * g
            }
        }

        val masterTarget = if (ducked) 0.26f else 0.9f
        masterEnv += (masterTarget - masterEnv) * 0.1f
        val master = masterEnv * scale
        for (i in 0 until blockSize) {
            interleaved[i * 2] = softClip(mixL[i] * master)
            interleaved[i * 2 + 1] = softClip(mixR[i] * master)
        }
    }
}
