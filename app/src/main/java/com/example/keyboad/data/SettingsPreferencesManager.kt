package com.example.keyboad.data

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("linguakey_settings", Context.MODE_PRIVATE)

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString("gemini_api_key", apiKey.trim()).apply()
    }

    fun getApiKey(): String? {
        val key = prefs.getString("gemini_api_key", null)
        return if (key.isNullOrBlank()) null else key
    }
}
