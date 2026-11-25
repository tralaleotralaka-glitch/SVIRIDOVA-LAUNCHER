package com.sviridova.nexus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import java.util.UUID

/** Локальные заметки рабочего стола (без интернета и аккаунтов). */
object NotesStore {

    data class Note(val id: String, val text: String, val at: Long)

    private const val FILE = "nexus_notes"
    private const val KEY = "items"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun all(context: Context): List<Note> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(Note(obj.getString("id"), obj.getString("text"), obj.optLong("at")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, text: String): List<Note> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return all(context)
        val updated = listOf(Note(UUID.randomUUID().toString(), trimmed, System.currentTimeMillis())) + all(context)
        save(context, updated)
        return updated
    }

    fun remove(context: Context, id: String): List<Note> {
        val updated = all(context).filterNot { it.id == id }
        save(context, updated)
        return updated
    }

    fun count(context: Context): Int = all(context).size

    private fun save(context: Context, notes: List<Note>) {
        val array = JSONArray()
        for (note in notes) {
            array.put(org.json.JSONObject().apply {
                put("id", note.id)
                put("text", note.text)
                put("at", note.at)
            })
        }
        prefs(context).edit { putString(KEY, array.toString()) }
    }
}
