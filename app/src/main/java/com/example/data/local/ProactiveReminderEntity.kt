package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proactive_reminders")
data class ProactiveReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeLabel: String, // e.g. "10:30 AM", "সন্ধ্যা ৭:০০"
    val category: String, // "MEETING", "ROUTINE", "HEALTH", "TASK"
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
