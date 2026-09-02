package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PrivacyAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyAuditDao {
    @Query("SELECT * FROM privacy_audits ORDER BY timestamp DESC")
    fun getAllAudits(): Flow<List<PrivacyAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: PrivacyAuditEntity): Long

    @Query("DELETE FROM privacy_audits")
    suspend fun clearAllAudits()
}
