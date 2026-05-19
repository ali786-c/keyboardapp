package com.example.keyboad.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiTranslator {

    suspend fun translate(romanUrdu: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (romanUrdu.isBlank()) return@withContext null
        
        return@withContext try {
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                systemInstruction = content {
                    text("You are an expert translator. Convert the following Roman Urdu text into natural, grammatically correct English. Provide only the translation.")
                }
            )
            val response = model.generateContent(romanUrdu)
            response.text?.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
