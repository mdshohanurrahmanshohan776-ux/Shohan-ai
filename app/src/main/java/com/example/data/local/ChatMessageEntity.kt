package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // "GALLERY_IMAGE", "AUDIO_TRANSCRIBE", "SEND_MESSAGE", "MAKE_CALL", "POST_SOCIAL", "TOGGLE_TORCH", "NONE"
    val actionPayload: String? = null,
    val status: String = "SUCCESS"
)
