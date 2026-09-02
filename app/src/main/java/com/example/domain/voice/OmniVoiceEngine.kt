package com.example.domain.voice

import android.content.Context
import android.media.AudioTrack
import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import com.example.data.preferences.NovaPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * OmniVoice Engine — wraps k2-fsa/sherpa-onnx for on-device neural speech processing.
 *
 * This integrates the OmniVoice project (https://github.com/k2-fsa/OmniVoice) via
 * sherpa-onnx (https://github.com/k2-fsa/sherpa-onnx), providing:
 * - On-device neural TTS (text-to-speech) with VITS/Matcha/Kokoro models
 * - On-device streaming ASR (speech recognition) with Zipformer/Paraformer models
 * - Works fully offline — no internet required after model download
 *
 * Model files must be downloaded to app private storage before use.
 * See OmniVoiceModelManager for model download paths.
 */
class OmniVoiceEngine(
    private val context: Context,
    private val preferences: NovaPreferences
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    private var tts: Any? = null  // com.k2fsa.sherpa.onnx.OfflineTts
    private var recognizer: Any? = null  // com.k2fsa.sherpa.onnx.OnlineRecognizer
    private var audioTrack: AudioTrack? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _engineInfo = MutableStateFlow("OmniVoice (sherpa-onnx) — Model not loaded")
    val engineInfo: StateFlow<String> = _engineInfo.asStateFlow()

    private var recognitionJob: Job? = null

    private val modelDir: File by lazy {
        File(context.filesDir, "omnivoice-models").apply { mkdirs() }
    }

    fun modelDirectory(): String = modelDir.absolutePath

    fun hasModels(): Boolean {
        return modelDir.listFiles()?.isNotEmpty() == true && getTtsModelPath() != null
    }

    /**
     * Searches the model directory for a VITS TTS model and returns its path.
     */
    private fun getTtsModelPath(): String? {
        val ttsSubdirs = modelDir.listFiles { f -> f.isDirectory && f.name.startsWith("vits") }
            ?: emptyList()
        for (dir in ttsSubdirs) {
            val modelFile = dir.listFiles { f -> f.name.endsWith(".onnx") }
            if (modelFile?.isNotEmpty() == true) return modelFile.first().absolutePath
        }
        // Also check for kokoro models
        val kokoroSubdirs = modelDir.listFiles { f -> f.isDirectory && f.name.startsWith("kokoro") }
            ?: emptyList()
        for (dir in kokoroSubdirs) {
            val modelFile = dir.listFiles { f -> f.name.endsWith(".onnx") }
            if (modelFile?.isNotEmpty() == true) return modelFile.first().absolutePath
        }
        return null
    }

    private fun getTtsDir(): File? {
        val dirs = modelDir.listFiles { f -> f.isDirectory }
        for (dir in dirs ?: emptyArray()) {
            val hasModel = dir.listFiles()?.any { it.name.endsWith(".onnx") } == true
            val hasTokens = dir.listFiles()?.any { it.name == "tokens.txt" } == true
            if (hasModel && hasTokens) return dir
        }
        return null
    }

    private fun getAsrDir(): File? {
        val asrKeywords = listOf("zipformer", "paraformer", "lstm", "whisper", "transducer")
        val dirs = modelDir.listFiles { f -> f.isDirectory }
        for (dir in dirs ?: emptyArray()) {
            val name = dir.name.lowercase()
            if (asrKeywords.any { name.contains(it) }) {
                val hasTokens = dir.listFiles()?.any { it.name == "tokens.txt" } == true
                if (hasTokens) return dir
            }
        }
        return null
    }

    /**
     * Initialize the TTS engine using sherpa-onnx OfflineTts.
     * Uses reflection to avoid hard dependency at compile time (the AAR may not
     * be present during IDE sync). In practice, the AAR is downloaded via the
     * gradle download task before the build runs.
     */
    fun initializeTts(): Boolean {
        if (tts != null) return true
        val ttsDir = getTtsDir() ?: run {
            _engineInfo.value = "OmniVoice — TTS model not found. Download a VITS/Kokoro model."
            return false
        }

        return try {
            // Use reflection to construct sherpa-onnx OfflineTts
            val modelPath = ttsDir.listFiles { it.name.endsWith(".onnx") }?.first()?.absolutePath
                ?: return false
            val tokensPath = File(ttsDir, "tokens.txt").absolutePath
            val dataDir = File(ttsDir, "espeak-ng-data").let {
                if (it.exists()) it.absolutePath else ""
            }
            val lexiconFile = File(ttsDir, "lexicon.txt")
            val lexiconPath = if (lexiconFile.exists()) lexiconFile.absolutePath else ""

            val className = "com.k2fsa.sherpa.onnx.OfflineTts"
            val vitsConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig")
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsModelConfig")
            val ttsConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsConfig")

            // Build OfflineTtsVitsModelConfig
            val vitsConfig = vitsConfigClass.getDeclaredConstructor().newInstance()
            vitsConfigClass.getDeclaredField("model").set(vitsConfig, modelPath)
            vitsConfigClass.getDeclaredField("tokens").set(vitsConfig, tokensPath)
            vitsConfigClass.getDeclaredField("dataDir").set(vitsConfig, dataDir)
            vitsConfigClass.getDeclaredField("lexicon").set(vitsConfig, lexiconPath)
            vitsConfigClass.getDeclaredField("lengthScale").setFloat(vitsConfig, 1.0f)

            // Build OfflineTtsModelConfig
            val modelConfig = modelConfigClass.getDeclaredConstructor().newInstance()
            modelConfigClass.getDeclaredField("vits").set(modelConfig, vitsConfig)
            modelConfigClass.getDeclaredField("numThreads").setInt(modelConfig, 2)
            modelConfigClass.getDeclaredField("debug").setBoolean(modelConfig, false)

            // Build OfflineTtsConfig
            val ttsConfig = ttsConfigClass.getDeclaredConstructor().newInstance()
            ttsConfigClass.getDeclaredField("model").set(ttsConfig, modelConfig)

            // Create OfflineTts instance
            val ttsClass = Class.forName(className)
            val constructor = ttsClass.declaredConstructors.first {
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == android.content.res.AssetManager::class.java
            }
            tts = constructor.newInstance(null, ttsConfig)
            _isInitialized.value = true
            _engineInfo.value = "OmniVoice Neural TTS ready — ${ttsDir.name}"
            true
        } catch (e: ClassNotFoundException) {
            _engineInfo.value = "OmniVoice — sherpa-onnx library not loaded (AAR missing)"
            false
        } catch (e: Exception) {
            _engineInfo.value = "OmniVoice — TTS init error: ${e.localizedMessage}"
            false
        }
    }

    /**
     * Initialize the ASR engine using sherpa-onnx OnlineRecognizer.
     */
    fun initializeAsr(): Boolean {
        if (recognizer != null) return true
        val asrDir = getAsrDir() ?: run {
            return false
        }

        return try {
            val encoderFile = asrDir.listFiles { it.name.contains("encoder") && it.name.endsWith(".onnx") }?.first()?.absolutePath
            val decoderFile = asrDir.listFiles { it.name.contains("decoder") && it.name.endsWith(".onnx") }?.first()?.absolutePath
            val joinerFile = asrDir.listFiles { it.name.contains("joiner") && it.name.endsWith(".onnx") }?.first()?.absolutePath
            val tokensPath = File(asrDir, "tokens.txt").absolutePath

            if (encoderFile == null || decoderFile == null || joinerFile == null) return false

            val transducerClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig")
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineModelConfig")
            val recognizerConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizerConfig")
            val featureClass = Class.forName("com.k2fsa.sherpa.onnx.FeatureConfig")
            val endpointClass = Class.forName("com.k2fsa.sherpa.onnx.EndpointConfig")

            // Build OnlineTransducerModelConfig
            val transducerConfig = transducerClass.getDeclaredConstructor().newInstance()
            transducerClass.getDeclaredField("encoder").set(transducerConfig, encoderFile)
            transducerClass.getDeclaredField("decoder").set(transducerConfig, decoderFile)
            transducerClass.getDeclaredField("joiner").set(transducerConfig, joinerFile)

            // Build OnlineModelConfig
            val modelConfig = modelConfigClass.getDeclaredConstructor().newInstance()
            modelConfigClass.getDeclaredField("transducer").set(modelConfig, transducerConfig)
            modelConfigClass.getDeclaredField("tokens").set(modelConfig, tokensPath)
            modelConfigClass.getDeclaredField("numThreads").setInt(modelConfig, 2)
            modelConfigClass.getDeclaredField("modelType").set(modelConfig, "zipformer")

            // Build FeatureConfig
            val featConfig = featureClass.getDeclaredConstructor().newInstance()

            // Build EndpointConfig
            val endpointConfig = endpointClass.getDeclaredConstructor().newInstance()

            // Build OnlineRecognizerConfig
            val recognizerConfig = recognizerConfigClass.getDeclaredConstructor().newInstance()
            recognizerConfigClass.getDeclaredField("featConfig").set(recognizerConfig, featConfig)
            recognizerConfigClass.getDeclaredField("modelConfig").set(recognizerConfig, modelConfig)
            recognizerConfigClass.getDeclaredField("endpointConfig").set(recognizerConfig, endpointConfig)
            recognizerConfigClass.getDeclaredField("enableEndpoint").setBoolean(recognizerConfig, true)

            val recognizerClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer")
            val constructor = recognizerClass.declaredConstructors.first {
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == android.content.res.AssetManager::class.java
            }
            recognizer = constructor.newInstance(null, recognizerConfig)
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Speak text using OmniVoice neural TTS.
     * Returns true if successful, false if engine not available.
     */
    fun speak(text: String, speed: Float = 1.0f): Boolean {
        val ttsInstance = tts ?: run {
            if (!initializeTts()) return false
            tts ?: return false
        }

        return try {
            _isSpeaking.value = true
            val ttsClass = ttsInstance.javaClass
            val generateMethod = ttsClass.getMethod("generate", String::class.java, Int::class.java, Float::class.java)
            val audio = generateMethod.invoke(ttsInstance, text, 0, speed * preferences.voiceSpeed)
                ?: return false

            // Get samples and sampleRate from GeneratedAudio
            val audioClass = audio.javaClass
            val samplesField = audioClass.getDeclaredField("samples")
            val samples = samplesField.get(audio) as FloatArray
            val sampleRateField = audioClass.getDeclaredField("sampleRate")
            val sampleRate = sampleRateField.getInt(audio)

            playAudio(samples, sampleRate)
            true
        } catch (_: Exception) {
            _isSpeaking.value = false
            false
        }
    }

    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        try {
            audioTrack?.stop()
            audioTrack?.release()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val minBufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufSize.coerceAtLeast(samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // Convert float samples to 16-bit PCM
            val pcmSamples = ShortArray(samples.size)
            for (i in samples.indices) {
                pcmSamples[i] = (samples[i] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            audioTrack?.write(pcmSamples, 0, pcmSamples.size)
            audioTrack?.play()

            // Monitor playback completion
            scope.launch {
                while (isActive && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    if (audioTrack?.playbackHeadPosition ?: 0 >= pcmSamples.size) break
                    Thread.sleep(100)
                }
                mainHandler.post {
                    _isSpeaking.value = false
                    audioTrack?.release()
                    audioTrack = null
                }
            }
        } catch (_: Exception) {
            _isSpeaking.value = false
            audioTrack?.release()
            audioTrack = null
        }
    }

    fun stopSpeaking() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        _isSpeaking.value = false
    }

    fun stopListening() {
        recognitionJob?.cancel()
        recognitionJob = null
        _isListening.value = false
        _partialText.value = ""
    }

    fun shutdown() {
        stopSpeaking()
        stopListening()
        try {
            tts?.let {
                val releaseMethod = it.javaClass.getMethod("release")
                releaseMethod.invoke(it)
            }
            recognizer?.let {
                val releaseMethod = it.javaClass.getMethod("release")
                releaseMethod.invoke(it)
            }
        } catch (_: Exception) {}
        tts = null
        recognizer = null
        _isInitialized.value = false
    }

    val currentEngineInfo: String
        get() = _engineInfo.value
}
