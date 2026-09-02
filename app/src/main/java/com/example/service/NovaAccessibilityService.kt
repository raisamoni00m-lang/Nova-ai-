package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NovaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track current active app or window changes if needed
        event?.packageName?.let {
            _currentPackage.value = it.toString()
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _isServiceActive.value = false
        return super.onUnbind(intent)
    }

    // --- Actions ---

    fun pressBackAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHomeAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun pressRecentsAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotificationsPanel(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun tapNodeWithText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        if (nodes.isNotEmpty()) {
            for (node in nodes) {
                if (node.isClickable) {
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    parent = parent.parent
                }
            }
        }
        return false
    }

    fun typeTextIntoFocusedNode(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    fun scrollForwardAction(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun scrollBackwardAction(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    fun tapCoordinates(x: Float, y: Float, durationMs: Long = 100): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipeGesture(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun extractVisibleScreenText(): String {
        val rootNode = rootInActiveWindow ?: return "No active window visible."
        val builder = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                builder.append(text).append(" | ")
            } else if (!desc.isNullOrBlank()) {
                builder.append("[$desc]").append(" | ")
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }
        traverse(rootNode)
        return builder.toString().trim().ifBlank { "Screen is empty or protected." }
    }

    companion object {
        @Volatile
        var instance: NovaAccessibilityService? = null

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _currentPackage = MutableStateFlow("")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()
    }
}
