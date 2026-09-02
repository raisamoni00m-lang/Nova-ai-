package com.example.domain.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.preferences.NovaPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class NovaTTSManager(
    private val context: Context,
    private val preferences: NovaPreferences
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSpeech: Pair<String, String?>? = null
    private var _omniVoiceEngine: OmniVoiceEngine? = null

    fun setOmniVoiceEngine(engine: OmniVoiceEngine) {
        _omniVoiceEngine = engine
    }

    var omniVoiceEngine: OmniVoiceEngine?
        get() = _omniVoiceEngine
        set(value) { _omniVoiceEngine = value }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentVoiceName = MutableStateFlow("OmniVoice Neural (k2-fsa/OmniVoice)")
    val currentVoiceName: StateFlow<String> = _currentVoiceName.asStateFlow()

    private val _activeEngine = MutableStateFlow(preferences.voiceEngine)
    val activeEngine: StateFlow<String> = _activeEngine.asStateFlow()

    init {
        mainHandler.post {
            try {
                tts = TextToSpeech(context, this)
            } catch (e: Exception) {
                isInitialized = false
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            try {
                // Set high quality speech audio attributes so voice plays through speaker with clarity
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            } catch (_: Exception) {}

            setupVoice()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })

            // If a speech was requested before TTS finished initializing, speak now
            pendingSpeech?.let { (text, lang) ->
                pendingSpeech = null
                speak(text, lang)
            }
        }
    }

    fun setupVoice() {
        val ttsInstance = tts ?: return
        if (!isInitialized) return

        _activeEngine.value = preferences.voiceEngine

        // OmniVoice Neural Engine (k2-fsa/OmniVoice) timbre tuning vs ChatGPT Sky
        val (calibratedPitch, calibratedSpeed) = when (preferences.voiceEngine) {
            "omnivoice" -> {
                when (preferences.omnivoicePreset) {
                    "female_bengali_expressive" -> Pair(1.20f, 1.02f)
                    "female_clarity" -> Pair(1.14f, 1.04f)
                    else -> Pair(1.18f, 1.00f) // default female_expressive
                }
            }
            "chatgpt_sky" -> Pair(1.16f, 1.00f)
            "sweet_bn" -> Pair(1.22f, 1.02f)
            else -> Pair(preferences.voicePitch, preferences.voiceSpeed)
        }

        ttsInstance.setPitch(calibratedPitch)
        ttsInstance.setSpeechRate(calibratedSpeed)

        // Find high-quality natural female neural voice across installed voices
        try {
            val voices = ttsInstance.voices
            if (!voices.isNullOrEmpty()) {
                val femaleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    val isFemaleKeyword = name.contains("female") || name.contains("fem") ||
                            name.contains("sfg") || name.contains("bnf") || name.contains("ban") ||
                            name.contains("cfl") || name.contains("rjs") || name.contains("tpd") ||
                            name.contains("iol") || name.contains("woman") || name.contains("girl") ||
                            name.contains("wavenet") || name.contains("neural") || name.contains("omnivoice")
                    val notMale = !name.contains("male") && !name.contains("man") && !name.contains("boy")
                    isFemaleKeyword && notMale
                } ?: voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    !name.contains("male") && (voice.quality >= Voice.QUALITY_HIGH)
                } ?: voices.firstOrNull { !it.name.lowercase().contains("male") }

                if (femaleVoice != null) {
                    ttsInstance.voice = femaleVoice
                    _currentVoiceName.value = if (preferences.voiceEngine == "omnivoice") {
                        "OmniVoice: ${femaleVoice.name} (k2-fsa)"
                    } else {
                        femaleVoice.name
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun speak(text: String, languageCode: String? = null) {
        if (!preferences.autoSpeak || text.isBlank()) return

        // Try OmniVoice neural engine first when selected (k2-fsa/sherpa-onnx)
        if (preferences.voiceEngine == "omnivoice" && _omniVoiceEngine != null) {
            val engine = _omniVoiceEngine!!
            if (engine.initializeTts()) {
                val spokenText = sanitizeSpeechText(text)
                if (spokenText.isNotBlank() && engine.speak(spokenText)) {
                    _isSpeaking.value = true
                    return
                }
                // If OmniVoice failed, fall through to Android TTS
            }
        }

        if (!isInitialized || tts == null) {
            // Save pending speech to trigger once initialized
            pendingSpeech = Pair(text, languageCode)
            return
        }

        mainHandler.post {
            val ttsInstance = tts ?: return@post
            val cleanSpokenText = sanitizeSpeechText(text)
            if (cleanSpokenText.isBlank()) return@post

            ttsInstance.setPitch(preferences.voicePitch)
            ttsInstance.setSpeechRate(preferences.voiceSpeed)

            // Language locale check
            val hasBengali = cleanSpokenText.any { it in '\u0980'..'\u09FF' }
            val targetLocale = when {
                hasBengali || languageCode == "bn" -> Locale("bn", "BD")
                languageCode == "en" -> Locale.US
                else -> {
                    when (preferences.language) {
                        "bn" -> Locale("bn", "BD")
                        else -> Locale.US
                    }
                }
            }

            try {
                val result = ttsInstance.setLanguage(targetLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInstance.setLanguage(Locale.US)
                }
            } catch (_: Exception) {
                ttsInstance.setLanguage(Locale.US)
            }

            // Select highest quality female voice matching target language
            try {
                val voices = ttsInstance.voices
                if (!voices.isNullOrEmpty()) {
                    val localeVoices = voices.filter { it.locale.language == targetLocale.language }
                    val bestVoice = localeVoices.firstOrNull { voice ->
                        val n = voice.name.lowercase()
                        (n.contains("female") || n.contains("fem") || n.contains("bnf") || n.contains("sfg") || n.contains("ban")) &&
                                !n.contains("male")
                    } ?: localeVoices.firstOrNull { !it.name.lowercase().contains("male") }

                    if (bestVoice != null) {
                        ttsInstance.voice = bestVoice
                    }
                }
            } catch (_: Exception) {}

            val params = Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
            ttsInstance.speak(cleanSpokenText, TextToSpeech.QUEUE_FLUSH, params, "nova_utterance_${System.currentTimeMillis()}")
        }
    }

    /**
     * Cleans text to make female AI voice sound silky smooth and natural,
     * removing raw symbols, emojis, markdown formatting, and technical artifacts.
     */
    private fun sanitizeSpeechText(raw: String): String {
        var text = raw
            // Remove code fences & JSON blocks
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("\\{[\\s\\S]*?\\}"), "")
            // Remove markdown formatting
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("_(.*?)_"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex(">\\s*"), "")
            .replace(Regex("`([^`]+)`"), "$1")
            // Remove URLs
            .replace(Regex("https?://\\S+"), "link")
            // Remove common emoji ranges to prevent TTS robotic descriptions
            .replace(Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
            // Clean bullets and arrows
            .replace("•", "")
            .replace("✓", "")
            .replace("✗", "")
            .replace("⚠️", "")
            .replace("▶", "")
            .replace("→", "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return text
    }

    fun stop() {
        mainHandler.post {
            try {
                tts?.stop()
            } catch (_: Exception) {}
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}
            tts = null
            isInitialized = false
        }
    }
}
