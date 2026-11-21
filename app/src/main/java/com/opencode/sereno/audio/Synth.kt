package com.opencode.sereno.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class NoiseColor { WHITE, PINK, BROWN, GREEN, BLUE, GRAY }

class NoiseSource(private val color: NoiseColor, seed: Int) {
    private val rnd = Random(seed)
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var b3 = 0f
    private var b4 = 0f
    private var b5 = 0f
    private var b6 = 0f
    private var brown = 0f
    private var prevWhite = 0f
    private val band = Biquad(Biquad.FilterType.BAND, 520f, 0.8f)
    private val high = OnePoleHighPass(900f)
    private val grayHigh = OnePoleHighPass(280f)
    private val grayLow = OnePoleLowPass(6200f)

    fun next(): Float {
        val w = rnd.nextFloat() * 2f - 1f
        return when (color) {
            NoiseColor.WHITE -> w * 0.22f
            NoiseColor.PINK -> {
                b0 = 0.99886f * b0 + w * 0.0555179f
                b1 = 0.99332f * b1 + w * 0.0750759f
                b2 = 0.96900f * b2 + w * 0.1538520f
                b3 = 0.86650f * b3 + w * 0.3104856f
                b4 = 0.55000f * b4 + w * 0.5329522f
                b5 = -0.7616f * b5 - w * 0.0168980f
                val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f
                b6 = w * 0.115926f
                pink * 0.22f
            }
            NoiseColor.BROWN -> {
                brown = (brown + 0.02f * w) / 1.02f
                brown * 1.05f
            }
            NoiseColor.GREEN -> band.process(w) * 0.62f
            NoiseColor.BLUE -> high.process(w) * 0.24f
            NoiseColor.GRAY -> (grayLow.process(grayHigh.process(w)) * 0.8f + w * 0.03f) * 0.42f
        }
    }
}

class NoiseVoice(color: NoiseColor, seed: Int) : Voice {
    private val left = NoiseSource(color, seed)
    private val right = NoiseSource(color, seed * 31 + 7)

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            leftOut[i] = left.next()
            rightOut[i] = right.next()
        }
    }
}

class BinauralVoice(carrierHz: Float, beatHz: Float, seed: Int) : Voice {
    private val left = NoiseSource(NoiseColor.PINK, seed)
    private val right = NoiseSource(NoiseColor.PINK, seed + 11)
    private var phaseL = 0f
    private var phaseR = 0f
    private val incL = (2.0 * PI * (carrierHz - beatHz * 0.5) / SAMPLE_RATE).toFloat()
    private val incR = (2.0 * PI * (carrierHz + beatHz * 0.5) / SAMPLE_RATE).toFloat()

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            leftOut[i] = sin(phaseL) * 0.16f + left.next() * 0.01f
            rightOut[i] = sin(phaseR) * 0.16f + right.next() * 0.01f
            phaseL += incL
            phaseR += incR
            if (phaseL > 6.2831855f) phaseL -= 6.2831855f
            if (phaseR > 6.2831855f) phaseR -= 6.2831855f
        }
    }
}

class SolfeggioVoice(freqHz: Float) : Voice {
    private var p1 = 0f
    private var p2 = 0f
    private var p3 = 0f
    private var tremolo = 0f
    private val i1 = (2.0 * PI * freqHz / SAMPLE_RATE).toFloat()
    private val i2 = (2.0 * PI * freqHz * 2.0 / SAMPLE_RATE).toFloat()
    private val i3 = (2.0 * PI * freqHz * 3.0 / SAMPLE_RATE).toFloat()

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            tremolo += 0.000035f
            val env = 0.86f + 0.14f * sin(tremolo)
            val v = (sin(p1) + sin(p2) * 0.09f + sin(p3) * 0.03f) * env * 0.17f
            leftOut[i] = v
            rightOut[i] = v
            p1 += i1
            p2 += i2
            p3 += i3
            if (p1 > 6.2831855f) p1 -= 6.2831855f
            if (p2 > 6.2831855f) p2 -= 6.2831855f
            if (p3 > 6.2831855f) p3 -= 6.2831855f
        }
    }
}

