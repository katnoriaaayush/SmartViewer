package com.smartai.explorer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("message")   val message:   String,
    @SerializedName("sessionId") val sessionId: String,
)

data class ChatHistoryResponse(
    @SerializedName("messages") val messages: List<ChatMessageDto>,
)

data class ChatMessageDto(
    @SerializedName("id")        val id:        Long,
    @SerializedName("role")      val role:      String,
    @SerializedName("content")   val content:   String,
    @SerializedName("timestamp") val timestamp: Long,
)
