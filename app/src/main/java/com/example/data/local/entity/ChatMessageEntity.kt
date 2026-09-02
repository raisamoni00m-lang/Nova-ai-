package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER" or "NOVA"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String? = null,
    val toolStatus: String? = null, // "SUCCESS", "FAILED", "PENDING_CONFIRMATION", "CANCELLED"
    val toolResultDetail: String? = null,
    val isRiskyAction: Boolean = false
)
