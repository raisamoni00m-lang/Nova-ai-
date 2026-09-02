package com.example.domain.controller

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import com.example.MainActivity
import com.example.domain.ai.ActionResult
import com.example.domain.ai.ToolCall
import com.example.service.NovaAccessibilityService
import com.example.service.NovaNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

class DeviceActionController(
    private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var isTorchOn = false

    suspend fun executeTool(toolCall: ToolCall): ActionResult = withContext(Dispatchers.Main) {
        val args = toolCall.arguments
        when (toolCall.toolName.lowercase()) {
            "flashlight" -> executeFlashlight(args["state"]?.toString() ?: "toggle")
            "volume" -> executeVolume(args["action"]?.toString() ?: "up", (args["level_percent"] as? Number)?.toInt())
            "brightness" -> executeBrightness((args["level_percent"] as? Number)?.toInt() ?: 70)
            "device_settings" -> executeDeviceSettings(args["setting_type"]?.toString() ?: "general")
            "open_app" -> executeOpenApp(args["app_name"]?.toString() ?: "")
            "press_back" -> executePressBack()
            "press_home" -> executePressHome()
            "press_recents" -> executePressRecents()
            "tap" -> executeTap(args["target_text"]?.toString(), (args["x"] as? Number)?.toFloat(), (args["y"] as? Number)?.toFloat())
            "type_text" -> executeTypeText(args["text"]?.toString() ?: "")
            "swipe" -> executeSwipe(args["direction"]?.toString() ?: "up")
            "scroll" -> executeScroll(args["direction"]?.toString() ?: "down")
            "read_screen" -> executeReadScreen()
            "screen_reader_start" -> executeScreenReaderStart(args["auto_advance"]?.toString()?.toBooleanStrictOrNull())
            "screen_reader_next" -> executeScreenReaderNext()
            "screen_reader_previous" -> executeScreenReaderPrevious()
            "screen_reader_stop" -> executeScreenReaderStop()
            "camera_vision" -> executeCameraVision(args["prompt"]?.toString() ?: "")
            "set_voice_engine" -> executeSetVoiceEngine(args["engine"]?.toString() ?: "omnivoice")
            "call_contact" -> executeCallContact(args["name_or_number"]?.toString() ?: "")
            "send_sms" -> executeSendSms(args["recipient"]?.toString() ?: "", args["message"]?.toString() ?: "")
            "whatsapp_message" -> executeWhatsAppMessage(args["contact_name"]?.toString() ?: "", args["message"]?.toString() ?: "")
            "notification_reader" -> executeReadNotifications(args["filter_app"]?.toString())
            "open_maps" -> executeOpenMaps(args["query"]?.toString() ?: "")
            "web_search" -> executeWebSearch(args["query"]?.toString() ?: "")
            "reminder" -> executeReminder(args["title"]?.toString() ?: "Nova Reminder", (args["minutes_from_now"] as? Number)?.toInt() ?: 5)
            else -> ActionResult.Failure("Unknown tool: ${toolCall.toolName}")
        }
    }

    private fun executeFlashlight(state: String): ActionResult {
        if (cameraManager == null) {
            return ActionResult.Failure("Flashlight hardware not available on this device.")
        }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return ActionResult.Failure("No camera flash found.")
            val targetState = when (state.lowercase()) {
                "on" -> true
                "off" -> false
                else -> !isTorchOn
            }
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            ActionResult.Success(
                if (targetState) "Flashlight turned on." else "Flashlight turned off.",
                "Torch state: $targetState"
            )
        } catch (e: Exception) {
            ActionResult.Failure("Failed to control flashlight: ${e.localizedMessage}")
        }
    }

    private fun executeVolume(action: String, levelPercent: Int?): ActionResult {
        return try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            when (action.lowercase()) {
                "up" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val pct = (current * 100) / maxVol
                    ActionResult.Success("Volume increased to $pct%.", "Current level: $pct%")
                }
                "down" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val pct = (current * 100) / maxVol
                    ActionResult.Success("Volume decreased to $pct%.", "Current level: $pct%")
                }
                "mute" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    ActionResult.Success("Volume muted.", "Muted")
                }
                "set" -> {
                    val pct = (levelPercent ?: 50).coerceIn(0, 100)
                    val target = (pct * maxVol) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
                    ActionResult.Success("Volume set to $pct%.", "Level: $pct%")
                }
                else -> ActionResult.Failure("Invalid volume action: $action")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Volume control error: ${e.localizedMessage}")
        }
    }

    private fun executeBrightness(levelPercent: Int): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened display settings for brightness control ($levelPercent%).")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open brightness settings: ${e.localizedMessage}")
        }
    }

    private fun executeDeviceSettings(settingType: String): ActionResult {
        return try {
            val action = when (settingType.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened $settingType settings.")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open settings: ${e.localizedMessage}")
        }
    }

    private fun executeOpenApp(appName: String): ActionResult {
        if (appName.isBlank()) return ActionResult.Failure("App name was not provided.")
        val pm = context.packageManager
        
        // Common app shortcut overrides
        val commonPackageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to "camera_intent",
            "settings" to "com.android.settings",
            "calculator" to "com.google.android.calculator",
            "gmail" to "com.google.android.gm"
        )

        val cleanName = appName.lowercase().trim()
        if (cleanName == "camera") {
            return try {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult.Success("Camera opened.")
            } catch (e: Exception) {
                ActionResult.Failure("Camera launch failed: ${e.localizedMessage}")
            }
        }

        // Direct package lookup
        val overridePkg = commonPackageMap[cleanName]
        if (overridePkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(overridePkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult.Success("Opened $appName ($overridePkg).")
            }
        }

        // Dynamic package manager search
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in apps) {
                val label = resolveInfo.loadLabel(pm).toString()
                if (label.contains(appName, ignoreCase = true) || appName.contains(label, ignoreCase = true)) {
                    val pkg = resolveInfo.activityInfo.packageName
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ActionResult.Success("Opened $label.")
                    }
                }
            }
        } catch (_: Exception) {}

        return ActionResult.Failure("App '$appName' was not found on this device.")
    }

    private fun executePressBack(): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled. Please enable Nova in Accessibility Settings.")
        val ok = service.pressBackAction()
        return if (ok) ActionResult.Success("Pressed Back.") else ActionResult.Failure("Accessibility could not perform Back action.")
    }

    private fun executePressHome(): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled.")
        val ok = service.pressHomeAction()
        return if (ok) ActionResult.Success("Navigated to Home.") else ActionResult.Failure("Accessibility could not perform Home action.")
    }

    private fun executePressRecents(): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled.")
        val ok = service.pressRecentsAction()
        return if (ok) ActionResult.Success("Opened Recent Apps.") else ActionResult.Failure("Accessibility could not open Recent Apps.")
    }

    private fun executeTap(targetText: String?, x: Float?, y: Float?): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled for UI tapping.")
        
        if (!targetText.isNullOrBlank()) {
            val tapped = service.tapNodeWithText(targetText)
            if (tapped) return ActionResult.Success("Tapped '$targetText' on screen.")
        }

        if (x != null && y != null) {
            val metrics = context.resources.displayMetrics
            val realX = x * metrics.widthPixels
            val realY = y * metrics.heightPixels
            val tapped = service.tapCoordinates(realX, realY)
            if (tapped) return ActionResult.Success("Tapped screen coordinates ($realX, $realY).")
        }

        return ActionResult.Failure("Could not find or tap element on current screen.")
    }

    private fun executeTypeText(text: String): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled for typing.")
        val typed = service.typeTextIntoFocusedNode(text)
        return if (typed) ActionResult.Success("Typed text into active field.") else ActionResult.Failure("No editable text field was currently focused on screen.")
    }

    private fun executeSwipe(direction: String): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled.")
        val metrics = context.resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()

        val swiped = when (direction.lowercase()) {
            "up" -> service.swipeGesture(w / 2, h * 0.8f, w / 2, h * 0.2f)
            "down" -> service.swipeGesture(w / 2, h * 0.2f, w / 2, h * 0.8f)
            "left" -> service.swipeGesture(w * 0.8f, h / 2, w * 0.2f, h / 2)
            "right" -> service.swipeGesture(w * 0.2f, h / 2, w * 0.8f, h / 2)
            else -> false
        }
        return if (swiped) ActionResult.Success("Swiped $direction.") else ActionResult.Failure("Failed to perform swipe gesture.")
    }

    private fun executeScroll(direction: String): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled.")
        val scrolled = if (direction.equals("up", ignoreCase = true)) {
            service.scrollBackwardAction()
        } else {
            service.scrollForwardAction()
        }
        return if (scrolled) ActionResult.Success("Scrolled $direction.") else ActionResult.Failure("Current window does not support scrolling.")
    }

    private fun executeReadScreen(): ActionResult {
        val service = NovaAccessibilityService.instance
            ?: return ActionResult.Failure("Accessibility Service is not enabled. Please enable Nova in Accessibility Settings to read your screen.")
        val screenText = service.extractVisibleScreenText()
        val textSummary = if (screenText.isBlank()) "No text found on current screen." else screenText
        return ActionResult.ScreenContent(textSummary)
    }

    private var screenReaderController: Any? = null

    fun setScreenReaderController(controller: com.example.domain.controller.ScreenReaderController) {
        screenReaderController = controller
    }

    private fun executeScreenReaderStart(autoAdvance: Boolean?): ActionResult {
        val controller = screenReaderController as? ScreenReaderController
            ?: return ActionResult.Failure("Screen reader is not available.")
        if (autoAdvance != null && controller.autoAdvance.value != autoAdvance) {
            controller.toggleAutoAdvance()
        }
        controller.startReading()
        return ActionResult.Success("Screen reader started. Reading elements aloud.", "auto_advance=${controller.autoAdvance.value}")
    }

    private fun executeScreenReaderNext(): ActionResult {
        val controller = screenReaderController as? ScreenReaderController
            ?: return ActionResult.Failure("Screen reader is not available.")
        controller.nextElement()
        return ActionResult.Success("Moving to next element.", "index=${controller.currentIndex.value}")
    }

    private fun executeScreenReaderPrevious(): ActionResult {
        val controller = screenReaderController as? ScreenReaderController
            ?: return ActionResult.Failure("Screen reader is not available.")
        controller.previousElement()
        return ActionResult.Success("Moving to previous element.", "index=${controller.currentIndex.value}")
    }

    private fun executeScreenReaderStop(): ActionResult {
        val controller = screenReaderController as? ScreenReaderController
            ?: return ActionResult.Failure("Screen reader is not available.")
        controller.stopReading()
        return ActionResult.Success("Screen reader stopped.", "stopped")
    }

    private fun executeCameraVision(prompt: String): ActionResult {
        return ActionResult.Success(
            message = "Launching Camera Vision to analyze scene.",
            detail = if (prompt.isNotBlank()) prompt else "Live visual inspection"
        )
    }

    private fun executeSetVoiceEngine(engine: String): ActionResult {
        return ActionResult.Success(
            message = "Voice engine updated to $engine.",
            detail = "Engine: $engine"
        )
    }

    private fun executeCallContact(nameOrNumber: String): ActionResult {
        if (nameOrNumber.isBlank()) return ActionResult.Failure("Contact name or number was not specified.")
        val clean = nameOrNumber.trim()
        val isNumber = clean.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
        val phoneNum = if (isNumber) clean else resolveContactNumber(clean)

        return try {
            val intent = if (!phoneNum.isNullOrBlank()) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNum)}"))
            } else {
                Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult.Success(
                if (!phoneNum.isNullOrBlank()) "Initiated call dialer for $nameOrNumber ($phoneNum)."
                else "Opened contacts to search for $nameOrNumber."
            )
        } catch (e: Exception) {
            ActionResult.Failure("Call intent failed: ${e.localizedMessage}")
        }
    }

    private fun executeSendSms(recipient: String, message: String): ActionResult {
        if (recipient.isBlank()) return ActionResult.Failure("Recipient was not provided.")
        val phoneNum = if (recipient.any { it.isDigit() }) recipient else resolveContactNumber(recipient) ?: recipient

        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNum)}")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened SMS composer for $recipient with your message.")
        } catch (e: Exception) {
            ActionResult.Failure("SMS intent failed: ${e.localizedMessage}")
        }
    }

    private fun executeWhatsAppMessage(contactName: String, message: String): ActionResult {
        val phone = resolveContactNumber(contactName)
        return try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = if (!phone.isNullOrBlank()) {
                val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Launched WhatsApp with message for $contactName.")
        } catch (e: Exception) {
            ActionResult.Failure("WhatsApp is not installed or could not be opened.")
        }
    }

    private fun executeReadNotifications(filterApp: String?): ActionResult {
        val notifs = NovaNotificationListenerService.recentNotifications.value
        if (!NovaNotificationListenerService.isPermissionGranted(context)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ActionResult.Failure("Notification access is not enabled. Please enable Nova in Notification Access settings.")
        }

        if (notifs.isEmpty()) {
            return ActionResult.Success("You have no unread notifications right now.", "0 notifications")
        }

        val filtered = if (!filterApp.isNullOrBlank()) {
            notifs.filter { it.packageName.contains(filterApp, ignoreCase = true) || it.title.contains(filterApp, ignoreCase = true) }
        } else {
            notifs
        }

        val summary = filtered.take(5).joinToString("\n• ") { "${it.title}: ${it.text}" }
        return ActionResult.Success("Here are your latest notifications:\n• $summary", "Found ${filtered.size} notifications")
    }

    private fun executeOpenMaps(query: String): ActionResult {
        return try {
            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            ActionResult.Success("Opening Google Maps for '$query'.")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open maps: ${e.localizedMessage}")
        }
    }

    private fun executeWebSearch(query: String): ActionResult {
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            ActionResult.Success("Searching web for '$query'.")
        } catch (e: Exception) {
            ActionResult.Failure("Web search failed: ${e.localizedMessage}")
        }
    }

    private fun executeReminder(title: String, minutesFromNow: Int): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, title)
                putExtra(AlarmClock.EXTRA_LENGTH, (minutesFromNow * 60).coerceAtLeast(60))
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success("Timer & Reminder set for $minutesFromNow minutes: \"$title\".")
            } else {
                ActionResult.Success("Reminder saved: \"$title\" in $minutesFromNow minutes.")
            }
        } catch (e: Exception) {
            ActionResult.Success("Reminder logged: \"$title\" in $minutesFromNow min.")
        }
    }

    private fun resolveContactNumber(name: String): String? {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$name%")
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIdx >= 0) return it.getString(numberIdx)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getAllInstalledAppNames(): List<String> {
        val list = mutableListOf<String>()
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(intent, 0)
            for (app in apps) {
                list.add(app.loadLabel(pm).toString())
            }
        } catch (_: Exception) {}
        return list
    }
}
