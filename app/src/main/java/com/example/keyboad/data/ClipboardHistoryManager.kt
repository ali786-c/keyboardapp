package com.example.keyboad.data

import android.content.Context
import android.content.SharedPreferences

class ClipboardHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
    private val MAX_ITEMS = 15

    fun saveToHistory(text: String) {
        if (text.isBlank()) return
        
        val currentHistory = getHistory().toMutableList()
        // Remove if already exists to move to top
        currentHistory.remove(text)
        currentHistory.add(0, text)
        
        // Trim to max items
        val trimmed = if (currentHistory.size > MAX_ITEMS) currentHistory.take(MAX_ITEMS) else currentHistory
        
        prefs.edit().putString("history", trimmed.joinToString("|||")).apply()
    }

    fun getHistory(): List<String> {
        val raw = prefs.getString("history", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|||")
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }
}
