package com.example

import android.app.Application
import com.example.data.local.NovaDatabase
import com.example.data.preferences.NovaPreferences
import com.example.data.remote.GeminiClient
import com.example.data.repository.NovaRepository
import com.example.domain.ai.NovaAIBrain
import com.example.domain.controller.DeviceActionController
import com.example.domain.voice.NovaSpeechRecognizer
import com.example.domain.voice.NovaTTSManager

class NovaApplication : Application() {

    lateinit var database: NovaDatabase
        private set

    lateinit var repository: NovaRepository
        private set

    lateinit var preferences: NovaPreferences
        private set

    lateinit var geminiClient: GeminiClient
        private set

    lateinit var aiBrain: NovaAIBrain
        private set

    lateinit var deviceController: DeviceActionController
        private set

    lateinit var ttsManager: NovaTTSManager
        private set

    lateinit var speechRecognizer: NovaSpeechRecognizer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = NovaPreferences(this)
        database = NovaDatabase.getInstance(this)
        repository = NovaRepository(
            memoryDao = database.memoryDao(),
            chatMessageDao = database.chatMessageDao(),
            automationDao = database.automationDao(),
            privacyAuditDao = database.privacyAuditDao()
        )

        geminiClient = GeminiClient { preferences.customApiKey }
        aiBrain = NovaAIBrain(geminiClient)
        deviceController = DeviceActionController(this)
        ttsManager = NovaTTSManager(this, preferences)
        speechRecognizer = NovaSpeechRecognizer(this, preferences)
    }

    companion object {
        lateinit var instance: NovaApplication
            private set
    }
}
