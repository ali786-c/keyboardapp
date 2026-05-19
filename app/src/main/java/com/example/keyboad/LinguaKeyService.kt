package com.example.keyboad

import android.content.ClipboardManager
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.keyboad.data.ClipboardHistoryManager
import com.example.keyboad.data.UserDictionaryManager
import com.example.keyboad.data.VoiceInputManager
import com.example.keyboad.data.SettingsPreferencesManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinguaKeyService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var viewModel: KeyboardViewModel
    private lateinit var dictionaryManager: UserDictionaryManager
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var settingsPreferencesManager: SettingsPreferencesManager
    private var lastSpaceTime: Long = 0
    private var vibrator: android.os.Vibrator? = null
    private var vibrationEffect: android.os.VibrationEffect? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrationEffect = android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
        }
        
        dictionaryManager = UserDictionaryManager(this)
        clipboardHistoryManager = ClipboardHistoryManager(this)
        settingsPreferencesManager = SettingsPreferencesManager(this)
        viewModel = KeyboardViewModel(dictionaryManager, settingsPreferencesManager)
        
        voiceInputManager = VoiceInputManager(
            context = this,
            onResult = { text ->
                currentInputConnection?.commitText(text, 1)
                updateCurrentText()
            },
            onError = { error ->
                if (error == "PERMISSION_DENIED") {
                    viewModel.setError("Allow Mic in Settings")
                    // Open MainActivity so user can grant permission
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } else {
                    viewModel.setError(error)
                }
            },
            onListeningStateChanged = { isListening ->
                viewModel.setListening(isListening)
            }
        )
    }

    override fun onConfigureWindow(win: Window, isFloating: Boolean, isSelectable: Boolean) {
        super.onConfigureWindow(win, isFloating, isSelectable)
        win.decorView.setViewTreeLifecycleOwner(this)
        win.decorView.setViewTreeViewModelStoreOwner(this)
        win.decorView.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        composeView.setContent {
            val state by viewModel.uiState.collectAsState()

            val onKeyClick: (String) -> Unit = remember { { key ->
                val ic = currentInputConnection
                ic?.commitText(key, 1)
                vibrate()
                if (viewModel.uiState.value.shiftState == ShiftState.ON) {
                    viewModel.setShift(ShiftState.OFF)
                }
                updateCurrentText()
            } }

            val onDeleteClick: (Boolean) -> Unit = remember { { isFinal ->
                val ic = currentInputConnection
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                vibrate()
                if (isFinal) updateCurrentText()
            } }

            val onSpaceClick: () -> Unit = remember { {
                val currentTime = System.currentTimeMillis()
                val ic = currentInputConnection
                if (currentTime - lastSpaceTime < 300) {
                    val textBefore = ic?.getTextBeforeCursor(2, 0) ?: ""
                    if (textBefore.length >= 1 && textBefore.endsWith(" ")) {
                        ic?.deleteSurroundingText(1, 0)
                        ic?.commitText(". ", 1)
                    } else {
                        ic?.commitText(" ", 1)
                    }
                    lastSpaceTime = 0
                } else {
                    val lastWord = ic?.getTextBeforeCursor(50, 0)?.toString()?.split(" ", "\n")?.lastOrNull() ?: ""
                    if (lastWord.isNotBlank()) {
                        viewModel.onWordCommitted(lastWord)
                    }
                    ic?.commitText(" ", 1)
                    lastSpaceTime = currentTime
                }
                vibrate()
                updateCurrentText()
            } }

            val onEnterClick: () -> Unit = remember { {
                val lastWord = currentInputConnection?.getTextBeforeCursor(50, 0)?.toString()?.split(" ", "\n")?.lastOrNull() ?: ""
                if (lastWord.isNotBlank()) {
                    viewModel.onWordCommitted(lastWord)
                }
                val ic = currentInputConnection
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                vibrate()
                updateCurrentText()
            } }

            val onSuggestionClick: (String) -> Unit = remember { { suggestion ->
                if (viewModel.uiState.value.isTranslationResult) {
                    replaceFullText(suggestion)
                } else {
                    replaceCurrentWord(suggestion)
                }
                viewModel.onWordCommitted(suggestion)
            } }

            val onTranslateClick: () -> Unit = remember { {
                viewModel.requestTranslation()
            } }

            val onShiftClick: () -> Unit = remember { {
                viewModel.toggleShift()
            } }

            val onLayoutToggle: () -> Unit = remember { {
                viewModel.toggleLayout()
            } }

            val onSymbols1Toggle: () -> Unit = remember { {
                viewModel.switchToSymbols1()
            } }

            val onSymbols2Toggle: () -> Unit = remember { {
                viewModel.switchToSymbols2()
            } }

            val onNumpadToggle: () -> Unit = remember { {
                viewModel.toggleNumpad()
            } }

            val onCursorMove: (Int) -> Unit = remember { { offset ->
                val ic = currentInputConnection
                val keyEvent = if (offset > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                repeat(Math.abs(offset)) {
                    ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
                    ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
                }
            } }

            val onSwipeDelete: (Int) -> Unit = remember { { count ->
                val ic = currentInputConnection
                repeat(count) {
                    ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                }
                vibrate()
            } }
            
            val onClipboardToggle: () -> Unit = remember { {
                checkAndSaveClipboard()
                viewModel.toggleClipboard(clipboardHistoryManager.getHistory())
            } }

            val onEmojiToggle: () -> Unit = remember { {
                viewModel.toggleEmojiPicker()
            } }

            val onEmojiClicked: (String) -> Unit = remember { { emoji ->
                currentInputConnection?.commitText(emoji, 1)
                viewModel.onEmojiClicked(emoji)
            } }

            val onClipboardItemClick: (String) -> Unit = remember { { item ->
                currentInputConnection?.commitText(item, 1)
                viewModel.toggleClipboard()
                updateCurrentText()
            } }

            val onDownloadDictionary: () -> Unit = remember { {
                viewModel.downloadDictionary()
            } }

            val onVoiceInputClick: () -> Unit = remember { {
                if (viewModel.uiState.value.isListening) {
                    voiceInputManager.stopListening()
                } else {
                    voiceInputManager.startListening()
                }
            } }

            com.example.keyboad.ui.KeyboardScreen(
                state = state,
                onKeyClick = onKeyClick,
                onDeleteClick = onDeleteClick,
                onSpaceClick = onSpaceClick,
                onEnterClick = onEnterClick,
                onSuggestionClick = onSuggestionClick,
                onTranslateClick = onTranslateClick,
                onShiftClick = onShiftClick,
                onLayoutToggle = onLayoutToggle,
                onSymbols1Toggle = onSymbols1Toggle,
                onSymbols2Toggle = onSymbols2Toggle,
                onNumpadToggle = onNumpadToggle,
                onCursorMove = onCursorMove,
                onSwipeDelete = onSwipeDelete,
                onClipboardToggle = onClipboardToggle,
                onEmojiToggle = onEmojiToggle,
                onClipboardItemClick = onClipboardItemClick,
                onDownloadDictionary = onDownloadDictionary,
                onVoiceInputClick = onVoiceInputClick
            )
        }
        
        return composeView
    }

    private fun checkAndSaveClipboard() {
        val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (cm?.hasPrimaryClip() == true) {
            val item = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (!item.isNullOrBlank()) {
                clipboardHistoryManager.saveToHistory(item)
            }
        }
    }

    private fun updateCurrentText() {
        lifecycleScope.launch(Dispatchers.IO) {
            val ic = currentInputConnection ?: return@launch
            val text = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
            withContext(Dispatchers.Main) {
                viewModel.onTextChange(text)
            }
        }
    }

    private fun vibrate() {
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrationEffect?.let { effect -> it.vibrate(effect) }
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(15)
            }
        }
    }

    private fun replaceFullText(newText: String) {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val textAfter = ic.getTextAfterCursor(1000, 0)?.toString() ?: ""
        
        ic.deleteSurroundingText(textBefore.length, textAfter.length)
        ic.commitText(newText, 1)
        updateCurrentText()
    }

    private fun replaceCurrentWord(newText: String) {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
        val lastWord = textBefore.split(" ", "\n").lastOrNull() ?: ""
        
        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }
        ic.commitText("$newText ", 1)
        updateCurrentText()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        checkAndSaveClipboard()
        updateCurrentText()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        voiceInputManager.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        voiceInputManager.stopListening()
        _viewModelStore.clear()
    }
}
