package com.example.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.preferences.NovaPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Processing(val partialText: String) : SpeechState()
    data class Result(val spokenText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class NovaSpeechRecognizer(
    private val context: Context,
    private val preferences: NovaPreferences
) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    fun startListening(onResult: (String) -> Unit) {
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _speechState.value = SpeechState.Error("Speech recognition is not available or disabled.")
                    return@post
                }

                stopListeningInternal()

                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _speechState.value = SpeechState.Listening
                        }

                        override fun onBeginningOfSpeech() {
                            _speechState.value = SpeechState.Listening
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _audioRmsDb.value = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0.1f, 1f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _speechState.value = SpeechState.Processing("")
                            _audioRmsDb.value = 0f
                        }

                        override fun onError(error: Int) {
                            _audioRmsDb.value = 0f
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic."
                                SpeechRecognizer.ERROR_CLIENT -> "Client error. Tap mic again."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                SpeechRecognizer.ERROR_NETWORK -> "Network error connecting speech service."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap and speak again."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy. Resetting..."
                                SpeechRecognizer.ERROR_SERVER -> "Server error."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic to speak."
                                else -> "Speech recognition error ($error)."
                            }
                            _speechState.value = SpeechState.Error(msg)
                            mainHandler.postDelayed({
                                if (_speechState.value is SpeechState.Error) {
                                    _speechState.value = SpeechState.Idle
                                }
                            }, 3000)
                        }

                        override fun onResults(results: Bundle?) {
                            _audioRmsDb.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotBlank()) {
                                _speechState.value = SpeechState.Result(text)
                                onResult(text)
                            } else {
                                _speechState.value = SpeechState.Idle
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _speechState.value = SpeechState.Processing(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)

                    val lang = when (preferences.language) {
                        "bn" -> "bn-BD"
                        "en" -> "en-US"
                        else -> Locale.getDefault().toLanguageTag()
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "en-US", "bn-IN"))
                }

                recognizer?.startListening(intent)
                _speechState.value = SpeechState.Listening
            } catch (e: Exception) {
                _speechState.value = SpeechState.Error("Speech listener error: ${e.localizedMessage}")
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
            _speechState.value = SpeechState.Idle
            _audioRmsDb.value = 0f
        }
    }
}
