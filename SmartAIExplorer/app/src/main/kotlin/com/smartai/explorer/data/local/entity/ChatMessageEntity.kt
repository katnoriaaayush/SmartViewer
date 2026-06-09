package com.smartai.explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartai.explorer.domain.model.ChatMessage
import com.smartai.explorer.domain.model.MessageRole

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role:      String,
    val content:   String,
    val timestamp: Long = System.currentTimeMillis(),
)

fun ChatMessageEntity.toDomain() =
    ChatMessage(id, sessionId, MessageRole.valueOf(role), content, timestamp)

fun ChatMessage.toEntity() =
    ChatMessageEntity(id, sessionId, role.name, content, timestamp)
