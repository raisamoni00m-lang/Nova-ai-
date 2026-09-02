package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AutomationDao
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.PrivacyAuditDao
import com.example.data.local.entity.AutomationEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.PrivacyAuditEntity

@Database(
    entities = [
        MemoryEntity::class,
        ChatMessageEntity::class,
        AutomationEntity::class,
        PrivacyAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun automationDao(): AutomationDao
    abstract fun privacyAuditDao(): PrivacyAuditDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_assistant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
