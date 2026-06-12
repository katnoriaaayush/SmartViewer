package com.smartai.explorer.ui.screens.viewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        modifier        = modifier,
        shape           = RoundedCornerShape(50),
        color           = MaterialTheme.colorScheme.surface,
        border          = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolbarAction("Summarize", Icons.Outlined.Notes,             SummaryPurple, onSummarize)
            ToolbarAction("Ask AI",    Icons.Outlined.ChatBubbleOutline, ChatBlue)      { onAsk(selectedText) }
            ToolbarAction("Explain",   Icons.Outlined.Lightbulb,         InsightsGreen, onExplain)
        }
    }
}

@Composable
private fun ToolbarAction(
    label:   String,
    icon:    ImageVector,
    color:   Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.height(48.dp),
        shape    = RoundedCornerShape(50),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
    }
}
