package com.opencode.sereno.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

const val SAMPLE_RATE = 44100

interface Voice {
    fun render(left: FloatArray, right: FloatArray, frames: Int)
}

class OnePoleLowPass(fc: Float, private val fs: Int = SAMPLE_RATE) {
    private val cutoff = fc.coerceIn(20f, fs * 0.45f)
    private val a = (1.0 - exp(-2.0 * PI * cutoff / fs)).toFloat()
    private var y = 0f

    fun process(x: Float): Float {
        y += a * (x - y)
        return y
    }

    fun reset() {
        y = 0f
    }
}

class OnePoleHighPass(fc: Float, private val fs: Int = SAMPLE_RATE) {
    private val cutoff = fc.coerceIn(10f, fs * 0.45f)
    private val rc = 1f / (2f * PI.toFloat() * cutoff)
    private val a = rc / (rc + 1f / fs)
    private var px = 0f
    private var py = 0f

    fun process(x: Float): Float {
        val y = a * (py + x - px)
        px = x
        py = y
        return y
    }

    fun reset() {
        px = 0f
        py = 0f
    }
}

class Biquad(
    type: FilterType,
    fc: Float,
    q: Float = 0.707f,
    fs: Int = SAMPLE_RATE
) {
    enum class FilterType { LOW, HIGH, BAND }

    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var z1 = 0f
    private var z2 = 0f

    init {
        val f = fc.coerceIn(20f, fs * 0.45f)
        val w0 = 2.0 * PI * f / fs
        val cosw = cos(w0).toFloat()
        val alpha = (sin(w0) / (2.0 * q)).toFloat()
        val a0 = 1f + alpha
        when (type) {
            FilterType.LOW -> {
                b0 = (1f - cosw) / 2f
                b1 = 1f - cosw
                b2 = b0
            }
            FilterType.HIGH -> {
                b0 = (1f + cosw) / 2f
                b1 = -(1f + cosw)
                b2 = b0
            }
            FilterType.BAND -> {
                b0 = alpha
                b1 = 0f
                b2 = -alpha
            }
        }
        val norm = 1f / a0
        b0 *= norm
        b1 *= norm
        b2 *= norm
        a1 = (-2f * cosw) * norm
        a2 = (1f - alpha) * norm
    }

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
    }
}

class DelayBuffer(size: Int) {
    private val buffer = FloatArray(size.coerceAtLeast(1))
    private var index = 0

    fun read(delay: Int): Float {
        var i = index - delay
        while (i < 0) i += buffer.size
        return buffer[i % buffer.size]
    }

    fun write(value: Float) {
        buffer[index] = value
        index++
        if (index >= buffer.size) index = 0
    }
}

class StereoDelay(
    delayLeftMs: Float,
    delayRightMs: Float,
    private val feedback: Float = 0.35f,
    dampingHz: Float = 2200f,
    private val wet: Float = 0.25f
) {
    private val left = DelayBuffer((SAMPLE_RATE * delayLeftMs / 1000f).toInt() + 8)
    private val right = DelayBuffer((SAMPLE_RATE * delayRightMs / 1000f).toInt() + 8)
    private val delaySamplesLeft = (SAMPLE_RATE * delayLeftMs / 1000f).toInt()
    private val delaySamplesRight = (SAMPLE_RATE * delayRightMs / 1000f).toInt()
    private val dampL = OnePoleLowPass(dampingHz)
    private val dampR = OnePoleLowPass(dampingHz)

    fun process(inputLeft: Float, inputRight: Float, output: FloatArray) {
        val wetL = left.read(delaySamplesLeft)
        val wetR = right.read(delaySamplesRight)
        left.write(inputLeft + dampL.process(wetR) * feedback)
        right.write(inputRight + dampR.process(wetL) * feedback)
        output[0] = inputLeft + wetL * wet
        output[1] = inputRight + wetR * wet
    }
}

fun softClip(x: Float): Float = tanh(x * 1.15f) * 0.92f

fun panGains(pan: Float): FloatArray {
    val p = pan.coerceIn(-1f, 1f)
    return floatArrayOf(
        kotlin.math.sqrt((1f - p) * 0.5f),
        kotlin.math.sqrt((1f + p) * 0.5f)
    )
}
