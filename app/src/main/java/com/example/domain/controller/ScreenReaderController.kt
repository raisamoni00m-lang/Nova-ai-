package com.example.domain.controller

import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import com.example.domain.voice.NovaTTSManager
import com.example.service.NovaAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Screen Reader Controller — provides structured element-by-element screen reading
 * using the Android Accessibility Service. Traverses the accessibility tree to extract
 * readable elements (text, content descriptions, button labels), then speaks them
 * one at a time. Supports next/previous navigation and auto-advance mode.
 */
class ScreenReaderController(
    private val ttsManager: NovaTTSManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ScreenElement(
        val text: String,
        val type: String,      // "text", "button", "link", "image", "heading"
        val className: String,
        val index: Int
    )

    private val _elements = MutableStateFlow<List<ScreenElement>>(emptyList())
    val elements: StateFlow<List<ScreenElement>> = _elements.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _autoAdvance = MutableStateFlow(true)
    val autoAdvance: StateFlow<Boolean> = _autoAdvance.asStateFlow()

    /**
     * Starts screen reading: extracts all readable elements from the active window
     * and begins speaking them from the first element.
     */
    fun startReading() {
        val service = NovaAccessibilityService.instance
        if (service == null) {
            speak("Screen reader requires the Nova Accessibility Service. Please enable it in device settings.")
            return
        }

        val extracted = extractElements(service)
        if (extracted.isEmpty()) {
            speak("No readable elements found on this screen.")
            return
        }

        _elements.value = extracted
        _currentIndex.value = 0
        _isActive.value = true

        speakElement(0)
    }

    /**
     * Reads the next element. Stops at the last element.
     */
    fun nextElement() {
        if (!_isActive.value) {
            startReading()
            return
        }
        val i = _currentIndex.value
        if (i < _elements.value.size - 1) {
            ttsManager.stop()
            _currentIndex.value = i + 1
            speakElement(_currentIndex.value)
        } else {
            speak("End of screen reached.")
        }
    }

    /**
     * Reads the previous element.
     */
    fun previousElement() {
        if (!_isActive.value) return
        val i = _currentIndex.value
        if (i > 0) {
            ttsManager.stop()
            _currentIndex.value = i - 1
            speakElement(_currentIndex.value)
        } else {
            speak("Already at the top of the screen.")
        }
    }

    /**
     * Stops screen reading and clears state.
     */
    fun stopReading() {
        _isActive.value = false
        _currentIndex.value = -1
        _elements.value = emptyList()
        ttsManager.stop()
    }

    /**
     * Re-reads the current element after the TTS finishes,
     * if auto-advance is enabled.
     */
    fun onTtsFinished() {
        if (_isActive.value && _autoAdvance.value) {
            val i = _currentIndex.value
            if (i in 0.._elements.value.size - 2) {
                _currentIndex.value = i + 1
                mainHandler.postDelayed({
                    if (_isActive.value) speakElement(_currentIndex.value)
                }, 300)
            } else {
                _isActive.value = false
                speak("Screen reading complete.")
            }
        }
    }

    fun toggleAutoAdvance(): Boolean {
        _autoAdvance.value = !_autoAdvance.value
        return _autoAdvance.value
    }

    private fun speakElement(index: Int) {
        val els = _elements.value
        if (index < 0 || index >= els.size) return
        val el = els[index]
        val positionInfo = "Element ${index + 1} of ${els.size}. "
        speak(positionInfo + el.text)
    }

    private fun speak(text: String) {
        ttsManager.speak(text, null)
    }

    /**
     * Extracts readable elements from the accessibility tree.
     * Filters out empty nodes and groups them by type for better navigation.
     */
    private fun extractElements(service: NovaAccessibilityService): List<ScreenElement> {
        val rootNode = service.rootInActiveWindow
            ?: return emptyList()
        val result = mutableListOf<ScreenElement>()
        val visited = mutableSetOf<AccessibilityNodeInfo>()

        fun classify(className: String, text: String): String {
            return when {
                className.contains("Button") || className.contains("CheckBox") || className.contains("Switch") -> "button"
                className.contains("TextView") -> "text"
                className.contains("EditText") -> "input"
                className.contains("ImageView") -> "image"
                className.contains("RecyclerView") || className.contains("ScrollView") -> "list"
                className.contains("Toolbar") || className.contains("ActionBar") -> "toolbar"
                else -> "text"
            }
        }

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visited.contains(node)) return
            visited.add(node)

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val label = text?.takeIf { it.isNotBlank() } ?: desc?.takeIf { it.isNotBlank() }

            if (!label.isNullOrBlank()) {
                val className = node.className?.toString() ?: ""
                result.add(
                    ScreenElement(
                        text = label,
                        type = classify(className, label),
                        className = className,
                        index = result.size
                    )
                )
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(rootNode)
        return result
    }

    /**
     * Gets a raw text dump of the screen for AI processing.
     */
    fun getScreenTextSummary(): String {
        val service = NovaAccessibilityService.instance ?: return "Accessibility Service not enabled."
        val els = extractElements(service)
        if (els.isEmpty()) return "No readable content on this screen."
        return els.joinToString(separator = " | ") { "${it.index + 1}. ${it.text}" }
    }
}


