package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerPhrase: String,
    val actionsJson: String, // list of serialized actions (e.g. [{"tool":"flashlight","args":{"state":"on"}}, ...])
    val isEnabled: Boolean = true,
    val runCount: Int = 0,
    val lastRunTime: Long = 0L
)
