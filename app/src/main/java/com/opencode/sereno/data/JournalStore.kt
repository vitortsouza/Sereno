package com.opencode.sereno.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class JournalEntry(
    val id: Long,
    val createdAt: Long,
    val text: String,
    val mood: String?
)

data class JournalMood(val key: String, val label: String)

val JournalMoods = listOf(
    JournalMood("grato", "Grato"),
    JournalMood("calmo", "Calmo"),
    JournalMood("bem", "Bem"),
    JournalMood("dificil", "Difícil")
)

object JournalStore {
    private lateinit var file: File
    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        file = File(context.filesDir, "journal.json")
        _entries.value = read().sortedByDescending { it.createdAt }
    }

    fun add(text: String, mood: String?) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val now = System.currentTimeMillis()
        val id = maxOf(now, (_entries.value.firstOrNull()?.id ?: 0L) + 1)
        val entry = JournalEntry(id, now, clean.take(4000), mood)
        val next = (listOf(entry) + _entries.value).sortedByDescending { it.createdAt }
        _entries.value = next
        write(next)
    }

    fun delete(id: Long) {
        val next = _entries.value.filterNot { it.id == id }
        _entries.value = next
        write(next)
    }

    fun deleteAll() {
        _entries.value = emptyList()
        write(emptyList())
    }

    private fun write(entries: List<JournalEntry>) {
        runCatching {
            val array = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                    .put("id", entry.id)
                    .put("createdAt", entry.createdAt)
                    .put("text", entry.text)
                entry.mood?.let { obj.put("mood", it) }
                array.put(obj)
            }
            file.writeText(array.toString())
        }
    }

    private fun read(): List<JournalEntry> {
        if (!::file.isInitialized || !file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            val result = ArrayList<JournalEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val text = obj.optString("text", "").trim()
                if (text.isEmpty()) continue
                result.add(
                    JournalEntry(
                        id = obj.optLong("id", obj.optLong("createdAt", 0L)),
                        createdAt = obj.optLong("createdAt", 0L),
                        text = text.take(4000),
                        mood = if (obj.has("mood") && !obj.isNull("mood")) {
                            obj.getString("mood")
                        } else {
                            null
                        }
                    )
                )
            }
            result
        }.getOrDefault(emptyList())
    }
}
