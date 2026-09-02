package com.example.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NovaNotificationItem(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class NovaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isNotificationAccessEnabled.value = true
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        _isNotificationAccessEnabled.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { addNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            val key = it.key
            val current = _recentNotifications.value.toMutableList()
            current.removeAll { item -> item.id == key }
            _recentNotifications.value = current
        }
    }

    fun refreshNotifications() {
        try {
            val active = activeNotifications ?: return
            val list = mutableListOf<NovaNotificationItem>()
            for (sbn in active) {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                
                // Exclude sensitive / banking / OTP notifications from storage or speech
                if (isSensitiveNotification(title, text)) {
                    continue
                }

                if (title.isNotBlank() || text.isNotBlank()) {
                    list.add(
                        NovaNotificationItem(
                            id = sbn.key,
                            packageName = sbn.packageName,
                            title = title,
                            text = text,
                            postTime = sbn.postTime
                        )
                    )
                }
            }
            _recentNotifications.value = list.sortedByDescending { it.postTime }
        } catch (_: Exception) {}
    }

    private fun addNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (isSensitiveNotification(title, text)) return

        if (title.isNotBlank() || text.isNotBlank()) {
            val item = NovaNotificationItem(
                id = sbn.key,
                packageName = sbn.packageName,
                title = title,
                text = text,
                postTime = sbn.postTime
            )
            val current = _recentNotifications.value.toMutableList()
            current.removeAll { it.id == item.id }
            current.add(0, item)
            _recentNotifications.value = current.take(30)
        }
    }

    private fun isSensitiveNotification(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        return combined.contains("otp") ||
                combined.contains("verification code") ||
                combined.contains("bank") ||
                combined.contains("password") ||
                combined.contains("cvv") ||
                combined.contains("debit card") ||
                combined.contains("credit card")
    }

    companion object {
        @Volatile
        var instance: NovaNotificationListenerService? = null

        private val _isNotificationAccessEnabled = MutableStateFlow(false)
        val isNotificationAccessEnabled: StateFlow<Boolean> = _isNotificationAccessEnabled.asStateFlow()

        private val _recentNotifications = MutableStateFlow<List<NovaNotificationItem>>(emptyList())
        val recentNotifications: StateFlow<List<NovaNotificationItem>> = _recentNotifications.asStateFlow()

        fun isPermissionGranted(context: Context): Boolean {
            val component = ComponentName(context, NovaNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(component.flattenToString())
        }
    }
}
