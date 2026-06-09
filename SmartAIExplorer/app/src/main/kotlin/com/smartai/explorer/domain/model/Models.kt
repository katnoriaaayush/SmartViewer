package com.smartai.explorer.domain.model

data class SmartDocument(
    val documentId:   String,
    val fileName:     String,
    val mimeType:     String,
    val fileSize:     Long,
    val sessionId:    String,
    val localFileUri: String,
    val uploadedAt:   Long,
)

data class ChatMessage(
    val id:          Long = 0,
    val sessionId:   String,
    val role:        MessageRole,
    val content:     String,
    val timestamp:   Long,
    val isStreaming: Boolean = false,
)

data class Summary(
    val documentId: String,
    val mode:       SummaryMode,
    val content:    String,
    val cachedAt:   Long,
)

data class Flashcard(
    val id:         Long = 0,
    val front:      String,
    val back:       String,
    val difficulty: Difficulty,
    val page:       Int?,
)

data class DocumentInsights(
    val keyPoints:   List<String>,
    val entities:    List<NamedEntity>,
    val actionItems: List<String>,
    val topics:      List<String>,
)

data class NamedEntity(val name: String, val type: EntityType)

data class DocumentIndex(
    val documentId: String,
    val sections:   List<IndexSection>,
)

data class IndexSection(
    val title:   String,
    val page:    Int,
    val summary: String,
    val level:   Int,
)

data class ExplainResult(
    val selectedText: String,
    val mode:         ExplainMode,
    val explanation:  String,
)

sealed class UiState<out T> {
    data object Idle    : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class  Success<T>(val data: T) : UiState<T>()
    data class  Error(val message: String) : UiState<Nothing>()
}

enum class MessageRole { user, model }
enum class SummaryMode  { QUICK, DETAILED, CUSTOM }
enum class ExplainMode  { EXPLAIN, SIMPLIFY, DEFINE }
enum class Difficulty   { EASY, MEDIUM, HARD }
enum class EntityType   { PERSON, ORGANIZATION, DATE, LOCATION, OTHER }
enum class AiFeature    { CHAT, SUMMARY, FLASHCARDS, INSIGHTS, INDEX }
