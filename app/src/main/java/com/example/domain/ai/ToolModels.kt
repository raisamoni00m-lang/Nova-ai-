package com.example.domain.ai

import org.json.JSONObject

data class ToolCall(
    val toolName: String,
    val arguments: Map<String, Any?>,
    val isRisky: Boolean = false,
    val confirmationPrompt: String? = null
)

sealed class ActionResult {
    data class Success(val message: String, val detail: String? = null) : ActionResult()
    data class Failure(val error: String, val reason: String? = null) : ActionResult()
    data class RequiresConfirmation(val prompt: String, val toolCall: ToolCall) : ActionResult()
    data class ScreenContent(val text: String) : ActionResult()
}

data class AIPlan(
    val spokenResponse: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val language: String = "en"
)
