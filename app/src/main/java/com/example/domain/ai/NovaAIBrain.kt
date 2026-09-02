package com.example.domain.ai

import com.example.data.remote.GeminiClient
import org.json.JSONObject

class NovaAIBrain(
    private val geminiClient: GeminiClient
) {
    suspend fun processUserSpeech(
        rawPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        userMemories: List<String>,
        availableApps: List<String>,
        screenContext: String? = null
    ): AIPlan {
        // 1. Sanitize input to protect sensitive data (OTPs, passwords, cards)
        val sanitized = Sanitizer.sanitizeUserPrompt(rawPrompt)

        // 2. Try Gemini Brain
        val rawAiResponse = geminiClient.planActionsAndResponse(
            userPrompt = sanitized,
            conversationHistory = conversationHistory,
            userMemories = userMemories,
            availableApps = availableApps,
            screenContext = screenContext
        )

        // If Gemini returned a valid JSON structure, parse it
        if (!rawAiResponse.startsWith("ERROR_") && !rawAiResponse.startsWith("EXCEPTION_") && rawAiResponse != "MOCK_OR_OFFLINE") {
            try {
                val cleanJson = rawAiResponse.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val root = JSONObject(cleanJson)
                val spokenResponse = root.optString("spoken_response", "I'm on it.")
                val language = root.optString("language", "en")
                val toolsArray = root.optJSONArray("tools")

                val toolCalls = mutableListOf<ToolCall>()
                if (toolsArray != null) {
                    for (i in 0 until toolsArray.length()) {
                        val toolObj = toolsArray.optJSONObject(i) ?: continue
                        val name = toolObj.optString("name")
                        val argsObj = toolObj.optJSONObject("arguments") ?: JSONObject()
                        val argsMap = mutableMapOf<String, Any?>()
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            argsMap[key] = argsObj.opt(key)
                        }

                        val isRisky = Sanitizer.isSensitiveOperation(name, argsMap)
                        val prompt = if (isRisky) "Nova wants to perform $name. Do you confirm?" else null
                        toolCalls.add(ToolCall(name, argsMap, isRisky, prompt))
                    }
                }

                return AIPlan(
                    spokenResponse = spokenResponse,
                    toolCalls = toolCalls,
                    language = language
                )
            } catch (_: Exception) {
                // fallback to local parser if JSON parse failed
            }
        }

        // 3. Fallback to Local Command Parser
        val localPlan = LocalCommandParser.parseDeterministicCommand(sanitized)
        if (localPlan != null) {
            return localPlan
        }

        // General fallback
        val isBengali = sanitized.any { it in '\u0980'..'\u09FF' }
        return if (isBengali) {
            AIPlan(
                spokenResponse = "আমি আপনার অনুরোধটি বুঝতে পেরেছি। দয়া করে সেটিংস থেকে জেমিনি এপিআই কি দিন অথবা কমান্ড পুনরায় স্পষ্ট করে বলুন।",
                toolCalls = emptyList(),
                language = "bn"
            )
        } else {
            AIPlan(
                spokenResponse = "I processed your request: \"$sanitized\". Configure a Gemini API key in Settings for full natural conversational reasoning.",
                toolCalls = emptyList(),
                language = "en"
            )
        }
    }
}