class RainVoice(
    private val intensity: Float,
    private val brightness: Float,
    private val dropRate: Float,
    private val roof: Boolean,
    private val leaves: Boolean,
    seed: Int
) : Voice {
    private val rnd = Random(seed)
    private val noise = Array(2) { NoiseSource(NoiseColor.WHITE, seed + it * 97) }
    private val lp = Array(2) { OnePoleLowPass(brightness) }
    private val hp = Array(2) { OnePoleHighPass(180f) }
    private val body = Array(2) { OnePoleLowPass(if (roof) 520f else 260f) }
    private val rustle = Array(2) { OnePoleHighPass(5200f) }
    private val lfo = floatArrayOf(0f, 1.9f)
    private val drops = Array(16) { Drop() }
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE

    private class Drop {
        var phase = 0f
        var inc = 0f
        var env = 0f
        var decay = 0f
        var gain = 0f
        var panL = 0f
        var panR = 0f
    }

    private fun spawn() {
        val d = drops[rnd.nextInt(drops.size)]
        val freq = when {
            roof -> 700f + rnd.nextFloat() * 1400f
            leaves -> 2400f + rnd.nextFloat() * 2800f
            else -> 1400f + rnd.nextFloat() * 2600f
        }
        d.phase = 0f
        d.inc = (2.0 * PI * freq / SAMPLE_RATE).toFloat()
        d.env = 1f
        val tau = if (roof) 0.018f + rnd.nextFloat() * 0.03f else 0.03f + rnd.nextFloat() * 0.05f
        d.decay = exp(-1.0 / (tau * SAMPLE_RATE)).toFloat()
        d.gain = (if (roof) 0.24f else 0.13f) * (0.6f + rnd.nextFloat() * 0.8f)
        val pan = rnd.nextFloat() * 2f - 1f
        val gains = panGains(pan)
        d.panL = gains[0]
        d.panR = gains[1]
    }

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            if (rnd.nextFloat() < dropRate * dt) spawn()
            var sl = 0f
            var sr = 0f
            for (ch in 0..1) {
                val w = noise[ch].next() * 4.6f
                var s = hp[ch].process(lp[ch].process(w)) * 1.6f
                s *= 0.8f + 0.2f * sin(2.0 * PI * 0.07 * time + lfo[ch]).toFloat()
                s += body[ch].process(w) * (if (roof) 0.22f else 0.1f)
                if (leaves) s += rustle[ch].process(w) * 0.26f
                if (ch == 0) sl = s else sr = s
            }
            for (d in drops) {
                if (d.env > 0.0006f) {
                    d.phase += d.inc
                    d.env *= d.decay
                    val v = sin(d.phase) * d.env * d.gain
                    sl += v * d.panL
                    sr += v * d.panR
                }
            }
            leftOut[i] = sl * intensity
            rightOut[i] = sr * intensity
        }
    }
}

class OceanVoice(
    private val swellHz: Float,
    private val swellHz2: Float,
    private val slow: Boolean,
    seed: Int
) : Voice {
    private val rnd = Random(seed)
    private val noise = Array(2) { NoiseSource(NoiseColor.WHITE, seed + it * 53) }
    private val brown = FloatArray(2)
    private val lp = Array(2) { OnePoleLowPass(560f) }
    private val surf = Array(2) { Biquad(Biquad.FilterType.BAND, 820f, 0.6f) }
    private val phases = floatArrayOf(0f, 1.35f)
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE
    private val amp = if (slow) 0.55f else 0.8f

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            var sl = 0f
            var sr = 0f
            for (ch in 0..1) {
                val w = noise[ch].next() * 4.6f
                brown[ch] = (brown[ch] + 0.02f * w) / 1.02f
                val base = lp[ch].process(brown[ch] * 3.6f)
                val swell = 0.5f + 0.5f * sin(2.0 * PI * swellHz * time + phases[ch]).toFloat()
                val swell2 = 0.5f + 0.5f * sin(2.0 * PI * swellHz2 * time + phases[ch] * 0.7f).toFloat()
                val wash = surf[ch].process(w) * (0.12f + swell * swell2 * 1.5f)
                val v = (base * (0.4f + 0.5f * swell) + wash * 0.34f) * amp
                if (ch == 0) sl = v else sr = v
            }
            leftOut[i] = sl
            rightOut[i] = sr
        }
    }
}

class RiverVoice(intensity: Float, seed: Int) : Voice {
    private val strength = intensity
    private val noise = Array(2) { NoiseSource(NoiseColor.PINK, seed + it * 41) }
    private val low = Array(2) { Biquad(Biquad.FilterType.BAND, 620f, 0.55f) }
    private val high = Array(2) { Biquad(Biquad.FilterType.BAND, 2100f, 0.9f) }
    private val rnd = Random(seed + 5)
    private val bubbles = Array(10) { Bubble() }
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE

