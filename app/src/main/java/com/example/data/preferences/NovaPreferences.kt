package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class NovaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_user_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", "auto") ?: "auto" // "auto", "bn", "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var voicePitch: Float
        get() = prefs.getFloat("voice_pitch", 1.16f) // Sweet, expressive female neural timbre
        set(value) = prefs.edit().putFloat("voice_pitch", value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat("voice_speed", 1.00f) // Natural smooth cadence
        set(value) = prefs.edit().putFloat("voice_speed", value).apply()

    var voiceEngine: String
        get() = prefs.getString("voice_engine", "omnivoice") ?: "omnivoice" // "omnivoice", "chatgpt_sky", "sweet_bn"
        set(value) = prefs.edit().putString("voice_engine", value).apply()

    var omnivoicePreset: String
        get() = prefs.getString("omnivoice_preset", "female_expressive") ?: "female_expressive"
        set(value) = prefs.edit().putString("omnivoice_preset", value).apply()

    var screenReaderAutoSpeak: Boolean
        get() = prefs.getBoolean("screen_reader_auto_speak", true)
        set(value) = prefs.edit().putBoolean("screen_reader_auto_speak", value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean("auto_speak", true)
        set(value) = prefs.edit().putBoolean("auto_speak", value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake_word_enabled", false)
        set(value) = prefs.edit().putBoolean("wake_word_enabled", value).apply()

    var wakeWordSensitivity: Float
        get() = prefs.getFloat("wake_word_sensitivity", 0.65f)
        set(value) = prefs.edit().putFloat("wake_word_sensitivity", value).apply()

    var requireActionConfirmations: Boolean
        get() = prefs.getBoolean("require_action_confirmations", true)
        set(value) = prefs.edit().putBoolean("require_action_confirmations", value).apply()

    var customApiKey: String
        get() = prefs.getString("custom_api_key", "") ?: ""
        set(value) = prefs.edit().putString("custom_api_key", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", true)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()
}
