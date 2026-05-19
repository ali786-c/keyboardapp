package com.example.keyboad.data

import android.content.Context
import java.io.File

class UserDictionaryManager(private val context: Context) {
    private val dictionaryFile = File(context.filesDir, "user_dict.txt")
    private val bigramFile = File(context.filesDir, "bigram_dict.txt")
    private val learnedWords = mutableSetOf<String>()
    private val wordFrequencies = mutableMapOf<String, Int>()
    
    private val commonWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know", "take", "person", "into", "year", "your", "good", "some", "could", "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two", "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", "give", "day", "most", "us", "is", "am", "are", "was", "were", "been", "being", "doing", "going", "making", "having", "taking", "getting", "showing", "telling", "asking", "working", "playing", "saying", "coming", "starting", "ending", "running", "walking", "talking", "listening", "hearing", "seeing", "watching", "feeling", "touching", "smelling", "tasting", "hello", "hi", "hey", "thanks", "welcome", "please", "sorry", "yes", "no", "maybe", "ok", "okay", "good", "bad", "great", "awesome", "keyboard", "typing", "fast", "smart", "ai", "lingua", "key", "language", "input", "method", "brother", "sister", "friend", "family", "mother", "father", "son", "daughter"
    )

    private val commonBigrams = mapOf(
        "i" to listOf("am", "have", "need", "want", "will", "can", "don't", "think"),
        "you" to listOf("are", "have", "can", "will", "do", "want", "know", "did"),
        "it" to listOf("is", "was", "has", "will", "can", "seems", "works"),
        "he" to listOf("is", "was", "has", "will", "can", "does", "says"),
        "she" to listOf("is", "was", "has", "will", "can", "does", "says"),
        "we" to listOf("are", "have", "can", "will", "do", "want", "should"),
        "they" to listOf("are", "have", "can", "will", "do", "want", "should"),
        "how" to listOf("are", "is", "was", "can", "do", "about", "much"),
        "what" to listOf("is", "are", "was", "can", "do", "about", "time"),
        "where" to listOf("is", "are", "was", "can", "do", "did"),
        "when" to listOf("is", "are", "was", "can", "do", "did"),
        "hi" to listOf("there", "how", "everyone", "friend"),
        "need" to listOf("help", "money", "you", "to", "job"),
        "want" to listOf("to", "you", "it", "more", "some"),
        "good" to listOf("morning", "night", "evening", "day", "luck"),
        "thank" to listOf("you", "s", "fully"),
        "are" to listOf("you", "they", "we", "doing", "going", "coming"),
        "doing" to listOf("brother", "well", "great", "fine", "nothing")
    )

    private val shortcuts = mutableMapOf(
        "gm" to "Good Morning", "gn" to "Good Night", "ty" to "Thank You", "omw" to "On my way", "brb" to "Be right back"
    )

    private val bigrams = mutableMapOf<String, MutableMap<String, Int>>()

    init {
        loadDictionary()
        loadBigrams()
        commonWords.forEach { 
            learnedWords.add(it)
            wordFrequencies[it] = (wordFrequencies[it] ?: 0) + 10 // Base priority for common words
        }
    }

    private fun loadDictionary() {
        if (dictionaryFile.exists()) {
            dictionaryFile.readLines().forEach { line ->
                val parts = line.split("|")
                val word = parts[0].trim().lowercase()
                val freq = parts.getOrNull(1)?.toIntOrNull() ?: 1
                learnedWords.add(word)
                wordFrequencies[word] = freq
            }
        }
    }

    private fun loadBigrams() {
        if (bigramFile.exists()) {
            try {
                bigramFile.readLines().forEach { line ->
                    val parts = line.split(":")
                    if (parts.size == 2) {
                        val word = parts[0]
                        val followers = parts[1].split(",").mapNotNull {
                            val subParts = it.split("|")
                            if (subParts.size == 2) subParts[0] to subParts[1].toInt() else null
                        }.toMap().toMutableMap()
                        bigrams[word] = followers
                    }
                }
            } catch (e: Exception) { bigrams.clear() }
        }
    }

    fun getSuggestions(prefix: String, contextWords: List<String> = emptyList()): List<String> {
        val lowerPrefix = prefix.lowercase().trim()
        if (lowerPrefix.isBlank()) return emptyList()
        shortcuts[lowerPrefix]?.let { return listOf(it) }

        // 1. All candidates (Prefix + Fuzzy)
        val candidates = learnedWords.filter { 
            it.startsWith(lowerPrefix) || calculateDistance(it, lowerPrefix) <= (if (lowerPrefix.length > 4) 2 else 1)
        }

        // 2. Advanced Ranking Algorithm
        return candidates.distinct()
            .sortedWith(Comparator { a, b ->
                // Priority 1: Context Matching (Does it follow the last word?)
                val lastWord = contextWords.lastOrNull()?.lowercase()
                val isAFollower = if (lastWord != null) (bigrams[lastWord]?.containsKey(a) == true || commonBigrams[lastWord]?.contains(a) == true) else false
                val isBFollower = if (lastWord != null) (bigrams[lastWord]?.containsKey(b) == true || commonBigrams[lastWord]?.contains(b) == true) else false
                
                if (isAFollower && !isBFollower) return@Comparator -1
                if (isBFollower && !isAFollower) return@Comparator 1

                // Priority 2: Edit Distance
                val distA = calculateDistance(a, lowerPrefix)
                val distB = calculateDistance(b, lowerPrefix)
                if (distA != distB) return@Comparator distA - distB

                // Priority 3: Usage Frequency
                val freqA = wordFrequencies[a] ?: 0
                val freqB = wordFrequencies[b] ?: 0
                freqB - freqA
            })
            .filter { it != lowerPrefix }
            .take(3)
    }

    fun getNextWordPredictions(lastWord: String): List<String> {
        val word = lastWord.lowercase().trim()
        val learnedFollowers = bigrams[word]?.entries?.sortedByDescending { it.value }?.map { it.key } ?: emptyList()
        val staticFollowers = commonBigrams[word] ?: emptyList()
        return (learnedFollowers + staticFollowers).distinct().take(3)
    }

    fun learnWord(word: String) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.length > 1) {
            learnedWords.add(cleanWord)
            wordFrequencies[cleanWord] = (wordFrequencies[cleanWord] ?: 0) + 1
            saveDictionary()
        }
    }

    fun learnBigram(word1: String, word2: String) {
        val w1 = word1.lowercase().trim()
        val w2 = word2.lowercase().trim()
        if (w1.isBlank() || w2.isBlank()) return
        val followers = bigrams.getOrPut(w1) { mutableMapOf() }
        followers[w2] = (followers[w2] ?: 0) + 1
        saveBigrams()
    }

    private fun saveDictionary() {
        try { 
            val content = learnedWords.joinToString("\n") { "$it|${wordFrequencies[it] ?: 1}" }
            dictionaryFile.writeText(content) 
        } catch (e: Exception) {}
    }

    private fun saveBigrams() {
        try {
            val content = bigrams.entries.joinToString("\n") { entry ->
                "${entry.key}:${entry.value.entries.joinToString(",") { "${it.key}|${it.value}" }}"
            }
            bigramFile.writeText(content)
        } catch (e: Exception) {}
    }

    private fun calculateDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        val n = s1.length
        val m = s2.length
        if (Math.abs(n - m) > 2) return 99
        var prevRow = IntArray(m + 1) { it }
        for (i in 1..n) {
            val currRow = IntArray(m + 1)
            currRow[0] = i
            for (j in 1..m) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = minOf(prevRow[j] + 1, currRow[j - 1] + 1, prevRow[j - 1] + cost)
            }
            prevRow = currRow
        }
        return prevRow[m]
    }

    fun isFullDictionaryDownloaded(): Boolean {
        // Only return true if we have significantly more words than the base list
        return learnedWords.size > (commonWords.size + 100)
    }

    fun downloadFullDictionary(data: String) {
        learnedWords.clear()
        wordFrequencies.clear()
        
        // Re-add basic common words
        commonWords.forEach { 
            learnedWords.add(it)
            wordFrequencies[it] = 10 
        }

        data.split("\n", " ", ",").forEach { 
            val word = it.trim().lowercase()
            if (word.length > 1) {
                learnedWords.add(word)
                wordFrequencies[word] = (wordFrequencies[word] ?: 0) + 1
            }
        }
        saveDictionary()
    }
}
