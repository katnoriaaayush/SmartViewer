package com.smartai.explorer.ui.screens.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.ui.theme.ChatBlue
import com.smartai.explorer.ui.theme.InsightsGreen
import com.smartai.explorer.ui.theme.SummaryPurple

@Composable
fun TextSelectionToolbar(
    selectedText: String,
    onSummarize:  () -> Unit,
    onAsk:        (String) -> Unit,
    onExplain:    () -> Unit,
    modifier:     Modifier = Modifier,
) {
    Surface(
        modifier  = modifier,
        shape     = RoundedCornerShape(50),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color     = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolbarAction("Summarize", SummaryPurple, onSummarize)
            ToolbarAction("Ask AI",    ChatBlue,      { onAsk(selectedText) })
            ToolbarAction("Explain",   InsightsGreen, onExplain)
        }
    }
}

@Composable
private fun ToolbarAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    FilledTonalButton(
        onClick  = onClick,
        modifier = Modifier.height(56.dp),
        colors   = ButtonDefaults.filledTonalButtonColors(containerColor = color.copy(alpha = 0.15f)),
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
    }
}
