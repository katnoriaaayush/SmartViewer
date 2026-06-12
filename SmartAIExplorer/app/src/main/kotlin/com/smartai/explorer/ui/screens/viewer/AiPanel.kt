package com.smartai.explorer.ui.screens.viewer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
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
                                messages       = state.chatMessages,
                                streamingText  = state.streamingText,
                                isChatLoading  = state.isChatLoading,
                                onSend         = viewModel::sendChatMessage,
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Text(
                text  = "Getting AI ready…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "Uploading the document for analysis",
                style = MaterialTheme.typography.bodyMedium,
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(40.dp),
            )
            Text(
                text  = "AI features unavailable",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry, modifier = Modifier.height(48.dp), shape = MaterialTheme.shapes.small) {
                Text("Try again")
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
        modifier        = modifier.padding(12.dp),
        shape           = MaterialTheme.shapes.large,
        color           = MaterialTheme.colorScheme.surface,
        border          = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint               = InsightsGreen,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = "Explanation",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss explanation",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

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
                    Text(
                        "Analysing selected text…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is UiState.Success -> {
                    // Quoted source text
                    if (state.data.selectedText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text     = "“${state.data.selectedText.take(120)}${if (state.data.selectedText.length > 120) "…" else ""}”",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                    // Explanation body (scrollable)
                    Text(
                        text     = state.data.explanation,
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                    // Mode picker — re-run with a different mode
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExplainMode.entries.forEach { mode ->
                            val active = mode == state.data.mode
                            FilterChip(
                                selected = active,
                                onClick  = { if (!active) onReExplain(mode) },
                                label    = {
                                    Text(
                                        mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InsightsGreenSoft,
                                    selectedLabelColor     = InsightsGreen,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled     = true,
                                    selected    = active,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                ),
                                shape = MaterialTheme.shapes.extraLarge,
                            )
                        }
                    }
                }

                is UiState.Error -> Text(
                    text  = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> {}
            }
        }
    }
}

// ── Feature tab row ───────────────────────────────────────────────────────────

private data class FeatureTab(
    val feature: AiFeature,
    val label:   String,
    val icon:    ImageVector,
    val accent:  Color,
    val soft:    Color,
)

@Composable
private fun FeatureTabRow(
    activeFeature: AiFeature,
    onSelect:      (AiFeature) -> Unit,
) {
    val tabs = listOf(
        FeatureTab(AiFeature.CHAT,       "Chat",     Icons.Outlined.ChatBubbleOutline,   ChatBlue,       ChatBlueSoft),
        FeatureTab(AiFeature.SUMMARY,    "Summary",  Icons.Outlined.Notes,               SummaryPurple,  SummaryPurpleSoft),
        FeatureTab(AiFeature.FLASHCARDS, "Cards",    Icons.Outlined.Style,               FlashcardAmber, FlashcardAmberSoft),
        FeatureTab(AiFeature.INSIGHTS,   "Insights", Icons.Outlined.Lightbulb,           InsightsGreen,  InsightsGreenSoft),
        FeatureTab(AiFeature.INDEX,      "Index",    Icons.AutoMirrored.Outlined.ListAlt, IndexSlate,    IndexSlateSoft),
    )

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { tab ->
            val active = tab.feature == activeFeature
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (active) tab.soft else Color.Transparent)
                    .clickable { onSelect(tab.feature) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector        = tab.icon,
                    contentDescription = tab.label,
                    tint               = if (active) tab.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(22.dp),
                )
                Text(
                    text  = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) tab.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
