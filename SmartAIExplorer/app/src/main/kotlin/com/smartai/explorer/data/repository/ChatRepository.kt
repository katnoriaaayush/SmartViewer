package com.smartai.explorer.data.repository

import com.smartai.explorer.data.local.dao.ChatMessageDao
import com.smartai.explorer.data.local.entity.ChatMessageEntity
import com.smartai.explorer.data.local.entity.toDomain
import com.smartai.explorer.data.remote.SSEStreamParser
import com.smartai.explorer.data.remote.SmartAIApiService
import com.smartai.explorer.data.remote.dto.ChatRequest
import com.smartai.explorer.domain.model.ChatMessage
import com.smartai.explorer.domain.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: SmartAIApiService,
    private val dao: ChatMessageDao,
) {
    fun getMessages(sessionId: String): Flow<List<ChatMessage>> =
        dao.getMessagesForSession(sessionId).map { it.map { e -> e.toDomain() } }

    fun streamChat(documentId: String, message: String, sessionId: String): Flow<String> =
        flow {
            val response = withContext(Dispatchers.IO) {
                api.chat(documentId, ChatRequest(message, sessionId)).execute()
            }
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response body")
            SSEStreamParser.parse(body).collect { emit(it) }
        }.flowOn(Dispatchers.IO)

    suspend fun saveMessage(sessionId: String, role: MessageRole, content: String) {
        dao.insertMessage(ChatMessageEntity(sessionId = sessionId, role = role.name, content = content))
    }

    suspend fun clearHistory(sessionId: String) {
        dao.clearSession(sessionId)
        withContext(Dispatchers.IO) { runCatching { api.clearChatHistory(sessionId) } }
    }
}
