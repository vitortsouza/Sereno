package com.opencode.sereno.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object Prefs {
    private const val FILE = "sereno_prefs"
    private lateinit var sp: SharedPreferences

    val fadeSeconds = MutableStateFlow(20)
    val headphonesWarning = MutableStateFlow(true)
    val autoPlay = MutableStateFlow(true)
    val layersEnabled = MutableStateFlow(true)
    val sleepMode = MutableStateFlow(true)
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val breathHaptics = MutableStateFlow(true)
    val breathCues = MutableStateFlow(true)
    val breathAmbient = MutableStateFlow("none")
    val breathAmbientVolume = MutableStateFlow(0.45f)
    val breathCueVolume = MutableStateFlow(0.8f)
    val meditationAmbient = MutableStateFlow("none")
    val meditationAmbientVolume = MutableStateFlow(0.55f)
    val meditationSpeechRate = MutableStateFlow(0.70f)
    val ttsVoice = MutableStateFlow("")
    val breathNarration = MutableStateFlow(false)
    val breathNarrationVolume = MutableStateFlow(0.85f)
    val breathCountdown = MutableStateFlow(true)
    val favoriteSounds = MutableStateFlow<Set<String>>(emptySet())
    val journalReminder = MutableStateFlow(false)

    fun init(context: Context) {
        sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        fadeSeconds.value = sp.getInt("fade_seconds", 20)
        headphonesWarning.value = sp.getBoolean("headphones_warning", true)
        autoPlay.value = sp.getBoolean("auto_play", true)
        layersEnabled.value = sp.getBoolean("layers_enabled", true)
        sleepMode.value = sp.getBoolean("sleep_mode", true)
        themeMode.value = runCatching {
            ThemeMode.valueOf(sp.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        breathHaptics.value = sp.getBoolean("breath_haptics", true)
        breathCues.value = sp.getBoolean("breath_cues", true)
        breathAmbient.value = sp.getString("breath_ambient", "none") ?: "none"
        breathAmbientVolume.value = sp.getFloat("breath_ambient_volume", 0.45f)
        breathCueVolume.value = sp.getFloat("breath_cue_volume", 0.8f)
        meditationAmbient.value = sp.getString("meditation_ambient", "none") ?: "none"
        meditationAmbientVolume.value = sp.getFloat("meditation_ambient_volume", 0.55f)
        meditationSpeechRate.value = sp.getFloat("meditation_speech_rate", 0.70f)
        ttsVoice.value = sp.getString("tts_voice", "") ?: ""
        breathNarration.value = sp.getBoolean("breath_narration", false)
        breathNarrationVolume.value = sp.getFloat("breath_narration_volume", 0.85f)
        breathCountdown.value = sp.getBoolean("breath_countdown", true)
        favoriteSounds.value = sp.getStringSet("favorite_sounds", emptySet())?.toSet() ?: emptySet()
        journalReminder.value = sp.getBoolean("journal_reminder", false)
    }

    fun setFadeSeconds(value: Int) {
        sp.edit().putInt("fade_seconds", value).apply()
        fadeSeconds.value = value
    }

    fun setHeadphonesWarning(value: Boolean) {
        sp.edit().putBoolean("headphones_warning", value).apply()
        headphonesWarning.value = value
    }

    fun setAutoPlay(value: Boolean) {
        sp.edit().putBoolean("auto_play", value).apply()
        autoPlay.value = value
    }

    fun setLayersEnabled(value: Boolean) {
        sp.edit().putBoolean("layers_enabled", value).apply()
        layersEnabled.value = value
    }

    fun setSleepMode(value: Boolean) {
        sp.edit().putBoolean("sleep_mode", value).apply()
        sleepMode.value = value
    }

    fun setThemeMode(value: ThemeMode) {
        sp.edit().putString("theme_mode", value.name).apply()
        themeMode.value = value
    }

    fun setBreathHaptics(value: Boolean) {
        sp.edit().putBoolean("breath_haptics", value).apply()
        breathHaptics.value = value
    }

    fun setBreathCues(value: Boolean) {
        sp.edit().putBoolean("breath_cues", value).apply()
        breathCues.value = value
    }

    fun setBreathAmbient(value: String) {
        sp.edit().putString("breath_ambient", value).apply()
        breathAmbient.value = value
    }

    fun setBreathAmbientVolume(value: Float) {
        sp.edit().putFloat("breath_ambient_volume", value).apply()
        breathAmbientVolume.value = value
    }

    fun setBreathCueVolume(value: Float) {
        sp.edit().putFloat("breath_cue_volume", value).apply()
        breathCueVolume.value = value
    }

    fun setMeditationAmbient(value: String) {
        sp.edit().putString("meditation_ambient", value).apply()
        meditationAmbient.value = value
    }

    fun setMeditationAmbientVolume(value: Float) {
        sp.edit().putFloat("meditation_ambient_volume", value).apply()
        meditationAmbientVolume.value = value
    }

    fun setMeditationSpeechRate(value: Float) {
        sp.edit().putFloat("meditation_speech_rate", value).apply()
        meditationSpeechRate.value = value
    }

    fun setTtsVoice(value: String) {
        sp.edit().putString("tts_voice", value).apply()
        ttsVoice.value = value
    }

    fun setBreathNarration(value: Boolean) {
        sp.edit().putBoolean("breath_narration", value).apply()
        breathNarration.value = value
    }

    fun setBreathNarrationVolume(value: Float) {
        sp.edit().putFloat("breath_narration_volume", value).apply()
        breathNarrationVolume.value = value
    }

    fun setBreathCountdown(value: Boolean) {
        sp.edit().putBoolean("breath_countdown", value).apply()
        breathCountdown.value = value
    }

    fun setJournalReminder(value: Boolean) {
        sp.edit().putBoolean("journal_reminder", value).apply()
        journalReminder.value = value
    }

    fun toggleFavoriteSound(id: String) {
        val next = favoriteSounds.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        favoriteSounds.value = next
        sp.edit().putStringSet("favorite_sounds", next).apply()
    }

    fun notificationsAsked(): Boolean = sp.getBoolean("notifications_asked", false)

    fun markNotificationsAsked() {
        sp.edit().putBoolean("notifications_asked", true).apply()
    }
}
