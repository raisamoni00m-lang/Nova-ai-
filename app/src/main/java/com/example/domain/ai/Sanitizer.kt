package com.example.domain.ai

object Sanitizer {
    private val OTP_PATTERN = Regex("\\b(?:otp|code|pin|password|passcode)\\s*(?:is|:)?\\s*(\\d{4,8}|[A-Za-z0-9]{6,12})\\b", RegexOption.IGNORE_CASE)
    private val CARD_PATTERN = Regex("\\b(?:\\d[ -]*?){13,16}\\b")
    private val CVV_PATTERN = Regex("\\b(?:cvv|cvc|security code)\\s*(?:is|:)?\\s*(\\d{3,4})\\b", RegexOption.IGNORE_CASE)
    private val PASSWORD_KEYWORDS = Regex("\\b(?:password|passwd|pin)\\s*[:=]\\s*(\\S+)", RegexOption.IGNORE_CASE)

    fun sanitizeUserPrompt(input: String): String {
        var sanitized = input
        sanitized = OTP_PATTERN.replace(sanitized) { matchResult ->
            val prefix = matchResult.value.substring(0, matchResult.value.lastIndexOf(matchResult.groupValues[1]))
            "$prefix[REDACTED_SENSITIVE_CODE]"
        }
        sanitized = CARD_PATTERN.replace(sanitized, "[REDACTED_CARD_NUMBER]")
        sanitized = CVV_PATTERN.replace(sanitized, "[REDACTED_CVV]")
        sanitized = PASSWORD_KEYWORDS.replace(sanitized, "[REDACTED_CREDENTIAL]")
        return sanitized
    }

    fun isSensitiveOperation(toolName: String, args: Map<String, Any?>): Boolean {
        return when (toolName.lowercase()) {
            "call_contact", "send_sms", "whatsapp_message" -> true
            "clear_all_memory", "delete_memory" -> true
            "type_text" -> {
                val text = args["text"]?.toString() ?: ""
                text.contains("password", ignoreCase = true) || text.contains("pin", ignoreCase = true)
            }
            else -> false
        }
    }
}
