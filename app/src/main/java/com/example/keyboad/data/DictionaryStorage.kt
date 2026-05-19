package com.example.keyboad.data

import android.content.Context
import java.io.File

class DictionaryStorage(private val context: Context) {
    private val fileName = "eng_dict.txt"
    private val dictionaryFile: File get() = File(context.filesDir, fileName)

    fun isDownloaded(): Boolean {
        return dictionaryFile.exists() && dictionaryFile.length() > 0
    }

    fun saveDictionary(words: String) {
        dictionaryFile.writeText(words)
    }

    fun loadWords(): Set<String> {
        if (!isDownloaded()) return emptySet()
        return try {
            dictionaryFile.readLines().toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