    private class Bubble {
        var phase = 0f
        var inc = 0f
        var env = 0f
        var decay = 0f
        var panL = 0f
        var panR = 0f
    }

    private fun spawn() {
        val b = bubbles[rnd.nextInt(bubbles.size)]
        val f = 520f + rnd.nextFloat() * 900f
        b.phase = 0f
        b.inc = (2.0 * PI * f / SAMPLE_RATE).toFloat()
        b.env = 1f
        b.decay = exp(-1.0 / (0.05f * SAMPLE_RATE)).toFloat()
        val g = panGains(rnd.nextFloat() * 2f - 1f)
        b.panL = g[0] * 0.055f
        b.panR = g[1] * 0.055f
    }

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            if (rnd.nextFloat() < 5.5f * dt) spawn()
            var sl = 0f
            var sr = 0f
            for (ch in 0..1) {
                val w = noise[ch].next() * 3.4f
                val l = low[ch].process(w)
                val h = high[ch].process(w) * (0.65f + 0.35f * sin(0.9 * time + ch).toFloat())
                val v = (l * 0.75f + h * 0.3f) * strength
                if (ch == 0) sl = v else sr = v
            }
            for (b in bubbles) {
                if (b.env > 0.0008f) {
                    b.phase += b.inc
                    b.env *= b.decay
                    val v = sin(b.phase) * b.env
                    sl += v * b.panL
                    sr += v * b.panR
                }
            }
            leftOut[i] = sl
            rightOut[i] = sr
        }
    }
}

class WaterfallVoice(seed: Int) : Voice {
    private val noise = Array(2) { NoiseSource(NoiseColor.WHITE, seed + it * 67) }
    private val pink = Array(2) { NoiseSource(NoiseColor.PINK, seed + 200 + it * 67) }
    private val body = Array(2) { OnePoleLowPass(340f) }
    private val band = Array(2) { Biquad(Biquad.FilterType.BAND, 1100f, 0.5f) }
    private val hiss = Array(2) { OnePoleLowPass(6200f) }
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            var sl = 0f
            var sr = 0f
            for (ch in 0..1) {
                val w = noise[ch].next() * 4.6f
                val p = pink[ch].next() * 6f
                val low = body[ch].process(p)
                val mid = band[ch].process(w)
                val air = hiss[ch].process(w)
                val v = (low * 1.1f + mid * 0.55f + air * 0.18f) *
                    0.8f * (0.9f + 0.1f * sin(0.4 * time + ch).toFloat())
                if (ch == 0) sl = v else sr = v
            }
            leftOut[i] = sl
            rightOut[i] = sr
        }
    }
}

class WindVoice(seed: Int) : Voice {
    private val brown = Array(2) { NoiseSource(NoiseColor.BROWN, seed + it * 29) }
    private val white = Array(2) { NoiseSource(NoiseColor.WHITE, seed + 99 + it * 29) }
    private val low = Array(2) { OnePoleLowPass(420f) }
    private val whistle = Array(2) { Biquad(Biquad.FilterType.BAND, 760f, 4.5f) }
    private val phases = floatArrayOf(0f, 2.1f)
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            var sl = 0f
            var sr = 0f
            for (ch in 0..1) {
                val gust = 0.5f + 0.5f * sin(2.0 * PI * 0.055 * time + phases[ch]).toFloat()
                val gust2 = 0.5f + 0.5f * sin(2.0 * PI * 0.019 * time + phases[ch] * 1.6f).toFloat()
                val base = low[ch].process(brown[ch].next() * 3f) * (0.35f + gust * 0.7f)
                val air = whistle[ch].process(white[ch].next() * 4.6f) *
                    (0.5f + gust2 * 0.6f) * 0.09f
                val v = (base + air) * 0.9f
                if (ch == 0) sl = v else sr = v
            }
            leftOut[i] = sl
            rightOut[i] = sr
        }
    }
}

