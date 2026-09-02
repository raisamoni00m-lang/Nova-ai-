package com.example.data.repository

import com.example.data.local.dao.AutomationDao
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.PrivacyAuditDao
import com.example.data.local.entity.AutomationEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.PrivacyAuditEntity
import kotlinx.coroutines.flow.Flow

class NovaRepository(
    private val memoryDao: MemoryDao,
    private val chatMessageDao: ChatMessageDao,
    private val automationDao: AutomationDao,
    private val privacyAuditDao: PrivacyAuditDao
) {
    // Memory
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun saveMemory(key: String, value: String, category: String = "general"): Long {
        return memoryDao.insertMemory(
            MemoryEntity(
                key = key.trim(),
                value = value.trim(),
                category = category
            )
        )
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> {
        return memoryDao.searchMemories(query)
    }

    suspend fun deleteMemoryById(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAllMemories()
    }

    // Chat Messages
    val allMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun addMessage(
        sender: String,
        text: String,
        toolName: String? = null,
        toolStatus: String? = null,
        toolResultDetail: String? = null,
        isRiskyAction: Boolean = false
    ): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                sender = sender,
                text = text,
                toolName = toolName,
                toolStatus = toolStatus,
                toolResultDetail = toolResultDetail,
                isRiskyAction = isRiskyAction
            )
        )
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        chatMessageDao.updateMessage(message)
    }

    suspend fun clearAllMessages() {
        chatMessageDao.clearAllMessages()
    }

    // Automations
    val allAutomations: Flow<List<AutomationEntity>> = automationDao.getAllAutomations()

    suspend fun saveAutomation(title: String, triggerPhrase: String, actionsJson: String): Long {
        return automationDao.insertAutomation(
            AutomationEntity(
                title = title,
                triggerPhrase = triggerPhrase,
                actionsJson = actionsJson
            )
        )
    }

    suspend fun updateAutomation(automation: AutomationEntity) {
        automationDao.updateAutomation(automation)
    }

    suspend fun deleteAutomation(id: Long) {
        automationDao.deleteById(id)
    }

    suspend fun getEnabledAutomations(): List<AutomationEntity> {
        return automationDao.getEnabledAutomations()
    }

    // Privacy Audits
    val allAudits: Flow<List<PrivacyAuditEntity>> = privacyAuditDao.getAllAudits()

    suspend fun logAudit(actionName: String, description: String, status: String, riskLevel: String = "LOW") {
        privacyAuditDao.insertAudit(
            PrivacyAuditEntity(
                actionName = actionName,
                description = description,
                status = status,
                riskLevel = riskLevel
            )
        )
    }

    suspend fun clearAllAudits() {
        privacyAuditDao.clearAllAudits()
    }
}
