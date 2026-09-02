package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "privacy_audits")
data class PrivacyAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionName: String,
    val description: String,
    val status: String, // "ALLOWED", "BLOCKED", "CONFIRMED", "COMPLETED", "FAILED"
    val riskLevel: String = "LOW", // "LOW", "MEDIUM", "HIGH"
    val timestamp: Long = System.currentTimeMillis()
)