class FireVoice(
    private val rate: Float,
    seed: Int
) : Voice {
    private val rnd = Random(seed)
    private val base = Array(2) { NoiseSource(NoiseColor.BROWN, seed + it * 73) }
    private val baseLp = Array(2) { OnePoleLowPass(300f) }
    private val hiss = Array(2) { NoiseSource(NoiseColor.WHITE, seed + 300 + it * 73) }
    private val hissLp = Array(2) { OnePoleLowPass(2600f) }
    private val crackles = Array(14) { index ->
        Crackle(
            freq = 1200f + index * 210f,
            tau = 0.018f + (index % 5) * 0.012f,
            gain = 0.18f + (index % 4) * 0.09f,
            pan = Random(seed + index * 13).nextFloat() * 2f - 1f
        )
    }
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE

    private class Crackle(freq: Float, tau: Float, val gain: Float, pan: Float) {
        val filter = Biquad(Biquad.FilterType.BAND, freq, 1.4f)
        val decay = exp(-1.0 / (tau * SAMPLE_RATE)).toFloat()
        val panL: Float
        val panR: Float
        var env = 0f

        init {
            val g = panGains(pan)
            panL = g[0]
            panR = g[1]
        }
    }

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            if (rnd.nextFloat() < rate * dt) {
                crackles[rnd.nextInt(crackles.size)].env = 1f
            }
            var sl = 0f
            var sr = 0f
            for (ch in 0..1) {
                val b = baseLp[ch].process(base[ch].next() * 3f)
                val h = hissLp[ch].process(hiss[ch].next() * 4.6f)
                val v = (b * 0.7f + h * 0.035f) * 0.9f
                if (ch == 0) sl = v else sr = v
            }
            for (c in crackles) {
                if (c.env > 0.0008f) {
                    val n = (rnd.nextFloat() * 2f - 1f) * c.env
                    val v = c.filter.process(n) * c.gain * 2.4f
                    c.env *= c.decay
                    sl += v * c.panL
                    sr += v * c.panR
                }
            }
            leftOut[i] = sl
            rightOut[i] = sr
        }
    }
}

class PadVoice(
    frequencies: FloatArray,
    private val seed: Int
) : Voice {
    private val count = frequencies.size
    private val phases = FloatArray(count) { Random(seed + it).nextFloat() * 6.283f }
    private val lfoPhases = FloatArray(count) { Random(seed + 50 + it).nextFloat() * 6.283f }
    private val lfoRates = FloatArray(count) { 0.018f + Random(seed + 90 + it).nextFloat() * 0.05f }
    private val pans = FloatArray(count) { (it.toFloat() / (count - 1).coerceAtLeast(1)) * 1.4f - 0.7f }
    private val detunes = FloatArray(count) { 0.9985f + Random(seed + 140 + it).nextFloat() * 0.003f }
    private val incs = FloatArray(count) { (2.0 * PI * frequencies[it] * detunes[it] / SAMPLE_RATE).toFloat() }
    private val panL = FloatArray(count) { panGains(pans[it])[0] }
    private val panR = FloatArray(count) { panGains(pans[it])[1] }
    private val lows = Array(2) { OnePoleLowPass(900f) }
    private val delay = StereoDelay(215f, 287f, feedback = 0.32f, dampingHz = 1800f, wet = 0.3f)
    private val delayOut = FloatArray(2)
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            var l = 0f
            var r = 0f
            for (v in 0 until count) {
                phases[v] += incs[v]
                if (phases[v] > 6.2831855f) phases[v] -= 6.2831855f
                lfoPhases[v] += lfoRates[v] * dt * 6.2831855f
                val amp = 0.034f * (0.72f + 0.28f * sin(lfoPhases[v]))
                val s = sin(phases[v]) * amp
                l += s * panL[v]
                r += s * panR[v]
            }
            l = lows[0].process(l)
            r = lows[1].process(r)
            delay.process(l, r, delayOut)
            leftOut[i] = delayOut[0]
            rightOut[i] = delayOut[1]
        }
    }
}

