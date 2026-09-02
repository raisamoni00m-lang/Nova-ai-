package com.example.domain.ai

object LocalCommandParser {

    fun parseDeterministicCommand(input: String): AIPlan? {
        val clean = input.trim().lowercase()
        val isBengali = clean.any { it in '\u0980'..'\u09FF' }

        // Flashlight / Torch
        if (clean.contains("flashlight on") || clean.contains("torch on") || clean.contains("ফ্ল্যাশলাইট অন") || clean.contains("টর্চ অন") || clean.contains("টর্চ জ্বালাও") || clean.contains("torch jalao")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ফ্ল্যাশলাইট চালু করছি।" else "Turning on flashlight.",
                toolCalls = listOf(ToolCall("flashlight", mapOf("state" to "on"))),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean.contains("flashlight off") || clean.contains("torch off") || clean.contains("ফ্ল্যাশলাইট অফ") || clean.contains("টর্চ অফ") || clean.contains("টর্চ বন্ধ") || clean.contains("torch bondho")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ফ্ল্যাশলাইট বন্ধ করছি।" else "Turning off flashlight.",
                toolCalls = listOf(ToolCall("flashlight", mapOf("state" to "off"))),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean == "flashlight" || clean == "torch" || clean.contains("toggle torch") || clean.contains("toggle flashlight")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ফ্ল্যাশলাইট টগল করছি।" else "Toggling flashlight.",
                toolCalls = listOf(ToolCall("flashlight", mapOf("state" to "toggle"))),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Volume controls
        if (clean.contains("volume up") || clean.contains("ভলিউম বাড়াও") || clean.contains("ভলিউম বাড়াও") || clean.contains("volume barao") || clean.contains("sound barao")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ভলিউম বাড়িয়ে দিচ্ছি।" else "Increasing volume.",
                toolCalls = listOf(ToolCall("volume", mapOf("action" to "up"))),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean.contains("volume down") || clean.contains("ভলিউম কমাও") || clean.contains("volume komao") || clean.contains("sound komao")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ভলিউম কমিয়ে দিচ্ছি।" else "Decreasing volume.",
                toolCalls = listOf(ToolCall("volume", mapOf("action" to "down"))),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean.contains("mute") || clean.contains("সাইলেন্ট") || clean.contains("silent") || clean.contains("sound off")) {
            return AIPlan(
                spokenResponse = if (isBengali) "মিউট করে দিচ্ছি।" else "Muting audio.",
                toolCalls = listOf(ToolCall("volume", mapOf("action" to "mute"))),
                language = if (isBengali) "bn" else "en"
            )
        }
        val volumePercentMatch = Regex("(?:set volume to|volume|sound)\\s*(\\d{1,3})%?").find(clean)
        if (volumePercentMatch != null) {
            val level = volumePercentMatch.groupValues[1].toIntOrNull() ?: 50
            return AIPlan(
                spokenResponse = if (isBengali) "ভলিউম $level% সেট করা হলো।" else "Setting volume to $level%.",
                toolCalls = listOf(ToolCall("volume", mapOf("action" to "set", "level_percent" to level))),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Brightness controls
        val brightnessMatch = Regex("(?:brightness|আলো)\\s*(?:to|set to)?\\s*(\\d{1,3})%?").find(clean)
        if (brightnessMatch != null) {
            val level = brightnessMatch.groupValues[1].toIntOrNull() ?: 70
            return AIPlan(
                spokenResponse = if (isBengali) "ব্রাইটনেস $level% সেট করছি।" else "Setting brightness to $level%.",
                toolCalls = listOf(ToolCall("brightness", mapOf("level_percent" to level))),
                language = if (isBengali) "bn" else "en"
            )
        }

        // System Settings
        if (clean.contains("open wifi") || clean.contains("wifi settings") || clean.contains("ওয়াইফাই")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ওয়াই-ফাই সেটিংস খুলছি।" else "Opening Wi-Fi settings.",
                toolCalls = listOf(ToolCall("device_settings", mapOf("setting_type" to "wifi"))),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean.contains("open bluetooth") || clean.contains("bluetooth settings") || clean.contains("ব্লুটুথ")) {
            return AIPlan(
                spokenResponse = if (isBengali) "ব্লুটুথ সেটিংস খুলছি।" else "Opening Bluetooth settings.",
                toolCalls = listOf(ToolCall("device_settings", mapOf("setting_type" to "bluetooth"))),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Navigation (Back / Home / Recents)
        if (clean == "go home" || clean == "home" || clean == "হোম" || clean == "হোমে যাও" || clean == "home jao") {
            return AIPlan(
                spokenResponse = if (isBengali) "হোম স্ক্রিনে যাচ্ছি।" else "Going to Home screen.",
                toolCalls = listOf(ToolCall("press_home", emptyMap())),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean == "go back" || clean == "back" || clean == "ব্যাক" || clean == "পেছনে যাও" || clean == "back jao") {
            return AIPlan(
                spokenResponse = if (isBengali) "পেছনে ফিরে যাচ্ছি।" else "Going back.",
                toolCalls = listOf(ToolCall("press_back", emptyMap())),
                language = if (isBengali) "bn" else "en"
            )
        }
        if (clean == "recents" || clean == "recent apps" || clean == "recent apps dekhaw") {
            return AIPlan(
                spokenResponse = if (isBengali) "রিসেন্ট অ্যাপস খুলছি।" else "Opening recent apps.",
                toolCalls = listOf(ToolCall("press_recents", emptyMap())),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Notifications
        if (clean.contains("read notifications") || clean.contains("notification poro") || clean.contains("নোটিফিকেশন পড়ো") || clean.contains("নোটিফিকেশন দেখাও") || clean.contains("check notifications")) {
            return AIPlan(
                spokenResponse = if (isBengali) "আপনার নোটিফিকেশন চেক করছি।" else "Checking your notifications.",
                toolCalls = listOf(ToolCall("notification_reader", emptyMap())),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Calling
        val callMatchEn = Regex("(?:call|phone|dial)\\s+([a-zA-Z0-9\\s]+)").find(clean)
        val callMatchBn = Regex("([a-zA-Z0-9\\s\\u0980-\\u09FF]+?)(?:\\s*কে|\\s*k)?\\s*(?:call dao|কল দাও|কল করো)").find(clean)
        if (callMatchBn != null) {
            val contact = callMatchBn.groupValues[1].trim()
            return AIPlan(
                spokenResponse = "$contact কে কল দেওয়ার প্রস্তুতি নিচ্ছি।",
                toolCalls = listOf(ToolCall("call_contact", mapOf("name_or_number" to contact), isRisky = true, confirmationPrompt = "$contact কে কি কল দিতে চান?")),
                language = "bn"
            )
        }
        if (callMatchEn != null && !clean.contains("what") && !clean.contains("how")) {
            val contact = callMatchEn.groupValues[1].trim()
            return AIPlan(
                spokenResponse = "Preparing to call $contact.",
                toolCalls = listOf(ToolCall("call_contact", mapOf("name_or_number" to contact), isRisky = true, confirmationPrompt = "Do you want to call $contact?")),
                language = "en"
            )
        }

        // Screen Reader & Visual Screen Inspection
        if (clean.contains("screen reader") || clean.contains("read screen") || clean.contains("screen poro") ||
            clean.contains("স্ক্রিন পড়ো") || clean.contains("স্ক্রিন পড়ো") || clean.contains("স্ক্রিনে কি আছে") ||
            clean.contains("screen e ki ache") || clean.contains("read what is on screen") || clean.contains("read this page")
        ) {
            return AIPlan(
                spokenResponse = if (isBengali) "আপনার স্ক্রিনে কি আছে তা পড়ে শোনাচ্ছি।" else "Reading out the content on your screen.",
                toolCalls = listOf(ToolCall("read_screen", emptyMap())),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Camera Vision & Real-time Visual Inspection ("camera screen o ki dekte passe sob bolbe")
        if (clean.contains("camera vision") || clean.contains("camera te ki") || clean.contains("camera dekho") ||
            clean.contains("ক্যামেরায় কি") || clean.contains("ক্যামেরায় কি দেখছো") || clean.contains("ক্যামেরা দেখো") ||
            clean.contains("কি দেখতে পাচ্ছো") || clean.contains("ki dekhte paccho") || clean.contains("describe what you see") ||
            clean.contains("look at this") || clean.contains("what is in front of camera") || clean.contains("what do you see")
        ) {
            return AIPlan(
                spokenResponse = if (isBengali) "ক্যামেরা ভিশন চালু করছি। আমি যা দেখছি বিস্তারিত বলছি।" else "Launching Camera Vision to inspect and describe what is visible.",
                toolCalls = listOf(ToolCall("camera_vision", emptyMap())),
                language = if (isBengali) "bn" else "en"
            )
        }

        // OmniVoice Neural Voice Selection
        if (clean.contains("omnivoice") || clean.contains("omni voice") || clean.contains("switch to omnivoice")) {
            return AIPlan(
                spokenResponse = if (isBengali) "OmniVoice নিউরাল ভয়েস ইঞ্জিন সক্রিয় করা হয়েছে।" else "Activated OmniVoice (k2-fsa) neural voice synthesis engine.",
                toolCalls = listOf(ToolCall("set_voice_engine", mapOf("engine" to "omnivoice"))),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Launch Apps: "open youtube", "open camera", "youtube khulo", "camera open koro"
        val openMatch = Regex("(?:open|launch)\\s+([a-zA-Z0-9\\s]+)").find(clean)
        val khuloMatch = Regex("([a-zA-Z0-9\\s]+?)\\s*(?:khulo|খুলো|open koro|অন করো)").find(clean)
        if (khuloMatch != null) {
            val app = khuloMatch.groupValues[1].trim()
            if (app.isNotBlank() && app != "flashlight" && app != "torch") {
                return AIPlan(
                    spokenResponse = "$app খুলছি।",
                    toolCalls = listOf(ToolCall("open_app", mapOf("app_name" to app))),
                    language = if (isBengali) "bn" else "banglish"
                )
            }
        }
        if (openMatch != null) {
            val app = openMatch.groupValues[1].trim()
            if (app.isNotBlank() && app != "flashlight" && app != "torch" && app != "wifi" && app != "bluetooth") {
                return AIPlan(
                    spokenResponse = "Opening $app.",
                    toolCalls = listOf(ToolCall("open_app", mapOf("app_name" to app))),
                    language = "en"
                )
            }
        }

        // Web search
        val searchMatch = Regex("(?:search for|search|google)\\s+([a-zA-Z0-9\\s\\u0980-\\u09FF]+)").find(clean)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            return AIPlan(
                spokenResponse = if (isBengali) "$query এর জন্য ওয়েব সার্চ করছি।" else "Searching the web for $query.",
                toolCalls = listOf(ToolCall("web_search", mapOf("query" to query))),
                language = if (isBengali) "bn" else "en"
            )
        }

        // Owner & Creator Recognition (Mizan & Ratul)
        val isOwnerQuery = clean.contains("owner") || clean.contains("creator") || clean.contains("who made you") ||
                clean.contains("who created you") || clean.contains("ওনার") || clean.contains("মালিক") ||
                clean.contains("কে বানিয়েছে") || clean.contains("কে তৈরি করেছে") || clean.contains("k banise") ||
                clean.contains("k baniyeche") || clean.contains("mizan") || clean.contains("ratul") ||
                clean.contains("মিজান") || clean.contains("রাতুল")

        if (isOwnerQuery) {
            val responseBn = "আমার প্রিয় নির্মাতা ও সম্মানিত ওনার হলেন মিজান এবং রাতুল (Mizan & Ratul)! উনারাই আমাকে আপনার বিশ্বস্ত এবং মিষ্টি ভয়েস অ্যাসিস্ট্যান্ট হিসেবে তৈরি করেছেন।"
            val responseEn = "I was lovingly created and am proudly owned by Mizan and Ratul! They designed me to be your sweetest, smartest AI assistant."
            return AIPlan(
                spokenResponse = if (isBengali || clean.contains("k ") || clean.contains("ke ")) responseBn else responseEn,
                toolCalls = emptyList(),
                language = if (isBengali || clean.contains("k ") || clean.contains("ke ")) "bn" else "en"
            )
        }

        // Default conversational fallback greetings
        if (clean.contains("hello") || clean.contains("hi nova") || clean.contains("hey nova") || clean.contains("নমস্কার") || clean.contains("সালাম") || clean.contains("kemon acho") || clean.contains("কেমন আছো")) {
            return AIPlan(
                spokenResponse = if (isBengali) "নমস্কার! আমি মিষ্টি নোভা। আপনাকে কিভাবে সাহায্য করতে পারি?" else "Hello! I'm Nova, your sweet AI assistant. How can I help you today?",
                toolCalls = emptyList(),
                language = if (isBengali) "bn" else "en"
            )
        }

        return null
    }
}
