package com.smartai.explorer.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartai.explorer.data.repository.AIFeatureRepository
import com.smartai.explorer.data.repository.ChatRepository
import com.smartai.explorer.data.repository.DocumentRepository
import com.smartai.explorer.domain.model.*
import com.smartai.explorer.util.AppLog
import com.smartai.explorer.util.toMultipartPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "ViewerVM"

data class ViewerUiState(
    val uploadState:     UiState<SmartDocument>    = UiState.Idle,
    val activeFeature:   AiFeature                 = AiFeature.CHAT,
    val selectedText:    String?                   = null,
    val chatMessages:    List<ChatMessage>         = emptyList(),
    val streamingText:   String                    = "",
    val isChatLoading:   Boolean                   = false,
    val summaryState:    UiState<Summary>          = UiState.Idle,
    val flashcardsState: UiState<List<Flashcard>>  = UiState.Idle,
    val insightsState:   UiState<DocumentInsights> = UiState.Idle,
    val indexState:      UiState<DocumentIndex>    = UiState.Idle,
    val explainState:    UiState<ExplainResult>    = UiState.Idle,
    val zoomScale:       Float                     = 1.5f,
    // Pair<pageNum, nonce> — the nonce ensures tapping the same page twice re-fires scroll
    val scrollTarget:    Pair<Int, Long>?          = null,
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val documentRepo:  DocumentRepository,
    private val chatRepo:      ChatRepository,
    private val aiFeatureRepo: AIFeatureRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var documentId: String? = null
    private var sessionId:  String? = null
    private var chatJob:    Job?    = null

    // Upload the file to the server and cache the result locally
    fun uploadDocument(fileUri: String) {
        if (_uiState.value.uploadState is UiState.Loading) return
        AppLog.i(TAG, "uploadDocument: $fileUri")
        _uiState.update { it.copy(uploadState = UiState.Loading) }

        viewModelScope.launch {
            runCatching {
                // fileUri is always a local file path — no ContentResolver needed
                val part = File(fileUri).toMultipartPart()
                documentRepo.uploadAndCache(part, fileUri)
            }.onSuccess { doc ->
                documentId = doc.documentId
                sessionId  = doc.sessionId
                AppLog.i(TAG, "Upload success: documentId=${doc.documentId}")
                _uiState.update { it.copy(uploadState = UiState.Success(doc)) }
                observeChatMessages(doc.sessionId)
            }.onFailure { e ->
                AppLog.e(TAG, "Upload failed", e)
                _uiState.update { it.copy(uploadState = UiState.Error(e.message ?: "Upload failed")) }
            }
        }
    }

    fun setActiveFeature(feature: AiFeature) {
        // Dismiss the explain overlay when the user navigates to a tab
        _uiState.update { it.copy(activeFeature = feature, explainState = UiState.Idle) }
    }

    fun onTextSelected(text: String) {
        _uiState.update { it.copy(selectedText = text.ifBlank { null }) }
    }

    fun clearSelectedText() {
        _uiState.update { it.copy(selectedText = null) }
    }

    fun clearExplainState() {
        _uiState.update { it.copy(explainState = UiState.Idle) }
    }

    fun zoomIn() {
        _uiState.update { it.copy(zoomScale = (it.zoomScale + 0.25f).coerceAtMost(4.0f)) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(zoomScale = (it.zoomScale - 0.25f).coerceAtLeast(0.5f)) }
    }

    fun scrollToPage(pageNum: Int) {
        _uiState.update { it.copy(scrollTarget = Pair(pageNum, System.currentTimeMillis())) }
    }

    // ─── Chat ───────────────────────────────────────────────────────────────

    fun sendChatMessage(message: String) {
        val docId = documentId ?: run { AppLog.w(TAG, "sendChatMessage ignored: no documentId yet"); return }
        val sid   = sessionId  ?: run { AppLog.w(TAG, "sendChatMessage ignored: no sessionId yet"); return }
        chatJob?.cancel()
        AppLog.i(TAG, "sendChatMessage (len=${message.length})")
        _uiState.update { it.copy(streamingText = "", isChatLoading = true, activeFeature = AiFeature.CHAT) }

        chatJob = viewModelScope.launch {
            // Save the user bubble immediately so it appears before the server responds.
            chatRepo.saveMessage(sid, MessageRole.user, message)

            var accumulated = ""
            chatRepo.streamChat(docId, message, sid)
                .onEach { token ->
                    accumulated += token
                    _uiState.update { it.copy(streamingText = accumulated, isChatLoading = false) }
                }
                .onCompletion { err ->
                    if (err == null) {
                        AppLog.i(TAG, "Chat stream complete (${accumulated.length} chars)")
                        chatRepo.saveMessage(sid, MessageRole.model, accumulated)
                    }
                    _uiState.update { it.copy(streamingText = "", isChatLoading = false) }
                }
                .catch { e ->
                    AppLog.e(TAG, "Chat stream error", e)
                    chatRepo.saveMessage(sid, MessageRole.model, "⚠️ ${e.message ?: "Connection error"}")
                }
                .collect()
        }
    }

    // ─── Summary ────────────────────────────────────────────────────────────

    fun loadSummary(mode: SummaryMode = SummaryMode.QUICK, customPrompt: String? = null) {
        val docId = documentId ?: return
        _uiState.update { it.copy(summaryState = UiState.Loading, activeFeature = AiFeature.SUMMARY) }
        viewModelScope.launch {
            runCatching {
                aiFeatureRepo.getSummary(docId, mode,
                    com.smartai.explorer.data.remote.dto.SummarizeRequest(mode.name.lowercase(), customPrompt))
            }.onSuccess { s -> _uiState.update { it.copy(summaryState = UiState.Success(s)) } }
             .onFailure { e ->
                 AppLog.e(TAG, "loadSummary failed", e)
                 _uiState.update { it.copy(summaryState = UiState.Error(e.message ?: "Failed")) }
             }
        }
    }

    // ─── Flashcards ──────────────────────────────────────────────────────────

    fun loadFlashcards() {
        val docId = documentId ?: return
        _uiState.update { it.copy(flashcardsState = UiState.Loading, activeFeature = AiFeature.FLASHCARDS) }
        viewModelScope.launch {
            runCatching { aiFeatureRepo.getFlashcards(docId) }
                .onSuccess { cards -> _uiState.update { it.copy(flashcardsState = UiState.Success(cards)) } }
                .onFailure { e ->
                    AppLog.e(TAG, "loadFlashcards failed", e)
                    _uiState.update { it.copy(flashcardsState = UiState.Error(e.message ?: "Failed")) }
                }
        }
    }

    // ─── Insights ────────────────────────────────────────────────────────────

    fun loadInsights() {
        val docId = documentId ?: return
        _uiState.update { it.copy(insightsState = UiState.Loading, activeFeature = AiFeature.INSIGHTS) }
        viewModelScope.launch {
            runCatching { aiFeatureRepo.getInsights(docId) }
                .onSuccess { ins -> _uiState.update { it.copy(insightsState = UiState.Success(ins)) } }
                .onFailure { e ->
                    AppLog.e(TAG, "loadInsights failed", e)
                    _uiState.update { it.copy(insightsState = UiState.Error(e.message ?: "Failed")) }
                }
        }
    }

    // ─── Index ───────────────────────────────────────────────────────────────

    fun loadIndex() {
        val docId = documentId ?: return
        _uiState.update { it.copy(indexState = UiState.Loading, activeFeature = AiFeature.INDEX) }
        viewModelScope.launch {
            runCatching { aiFeatureRepo.getIndex(docId) }
                .onSuccess { idx -> _uiState.update { it.copy(indexState = UiState.Success(idx)) } }
                .onFailure { e ->
                    AppLog.e(TAG, "loadIndex failed", e)
                    _uiState.update { it.copy(indexState = UiState.Error(e.message ?: "Failed")) }
                }
        }
    }

    // ─── Explain (triggered by text selection) ───────────────────────────────

    fun reExplainWithMode(mode: ExplainMode) {
        val docId = documentId ?: return
        val text  = (_uiState.value.explainState as? UiState.Success)?.data?.selectedText ?: return
        _uiState.update { it.copy(explainState = UiState.Loading) }
        viewModelScope.launch {
            runCatching { aiFeatureRepo.explain(docId, text, mode) }
                .onSuccess { r -> _uiState.update { it.copy(explainState = UiState.Success(r)) } }
                .onFailure { e ->
                    AppLog.e(TAG, "explain failed", e)
                    _uiState.update { it.copy(explainState = UiState.Error(e.message ?: "Failed")) }
                }
        }
    }

    fun explainSelectedText(mode: ExplainMode = ExplainMode.EXPLAIN) {
        val docId = documentId ?: return
        // Read and clear the selected text atomically — avoids the race where
        // the caller clears the selection before we can read it.
        var captured: String? = null
        _uiState.update { s ->
            captured = s.selectedText
            s.copy(selectedText = null, explainState = UiState.Loading)
        }
        val text = captured ?: return
        viewModelScope.launch {
            runCatching { aiFeatureRepo.explain(docId, text, mode) }
                .onSuccess { r -> _uiState.update { it.copy(explainState = UiState.Success(r)) } }
                .onFailure { e ->
                    AppLog.e(TAG, "explain failed", e)
                    _uiState.update { it.copy(explainState = UiState.Error(e.message ?: "Failed")) }
                }
        }
    }

    private fun observeChatMessages(sid: String) {
        viewModelScope.launch {
            chatRepo.getMessages(sid).collect { messages ->
                _uiState.update { it.copy(chatMessages = messages) }
            }
        }
    }
}
