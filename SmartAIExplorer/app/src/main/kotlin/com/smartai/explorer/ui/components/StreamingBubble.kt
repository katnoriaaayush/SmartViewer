package com.smartai.explorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamingBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape    = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.widthIn(max = 360.dp),
    ) {
        // Append the blinking cursor to the raw text so the inline parser
        // sees it as a plain character at the end — markdown tags will still
        // render correctly while the stream is in flight.
        MarkdownText(
            text     = "$text▋",
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