class BowlVoice(
    private val baseFreq: Float,
    private val ratios: FloatArray,
    private val amps: FloatArray,
    private val taus: FloatArray,
    private val minInterval: Float,
    private val maxInterval: Float,
    private val strikeGain: Float,
    seed: Int
) : Voice {
    private val rnd = Random(seed)
    private val count = ratios.size
    private val phases = FloatArray(count)
    private val detunes = FloatArray(count)
    private val incs = FloatArray(count)
    private val envs = FloatArray(count)
    private val decays = FloatArray(count) { exp(-1.0 / (taus[it] * SAMPLE_RATE)).toFloat() }
    private var hitTimer = 0.8f
    private var panL = 0.707f
    private var panR = 0.707f
    private var malletEnv = 0f
    private var malletPhase = 0f
    private var malletInc = 0f
    private val malletDecay = exp(-1.0 / (0.03f * SAMPLE_RATE)).toFloat()
    private val malletLp = OnePoleLowPass(2800f)
    private var center = 0f
    private val dt = 1f / SAMPLE_RATE

    private fun strike() {
        for (i in 0 until count) {
            phases[i] = rnd.nextFloat() * 6.2831855f
            detunes[i] = 0.9985f + rnd.nextFloat() * 0.003f
            incs[i] = (2.0 * PI * baseFreq * ratios[i] * detunes[i] / SAMPLE_RATE).toFloat()
            envs[i] = 1f
        }
        val g = panGains(rnd.nextFloat() * 2f - 1f)
        panL = g[0]
        panR = g[1]
        malletPhase = 0f
        malletInc = (2.0 * PI * (baseFreq * 4.2) / SAMPLE_RATE).toFloat()
        malletEnv = 1f
        hitTimer = minInterval + rnd.nextFloat() * (maxInterval - minInterval)
    }

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (hitTimer <= 0f) strike()
            hitTimer -= dt
            var s = 0f
            for (p in 0 until count) {
                phases[p] += incs[p]
                if (phases[p] > 6.2831855f) phases[p] -= 6.2831855f
                envs[p] *= decays[p]
                s += sin(phases[p]) * envs[p] * amps[p]
            }
            var mallet = 0f
            if (malletEnv > 0.0008f) {
                malletPhase += malletInc
                malletEnv *= malletDecay
                mallet = malletLp.process((rnd.nextFloat() * 2f - 1f) * malletEnv)
            }
            val total = (s * strikeGain + mallet * 0.12f)
            center = total * 0.22f
            leftOut[i] = total * panL + center
            rightOut[i] = total * panR + center
        }
    }
}

class PluckVoice(
    private val notes: FloatArray,
    private val minInterval: Float,
    private val maxInterval: Float,
    private val damping: Float,
    private val amplitude: Float,
    private val tauSeconds: Float,
    seed: Int
) : Voice {
    private val rnd = Random(seed)
    private val strings = Array(4) { StringVoice() }
    private var next = 0
    private var timer = 0.2f
    private var time = 0f
    private val dt = 1f / SAMPLE_RATE
    private val envDecay = exp(-1.0 / (tauSeconds * SAMPLE_RATE)).toFloat()

    private class StringVoice {
        val buffer = FloatArray(1200)
        var length = 400
        var index = 0
        var env = 0f
        var panL = 0.707f
        var panR = 0.707f
    }

    private fun pluck() {
        val freq = notes[rnd.nextInt(notes.size)] *
            (if (rnd.nextFloat() < 0.25f) 0.5f else 1f)
        val v = strings[next]
        next = (next + 1) % strings.size
        v.length = (SAMPLE_RATE / freq).toInt().coerceIn(24, v.buffer.size - 2)
        v.index = 0
        v.env = 1f
        for (i in 0 until v.length) {
            v.buffer[i] = (rnd.nextFloat() * 2f - 1f) * 0.55f
        }
        val g = panGains(rnd.nextFloat() * 2f - 1f)
        v.panL = g[0]
        v.panR = g[1]
        timer = minInterval + rnd.nextFloat() * (maxInterval - minInterval)
    }

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            time += dt
            timer -= dt
            if (timer <= 0f) pluck()
            var sl = 0f
            var sr = 0f
            for (v in strings) {
                if (v.env <= 0.0008f) continue
                val current = v.buffer[v.index]
                val nextIndex = if (v.index + 1 >= v.length) 0 else v.index + 1
                val filtered = (current + v.buffer[nextIndex]) * 0.5f * damping
                v.buffer[v.index] = filtered
                v.index = nextIndex
                v.env *= envDecay
                val s = current * v.env * amplitude
                sl += s * v.panL
                sr += s * v.panR
            }
            leftOut[i] = sl
            rightOut[i] = sr
        }
    }
}

class BellVoice(
    baseFreq: Float,
    minInterval: Float,
    maxInterval: Float,
    strikeGain: Float,
    seed: Int
) : Voice {
    private val inner = BowlVoice(
        baseFreq = baseFreq,
        ratios = floatArrayOf(1f, 2.76f, 5.4f, 8.2f),
        amps = floatArrayOf(1f, 0.42f, 0.2f, 0.09f),
        taus = floatArrayOf(8f, 5.5f, 3.2f, 1.8f),
        minInterval = minInterval,
        maxInterval = maxInterval,
        strikeGain = strikeGain,
        seed = seed
    )

    override fun render(leftOut: FloatArray, rightOut: FloatArray, frames: Int) =
        inner.render(leftOut, rightOut, frames)
}
