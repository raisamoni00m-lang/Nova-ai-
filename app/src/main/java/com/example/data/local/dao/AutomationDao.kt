package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY id DESC")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    suspend fun getEnabledAutomations(): List<AutomationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity): Long

    @Update
    suspend fun updateAutomation(automation: AutomationEntity)

    @Delete
    suspend fun deleteAutomation(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
