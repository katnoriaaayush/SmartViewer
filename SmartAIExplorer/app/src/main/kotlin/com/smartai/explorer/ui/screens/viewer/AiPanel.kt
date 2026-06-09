package com.smartai.explorer.ui.screens.viewer

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.AiFeature
import com.smartai.explorer.ui.screens.viewer.tabs.*
import com.smartai.explorer.ui.theme.*

@Composable
fun AiPanel(
    state:     ViewerUiState,
    viewModel: ViewerViewModel,
    modifier:  Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 0.dp),
    ) {
        // Feature tab row
        FeatureTabRow(
            activeFeature = state.activeFeature,
            onSelect      = viewModel::setActiveFeature,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Animated content swap
        AnimatedContent(
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
                    state    = state.summaryState,
                    onLoad   = viewModel::loadSummary,
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
                    state  = state.indexState,
                    onLoad = viewModel::loadIndex,
                )
            }
        }
    }
}

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
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (feature, label) ->
            val accent  = accentFor[feature] ?: MaterialTheme.colorScheme.primary
            val active  = feature == activeFeature
            FilterChip(
                selected = active,
                onClick  = { onSelect(feature) },
                label    = { Text(label, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.height(48.dp).weight(1f),
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = accent.copy(alpha = 0.2f),
                    selectedLabelColor        = accent,
                    containerColor            = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor                = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape    = RoundedCornerShape(50),
            )
        }
    }
}
