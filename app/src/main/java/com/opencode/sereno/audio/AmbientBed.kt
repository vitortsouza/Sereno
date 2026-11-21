package com.opencode.sereno.audio

class AmbientBed {
    private var engine: AudioEngine? = null
    private var currentId: String? = null
    private var ducked = false

    fun setDucked(value: Boolean) {
        ducked = value
        engine?.setDucked(value)
    }

    fun start(soundId: String, gain: Float = 0.28f) {
        val volume = gain.coerceIn(0f, 1f)
        if (volume <= 0.001f) {
            stop()
            return
        }
        val active = engine
        if (currentId == soundId && active != null) {
            active.update(listOf(LayerSpec(uid = 1L, soundId = soundId, gain = volume)))
            return
        }
        val wasDucked = ducked
        stop()
        SampleBank.ensure(soundId)
        val next = AudioEngine { }
        next.start(
            initial = listOf(LayerSpec(uid = 1L, soundId = soundId, gain = volume)),
            durationMs = 0L,
            fadeMs = 1_500L
        )
        next.setDucked(wasDucked)
        ducked = wasDucked
        engine = next
        currentId = soundId
    }

    fun stop() {
        engine?.requestStop(1_100L)
        engine = null
        currentId = null
        ducked = false
    }
}
