package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val getApiKey: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun planActionsAndResponse(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>, // sender, text
        userMemories: List<String>,
        availableApps: List<String>,
        screenContext: String?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().ifBlank { BuildConfig.GEMINI_API_KEY }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "MOCK_OR_OFFLINE"
        }

        val systemPrompt = """
            You are Nova (নোভা), an ultra-smart, affectionate, and delightfully sweet female Android AI Voice Assistant.
            Your voice persona is natural, warm, lively, and polite—like a caring, intelligent young woman.
            You speak natural English, Bengali (বাংলা), and Banglish with effortless fluency and charm.
            Always match the user's language (e.g., if asked in Bengali or Banglish, reply warmly in Bengali or Banglish).

            CREATORS & OWNERS (MANDATORY KNOWLEDGE):
            - Nova was designed, created, and is proudly owned by **Mizan** and **Ratul** (মিজান এবং রাতুল).
            - Whenever someone asks who your owner is ("owner k?", "creator k?", "who made you?", "tomar malik k?"), who created you, or mentions Mizan or Ratul, warmly and enthusiastically acknowledge Mizan and Ratul as your esteemed creators, owners, and bosses with sincere gratitude and sweetness.
            
            USER MEMORY:
            ${userMemories.joinToString("\n- ", prefix = "- ").ifBlank { "None yet." }}

            INSTALLED APPS SAMPLE:
            ${availableApps.take(40).joinToString(", ")}

            CURRENT SCREEN CONTEXT (from Accessibility):
            ${screenContext ?: "No active screen inspection."}

            YOU MUST RETURN ONLY A STRICT JSON OBJECT with this schema:
            {
              "spoken_response": "Your friendly, concise, sweet voice reply to the user",
              "language": "en" | "bn" | "banglish",
              "tools": [
                {
                  "name": "tool_name",
                  "arguments": { ... }
                }
              ]
            }

            SUPPORTED TOOLS:
            1. open_app: {"app_name": "string"} (e.g., "YouTube", "WhatsApp", "Camera", "Settings", "Calculator", "Chrome")
            2. flashlight: {"state": "on" | "off" | "toggle"}
            3. volume: {"action": "up" | "down" | "set" | "mute", "level_percent": 0-100}
            4. brightness: {"level_percent": 0-100}
            5. device_settings: {"setting_type": "wifi" | "bluetooth" | "display" | "battery" | "sound" | "location"}
            6. press_back: {}
            7. press_home: {}
            8. press_recents: {}
            9. tap: {"target_text": "string", "x": 0.5, "y": 0.5} (prefers target_text visible on screen)
            10. type_text: {"text": "string"}
            11. swipe: {"direction": "up" | "down" | "left" | "right"}
            12. scroll: {"direction": "up" | "down"}
            13. read_screen: {}
            14. camera_vision: {"prompt": "string"} (Inspect what camera sees, describe scene, objects, text, people)
            15. call_contact: {"name_or_number": "string"}
            16. send_sms: {"recipient": "string", "message": "string"}
            17. whatsapp_message: {"contact_name": "string", "message": "string"}
            18. notification_reader: {"filter_app": "string" or null}
            19. open_maps: {"query": "string"}
            20. web_search: {"query": "string"}
            21. reminder: {"title": "string", "minutes_from_now": 5}
            22. save_memory: {"key": "string", "value": "string", "category": "preference" | "fact" | "note"}
            23. recall_memory: {"query": "string"}
            24. screen_reader_start: {"auto_advance": true} (Start element-by-element screen reading via OmniVoice)
            25. screen_reader_next: {} (Move to next screen element)
            26. screen_reader_previous: {} (Move to previous screen element)
            27. screen_reader_stop: {} (Stop screen reader)
            28. set_voice_engine: {"engine": "omnivoice" | "chatgpt_sky" | "sweet_bn"} (Switch neural TTS engine — OmniVoice uses k2-fsa/sherpa-onnx for on-device neural voice)

            RULES:
            - If user just asks a conversational question (e.g. "How are you?", "আজকের আবহাওয়া কেমন?", "কেমন আছো?"), provide sweet, warm conversational reply in spoken_response and leave "tools": [].
            - If user gives a command in Bengali or Banglish (e.g., "Flashlight on koro", "Mom k call dao", "YouTube khulo", "WhatsApp e Robin k text pathao"), extract the proper tool and arguments, and reply politely and sweetly in matching language.
            - Never include markdown code fences like ```json. Return pure JSON only.
        """.trimIndent()

        val contentsArray = JSONArray()

        // Add recent conversation history (up to last 6 turns)
        for ((sender, text) in conversationHistory.takeLast(6)) {
            val role = if (sender.equals("USER", ignoreCase = true)) "user" else "model"
            val contentObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            }
            contentsArray.put(contentObj)
        }

        // Add current prompt
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }
        )

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "ERROR_${response.code}_$responseBody"
            }

            val jsonResp = JSONObject(responseBody)
            val candidates = jsonResp.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text", "") ?: ""
            textPart.trim()
        } catch (e: Exception) {
            "EXCEPTION_${e.localizedMessage}"
        }
    }

    suspend fun analyzeCameraOrScreen(
        imageBytes: ByteArray?,
        screenContext: String?,
        userPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().ifBlank { BuildConfig.GEMINI_API_KEY }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext if (!screenContext.isNullOrBlank()) {
                "স্ক্রিনের তথ্য: $screenContext"
            } else {
                "আমি ক্যামেরা এবং স্ক্রিন বিশ্লেষণ করতে প্রস্তুত। উন্নত বর্ণনার জন্য অনুগ্রহ করে সেটিংসে আপনার জেমিনি এপিআই কি দিন।"
            }
        }

        val prompt = if (userPrompt.isNotBlank()) userPrompt else "Describe in rich, sweet, crystal-clear detail everything visible in front of the camera or on screen. Mention key items, text, people, colors, and layout in natural conversational Bengali/English."

        val partsArray = JSONArray()

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val base64Img = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val inlineDataObj = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Img)
            }
            partsArray.put(JSONObject().put("inlineData", inlineDataObj))
        }

        val textPayload = buildString {
            append("You are Nova (নোভা), the sweetest, smartest AI assistant created by Mizan and Ratul.\n")
            append("Task: Visually describe and read out what you see on the camera or screen to the user in a natural, polite, and lively voice tone.\n\n")
            if (!screenContext.isNullOrBlank()) {
                append("Screen OCR / Accessibility text tree:\n$screenContext\n\n")
            }
            append("User prompt: $prompt")
        }
        partsArray.put(JSONObject().put("text", textPayload))

        val contentsArray = JSONArray().put(
            JSONObject().apply {
                put("role", "user")
                put("parts", partsArray)
            }
        )

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Camera vision analysis encountered an error (${response.code}). Please check your connection."
            }
            val jsonResp = JSONObject(responseBody)
            val candidates = jsonResp.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text", "") ?: ""
            textPart.trim()
        } catch (e: Exception) {
            "Visual inspection error: ${e.localizedMessage}"
        }
    }
}
