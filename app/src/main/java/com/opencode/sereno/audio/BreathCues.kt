package com.opencode.sereno.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class BreathCue { INHALE, HOLD, EXHALE, COMPLETE }

object BreathCuePlayer {
    private val tracks = HashMap<BreathCue, AudioTrack>()

    fun play(cue: BreathCue, volume: Float = 0.8f) {
        val track = tracks[cue] ?: createTrack(cue)?.also { tracks[cue] = it } ?: return
        runCatching {
            track.setVolume(volume.coerceIn(0f, 1f))
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    fun release() {
        tracks.values.forEach {
            runCatching { it.stop() }
            it.release()
        }
        tracks.clear()
    }

    private fun createTrack(cue: BreathCue): AudioTrack? = runCatching {
        val samples = when (cue) {
            BreathCue.INHALE -> buildInhale()
            BreathCue.HOLD -> buildHold()
            BreathCue.EXHALE -> buildExhale()
            BreathCue.COMPLETE -> buildComplete()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        track
    }.getOrNull()

    private fun buildInhale(): ShortArray = render(0.62f) { out ->
        addChime(out, start = 0f, duration = 0.36f, freq = 523.25f, gain = 0.19f)
        addChime(out, start = 0.20f, duration = 0.42f, freq = 783.99f, gain = 0.17f)
    }

    private fun buildExhale(): ShortArray = render(0.64f) { out ->
        addChime(out, start = 0f, duration = 0.36f, freq = 783.99f, gain = 0.17f)
        addChime(out, start = 0.22f, duration = 0.42f, freq = 523.25f, gain = 0.19f)
    }

    private fun buildHold(): ShortArray = render(0.14f) { out ->
        val count = out.size
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val att = (t / 0.006f).coerceAtMost(1f)
            val env = att * exp(-t / 0.028f)
            val tone = sin(2.0 * PI * 1046.5 * t) + 0.32 * sin(2.0 * PI * 2093.0 * t)
            out[i] += (tone * env * 0.16f).toFloat()
        }
    }

    private fun buildComplete(): ShortArray = render(2.6f) { out ->
        addBowl(out, freq = 196.00f, gain = 0.11f, tau = 1.05f)
        addBowl(out, freq = 293.66f, gain = 0.055f, tau = 0.85f)
        addBowl(out, freq = 392.00f, gain = 0.03f, tau = 0.70f)
    }

    private fun render(seconds: Float, block: (FloatArray) -> Unit): ShortArray {
        val out = FloatArray((seconds * SAMPLE_RATE).toInt())
        block(out)
        return ShortArray(out.size) { i ->
            (out[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
    }

    private fun addChime(
        out: FloatArray,
        start: Float,
        duration: Float,
        freq: Float,
        gain: Float
    ) {
        val startIndex = (start * SAMPLE_RATE).toInt()
        val frames = (duration * SAMPLE_RATE).toInt()
        for (i in 0 until frames) {
            val index = startIndex + i
            if (index >= out.size) break
            val t = i.toFloat() / SAMPLE_RATE
            val attack = (t / 0.018f).coerceAtMost(1f)
            val env = attack * exp(-2.4 * t / duration)
            val phase = 2.0 * PI * freq * t
            val tone = sin(phase) + 0.24 * sin(phase * 2.0) + 0.06 * sin(phase * 3.0)
            out[index] += (tone * env * gain).toFloat()
        }
    }

    private fun addBowl(out: FloatArray, freq: Float, gain: Float, tau: Float) {
        for (i in out.indices) {
            val t = i.toFloat() / SAMPLE_RATE
            val attack = (t / 0.012f).coerceAtMost(1f)
            val env = attack * exp(-t / tau)
            val phase = 2.0 * PI * freq * t
            val tone = sin(phase) + 0.35 * sin(phase * 2.01) + 0.12 * sin(phase * 3.02)
            out[i] += (tone * env * gain).toFloat()
        }
    }
}

object BreathHaptics {
    fun play(context: Context, cue: BreathCue) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val timings: LongArray
        val amplitudes: IntArray
        when (cue) {
            BreathCue.INHALE -> {
                timings = longArrayOf(0, 55, 70, 55, 70, 70)
                amplitudes = intArrayOf(0, 60, 0, 95, 0, 135)
            }
            BreathCue.HOLD -> {
                timings = longArrayOf(0, 90)
                amplitudes = intArrayOf(0, 85)
            }
            BreathCue.EXHALE -> {
                timings = longArrayOf(0, 90, 80, 220)
                amplitudes = intArrayOf(0, 120, 0, 70)
            }
            BreathCue.COMPLETE -> {
                timings = longArrayOf(0, 140, 110, 90, 110, 220)
                amplitudes = intArrayOf(0, 80, 0, 110, 0, 150)
            }
        }
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        runCatching { vibrator.vibrate(effect) }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
