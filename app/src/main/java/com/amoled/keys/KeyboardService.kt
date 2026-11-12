package com.amoled.keys

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.*

class KeyboardService : InputMethodService(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private lateinit var wordEngine: WordEngine
    private var speechRecognizer: SpeechRecognizer? = null
    private var inputView: View? = null
    private val _viewModelStore = ViewModelStore()

    private var uiState by mutableStateOf(KeyboardUiState())
    private val prefs by lazy { getSharedPreferences("keys_settings", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        wordEngine = WordEngine(this)
        loadSettings()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        uiState = uiState.copy(composingWord = "", suggestions = emptyList())
        updateEnterLabel(attribute)
    }

    private fun updateEnterLabel(info: EditorInfo?) {
        val label = when (info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_SEND -> "send"
            EditorInfo.IME_ACTION_GO -> "go"
            EditorInfo.IME_ACTION_SEARCH -> "search"
            EditorInfo.IME_ACTION_NEXT -> "next"
            EditorInfo.IME_ACTION_DONE -> "done"
            else -> "↵"
        }
        uiState = uiState.copy(enterLabel = label)
    }

    override fun onCreateInputView(): View {
        if (inputView == null) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            inputView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setViewTreeLifecycleOwnerReflect(this, this@KeyboardService)
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides this@KeyboardService) {
                        val state = uiState
                        KeyboardScreen(state = state, onKey = { handleKey(it) })
                    }
                }
            }
        }
        loadSettings()
        return inputView!!
    }

    override fun onFinishInput() {
        super.onFinishInput()
        uiState = uiState.copy(composingWord = "", suggestions = emptyList())
        stopListening()
    }

    override fun onDestroy() {
        stopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        _viewModelStore.clear()
        super.onDestroy()

    }

    private fun handleKey(action: KeyAction) {
        vibrate()
        when (action) {
            is KeyAction.Char -> insertChar(action.char)
            is KeyAction.Backspace -> deleteChar()
            is KeyAction.BackspaceWord -> deleteWord()
            is KeyAction.Space -> insertSpace()
            is KeyAction.Enter -> performEnter()
            is KeyAction.ShiftTap -> handleShift()
            is KeyAction.SwitchToSym -> uiState = uiState.copy(page = KeyPage.Sym)
            is KeyAction.SwitchToAlpha -> uiState = uiState.copy(page = KeyPage.Alpha)
            is KeyAction.Voice -> if (uiState.isListening) stopListening() else startListening()
            is KeyAction.Suggestion -> applySuggestion(action.word)
        }
    }

    private fun insertChar(char: Char) {
        val ic = currentInputConnection ?: return
        val shifted = uiState.shiftState != ShiftState.Off
        val c = if (shifted) char.uppercaseChar() else char
        ic.commitText(c.toString(), 1)
        val newWord = if (char.isLetter()) uiState.composingWord + char.lowercaseChar() else ""
        uiState = uiState.copy(
            composingWord = newWord,
            suggestions = if (newWord.length >= 2) wordEngine.getSuggestions(newWord) else emptyList(),
            shiftState = if (uiState.shiftState == ShiftState.Once) ShiftState.Off else uiState.shiftState,
        )
    }

    private fun insertSpace() {
        val ic = currentInputConnection ?: return
        val word = uiState.composingWord
        if (uiState.autocorrectEnabled && word.length >= 3) {
            val corrected = wordEngine.autocorrect(word)
            if (corrected != null) {
                ic.deleteSurroundingText(word.length, 0)
                ic.commitText("$corrected ", 1)
                wordEngine.learnWord(corrected)
                uiState = uiState.copy(composingWord = "", suggestions = emptyList())
                return
            }
        }
        if (word.isNotBlank()) wordEngine.learnWord(word)
        ic.commitText(" ", 1)
        uiState = uiState.copy(composingWord = "", suggestions = emptyList())
    }

    private fun performEnter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
        if (uiState.composingWord.isNotBlank()) wordEngine.learnWord(uiState.composingWord)
        uiState = uiState.copy(composingWord = "", suggestions = emptyList())
    }

    private fun deleteChar() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
        val word = uiState.composingWord
        val newWord = if (word.isNotEmpty()) word.dropLast(1) else ""
        uiState = uiState.copy(
            composingWord = newWord,
            suggestions = if (newWord.length >= 2) wordEngine.getSuggestions(newWord) else emptyList(),
        )
    }

    private fun deleteWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: return
        val trimmed = before.trimEnd()
        val lastSpace = trimmed.lastIndexOf(' ')
        val charsToDelete = if (lastSpace < 0) before.length else before.length - lastSpace - 1
        if (charsToDelete > 0) ic.deleteSurroundingText(charsToDelete, 0)
        uiState = uiState.copy(composingWord = "", suggestions = emptyList())
    }

    private fun handleShift() {
        uiState = uiState.copy(
            shiftState = when (uiState.shiftState) {
                ShiftState.Off -> ShiftState.Once
                ShiftState.Once -> ShiftState.Locked
                ShiftState.Locked -> ShiftState.Off
            }
        )
    }

    private fun applySuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val composing = uiState.composingWord
        if (composing.isNotEmpty()) ic.deleteSurroundingText(composing.length, 0)
        ic.commitText("$word ", 1)
        wordEngine.learnWord(word)
        uiState = uiState.copy(composingWord = "", suggestions = emptyList())
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        stopListening()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                words?.firstOrNull()?.let { text ->
                    currentInputConnection?.commitText("$text ", 1)
                }
                uiState = uiState.copy(isListening = false, composingWord = "", suggestions = emptyList())
            }
            override fun onPartialResults(partial: Bundle) {}
            override fun onError(error: Int) { uiState = uiState.copy(isListening = false) }
            override fun onReadyForSpeech(params: Bundle?) { uiState = uiState.copy(isListening = true) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        uiState = uiState.copy(isListening = false)
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (!uiState.hapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    private fun loadSettings() {
        uiState = uiState.copy(
            hapticEnabled = prefs.getBoolean("haptic", true),
            autocorrectEnabled = prefs.getBoolean("autocorrect", true),
            showNumberRow = prefs.getBoolean("number_row", true),
        )
    }

    private fun setViewTreeLifecycleOwnerReflect(view: android.view.View, owner: LifecycleOwner) {
        try {
            Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
                .getMethod("set", android.view.View::class.java, LifecycleOwner::class.java)
                .invoke(null, view, owner)
        } catch (_: Exception) {}
    }
}
