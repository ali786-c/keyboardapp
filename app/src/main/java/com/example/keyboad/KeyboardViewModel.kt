package com.example.keyboad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyboad.ai.GeminiTranslator
import com.example.keyboad.data.EmojiManager
import com.example.keyboad.data.UserDictionaryManager
import com.example.keyboad.data.SettingsPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class KeyboardLayoutType {
    LETTERS, SYMBOLS_1, SYMBOLS_2, NUMPAD
}

enum class ShiftState {
    OFF, ON, CAPS_LOCK
}

data class KeyboardUiState(
    val suggestions: List<String> = emptyList(),
    val emojiSuggestions: List<String> = emptyList(),
    val clipboardHistory: List<String> = emptyList(),
    val isClipboardOpen: Boolean = false,
    val isEmojiPickerOpen: Boolean = false,
    val isDictionaryDownloaded: Boolean = true,
    val isTranslationResult: Boolean = false,
    val isListening: Boolean = false,
    val isLoading: Boolean = false,
    val currentText: String = "",
    val errorMessage: String? = null,
    val layoutType: KeyboardLayoutType = KeyboardLayoutType.LETTERS,
    val shiftState: ShiftState = ShiftState.OFF,
    val categorizedEmojis: Map<String, List<String>> = emptyMap(),
    val recentEmojis: List<String> = emptyList(),
    val themeColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF8AB4F8)
)

class KeyboardViewModel(
    private val dictionaryManager: UserDictionaryManager,
    private val settingsPreferencesManager: SettingsPreferencesManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyboardUiState(
        isDictionaryDownloaded = dictionaryManager.isFullDictionaryDownloaded()
    ))
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private val translator = GeminiTranslator()
    private val emojiManager = EmojiManager()
    
    private var translationJob: Job? = null
    private var suggestionJob: Job? = null
    private var emojiLoadingJob: Job? = null

    init {
        // Efficiency: Start with small set of recents
        _uiState.value = _uiState.value.copy(
            recentEmojis = listOf("😊", "😂", "🥰", "😍", "👍", "❤️", "🔥", "✨")
        )
    }

    fun onTextChange(text: String) {
        if (text == _uiState.value.currentText) return
        val oldText = _uiState.value.currentText
        _uiState.value = _uiState.value.copy(currentText = text, isTranslationResult = false)
        
        checkAutoShift(text)

        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            val words = text.split(" ", "\n").filter { it.isNotBlank() }
            val lastWord = words.lastOrNull() ?: ""
            
            if (text.endsWith(" ") && words.isNotEmpty()) {
                // Efficiency: Background next-word prediction
                val nextPredictions = withContext(Dispatchers.Default) {
                    dictionaryManager.getNextWordPredictions(words.last())
                }
                if (nextPredictions.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(suggestions = nextPredictions)
                }
            } else if (lastWord.isNotBlank()) {
                val emojis = emojiManager.getEmojisForWord(lastWord)
                val suggestions = withContext(Dispatchers.Default) {
                    dictionaryManager.getSuggestions(lastWord, words.dropLast(1))
                }
                _uiState.value = _uiState.value.copy(
                    suggestions = suggestions,
                    emojiSuggestions = emojis
                )
            } else {
                _uiState.value = _uiState.value.copy(suggestions = emptyList(), emojiSuggestions = emptyList())
            }
        }
    }

    fun toggleEmojiPicker() {
        val isOpen = !_uiState.value.isEmojiPickerOpen
        _uiState.value = _uiState.value.copy(
            isEmojiPickerOpen = isOpen,
            isClipboardOpen = false
        )
        
        // Efficiency: Lazy load heavy emoji data ONLY when picker opens
        if (isOpen && _uiState.value.categorizedEmojis.isEmpty()) {
            emojiLoadingJob?.cancel()
            emojiLoadingJob = viewModelScope.launch(Dispatchers.Default) {
                val data = emojiManager.getAllEmojis()
                _uiState.value = _uiState.value.copy(categorizedEmojis = data)
            }
        }
    }

    fun onEmojiClicked(emoji: String) {
        val currentRecents = _uiState.value.recentEmojis.toMutableList()
        currentRecents.remove(emoji)
        currentRecents.add(0, emoji)
        _uiState.value = _uiState.value.copy(
            recentEmojis = currentRecents.take(24)
        )
    }

    fun onWordCommitted(word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryManager.learnWord(word)
            // Track bigram
            val words = _uiState.value.currentText.trim().split(" ")
            if (words.size >= 2) {
                dictionaryManager.learnBigram(words[words.size-2], word)
            }
        }
    }

    // --- Remaining logic stays the same but is moved to Background threads where possible ---
    fun toggleClipboard(history: List<String> = emptyList()) {
        _uiState.value = _uiState.value.copy(
            isClipboardOpen = !_uiState.value.isClipboardOpen,
            isEmojiPickerOpen = false,
            clipboardHistory = history
        )
    }

    fun requestTranslation() {
        val text = _uiState.value.currentText
        if (text.isBlank()) return
        
        val apiKey = settingsPreferencesManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please set your Gemini API Key in App Settings."
            )
            return
        }
        
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isTranslationResult = false)
            val aiResult = withContext(Dispatchers.IO) { translator.translate(text, apiKey) }
            _uiState.value = _uiState.value.copy(
                suggestions = if (aiResult != null) listOf(aiResult) else emptyList(),
                isTranslationResult = aiResult != null,
                isLoading = false,
                errorMessage = if (aiResult == null) "AI could not translate this." else null
            )
        }
    }

    fun toggleShift() {
        val newState = when (_uiState.value.shiftState) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> ShiftState.CAPS_LOCK
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        _uiState.value = _uiState.value.copy(shiftState = newState)
    }

    fun toggleLayout() {
        val newLayout = when (_uiState.value.layoutType) {
            KeyboardLayoutType.LETTERS -> KeyboardLayoutType.SYMBOLS_1
            else -> KeyboardLayoutType.LETTERS
        }
        _uiState.value = _uiState.value.copy(layoutType = newLayout)
    }

    fun switchToSymbols2() { _uiState.value = _uiState.value.copy(layoutType = KeyboardLayoutType.SYMBOLS_2) }
    fun switchToSymbols1() { _uiState.value = _uiState.value.copy(layoutType = KeyboardLayoutType.SYMBOLS_1) }
    
    fun setShift(state: ShiftState) {
        _uiState.value = _uiState.value.copy(shiftState = state)
    }

    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = error)
    }

    fun setListening(isListening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = isListening)
    }

    fun downloadDictionary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Fetching the professional 10,000 common words dictionary
                val url = java.net.URL("https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english.txt")
                val data = url.readText()
                
                withContext(Dispatchers.Main) {
                    dictionaryManager.downloadFullDictionary(data)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        isDictionaryDownloaded = true,
                        errorMessage = "Dictionary Updated (10,000 words)!"
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        errorMessage = "Download Failed: Check Internet Connection"
                    )
                }
            }
        }
    }

    fun toggleNumpad() {
        val newLayout = if (_uiState.value.layoutType == KeyboardLayoutType.NUMPAD) KeyboardLayoutType.SYMBOLS_1 else KeyboardLayoutType.NUMPAD
        _uiState.value = _uiState.value.copy(layoutType = newLayout)
    }

    private fun checkAutoShift(text: String) {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty() || trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?") || trimmed.endsWith("\n")) {
            if (_uiState.value.shiftState == ShiftState.OFF) _uiState.value = _uiState.value.copy(shiftState = ShiftState.ON)
        }
    }
}