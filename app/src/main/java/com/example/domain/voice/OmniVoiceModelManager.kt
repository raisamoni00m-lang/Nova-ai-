package com.example.domain.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages downloading and updating OmniVoice (sherpa-onnx) neural model files.
 *
 * Models are stored in app private storage at filesDir/omnivoice-models/.
 * Each model lives in its own subdirectory (e.g., vits-mms-bn/, sherpa-onnx-streaming-zipformer-en/).
 *
 * Recommended TTS models (VITS):
 * - Bengali: vits-mms-bn (from HuggingFace csukuangfj/vits-mms-bn)
 * - English: vits-piper-en_US-amy-medium
 * - Multilingual: vits-mms-300k-multilingual-v1 (supports 1000+ languages including Bengali)
 *
 * Recommended ASR models (streaming Zipformer):
 * - English: sherpa-onnx-streaming-zipformer-en-2023-06-26
 * - Bengali: sherpa-onnx-streaming-zipformer-bn (if available) or Whisper-based models
 */
class OmniVoiceModelManager(private val context: Context) {

    private val modelDir: File by lazy {
        File(context.filesDir, "omnivoice-models").apply { mkdirs() }
    }

    data class ModelInfo(
        val name: String,
        val displayName: String,
        val description: String,
        val downloadUrl: String,
        val type: String, // "tts" or "asr"
        val estimatedSizeMb: Int
    )

    val availableModels: List<ModelInfo> = listOf(
        ModelInfo(
            name = "vits-mms-bn",
            displayName = "OmniVoice Bengali TTS",
            description = "Neural VITS voice synthesis for Bengali (বাংলা). Female voice, offline.",
            downloadUrl = "https://huggingface.co/csukuangfj/vits-mms-bn/resolve/main/model.onnx",
            type = "tts",
            estimatedSizeMb = 80
        ),
        ModelInfo(
            name = "vits-piper-en_US-amy-medium",
            displayName = "OmniVoice English TTS",
            description = "Neural Piper VITS voice synthesis for English. Female voice (Amy), offline.",
            downloadUrl = "https://huggingface.co/csukuangfj/vits-piper-en_US-amy-medium/resolve/main/en_US-amy-medium.onnx",
            type = "tts",
            estimatedSizeMb = 65
        ),
        ModelInfo(
            name = "sherpa-onnx-streaming-zipformer-en-2023-06-26",
            displayName = "OmniVoice English ASR",
            description = "Real-time speech recognition for English. Streaming Zipformer, offline.",
            downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26",
            type = "asr",
            estimatedSizeMb = 300
        )
    )

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    fun modelDirectory(): String = modelDir.absolutePath

    fun isModelDownloaded(modelName: String): Boolean {
        val dir = File(modelDir, modelName)
        return dir.exists() && dir.isDirectory && (dir.listFiles()?.isNotEmpty() == true)
    }

    fun listDownloadedModels(): List<String> {
        return modelDir.listFiles { f -> f.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }

    /**
     * Downloads a model from its HuggingFace URL.
     * TTS models are single .onnx files (plus tokens.txt).
     * ASR models are directories with multiple files.
     */
    suspend fun downloadModel(model: ModelInfo): Boolean = withContext(Dispatchers.IO) {
        if (_isDownloading.value) return@withContext false

        _isDownloading.value = true
        _downloadStatus.value = "Downloading ${model.displayName}..."

        return@withContext try {
            val targetDir = File(modelDir, model.name).apply { mkdirs() }

            if (model.type == "tts") {
                downloadTtsModel(model, targetDir)
            } else {
                _downloadStatus.value = "ASR models require multiple files. See https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html"
                _isDownloading.value = false
                return@withContext false
            }

            _downloadStatus.value = "${model.displayName} downloaded successfully!"
            _downloadProgress.value = null
            true
        } catch (e: Exception) {
            _downloadStatus.value = "Download failed: ${e.localizedMessage}"
            false
        } finally {
            _isDownloading.value = false
        }
    }

    private fun downloadTtsModel(model: ModelInfo, targetDir: File) {
        // Download the main .onnx model file
        val modelFile = File(targetDir, if (model.name.startsWith("vits-piper")) "en_US-amy-medium.onnx" else "model.onnx")
        downloadFile(model.downloadUrl, modelFile)

        // Download tokens.txt
        val tokensUrl = model.downloadUrl.substringBeforeLast("/") + "/tokens.txt"
        downloadFile(tokensUrl, File(targetDir, "tokens.txt"))

        // Download lexicon if available (for VITS models)
        val lexiconUrl = model.downloadUrl.substringBeforeLast("/") + "/lexicon.txt"
        try {
            downloadFile(lexiconUrl, File(targetDir, "lexicon.txt"))
        } catch (_: Exception) { /* optional file */ }

        // Download espeak-ng-data if available (for MMS models)
        val espeakUrl = model.downloadUrl.substringBeforeLast("/") + "/espeak-ng-data.tar.bz2"
        try {
            val espeakFile = File(targetDir, "espeak-ng-data.tar.bz2")
            downloadFile(espeakUrl, espeakFile)
            // Note: actual extraction would require a tar/bz2 decompressor
            // On Android, this can be done via ProcessBuilder or a library
        } catch (_: Exception) { /* optional for some models */ }
    }

    private fun downloadFile(urlStr: String, target: File) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true

        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw Exception("HTTP ${conn.responseCode} for ${urlStr.take(60)}...")
        }

        val totalBytes = conn.contentLengthLong
        var downloadedBytes = 0L
        val buffer = ByteArray(8192)

        conn.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } > 0) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = downloadedBytes.toFloat() / totalBytes
                        _downloadProgress.value = progress
                    }
                    _downloadStatus.value = "Downloading... ${(downloadedBytes / 1024 / 1024)}MB"
                }
            }
        }
        conn.disconnect()
    }

    fun deleteModel(modelName: String) {
        val dir = File(modelDir, modelName)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        _downloadStatus.value = "Deleted $modelName"
    }
}
