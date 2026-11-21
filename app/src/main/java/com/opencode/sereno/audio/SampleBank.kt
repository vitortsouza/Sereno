package com.opencode.sereno.audio

import android.content.Context
import android.content.res.AssetManager
import android.util.LruCache
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PcmData(
    val samples: ShortArray,
    val sampleRate: Int,
    val channels: Int
)

object SampleBank {
    private const val TARGET_RMS = 0.06
    private const val PEAK_LIMIT = 0.88
    private const val MAX_GAIN = 8.0
    private lateinit var assets: AssetManager
    private val cache = object : LruCache<String, PcmData>(6) {}

    fun init(context: Context) {
        assets = context.assets
    }

    @Synchronized
    fun ensure(soundId: String): PcmData? {
        cache.get(soundId)?.let { return it }
        val data = runCatching {
            assets.open("sounds/$soundId.pcm").use { it.readBytes() }
        }.getOrNull() ?: return null
        val loaded = runCatching { normalize(data) }.getOrNull() ?: return null
        cache.put(soundId, loaded)
        return loaded
    }

    fun voice(soundId: String): Voice? = cache.get(soundId)?.let { PcmVoice(it) }

    fun bellVoice(soundId: String, gapSeconds: Float): Voice? =
        ensure(soundId)?.let { PcmBellVoice(it, gapSeconds) }

    private fun normalize(bytes: ByteArray): PcmData? {
        if (bytes.size < 8) return null
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        val frames = shorts.size / 2
        if (frames < 8) return null
        var meanL = 0.0
        var meanR = 0.0
        for (i in 0 until frames) {
            meanL += shorts[i * 2]
            meanR += shorts[i * 2 + 1]
        }
        meanL /= frames
        meanR /= frames
        var energy = 0.0
        var peak = 0.0
        for (i in 0 until frames) {
            val l = shorts[i * 2] - meanL
            val r = shorts[i * 2 + 1] - meanR
            energy += l * l + r * r
            peak = max(peak, max(abs(l), abs(r)))
        }
        if (energy <= 0.0 || peak <= 0.0) return null
        val rms = sqrt(energy / (frames * 2.0)) / 32768.0
        val gain = min(MAX_GAIN, min(TARGET_RMS / rms, PEAK_LIMIT * 32768.0 / peak))
        val out = ShortArray(shorts.size)
        for (i in 0 until frames) {
            out[i * 2] = ((shorts[i * 2] - meanL) * gain).roundToInt().coerceIn(-32768, 32767).toShort()
            out[i * 2 + 1] = ((shorts[i * 2 + 1] - meanR) * gain).roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return PcmData(out, 22050, 2)
    }
}

class PcmVoice(private val data: PcmData) : Voice {
    private val frames = data.samples.size / 2
    private val step = data.sampleRate.toDouble() / SAMPLE_RATE
    private val lfoStep = (2.0 * Math.PI * 0.018 / SAMPLE_RATE).toFloat()
    private var position = 0.0
    private var lfo = 0.0

    override fun render(left: FloatArray, right: FloatArray, framesOut: Int) {
        if (frames <= 0) return
        for (i in 0 until framesOut) {
            val index = position.toInt().coerceIn(0, frames - 1)
            val next = if (index + 1 >= frames) 0 else index + 1
            val frac = (position - index).toFloat()
            val l0 = data.samples[index * 2]
            val l1 = data.samples[next * 2]
            val r0 = data.samples[index * 2 + 1]
            val r1 = data.samples[next * 2 + 1]
            val gain = (0.965 + 0.035 * kotlin.math.sin(lfo)).toFloat()
            left[i] = ((l0 + (l1 - l0) * frac) / 32768f) * gain
            right[i] = ((r0 + (r1 - r0) * frac) / 32768f) * gain
            position += step
            if (position >= frames) position -= frames
            lfo += lfoStep
            if (lfo > 2.0 * Math.PI) lfo -= 2.0 * Math.PI
        }
    }
}

class PcmBellVoice(private val data: PcmData, gapSeconds: Float) : Voice {
    private val frames = data.samples.size / 2
    private val step = data.sampleRate.toDouble() / SAMPLE_RATE
    private val gapFrames = (gapSeconds.coerceAtLeast(0f) * SAMPLE_RATE).toInt()
    private var position = 0.0
    private var played = 0L
    private var silence = 0

    override fun render(left: FloatArray, right: FloatArray, framesOut: Int) {
        if (frames <= 0) return
        for (i in 0 until framesOut) {
            if (silence > 0) {
                left[i] = 0f
                right[i] = 0f
                silence--
                continue
            }
            if (played >= frames) {
                position = 0.0
                played = 0L
                silence = gapFrames
                left[i] = 0f
                right[i] = 0f
                continue
            }
            val index = position.toInt().coerceIn(0, frames - 1)
            val next = if (index + 1 >= frames) 0 else index + 1
            val frac = (position - index).toFloat()
            val l0 = data.samples[index * 2]
            val l1 = data.samples[next * 2]
            val r0 = data.samples[index * 2 + 1]
            val r1 = data.samples[next * 2 + 1]
            left[i] = (l0 + (l1 - l0) * frac) / 32768f
            right[i] = (r0 + (r1 - r0) * frac) / 32768f
            position += step
            played++
        }
    }
}
