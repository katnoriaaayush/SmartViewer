package com.smartai.explorer.ui.screens.viewer

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.AiFeature
import com.smartai.explorer.domain.model.ExplainMode
import com.smartai.explorer.domain.model.ExplainResult
import com.smartai.explorer.domain.model.UiState
import com.smartai.explorer.ui.screens.viewer.tabs.*
import com.smartai.explorer.ui.theme.*

@Composable
fun AiPanel(
    state:          ViewerUiState,
    viewModel:      ViewerViewModel,
    onRetryUpload:  () -> Unit,
    modifier:       Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // ── Permanent feature tabs ─────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            FeatureTabRow(
                activeFeature = state.activeFeature,
                onSelect      = viewModel::setActiveFeature,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Content area — shows upload state until document is ready
            Box(modifier = Modifier.fillMaxSize()) {
                when (val upload = state.uploadState) {
                    is UiState.Loading -> UploadingPanel()
                    is UiState.Error   -> UploadErrorPanel(
                        message   = upload.message,
                        onRetry   = onRetryUpload,
                    )
                    else -> AnimatedContent(
                        targetState = state.activeFeature,
                        modifier    = Modifier.fillMaxSize(),
                        label       = "ai_panel_content",
                    ) { feature ->
                        when (feature) {
                            AiFeature.CHAT       -> ChatTab(
                                messages      = state.chatMessages,
                                streamingText = state.streamingText,
                                onSend        = viewModel::sendChatMessage,
                            )
                            AiFeature.SUMMARY    -> SummaryTab(
                                state  = state.summaryState,
                                onLoad = viewModel::loadSummary,
                            )
                            AiFeature.FLASHCARDS -> FlashcardsTab(
                                state  = state.flashcardsState,
                                onLoad = viewModel::loadFlashcards,
                            )
                            AiFeature.INSIGHTS   -> InsightsTab(
                                state  = state.insightsState,
                                onLoad = viewModel::loadInsights,
                            )
                            AiFeature.INDEX      -> IndexTab(
                                state          = state.indexState,
                                onLoad         = viewModel::loadIndex,
                                onScrollToPage = viewModel::scrollToPage,
                            )
                        }
                    }
                }
            }
        }

        // ── Explain result overlay — slides up from the bottom ────────────────
        AnimatedVisibility(
            visible  = state.explainState !is UiState.Idle,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            ExplainSheet(
                state         = state.explainState,
                onDismiss     = viewModel::clearExplainState,
                onReExplain   = viewModel::reExplainWithMode,
            )
        }
    }
}

// ── Upload state panels ────────────────────────────────────────────────────────

@Composable
private fun UploadingPanel() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(color = ChatBlue)
            Text(
                text  = "Uploading document to AI server…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UploadErrorPanel(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text  = "Upload failed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry, modifier = Modifier.height(56.dp)) {
                Text("Retry Upload")
            }
        }
    }
}

// ── Explain result sheet ───────────────────────────────────────────────────────

@Composable
private fun ExplainSheet(
    state:        UiState<ExplainResult>,
    onDismiss:    () -> Unit,
    onReExplain:  (ExplainMode) -> Unit,
    modifier:     Modifier = Modifier,
) {
    Surface(
        modifier        = modifier.padding(8.dp),
        shape           = RoundedCornerShape(20.dp),
        color           = MaterialTheme.colorScheme.surface,
        tonalElevation  = 8.dp,
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = "Explanation",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = InsightsGreen,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss explanation")
                }
            }
            HorizontalDivider()

            when (state) {
                is UiState.Loading -> Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.padding(vertical = 8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = InsightsGreen,
                    )
                    Text("Analysing selected text…", style = MaterialTheme.typography.bodyMedium)
                }

                is UiState.Success -> {
                    // Quoted source text
                    if (state.data.selectedText.isNotBlank()) {
                        Surface(
                            color  = MaterialTheme.colorScheme.surfaceVariant,
                            shape  = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text     = "\"${state.data.selectedText.take(120)}${if (state.data.selectedText.length > 120) "…" else ""}\"",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                    // Explanation body (scrollable)
                    Text(
                        text     = state.data.explanation,
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                    // Mode picker — re-run with a different mode
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExplainMode.entries.forEach { mode ->
                            val active = mode == state.data.mode
                            FilterChip(
                                selected = active,
                                onClick  = { if (!active) onReExplain(mode) },
                                label    = {
                                    Text(
                                        mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InsightsGreen.copy(alpha = 0.2f),
                                    selectedLabelColor     = InsightsGreen,
                                ),
                                shape = RoundedCornerShape(50),
                            )
                        }
                    }
                }

                is UiState.Error -> Text(
                    text  = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> {}
            }
        }
    }
}

// ── Feature tab row ───────────────────────────────────────────────────────────

@Composable
private fun FeatureTabRow(
    activeFeature: AiFeature,
    onSelect:      (AiFeature) -> Unit,
) {
    val tabs = listOf(
        AiFeature.CHAT       to "Chat",
        AiFeature.SUMMARY    to "Summary",
        AiFeature.FLASHCARDS to "Flashcards",
        AiFeature.INSIGHTS   to "Insights",
        AiFeature.INDEX      to "Index",
    )
    val accentFor = mapOf(
        AiFeature.CHAT       to ChatBlue,
        AiFeature.SUMMARY    to SummaryPurple,
        AiFeature.FLASHCARDS to FlashcardAmber,
        AiFeature.INSIGHTS   to InsightsGreen,
        AiFeature.INDEX      to IndexSlate,
    )

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (feature, label) ->
            val accent = accentFor[feature] ?: MaterialTheme.colorScheme.primary
            FilterChip(
                selected = feature == activeFeature,
                onClick  = { onSelect(feature) },
                label    = { Text(label, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.height(48.dp).weight(1f),
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.2f),
                    selectedLabelColor     = accent,
                    containerColor         = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor             = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape    = RoundedCornerShape(50),
            )
        }
    }
}
