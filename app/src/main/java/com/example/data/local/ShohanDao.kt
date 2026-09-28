package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShohanDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()

    @Query("SELECT * FROM proactive_reminders ORDER BY isCompleted ASC, timestamp ASC")
    fun getAllReminders(): Flow<List<ProactiveReminderEntity>>

    @Query("SELECT * FROM proactive_reminders WHERE isCompleted = 0 ORDER BY timestamp ASC")
    suspend fun getPendingReminders(): List<ProactiveReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ProactiveReminderEntity): Long

    @Query("UPDATE proactive_reminders SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM proactive_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}
