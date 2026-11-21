package com.opencode.sereno.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavedLayer(val soundId: String, val gain: Float)

data class SavedMix(
    val name: String,
    val durationMs: Long,
    val layers: List<SavedLayer>
)

object MixStore {
    private lateinit var file: File
    private val _mixes = MutableStateFlow<List<SavedMix>>(emptyList())
    val mixes: StateFlow<List<SavedMix>> = _mixes.asStateFlow()
    private var lastDeleted: SavedMix? = null

    fun init(context: Context) {
        file = File(context.filesDir, "mixes.json")
        _mixes.value = read()
    }

    fun save(name: String, durationMs: Long, layers: List<SavedLayer>) {
        val clean = name.trim().ifEmpty { "Mistura" }
        if (layers.isEmpty()) return
        val mix = SavedMix(clean, durationMs, layers.take(3))
        val next = _mixes.value.filterNot { it.name.equals(clean, ignoreCase = true) } + mix
        _mixes.value = next
        write(next)
    }

    fun delete(name: String): SavedMix? {
        val removed = _mixes.value.firstOrNull { it.name == name }
        lastDeleted = removed
        val next = _mixes.value.filterNot { it.name == name }
        _mixes.value = next
        write(next)
        return removed
    }

    fun restoreLast(): Boolean {
        val mix = lastDeleted ?: return false
        lastDeleted = null
        save(mix.name, mix.durationMs, mix.layers)
        return true
    }

    fun rename(oldName: String, newName: String) {
        val clean = newName.trim().take(32).ifEmpty { return }
        if (clean == oldName) return
        val current = _mixes.value.firstOrNull { it.name == oldName } ?: return
        val renamed = current.copy(name = clean)
        val next = _mixes.value.filterNot {
            it.name == oldName || it.name.equals(clean, ignoreCase = true)
        } + renamed
        _mixes.value = next
        write(next)
    }

    fun deleteAll() {
        lastDeleted = null
        _mixes.value = emptyList()
        write(emptyList())
    }

    private fun write(mixes: List<SavedMix>) {
        runCatching {
            val array = JSONArray()
            for (mix in mixes) {
                val obj = JSONObject()
                obj.put("name", mix.name)
                obj.put("durationMs", mix.durationMs)
                val layerArray = JSONArray()
                for (layer in mix.layers) {
                    layerArray.put(
                        JSONObject()
                            .put("soundId", layer.soundId)
                            .put("gain", layer.gain.toDouble())
                    )
                }
                obj.put("layers", layerArray)
                array.put(obj)
            }
            file.writeText(array.toString())
        }
    }

    private fun migrateSoundId(soundId: String): String = when (soundId) {
        "rain_roof", "rain_leaves" -> "rain_heavy"
        "drizzle" -> "rain_soft"
        else -> soundId
    }

    private fun read(): List<SavedMix> {
        if (!::file.isInitialized || !file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            val result = ArrayList<SavedMix>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val layerArray = obj.optJSONArray("layers") ?: JSONArray()
                val layers = ArrayList<SavedLayer>()
                for (j in 0 until layerArray.length()) {
                    val layerObj = layerArray.getJSONObject(j)
                    val soundId = migrateSoundId(layerObj.getString("soundId"))
                    if (layers.any { it.soundId == soundId }) continue
                    layers.add(
                        SavedLayer(
                            soundId = soundId,
                            gain = layerObj.optDouble("gain", 0.62).toFloat()
                        )
                    )
                }
                if (layers.isNotEmpty()) {
                    result.add(
                        SavedMix(
                            name = obj.optString("name", "Mistura"),
                            durationMs = obj.optLong("durationMs", 15 * 60_000L),
                            layers = layers.take(3)
                        )
                    )
                }
            }
            result
        }.getOrDefault(emptyList())
    }
}
