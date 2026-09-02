package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NovaApplication
import com.example.data.local.entity.AutomationEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.PrivacyAuditEntity
import com.example.domain.ai.ActionResult
import com.example.domain.ai.ToolCall
import com.example.domain.controller.ScreenReaderController
import com.example.domain.voice.OmniVoiceEngine
import com.example.domain.voice.OmniVoiceModelManager
import com.example.domain.voice.SpeechState
import com.example.service.NovaAccessibilityService
import com.example.service.NovaNotificationItem
import com.example.service.NovaNotificationListenerService
import com.example.service.NovaWakeWordForegroundService
import com.example.ui.components.AssistantVisualState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NovaApplication
    private val repository = app.repository
    private val preferences = app.preferences
    private val aiBrain = app.aiBrain
    private val deviceController = app.deviceController
    private val ttsManager = app.ttsManager
    private val speechRecognizer = app.speechRecognizer
    val omniVoiceEngine = app.omniVoiceEngine
    val modelManager = app.modelManager
    val screenReader = app.screenReader

    // --- State flows ---
    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automations: StateFlow<List<AutomationEntity>> = repository.allAutomations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audits: StateFlow<List<PrivacyAuditEntity>> = repository.allAudits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechState: StateFlow<SpeechState> = speechRecognizer.speechState
    val audioRmsDb: StateFlow<Float> = speechRecognizer.audioRmsDb
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    val isAccessibilityActive: StateFlow<Boolean> = NovaAccessibilityService.isServiceActive
    val isNotificationListenerActive: StateFlow<Boolean> = NovaNotificationListenerService.isNotificationAccessEnabled
    val recentNotifications: StateFlow<List<NovaNotificationItem>> = NovaNotificationListenerService.recentNotifications

    val isWakeWordActive: StateFlow<Boolean> = NovaWakeWordForegroundService.isWakeWordActive

    // Screen Reader state
    val screenReaderActive: StateFlow<Boolean> = screenReader.isActive
    val screenReaderElements: StateFlow<List<ScreenReaderController.ScreenElement>> = screenReader.elements
    val screenReaderIndex: StateFlow<Int> = screenReader.currentIndex
    val screenReaderAutoAdvance: StateFlow<Boolean> = screenReader.autoAdvance

    // OmniVoice state
    val omniVoiceReady: StateFlow<Boolean> = omniVoiceEngine.isInitialized
    val omniVoiceInfo: StateFlow<String> = omniVoiceEngine.engineInfo
    val modelDownloadProgress: StateFlow<Float?> = modelManager.downloadProgress
    val isModelDownloading: StateFlow<Boolean> = modelManager.isDownloading
    val downloadStatus: StateFlow<String?> = modelManager.downloadStatus

    private val _visualState = MutableStateFlow(AssistantVisualState.IDLE)
    val visualState: StateFlow<AssistantVisualState> = _visualState.asStateFlow()

    private val _activeExecutingTool = MutableStateFlow<ToolCall?>(null)
    val activeExecutingTool: StateFlow<ToolCall?> = _activeExecutingTool.asStateFlow()

    private val _latestAssistantResponse = MutableStateFlow("Hi! I'm Nova. How can I assist you today?")
    val latestAssistantResponse: StateFlow<String> = _latestAssistantResponse.asStateFlow()

    private val _pendingConfirmationTool = MutableStateFlow<ToolCall?>(null)
    val pendingConfirmationTool: StateFlow<ToolCall?> = _pendingConfirmationTool.asStateFlow()

    // Preferences exposed
    val languagePreference = MutableStateFlow(preferences.language)
    val voicePitch = MutableStateFlow(preferences.voicePitch)
    val voiceSpeed = MutableStateFlow(preferences.voiceSpeed)
    val voiceEngine = MutableStateFlow(preferences.voiceEngine)
    val omnivoicePreset = MutableStateFlow(preferences.omnivoicePreset)
    val autoSpeak = MutableStateFlow(preferences.autoSpeak)
    val wakeWordEnabled = MutableStateFlow(preferences.wakeWordEnabled)
    val customApiKey = MutableStateFlow(preferences.customApiKey)
    val isDarkMode = MutableStateFlow(preferences.isDarkMode)

    // Camera Vision & Screen Reader State
    private val _isVisionAnalyzing = MutableStateFlow(false)
    val isVisionAnalyzing: StateFlow<Boolean> = _isVisionAnalyzing.asStateFlow()

    private val _visionAnalysisResult = MutableStateFlow<String?>(null)
    val visionAnalysisResult: StateFlow<String?> = _visionAnalysisResult.asStateFlow()

    private val _showCameraVisionDialog = MutableStateFlow(false)
    val showCameraVisionDialog: StateFlow<Boolean> = _showCameraVisionDialog.asStateFlow()

    init {
        // Collect speech state changes for visualizer
        viewModelScope.launch {
            speechRecognizer.speechState.collect { state ->
                when (state) {
                    is SpeechState.Listening -> _visualState.value = AssistantVisualState.LISTENING
                    is SpeechState.Processing -> _visualState.value = AssistantVisualState.THINKING
                    is SpeechState.Result -> {}
                    is SpeechState.Idle, is SpeechState.Error -> {
                        if (!ttsManager.isSpeaking.value && _visualState.value != AssistantVisualState.EXECUTING) {
                            _visualState.value = AssistantVisualState.IDLE
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _visualState.value = AssistantVisualState.SPEAKING
                } else if (_visualState.value == AssistantVisualState.SPEAKING) {
                    _visualState.value = AssistantVisualState.IDLE
                }
            }
        }

        // Auto-seed default starter memories & automations including creators Mizan & Ratul
        viewModelScope.launch(Dispatchers.IO) {
            val existingMemories = repository.searchMemories("")
            if (existingMemories.isEmpty()) {
                repository.saveMemory("Owners & Creators", "Mizan & Ratul (মিজান এবং রাতুল) - The creators and owners of Nova AI Assistant", "fact")
                repository.saveMemory("Assistant Persona", "Sweet, intelligent, natural female voice assistant fluent in Bengali, Banglish, and English", "preference")
                repository.saveMemory("Primary Language", "Bengali (বাংলা), Banglish, and English", "preference")
                repository.saveMemory("Default Search Engine", "Google Search", "preference")
            }

            val existingRoutines = repository.getEnabledAutomations()
            if (existingRoutines.isEmpty()) {
                repository.saveAutomation(
                    title = "Good Morning Routine",
                    triggerPhrase = "Good morning",
                    actionsJson = """[{"tool":"notification_reader","args":{}},{"tool":"volume","args":{"action":"set","level_percent":60}}]"""
                )
                repository.saveAutomation(
                    title = "Flashlight Toggle",
                    triggerPhrase = "Torch on",
                    actionsJson = """[{"tool":"flashlight","args":{"state":"on"}}]"""
                )
            }
        }
    }

    // --- Voice Input Actions ---

    fun toggleVoiceListening() {
        if (speechState.value is SpeechState.Listening) {
            speechRecognizer.stopListening()
            _visualState.value = AssistantVisualState.IDLE
        } else {
            ttsManager.stop()
            speechRecognizer.startListening { transcribedText ->
                processUserPrompt(transcribedText)
            }
        }
    }

    fun processUserPrompt(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        viewModelScope.launch {
            _visualState.value = AssistantVisualState.THINKING
            
            // 1. Save user chat message
            repository.addMessage(sender = "USER", text = cleanPrompt)

            // 2. Fetch context (memories, history, apps, screen)
            val history = messages.value.takeLast(6).map { it.sender to it.text }
            val mems = memories.value.map { "${it.key}: ${it.value}" }
            val apps = deviceController.getAllInstalledAppNames()
            val screenContext = if (isAccessibilityActive.value) {
                NovaAccessibilityService.instance?.extractVisibleScreenText()
            } else null

            // 3. Plan AI response & tools
            val plan = aiBrain.processUserSpeech(
                rawPrompt = cleanPrompt,
                conversationHistory = history,
                userMemories = mems,
                availableApps = apps,
                screenContext = screenContext
            )

            _latestAssistantResponse.value = plan.spokenResponse

            // 4. Handle tools / confirmations
            if (plan.toolCalls.isNotEmpty()) {
                val firstTool = plan.toolCalls.first()
                if (firstTool.isRisky && preferences.requireActionConfirmations) {
                    _pendingConfirmationTool.value = firstTool
                    val confirmMsg = "Nova requests confirmation to perform: ${firstTool.toolName} with ${firstTool.arguments}"
                    repository.addMessage(
                        sender = "NOVA",
                        text = plan.spokenResponse + "\n\n⚠️ " + confirmMsg,
                        toolName = firstTool.toolName,
                        toolStatus = "PENDING_CONFIRMATION",
                        isRiskyAction = true
                    )
                    speakResponse(plan.spokenResponse, plan.language)
                } else {
                    executePlannedTools(plan.toolCalls, plan.spokenResponse, plan.language)
                }
            } else {
                // Conversational only
                repository.addMessage(sender = "NOVA", text = plan.spokenResponse)
                speakResponse(plan.spokenResponse, plan.language)
            }
        }
    }

    fun confirmPendingAction() {
        val tool = _pendingConfirmationTool.value ?: return
        _pendingConfirmationTool.value = null
        viewModelScope.launch {
            repository.logAudit(tool.toolName, "User confirmed action: ${tool.arguments}", "CONFIRMED", "MEDIUM")
            executePlannedTools(listOf(tool), "Proceeding with confirmed action.", "en")
        }
    }

    fun dismissPendingAction() {
        val tool = _pendingConfirmationTool.value
        _pendingConfirmationTool.value = null
        if (tool != null) {
            viewModelScope.launch {
                repository.logAudit(tool.toolName, "User rejected action: ${tool.arguments}", "BLOCKED", "MEDIUM")
                repository.addMessage(sender = "NOVA", text = "Action cancelled.")
                speakResponse("Action cancelled.", "en")
            }
        }
    }

    private suspend fun executePlannedTools(tools: List<ToolCall>, initialSpeech: String, language: String) {
        _visualState.value = AssistantVisualState.EXECUTING
        val resultsSummary = StringBuilder()

        for (tool in tools) {
            _activeExecutingTool.value = tool
            kotlinx.coroutines.delay(400) // Brief animation aura so user sees the glowing tool execution HUD
            val result = deviceController.executeTool(tool)
            when (result) {
                is ActionResult.Success -> {
                    resultsSummary.append("✓ ${result.message}\n")
                    repository.logAudit(tool.toolName, "Executed: ${tool.arguments}. Result: ${result.message}", "COMPLETED", "LOW")
                    repository.addMessage(
                        sender = "NOVA",
                        text = "$initialSpeech\n\n${result.message}",
                        toolName = tool.toolName,
                        toolStatus = "SUCCESS",
                        toolResultDetail = result.detail
                    )
                }
                is ActionResult.Failure -> {
                    resultsSummary.append("✗ ${result.error}\n")
                    repository.logAudit(tool.toolName, "Failed: ${tool.arguments}. Reason: ${result.error}", "FAILED", "HIGH")
                    repository.addMessage(
                        sender = "NOVA",
                        text = "I tried to ${tool.toolName}, but encountered an issue: ${result.error}",
                        toolName = tool.toolName,
                        toolStatus = "FAILED",
                        toolResultDetail = result.reason
                    )
                }
                is ActionResult.ScreenContent -> {
                    performScreenReader(result.text)
                }
                is ActionResult.RequiresConfirmation -> {
                    _pendingConfirmationTool.value = result.toolCall
                }
            }

            if (tool.toolName.equals("camera_vision", ignoreCase = true)) {
                _showCameraVisionDialog.value = true
            }
            if (tool.toolName.equals("set_voice_engine", ignoreCase = true)) {
                val engine = tool.arguments["engine"]?.toString() ?: "omnivoice"
                updateVoiceEngine(engine)
            }
        }
        _activeExecutingTool.value = null

        val speechText = if (resultsSummary.isNotBlank()) {
            "$initialSpeech\n${resultsSummary.toString().trim()}"
        } else {
            initialSpeech
        }
        speakResponse(speechText, language)
    }

    // --- Camera Vision & Screen Reader Controls ---

    fun openCameraVision() {
        _showCameraVisionDialog.value = true
        _visionAnalysisResult.value = null
    }

    fun closeCameraVision() {
        _showCameraVisionDialog.value = false
    }

    fun performScreenReader(customScreenText: String? = null) {
        viewModelScope.launch {
            _isVisionAnalyzing.value = true
            _visualState.value = AssistantVisualState.THINKING

            val textContext = customScreenText ?: if (isAccessibilityActive.value) {
                NovaAccessibilityService.instance?.extractVisibleScreenText()
            } else null

            val screenText = if (textContext.isNullOrBlank()) {
                "No accessible text detected. Please ensure Nova Accessibility Service is turned on in device settings."
            } else {
                textContext
            }

            val analysis = app.geminiClient.analyzeCameraOrScreen(
                imageBytes = null,
                screenContext = screenText,
                userPrompt = "Describe in natural, sweet, crystal-clear spoken words what is currently displayed on this screen."
            )

            _visionAnalysisResult.value = analysis
            _isVisionAnalyzing.value = false
            _visualState.value = AssistantVisualState.SPEAKING

            repository.logAudit("screen_reader", "Read screen: ${screenText.take(100)}", "COMPLETED", "LOW")
            repository.addMessage(
                sender = "NOVA",
                text = "📱 **Screen Reader Analysis:**\n\n$analysis",
                toolName = "read_screen",
                toolStatus = "SUCCESS",
                toolResultDetail = screenText.take(200)
            )

            speakResponse(analysis)
        }
    }

    fun analyzeCameraBitmap(bitmap: android.graphics.Bitmap, userPrompt: String = "") {
        viewModelScope.launch {
            _isVisionAnalyzing.value = true
            _visualState.value = AssistantVisualState.THINKING

            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            val imageBytes = stream.toByteArray()

            val prompt = if (userPrompt.isNotBlank()) userPrompt else "Describe in rich, sweet detail everything visible in front of the camera. Mention key objects, people, text, colors, and layout."

            val analysis = app.geminiClient.analyzeCameraOrScreen(
                imageBytes = imageBytes,
                screenContext = null,
                userPrompt = prompt
            )

            _visionAnalysisResult.value = analysis
            _isVisionAnalyzing.value = false
            _visualState.value = AssistantVisualState.SPEAKING

            repository.logAudit("camera_vision", "Analyzed image: ${prompt.take(50)}", "COMPLETED", "LOW")
            repository.addMessage(
                sender = "NOVA",
                text = "📷 **Camera Vision Analysis:**\n\n$analysis",
                toolName = "camera_vision",
                toolStatus = "SUCCESS",
                toolResultDetail = "Visual inspection completed (${imageBytes.size / 1024} KB)"
            )

            speakResponse(analysis)
        }
    }

    fun updateVoiceEngine(engine: String) {
        preferences.voiceEngine = engine
        voiceEngine.value = engine
        ttsManager.setupVoice()
    }

    fun updateOmniVoicePreset(preset: String) {
        preferences.omnivoicePreset = preset
        omnivoicePreset.value = preset
        ttsManager.setupVoice()
    }

    fun applyVoicePreset(pitch: Float, speed: Float) {
        updateVoicePitch(pitch)
        updateVoiceSpeed(speed)
        speakResponse("নমস্কার! আমি মিষ্টি নোভা। OmniVoice ও ক্যামেরা ভিশন দিয়ে আপনার দিনটি সহজ করতে আমি তৈরি।", "bn")
    }

    fun speakResponse(text: String, language: String? = null) {
        if (preferences.autoSpeak) {
            ttsManager.speak(text, language)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    // --- Memory Operations ---

    fun saveMemory(key: String, value: String, category: String = "general") {
        viewModelScope.launch {
            repository.saveMemory(key, value, category)
            repository.logAudit("save_memory", "Saved memory: '$key' = '$value'", "COMPLETED", "LOW")
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemoryById(id)
            repository.logAudit("delete_memory", "Deleted memory id: $id", "COMPLETED", "LOW")
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
            repository.logAudit("clear_all_memory", "User cleared entire local memory store", "COMPLETED", "MEDIUM")
        }
    }

    fun clearAllChatMessages() {
        viewModelScope.launch {
            repository.clearAllMessages()
            repository.logAudit("clear_chat_history", "User cleared chat transcript", "COMPLETED", "LOW")
        }
    }

    // --- Automations ---

    fun toggleAutomation(automation: AutomationEntity) {
        viewModelScope.launch {
            repository.updateAutomation(automation.copy(isEnabled = !automation.isEnabled))
        }
    }

    fun createAutomation(title: String, triggerPhrase: String, actionTool: String, actionArg: String) {
        viewModelScope.launch {
            val json = """[{"tool":"$actionTool","args":{"$actionArg":"value"}}]"""
            repository.saveAutomation(title, triggerPhrase, json)
            repository.logAudit("create_automation", "Created routine: '$title' triggered by '$triggerPhrase'", "COMPLETED", "LOW")
        }
    }

    fun deleteAutomation(id: Long) {
        viewModelScope.launch {
            repository.deleteAutomation(id)
        }
    }

    // --- Settings & Configuration ---

    fun updateLanguage(lang: String) {
        preferences.language = lang
        languagePreference.value = lang
    }

    fun updateVoicePitch(pitch: Float) {
        preferences.voicePitch = pitch
        voicePitch.value = pitch
        ttsManager.setupVoice()
    }

    fun updateVoiceSpeed(speed: Float) {
        preferences.voiceSpeed = speed
        voiceSpeed.value = speed
        ttsManager.setupVoice()
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        preferences.autoSpeak = enabled
        autoSpeak.value = enabled
    }

    fun toggleWakeWord(enabled: Boolean) {
        preferences.wakeWordEnabled = enabled
        wakeWordEnabled.value = enabled
        if (enabled) {
            NovaWakeWordForegroundService.startService(app)
        } else {
            NovaWakeWordForegroundService.stopService(app)
        }
    }

    fun updateApiKey(key: String) {
        preferences.customApiKey = key
        customApiKey.value = key
    }

    fun toggleDarkMode(dark: Boolean) {
        preferences.isDarkMode = dark
        isDarkMode.value = dark
    }

    // --- Screen Reader Actions ---

    fun startScreenReader() {
        screenReader.startReading()
    }

    fun stopScreenReader() {
        screenReader.stopReading()
    }

    fun screenReaderNext() {
        screenReader.nextElement()
    }

    fun screenReaderPrevious() {
        screenReader.previousElement()
    }

    fun toggleScreenReaderAutoAdvance() {
        screenReader.toggleAutoAdvance()
    }

    // --- OmniVoice Model Management ---

    fun downloadOmniVoiceModel(model: OmniVoiceModelManager.ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            modelManager.downloadModel(model)
            if (modelManager.isModelDownloaded(model.name)) {
                omniVoiceEngine.initializeTts()
                omniVoiceEngine.initializeAsr()
            }
        }
    }

    fun deleteOmniVoiceModel(modelName: String) {
        modelManager.deleteModel(modelName)
    }

    fun getDownloadedModels(): List<String> = modelManager.listDownloadedModels()

    fun getAvailableModels(): List<OmniVoiceModelManager.ModelInfo> = modelManager.availableModels

    override fun onCleared() {
        ttsManager.shutdown()
        speechRecognizer.stopListening()
        omniVoiceEngine.shutdown()
        screenReader.stopReading()
        super.onCleared()
    }
}
